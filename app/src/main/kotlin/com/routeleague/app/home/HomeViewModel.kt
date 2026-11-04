package com.routeleague.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeleague.core.data.ActiveRouteStore
import com.routeleague.core.data.RecordRepository
import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Route
import com.routeleague.core.vehicle.VehicleCapabilities
import com.routeleague.core.vehicle.VehicleGateway
import com.routeleague.core.vehicle.fake.FakeVehicleGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gateway: VehicleGateway,
    private val repository: RecordRepository,
    private val activeRoute: ActiveRouteStore,
) : ViewModel() {

    val capabilities: StateFlow<VehicleCapabilities> = gateway.capabilities
    val selectedRouteId: StateFlow<String?> = activeRoute.routeId
    val routes: StateFlow<List<Route>> =
        repository.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isFake: Boolean get() = gateway is FakeVehicleGateway

    fun selectRoute(id: String) = activeRoute.select(id)

    /**
     * 시연용 경로 등록. 실제 경로 등록(최근 주행 궤적 → 경로 저장)은 후속 작업.
     * 실차 모드에서는 궤적이 없으므로 같은 좌표를 placeholder 로 사용한다.
     */
    fun registerDemoRoute() {
        val path = (gateway as? FakeVehicleGateway)?.demoPath ?: listOf(
            GeoPoint(37.4979, 127.0276),
            GeoPoint(37.5045, 127.0490),
        )
        viewModelScope.launch {
            val route = Route(id = DEMO_ROUTE_ID, name = "출근길 (시연)", path = path)
            repository.saveRoute(route)
            activeRoute.select(route.id)
        }
    }

    fun playScenario(scenario: FakeVehicleGateway.Scenario) {
        val fake = gateway as? FakeVehicleGateway ?: return
        fake.scenario = scenario
        fake.playDrive()
    }

    private companion object {
        const val DEMO_ROUTE_ID = "demo-commute"
    }
}
