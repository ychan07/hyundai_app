package com.routeleague.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeleague.core.data.RecordRepository
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.domain.rule.Mission
import com.routeleague.core.domain.rule.MissionEvaluator
import com.routeleague.core.domain.rule.WeeklyRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ResultUiState(
    val latest: RecordRepository.SessionSummary? = null,
    val weeklyQualified: Int = 0,
    val personalBest: Float? = null,
    val completedMissions: Set<Mission> = emptySet(),
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    repository: RecordRepository,
) : ViewModel() {

    private val missions = MissionEvaluator()

    val state: StateFlow<ResultUiState> = repository.observeSessions()
        .map { sessions ->
            val finished = sessions.filter { it.qualification != null }
            val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val qualified = finished.mapNotNull { s ->
                (s.qualification as? Qualification.Qualified)?.let { s.startedAtMs to it.consumption }
            }
            val thisWeek = qualified.filter { it.first >= weekAgo }.map { it.second }
            val before = qualified.filter { it.first < weekAgo }.map { it.second }
            ResultUiState(
                latest = finished.firstOrNull(),
                weeklyQualified = thisWeek.size,
                personalBest = qualified.minOfOrNull { it.second },
                completedMissions = missions.completedMissions(
                    qualifiedThisWeek = thisWeek,
                    baselineMedian = before.takeIf { it.isNotEmpty() }?.let { WeeklyRanking.median(it) },
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())
}
