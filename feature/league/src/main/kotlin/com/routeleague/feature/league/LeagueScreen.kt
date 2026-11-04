package com.routeleague.feature.league

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.routeleague.core.data.ActiveRouteStore
import com.routeleague.core.data.remote.RemoteSyncApi
import com.routeleague.core.domain.model.RankedEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LeagueUiState(val loading: Boolean = true, val entries: List<RankedEntry> = emptyList())

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val remote: RemoteSyncApi,
    activeRoute: ActiveRouteStore,
) : ViewModel() {
    // TODO: 시즌/주차는 서버 seasons 테이블에서 받아온다.
    val state: StateFlow<LeagueUiState> = activeRoute.routeId
        .map { routeId ->
            val entries = routeId?.let { remote.weeklyLeaderboard(it, seasonId = "S1", week = 1).getOrNull() }
            LeagueUiState(loading = false, entries = entries.orEmpty())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeagueUiState())
}

/** 주간 순위. 주행 중에는 표시하지 않는다 (app 에서 UX 제한 시 잠금). */
@Composable
fun LeagueScreen(viewModel: LeagueViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("주간 리그", style = MaterialTheme.typography.headlineMedium)
        when {
            state.loading -> Text("불러오는 중…", style = MaterialTheme.typography.bodyLarge)
            state.entries.isEmpty() -> Text(
                "비교할 기록이 아직 충분하지 않아요. 지금은 개인 기록만 제공돼요.",
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> state.entries.forEach {
                Text("${it.rank}위  ${it.userId}  %.1f".format(it.medianConsumption), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
