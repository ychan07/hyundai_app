package com.routeleague.core.domain.rule

/** 첫 버전 미션 2종 · 기본 배지 3종 (기획서 5장 "첫 버전 범위"). */
enum class Mission(val title: String) {
    BASELINE_COMPLETE("기준 기록 완성"),
    WEEKLY_CONSISTENCY("주간 효율 유지"),
}

enum class Badge(val title: String) {
    FIRST_BASELINE("첫 기준 완성"),
    PERSONAL_BEST("개인 기록 갱신"),
    WEEKLY_REPRESENTATIVE("주간 대표"),
}

/** 미션 달성 판정. 중복 지급 방지 키 = userId + seasonId + week + missionId (서버 unique 제약과 동일). */
class MissionEvaluator(
    private val baselineRecords: Int = 3,
    private val consistencyMaxWorseRatio: Float = 1.05f,
) {
    fun completedMissions(qualifiedThisWeek: List<Float>, baselineMedian: Float?): Set<Mission> {
        val done = mutableSetOf<Mission>()
        if (qualifiedThisWeek.size >= baselineRecords) done += Mission.BASELINE_COMPLETE
        if (baselineMedian != null && qualifiedThisWeek.size >= baselineRecords &&
            WeeklyRanking.median(qualifiedThisWeek) <= baselineMedian * consistencyMaxWorseRatio
        ) {
            done += Mission.WEEKLY_CONSISTENCY
        }
        return done
    }

    fun rewardKey(userId: String, seasonId: String, week: Int, mission: Mission): String =
        listOf(userId, seasonId, week.toString(), mission.name).joinToString(":")
}
