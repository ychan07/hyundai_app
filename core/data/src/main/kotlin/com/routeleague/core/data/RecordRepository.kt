package com.routeleague.core.data

import com.routeleague.core.data.local.RecordDao
import com.routeleague.core.data.local.RouteEntity
import com.routeleague.core.data.local.SampleEntity
import com.routeleague.core.data.local.SessionEntity
import com.routeleague.core.domain.model.DisqualifyReason
import com.routeleague.core.domain.model.DriveSample
import com.routeleague.core.domain.model.DriveSession
import com.routeleague.core.domain.model.EnergyUnit
import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Powertrain
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.domain.model.Route
import com.routeleague.core.domain.rule.QualificationJudge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

/** 주행 기록의 로컬 저장소. 네트워크가 끊겨도 여기에 먼저 저장하고, 동기화는 [RemoteSyncApi] 가 맡는다. */
class RecordRepository(
    private val dao: RecordDao,
    private val judge: QualificationJudge = QualificationJudge(),
) {
    data class SessionSummary(
        val id: String,
        val routeId: String?,
        val startedAtMs: Long,
        val endedAtMs: Long?,
        val qualification: Qualification?,
    )

    fun observeRoutes(): Flow<List<Route>> = dao.observeRoutes().map { list -> list.map { it.toDomain() } }

    suspend fun saveRoute(route: Route) = dao.upsertRoute(
        RouteEntity(route.id, route.name, encodePath(route.path), route.corridorMeters),
    )

    fun observeSessions(): Flow<List<SessionSummary>> =
        dao.observeSessions().map { list -> list.map { it.toSummary() } }

    suspend fun startSession(routeId: String?, powertrain: Powertrain, nowMs: Long): String {
        val id = UUID.randomUUID().toString()
        dao.insertSession(
            SessionEntity(
                id = id, routeId = routeId, startedAtMs = nowMs, endedAtMs = null,
                powertrain = powertrain.name, status = null, consumption = null, unit = null, reasons = null,
            ),
        )
        return id
    }

    suspend fun addSample(sessionId: String, s: DriveSample) = dao.insertSample(
        SampleEntity(
            sessionId = sessionId,
            timestampMs = s.timestampMs,
            speedKmh = s.speedKmh,
            latitude = s.location?.latitude,
            longitude = s.location?.longitude,
            sessionEfficiency = s.sessionEfficiency,
            roadSpeedLimitKmh = s.roadSpeedLimitKmh,
        ),
    )

    /** 주차 시 호출: 기록 자격을 판정해 저장하고 결과를 돌려준다. */
    suspend fun finishSession(sessionId: String, nowMs: Long): Qualification? {
        val entity = dao.session(sessionId) ?: return null
        val route = entity.routeId?.let { dao.route(it) }?.toDomain()
        val samples = dao.samples(sessionId).map { it.toDomain() }
        val session = DriveSession(
            id = entity.id, routeId = entity.routeId, startedAtMs = entity.startedAtMs, endedAtMs = nowMs,
            powertrain = Powertrain.valueOf(entity.powertrain), samples = samples,
        )
        val q = if (route == null) {
            Qualification.Disqualified(listOf(DisqualifyReason.ROUTE_MISMATCH))
        } else {
            judge.judge(session, route)
        }
        dao.upsertSession(
            when (q) {
                is Qualification.Qualified -> entity.copy(
                    endedAtMs = nowMs, status = "QUALIFIED", consumption = q.consumption, unit = q.unit.name,
                )
                is Qualification.Disqualified -> entity.copy(
                    endedAtMs = nowMs, status = "DISQUALIFIED", reasons = q.reasons.joinToString(",") { it.name },
                )
            },
        )
        return q
    }

    suspend fun unsyncedSessions(): List<SessionEntity> = dao.unsyncedSessions()

    suspend fun markSynced(id: String) = dao.markSynced(id)

    /** 원시 샘플 30일 보관 정책. */
    suspend fun purgeOldSamples(nowMs: Long, retentionDays: Long = 30) =
        dao.deleteSamplesBefore(nowMs - TimeUnit.DAYS.toMillis(retentionDays))

    // region mapping
    private fun RouteEntity.toDomain() = Route(id, name, decodePath(path), corridorMeters)

    private fun SampleEntity.toDomain() = DriveSample(
        timestampMs = timestampMs,
        speedKmh = speedKmh,
        location = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
        sessionEfficiency = sessionEfficiency,
        roadSpeedLimitKmh = roadSpeedLimitKmh,
    )

    private fun SessionEntity.toSummary(): SessionSummary {
        val q = when (status) {
            "QUALIFIED" -> Qualification.Qualified(
                consumption ?: 0f,
                EnergyUnit.valueOf(unit ?: "LITER_PER_100KM"),
            )
            "DISQUALIFIED" -> Qualification.Disqualified(
                reasons.orEmpty().split(",").filter { it.isNotBlank() }
                    .map { DisqualifyReason.valueOf(it) },
            )
            else -> null
        }
        return SessionSummary(id, routeId, startedAtMs, endedAtMs, q)
    }

    private fun encodePath(path: List<GeoPoint>) = path.joinToString(";") { "${it.latitude},${it.longitude}" }

    private fun decodePath(raw: String) = raw.split(";").filter { it.isNotBlank() }.map {
        val (lat, lng) = it.split(",")
        GeoPoint(lat.toDouble(), lng.toDouble())
    }
    // endregion
}
