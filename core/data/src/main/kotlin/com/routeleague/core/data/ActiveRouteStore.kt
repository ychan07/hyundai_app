package com.routeleague.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 주차 상태에서 사용자가 고른 "오늘의 도전 경로". 주행이 시작되면 이 경로로 기록한다.
 * TODO: DataStore 로 영속화 (프로세스 재생성 시 복원 — Pleos 화면 모델 가이드).
 */
class ActiveRouteStore {
    private val _routeId = MutableStateFlow<String?>(null)
    val routeId: StateFlow<String?> = _routeId.asStateFlow()

    fun select(routeId: String?) {
        _routeId.value = routeId
    }
}
