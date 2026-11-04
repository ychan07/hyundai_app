package com.routeleague.feature.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeleague.core.domain.model.EnergyUnit
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.domain.rule.Mission
import com.routeleague.core.ui.component.StatCard

/** 주차 후 결과: 기록 판정 → 주간 현황 → 미션 달성 순서로 보여준다. */
@Composable
fun ResultScreen(viewModel: ResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("오늘의 기록", style = MaterialTheme.typography.headlineMedium)

        when (val q = state.latest?.qualification) {
            null -> Text("아직 완료된 주행이 없어요.", style = MaterialTheme.typography.bodyLarge)
            is Qualification.Qualified -> StatCard("유효 기록", formatConsumption(q.consumption, q.unit))
            is Qualification.Disqualified -> StatCard(
                "순위 제외",
                q.reasons.joinToString(" · ") { it.label },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("이번 주 유효 기록", "${state.weeklyQualified}회", Modifier.weight(1f))
            StatCard(
                "개인 최고",
                state.personalBest?.let { "%.1f".format(it) } ?: "-",
                Modifier.weight(1f),
            )
        }

        Text("미션", style = MaterialTheme.typography.titleLarge)
        Mission.entries.forEach { m ->
            val done = m in state.completedMissions
            Text((if (done) "✓ " else "○ ") + m.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun formatConsumption(value: Float, unit: EnergyUnit): String = when (unit) {
    EnergyUnit.LITER_PER_100KM -> "%.1f L/100km".format(value)
    EnergyUnit.WH_PER_KM -> "%.0f Wh/km".format(value)
}
