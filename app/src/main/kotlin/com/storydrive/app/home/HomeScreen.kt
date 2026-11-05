package com.storydrive.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storydrive.core.ui.component.CarButton
import com.storydrive.core.vehicle.VehicleCapabilities.Support

/**
 * 주차 상태 홈. 지금은 차량 데이터 지원 여부만 보여준다.
 * TODO(기획서 3장): 아이 프로필 → 목적지 확인 → 동화 설계도 생성 → 출발 흐름으로 교체.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val caps by viewModel.capabilities.collectAsStateWithLifecycle()
    val driveState by viewModel.driveState.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("이야기 드라이브", style = MaterialTheme.typography.headlineMedium)
        if (viewModel.isFake) {
            Text("시뮬레이션 데이터 모드", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }

        Text("차량 데이터 지원", style = MaterialTheme.typography.titleLarge)
        Text(
            listOf(
                "속도" to caps.speed,
                "기어(주차)" to caps.gear,
                "내비" to caps.navigation,
            ).joinToString("   ") { (name, s) -> "$name ${s.mark()}" },
            style = MaterialTheme.typography.bodyLarge,
        )
        caps.lastError?.let { Text("최근 오류: $it", style = MaterialTheme.typography.bodyMedium) }

        Text("현재 상태: $driveState", style = MaterialTheme.typography.bodyLarge)
        location?.let { Text("위치: %.5f, %.5f".format(it.latitude, it.longitude), style = MaterialTheme.typography.bodyMedium) }

        if (viewModel.isFake) {
            CarButton("주행 재생", viewModel::playDemoDrive)
        }
    }
}

private fun Support.mark() = when (this) {
    Support.SUPPORTED -> "✓"
    Support.UNSUPPORTED -> "✕"
    Support.UNKNOWN -> "?"
}
