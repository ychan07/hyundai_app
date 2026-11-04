package com.routeleague.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeleague.core.ui.component.CarButton
import com.routeleague.core.vehicle.VehicleCapabilities.Support
import com.routeleague.core.vehicle.fake.FakeVehicleGateway.Scenario

/** 주차 상태 홈: 데이터 지원 여부 확인 → 도전 경로 선택 → (시연 모드) 주행 재생. */
@Composable
fun HomeScreen(
    onOpenResult: () -> Unit,
    onOpenLeague: () -> Unit,
    onOpenPassport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val caps by viewModel.capabilities.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val selected by viewModel.selectedRouteId.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Route League", style = MaterialTheme.typography.headlineMedium)
        if (viewModel.isFake) {
            Text("시뮬레이션 데이터 모드", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }

        Text("차량 데이터 지원", style = MaterialTheme.typography.titleLarge)
        Text(
            listOf(
                "속도" to caps.speed,
                "기어(주차)" to caps.gear,
                "연비" to caps.fuelEfficiency,
                "전비" to caps.evEfficiency,
                "내비" to caps.navigation,
            ).joinToString("   ") { (name, s) -> "$name ${s.mark()}" },
            style = MaterialTheme.typography.bodyLarge,
        )
        caps.lastError?.let { Text("최근 오류: $it", style = MaterialTheme.typography.bodyMedium) }
        if (!caps.canRecordEfficiency && caps.fuelEfficiency != Support.UNKNOWN) {
            Text("이 차량은 효율 기록을 지원하지 않아 개인 이동 기록만 제공돼요.", style = MaterialTheme.typography.bodyMedium)
        }

        Text("도전 경로", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            routes.forEach { r ->
                FilterChip(
                    selected = r.id == selected,
                    onClick = { viewModel.selectRoute(r.id) },
                    label = { Text(r.name, style = MaterialTheme.typography.bodyLarge) },
                )
            }
        }
        if (routes.isEmpty()) CarButton("시연용 출근길 등록", viewModel::registerDemoRoute)

        if (viewModel.isFake) {
            Text("주행 재생", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarButton("정상", { viewModel.playScenario(Scenario.NORMAL) }, Modifier.weight(1f), selected != null)
                CarButton("과속", { viewModel.playScenario(Scenario.SPEEDING) }, Modifier.weight(1f), selected != null)
                CarButton("데이터 누락", { viewModel.playScenario(Scenario.DATA_GAP) }, Modifier.weight(1f), selected != null)
            }
        }

        CarButton("최근 결과", onOpenResult)
        CarButton("주간 리그", onOpenLeague)
        CarButton("경로 여권", onOpenPassport)
    }
}

private fun Support.mark() = when (this) {
    Support.SUPPORTED -> "✓"
    Support.UNSUPPORTED -> "✕"
    Support.UNKNOWN -> "?"
}
