package com.storydrive.core.vehicle

import com.storydrive.core.domain.model.DriveState
import com.storydrive.core.domain.model.GeoPoint
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱이 차량을 바라보는 유일한 창구.
 * - [com.storydrive.core.vehicle.pleos.PleosVehicleGateway]: 실제 Pleos SDK + AAOS
 * - [com.storydrive.core.vehicle.fake.FakeVehicleGateway]: 주행 재생 (에뮬레이터 시연·테스트)
 *
 * 모든 값은 폴링이 아니라 SDK 리스너 구독으로 갱신된다 (주행 중 CPU 5% 미만 기준).
 * null = 미지원 또는 아직 값 없음.
 *
 * TODO(기획서 8장): 와이퍼·전조등·외기온, 목적지·남은 시간을 추가해 이야기 환경 반응에 쓴다.
 */
interface VehicleGateway {
    val capabilities: StateFlow<VehicleCapabilities>
    val driveState: StateFlow<DriveState>
    val speedKmh: StateFlow<Float?>
    val location: StateFlow<GeoPoint?>

    /** 주행 중 UX 제한(Distraction Optimization 필요) 상태. true 면 상호작용 화면을 잠근다. */
    val uxRestricted: StateFlow<Boolean>

    fun start()
    fun stop()
}

/** 기능별 지원 여부. 차종 분기 대신 이 값으로 UI 를 활성/비활성한다. */
data class VehicleCapabilities(
    val speed: Support = Support.UNKNOWN,
    val gear: Support = Support.UNKNOWN,
    val navigation: Support = Support.UNKNOWN,
    val lastError: String? = null,
) {
    enum class Support { SUPPORTED, UNSUPPORTED, UNKNOWN }
}
