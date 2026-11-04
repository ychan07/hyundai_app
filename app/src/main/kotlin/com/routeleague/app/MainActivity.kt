package com.routeleague.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.routeleague.app.home.HomeScreen
import com.routeleague.core.ui.component.DrivingLockScreen
import com.routeleague.core.ui.theme.RouteLeagueTheme
import com.routeleague.core.vehicle.VehicleGateway
import com.routeleague.feature.league.LeagueScreen
import com.routeleague.feature.passport.PassportScreen
import com.routeleague.feature.record.DriveRecorder
import com.routeleague.feature.record.RecordingService
import com.routeleague.feature.result.ResultScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var gateway: VehicleGateway
    @Inject lateinit var recorder: DriveRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecordingService.start(this)
        setContent {
            RouteLeagueTheme {
                AppRoot(gateway, recorder)
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val RESULT = "result"
    const val LEAGUE = "league"
    const val PASSPORT = "passport"
}

@Composable
private fun AppRoot(gateway: VehicleGateway, recorder: DriveRecorder) {
    val restricted by gateway.uxRestricted.collectAsStateWithLifecycle()
    val lastResult by recorder.lastResult.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    // 주차 후(UX 제한 해제 후) 결과 자동 표시. NavHost 가 구성된 뒤에만 이동한다.
    LaunchedEffect(lastResult?.sessionId, restricted) {
        if (lastResult != null && !restricted) nav.navigate(Routes.RESULT) { launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (restricted) {
            // 주행 중: 순위·상품·커뮤니티를 숨기고 기록 상태만 표시
            DrivingLockScreen()
        } else {
            NavHost(nav, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenResult = { nav.navigate(Routes.RESULT) },
                        onOpenLeague = { nav.navigate(Routes.LEAGUE) },
                        onOpenPassport = { nav.navigate(Routes.PASSPORT) },
                    )
                }
                composable(Routes.RESULT) { ResultScreen() }
                composable(Routes.LEAGUE) { LeagueScreen() }
                composable(Routes.PASSPORT) { PassportScreen() }
            }
        }
    }
}
