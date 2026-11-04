package com.routeleague.core.domain.rule

import com.routeleague.core.domain.model.RankedEntry
import com.routeleague.core.domain.model.WeeklyEntry
import kotlin.math.abs

/**
 * 주간 순위: 참여자별 유효 기록 소비량의 중앙값, 낮을수록 상위.
 * 측정 오차 [tieEpsilon] 안의 차이는 공동 순위. 서버(backend/)도 같은 규칙 버전으로 재계산한다.
 */
class WeeklyRanking(
    private val minRecordsPerUser: Int = 3,
    private val tieEpsilon: Float = 0.1f,
) {
    fun rank(entries: List<WeeklyEntry>): List<RankedEntry> {
        val medians = entries
            .filter { it.consumptions.size >= minRecordsPerUser }
            .map { it.userId to median(it.consumptions) }
            .sortedBy { it.second }

        val result = mutableListOf<RankedEntry>()
        medians.forEachIndexed { index, (userId, value) ->
            val prev = result.lastOrNull()
            val rank =
                if (prev != null && abs(prev.medianConsumption - value) <= tieEpsilon) prev.rank else index + 1
            result += RankedEntry(userId, rank, value)
        }
        return result
    }

    companion object {
        fun median(values: List<Float>): Float {
            require(values.isNotEmpty())
            val s = values.sorted()
            val mid = s.size / 2
            return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2f
        }
    }
}
