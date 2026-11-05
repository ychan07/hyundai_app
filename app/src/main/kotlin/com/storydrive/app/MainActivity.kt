package com.storydrive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storydrive.app.home.HomeScreen
import com.storydrive.core.ui.component.DrivingLockScreen
import com.storydrive.core.ui.theme.StoryDriveTheme
import com.storydrive.core.vehicle.VehicleGateway
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var gateway: VehicleGateway

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gateway.start()
        setContent {
            StoryDriveTheme {
                AppRoot(gateway)
            }
        }
    }

    override fun onDestroy() {
        // TODO: 이야기 재생을 Foreground Service 로 옮기면 차량 연결 수명도 서비스로 이동
        if (isFinishing) gateway.stop()
        super.onDestroy()
    }
}

@Composable
private fun AppRoot(gateway: VehicleGateway) {
    val restricted by gateway.uxRestricted.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (restricted) {
            // 주행 중: 화면 조작 없이 음성으로만 진행한다
            DrivingLockScreen()
        } else {
            HomeScreen()
        }
    }
}
