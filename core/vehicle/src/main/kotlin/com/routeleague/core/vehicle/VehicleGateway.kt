package com.routeleague.core.vehicle

import com.routeleague.core.domain.model.DriveState
import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Powertrain
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱이 차량을 바라보는 유일한 창구.
 * - [com.routeleague.core.vehicle.pleos.PleosVehicleGateway]: 실제 Pleos SDK + AAOS
 * - [com.routeleague.core.vehicle.fake.FakeVehicleGateway]: 시나리오 재생 (에뮬레이터 시연·테스트)
 *
 * 모든 값은 폴링이 아니라 SDK 리스너 구독으로 갱신된다 (주행 중 CPU 5% 미만 기준).
 * null = 미지원 또는 아직 값 없음.
 */
interface VehicleGateway {
    val capabilities: StateFlow<VehicleCapabilities>
    val powertrain: StateFlow<Powertrain>
    val driveState: StateFlow<DriveState>
    val speedKmh: StateFlow<Float?>

    /** 현재 주행 세션 효율. 소비량 단위로 정규화됨 (ICE: L/100km, EV: Wh/km). */
    val sessionEfficiency: StateFlow<Float?>
    val location: StateFlow<GeoPoint?>
    val roadSpeedLimitKmh: StateFlow<Int?>

    /** 주행 중 UX 제한(Distraction Optimization 필요) 상태. true 면 상호작용 화면을 잠근다. */
    val uxRestricted: StateFlow<Boolean>

    fun start()
    fun stop()
}

/** 기능별 지원 여부. 차종 분기 대신 이 값으로 UI 를 활성/비활성한다. */
data class VehicleCapabilities(
    val speed: Support = Support.UNKNOWN,
    val gear: Support = Support.UNKNOWN,
    val fuelEfficiency: Support = Support.UNKNOWN,
    val evEfficiency: Support = Support.UNKNOWN,
    val navigation: Support = Support.UNKNOWN,
    val lastError: String? = null,
) {
    enum class Support { SUPPORTED, UNSUPPORTED, UNKNOWN }

    val canRecordEfficiency: Boolean
        get() = fuelEfficiency == Support.SUPPORTED || evEfficiency == Support.SUPPORTED
}
