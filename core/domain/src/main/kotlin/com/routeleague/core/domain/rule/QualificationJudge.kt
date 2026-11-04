package com.routeleague.core.domain.rule

import com.routeleague.core.domain.model.DisqualifyReason
import com.routeleague.core.domain.model.DriveSession
import com.routeleague.core.domain.model.EnergyUnit
import com.routeleague.core.domain.model.Powertrain
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.domain.model.Route

/**
 * 기록 자격 판정. 기획서 3장 "기록 자격과 운전 중 사용" 규칙을 코드로 옮긴 것.
 * 임의의 과속 허용 폭을 두지 않는다 — [speedToleranceKmh] 는 측정 오차 실험 후 확정한다.
 */
class QualificationJudge(
    private val routeMatcher: RouteMatcher = RouteMatcher(),
    private val minSamples: Int = 30,
    private val maxGapMs: Long = 10_000,
    private val speedToleranceKmh: Float = 0f,
) {
    fun judge(session: DriveSession, route: Route): Qualification {
        val reasons = mutableListOf<DisqualifyReason>()
        val samples = session.samples.sortedBy { it.timestampMs }

        if (samples.size < minSamples) reasons += DisqualifyReason.INSUFFICIENT_DATA

        if (samples.zipWithNext().any { (a, b) -> b.timestampMs - a.timestampMs > maxGapMs }) {
            reasons += DisqualifyReason.DATA_GAP
        }

        val track = samples.mapNotNull { it.location }
        if (!routeMatcher.matches(route, track)) reasons += DisqualifyReason.ROUTE_MISMATCH

        val withLimit = samples.filter { it.roadSpeedLimitKmh != null && it.speedKmh != null }
        if (withLimit.isEmpty()) {
            reasons += DisqualifyReason.NO_SPEED_LIMIT_INFO
        } else if (withLimit.any { it.speedKmh!! > it.roadSpeedLimitKmh!! + speedToleranceKmh }) {
            reasons += DisqualifyReason.SPEEDING
        }

        // 순시값 평균이 아니라 차량의 "현재 주행 세션 효율" 마지막 값을 세션 효율로 사용한다.
        val consumption = samples.lastOrNull { it.sessionEfficiency != null }?.sessionEfficiency
        if (consumption == null) reasons += DisqualifyReason.NO_EFFICIENCY_DATA

        if (reasons.isNotEmpty() || consumption == null) {
            return Qualification.Disqualified(reasons.distinct())
        }

        val unit = when (session.powertrain) {
            Powertrain.EV -> EnergyUnit.WH_PER_KM
            else -> EnergyUnit.LITER_PER_100KM
        }
        return Qualification.Qualified(consumption, unit)
    }
}
