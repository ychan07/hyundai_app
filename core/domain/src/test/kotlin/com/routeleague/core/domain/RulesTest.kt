package com.routeleague.core.domain

import com.routeleague.core.domain.model.DisqualifyReason
import com.routeleague.core.domain.model.DriveSample
import com.routeleague.core.domain.model.DriveSession
import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Powertrain
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.domain.model.Route
import com.routeleague.core.domain.model.WeeklyEntry
import com.routeleague.core.domain.rule.QualificationJudge
import com.routeleague.core.domain.rule.WeeklyRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private val start = GeoPoint(37.4979, 127.0276)
    private val end = GeoPoint(37.5045, 127.0490)
    private val route = Route("r1", "테스트 출근길", listOf(start, end))

    private fun session(speed: Float, limit: Int?, n: Int = 40): DriveSession {
        val samples = (0 until n).map { i ->
            val t = i / (n - 1).toDouble()
            DriveSample(
                timestampMs = i * 1_000L,
                speedKmh = speed,
                location = GeoPoint(
                    start.latitude + (end.latitude - start.latitude) * t,
                    start.longitude + (end.longitude - start.longitude) * t,
                ),
                sessionEfficiency = 7.5f,
                roadSpeedLimitKmh = limit,
            )
        }
        return DriveSession("s1", "r1", 0, n * 1_000L, Powertrain.ICE, samples)
    }

    @Test
    fun normalDrive_isQualified() {
        val q = QualificationJudge().judge(session(speed = 50f, limit = 60), route)
        assertTrue(q is Qualification.Qualified)
    }

    @Test
    fun speeding_isDisqualified() {
        val q = QualificationJudge().judge(session(speed = 70f, limit = 60), route)
        assertTrue(q is Qualification.Disqualified && DisqualifyReason.SPEEDING in q.reasons)
    }

    @Test
    fun missingSpeedLimit_isDisqualified() {
        val q = QualificationJudge().judge(session(speed = 50f, limit = null), route)
        assertTrue(q is Qualification.Disqualified && DisqualifyReason.NO_SPEED_LIMIT_INFO in q.reasons)
    }

    @Test
    fun lowerMedianRanksHigher_andTiesShareRank() {
        val ranked = WeeklyRanking(tieEpsilon = 0.1f).rank(
            listOf(
                WeeklyEntry("a", listOf(7.0f, 7.2f, 7.1f)),
                WeeklyEntry("b", listOf(7.05f, 7.15f, 7.1f)),
                WeeklyEntry("c", listOf(8.0f, 8.1f, 8.2f)),
            ),
        )
        assertEquals(listOf(1, 1, 3), ranked.map { it.rank })
    }
}
