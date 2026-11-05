package com.storydrive.core.vehicle.pleos

import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.storydrive.core.domain.model.DriveState
import com.storydrive.core.domain.model.GeoPoint
import com.storydrive.core.vehicle.VehicleCapabilities
import com.storydrive.core.vehicle.VehicleCapabilities.Support
import com.storydrive.core.vehicle.VehicleGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 실제 차량 구현. API 시그니처는 Vehicle 2.3.2 / NaviHelper 3.0.5 AAR 기준.
 *
 * - 속도: AAOS CarPropertyManager PERF_VEHICLE_SPEED (Vehicle SDK 속도 리스너는 삭제됨)
 * - 주차 판정: Vehicle SDK Gear (P단). 신호대기 속도 0 은 주차로 보지 않는다.
 * - 위치: NaviHelper 이벤트 (FusedLocation SDK 는 콘솔 사용 가능 여부 확인 후 교체)
 * - UX 제한: CarUxRestrictionsManager
 */
class PleosVehicleGateway(context: Context) : VehicleGateway {

    private val appContext = context.applicationContext

    private val _capabilities = MutableStateFlow(VehicleCapabilities())
    private val _driveState = MutableStateFlow(DriveState.UNKNOWN)
    private val _speed = MutableStateFlow<Float?>(null)
    private val _location = MutableStateFlow<GeoPoint?>(null)
    private val _uxRestricted = MutableStateFlow(true) // 확인 전에는 잠금 (보수적 기본값)

    override val capabilities: StateFlow<VehicleCapabilities> = _capabilities.asStateFlow()
    override val driveState: StateFlow<DriveState> = _driveState.asStateFlow()
    override val speedKmh: StateFlow<Float?> = _speed.asStateFlow()
    override val location: StateFlow<GeoPoint?> = _location.asStateFlow()
    override val uxRestricted: StateFlow<Boolean> = _uxRestricted.asStateFlow()

    private var vehicle: Vehicle? = null
    private var naviHelper: NaviHelper? = null
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var uxManager: CarUxRestrictionsManager? = null

    // region listeners
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            _driveState.value = when (gear) {
                VehicleGear.GEAR_PARK -> DriveState.PARKED
                null, VehicleGear.GEAR_UNKNOWN -> DriveState.UNKNOWN
                else -> DriveState.DRIVING
            }
        }

        override fun onFailed(e: Exception) = reportError("gear", e)
    }

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val metersPerSecond = value.value as? Float ?: return
            _speed.value = metersPerSecond * 3.6f
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "speed property error: $propertyId/$areaId")
        }
    }

    private val naviListener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            _location.value = GeoPoint(currentLocationInfo.latitude, currentLocationInfo.longitude)
        }
    }
    // endregion

    override fun start() {
        if (vehicle != null) return
        startAaos()
        startVehicleSdk()
        startNavi()
    }

    override fun stop() {
        runCatching {
            vehicle?.getGear()?.unregisterGearSelection(gearListener)
            vehicle?.release()
        }
        runCatching {
            naviHelper?.removeListener(naviListener)
            naviHelper?.release()
        }
        runCatching {
            propertyManager?.unregisterCallback(speedCallback)
            uxManager?.unregisterListener()
            car?.disconnect()
        }
        vehicle = null
        naviHelper = null
        car = null
        propertyManager = null
        uxManager = null
    }

    private fun startAaos() {
        runCatching {
            val c = Car.createCar(appContext)
            car = c
            val pm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            propertyManager = pm

            val speedOk = pm.registerCallback(
                speedCallback,
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                CarPropertyManager.SENSOR_RATE_NORMAL,
            )
            setCapability { it.copy(speed = speedOk.toSupport()) }

            val ux = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            uxManager = ux
            _uxRestricted.value = ux.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: true
            ux.registerListener { restrictions ->
                _uxRestricted.value = restrictions.isRequiresDistractionOptimization
            }
        }.onFailure { reportError("aaos", it) }
    }

    private fun startVehicleSdk() {
        runCatching {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v

            v.getGear().registerGearSelection(gearListener)
            setCapability { it.copy(gear = Support.SUPPORTED) }
        }.onFailure { reportError("vehicle", it) }
    }

    private fun startNavi() {
        runCatching {
            val helper = NaviHelper(appContext)
            helper.initialize()
            helper.addListener(naviListener)
            naviHelper = helper
            setCapability { it.copy(navigation = Support.SUPPORTED) }
        }.onFailure {
            setCapability { c -> c.copy(navigation = Support.UNSUPPORTED) }
            reportError("navi", it)
        }
    }

    private fun setCapability(block: (VehicleCapabilities) -> VehicleCapabilities) = _capabilities.update(block)

    private fun reportError(source: String, t: Throwable) {
        Log.w(TAG, "[$source] ${t.message}", t)
        setCapability { it.copy(lastError = "$source: ${t.message}") }
    }

    private fun Boolean.toSupport() = if (this) Support.SUPPORTED else Support.UNSUPPORTED

    private companion object {
        const val TAG = "PleosVehicleGateway"
    }
}
