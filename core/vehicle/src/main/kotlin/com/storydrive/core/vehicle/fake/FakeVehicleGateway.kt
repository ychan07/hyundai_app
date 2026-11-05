package com.storydrive.core.vehicle.fake

import com.storydrive.core.domain.model.DriveState
import com.storydrive.core.domain.model.GeoPoint
import com.storydrive.core.vehicle.VehicleCapabilities
import com.storydrive.core.vehicle.VehicleCapabilities.Support
import com.storydrive.core.vehicle.VehicleGateway
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
 * 주행 재생용 가짜 차량. 2차 PT 시연에서 "시뮬레이션 데이터" 로 표시하고 사용한다.
 * TODO: 시연 경로(기획서 10장)와 비·밤 등 환경 변화 재생으로 교체.
 */
class FakeVehicleGateway(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : VehicleGateway {

    /** 임시 시연 경로 (직선 근사). 시연 경로가 정해지면 대체. */
    val demoPath: List<GeoPoint> = listOf(
        GeoPoint(37.4979, 127.0276),
        GeoPoint(37.5009, 127.0366),
        GeoPoint(37.5045, 127.0490),
    )

    private val _capabilities = MutableStateFlow(
        VehicleCapabilities(
            speed = Support.SUPPORTED,
            gear = Support.SUPPORTED,
            navigation = Support.SUPPORTED,
        ),
    )
    private val _driveState = MutableStateFlow(DriveState.PARKED)
    private val _speed = MutableStateFlow<Float?>(0f)
    private val _location = MutableStateFlow<GeoPoint?>(demoPath.first())
    private val _uxRestricted = MutableStateFlow(false)

    override val capabilities: StateFlow<VehicleCapabilities> = _capabilities.asStateFlow()
    override val driveState: StateFlow<DriveState> = _driveState.asStateFlow()
    override val speedKmh: StateFlow<Float?> = _speed.asStateFlow()
    override val location: StateFlow<GeoPoint?> = _location.asStateFlow()
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
                _location.value = interpolate(t)
                _speed.value = 40f + 15f * sin(t * Math.PI).toFloat()
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
