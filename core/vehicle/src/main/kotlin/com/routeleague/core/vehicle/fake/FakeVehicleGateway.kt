package com.routeleague.core.vehicle.fake

import com.routeleague.core.domain.model.DriveState
import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Powertrain
import com.routeleague.core.vehicle.VehicleCapabilities
import com.routeleague.core.vehicle.VehicleCapabilities.Support
import com.routeleague.core.vehicle.VehicleGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * 시나리오 재생용 가짜 차량. 2차 PT 시연에서 "시뮬레이션 데이터" 로 표시하고 사용한다.
 * 정상 기록 / 과속 / 데이터 누락 케이스를 버튼 하나로 재현해 자격 판정 흐름을 보여준다.
 */
class FakeVehicleGateway(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : VehicleGateway {

    enum class Scenario { NORMAL, SPEEDING, DATA_GAP }

    var scenario: Scenario = Scenario.NORMAL

    /** 시연용 출근길 (강남역 → 선릉역 방향 직선 근사). 실제 경로 등록 기능이 생기면 대체. */
    val demoPath: List<GeoPoint> = listOf(
        GeoPoint(37.4979, 127.0276),
        GeoPoint(37.5009, 127.0366),
        GeoPoint(37.5045, 127.0490),
    )

    private val _capabilities = MutableStateFlow(
        VehicleCapabilities(
            speed = Support.SUPPORTED,
            gear = Support.SUPPORTED,
            fuelEfficiency = Support.SUPPORTED,
            evEfficiency = Support.UNSUPPORTED,
            navigation = Support.SUPPORTED,
        ),
    )
    private val _powertrain = MutableStateFlow(Powertrain.ICE)
    private val _driveState = MutableStateFlow(DriveState.PARKED)
    private val _speed = MutableStateFlow<Float?>(0f)
    private val _efficiency = MutableStateFlow<Float?>(null)
    private val _location = MutableStateFlow<GeoPoint?>(demoPath.first())
    private val _speedLimit = MutableStateFlow<Int?>(60)
    private val _uxRestricted = MutableStateFlow(false)

    override val capabilities: StateFlow<VehicleCapabilities> = _capabilities.asStateFlow()
    override val powertrain: StateFlow<Powertrain> = _powertrain.asStateFlow()
    override val driveState: StateFlow<DriveState> = _driveState.asStateFlow()
    override val speedKmh: StateFlow<Float?> = _speed.asStateFlow()
    override val sessionEfficiency: StateFlow<Float?> = _efficiency.asStateFlow()
    override val location: StateFlow<GeoPoint?> = _location.asStateFlow()
    override val roadSpeedLimitKmh: StateFlow<Int?> = _speedLimit.asStateFlow()
    override val uxRestricted: StateFlow<Boolean> = _uxRestricted.asStateFlow()

    private var job: Job? = null

    override fun start() = Unit

    override fun stop() {
        job?.cancel()
        job = null
    }

    /** 주행 한 번을 재생한다: 주차 → 주행(UX 제한) → 주차. */
    fun playDrive(durationSec: Int = 60, tickMs: Long = 1_000) {
        job?.cancel()
        job = scope.launch {
            _driveState.value = DriveState.DRIVING
            _uxRestricted.value = true
            for (i in 0..durationSec) {
                val t = i / durationSec.toDouble()
                if (scenario == Scenario.DATA_GAP && i in (durationSec / 3)..(durationSec / 2)) {
                    delay(tickMs)
                    continue // 이 구간은 값 갱신이 멈춘다 → 수집기에서 결측 처리
                }
                _location.value = interpolate(t)
                val base = 40f + 15f * sin(t * Math.PI).toFloat()
                _speed.value = if (scenario == Scenario.SPEEDING && i in 20..25) 78f else base
                _speedLimit.value = 60
                _efficiency.value = 7.2f + 0.6f * sin(t * 6).toFloat() // L/100km
                delay(tickMs)
            }
            _speed.value = 0f
            _driveState.value = DriveState.PARKED
            _uxRestricted.value = false
        }
    }

    private fun interpolate(t: Double): GeoPoint {
        val segments = demoPath.size - 1
        val pos = (t * segments).coerceIn(0.0, segments.toDouble())
        val idx = pos.toInt().coerceAtMost(segments - 1)
        val f = pos - idx
        val a = demoPath[idx]
        val b = demoPath[idx + 1]
        return GeoPoint(a.latitude + (b.latitude - a.latitude) * f, a.longitude + (b.longitude - a.longitude) * f)
    }
}
