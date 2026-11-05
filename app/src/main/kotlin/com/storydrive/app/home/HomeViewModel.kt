package com.storydrive.app.home

import androidx.lifecycle.ViewModel
import com.storydrive.core.domain.model.DriveState
import com.storydrive.core.domain.model.GeoPoint
import com.storydrive.core.vehicle.VehicleCapabilities
import com.storydrive.core.vehicle.VehicleGateway
import com.storydrive.core.vehicle.fake.FakeVehicleGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gateway: VehicleGateway,
) : ViewModel() {

    val capabilities: StateFlow<VehicleCapabilities> = gateway.capabilities
    val driveState: StateFlow<DriveState> = gateway.driveState
    val location: StateFlow<GeoPoint?> = gateway.location

    val isFake: Boolean get() = gateway is FakeVehicleGateway

    /** 시연 모드: 주차 → 주행(UX 제한) → 주차 한 번을 재생한다. */
    fun playDemoDrive() {
        (gateway as? FakeVehicleGateway)?.playDrive()
    }
}
