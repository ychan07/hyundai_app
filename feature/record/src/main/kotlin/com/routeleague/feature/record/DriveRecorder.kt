package com.routeleague.feature.record

import com.routeleague.core.data.ActiveRouteStore
import com.routeleague.core.data.RecordRepository
import com.routeleague.core.domain.model.DriveSample
import com.routeleague.core.domain.model.DriveState
import com.routeleague.core.domain.model.Qualification
import com.routeleague.core.vehicle.VehicleGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 주행 자동 기록기. 화면 없이 동작한다.
 *  - DRIVING 진입 → 세션 시작
 *  - 위치 이벤트마다 그 시점의 속도·효율·제한속도를 합쳐 샘플 저장 (폴링 없음)
 *  - PARKED 진입 → 세션 종료 + 자격 판정 → [lastResult]
 * UNKNOWN 상태에서는 세션을 시작/종료하지 않는다 (신호대기 0km/h 를 주차로 오판하지 않기 위함).
 */
@Singleton
class DriveRecorder @Inject constructor(
    private val gateway: VehicleGateway,
    private val repository: RecordRepository,
    private val activeRoute: ActiveRouteStore,
) {
    data class Result(val sessionId: String, val qualification: Qualification)

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _lastResult = MutableStateFlow<Result?>(null)
    val lastResult: StateFlow<Result?> = _lastResult.asStateFlow()

    private val mutex = Mutex()
    private var jobs: List<Job> = emptyList()

    fun attach(scope: CoroutineScope, now: () -> Long = System::currentTimeMillis) {
        if (jobs.isNotEmpty()) return
        jobs = listOf(
            scope.launch {
                gateway.driveState.distinctUntilChanged().collect { state ->
                    mutex.withLock {
                        when (state) {
                            DriveState.DRIVING -> if (_activeSessionId.value == null) {
                                _activeSessionId.value = repository.startSession(
                                    routeId = activeRoute.routeId.value,
                                    powertrain = gateway.powertrain.value,
                                    nowMs = now(),
                                )
                            }
                            DriveState.PARKED -> _activeSessionId.value?.let { id ->
                                val q = repository.finishSession(id, now())
                                _activeSessionId.value = null
                                if (q != null) _lastResult.value = Result(id, q)
                            }
                            DriveState.UNKNOWN -> Unit
                        }
                    }
                }
            },
            scope.launch {
                gateway.location.filterNotNull().collect { location ->
                    val sessionId = _activeSessionId.value ?: return@collect
                    repository.addSample(
                        sessionId,
                        DriveSample(
                            timestampMs = now(),
                            speedKmh = gateway.speedKmh.value,
                            location = location,
                            sessionEfficiency = gateway.sessionEfficiency.value,
                            roadSpeedLimitKmh = gateway.roadSpeedLimitKmh.value,
                        ),
                    )
                }
            },
        )
    }

    fun detach() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
    }
}
