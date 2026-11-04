package com.routeleague.feature.passport

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
import com.routeleague.core.data.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PassportStamp(val routeName: String, val visits: Int)

@HiltViewModel
class PassportViewModel @Inject constructor(repository: RecordRepository) : ViewModel() {
    val stamps: StateFlow<List<PassportStamp>> =
        combine(repository.observeRoutes(), repository.observeSessions()) { routes, sessions ->
            routes.map { r -> PassportStamp(r.name, sessions.count { it.routeId == r.id && it.endedAtMs != null }) }
                .filter { it.visits > 0 }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** 경로 여권: 다녀온 경로가 수집 카드로 남는다. TODO: 공유 카드 이미지 생성. */
@Composable
fun PassportScreen(viewModel: PassportViewModel = hiltViewModel()) {
    val stamps by viewModel.stamps.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("경로 여권", style = MaterialTheme.typography.headlineMedium)
        if (stamps.isEmpty()) Text("아직 수집한 경로가 없어요.", style = MaterialTheme.typography.bodyLarge)
        stamps.forEach { Text("${it.routeName} · ${it.visits}회", style = MaterialTheme.typography.bodyLarge) }
    }
}
