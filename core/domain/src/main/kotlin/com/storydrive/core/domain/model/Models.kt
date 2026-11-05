package com.storydrive.core.domain.model

/** 위도/경도. NaviHelper 10/19 릴리즈부터 생성자 순서가 (latitude, longitude) 이므로 동일하게 맞춘다. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** 주행 상태. 신호대기(속도 0)와 주차(P단)를 구분한다. 확인 불가면 UNKNOWN → 상호작용 화면 잠금. */
enum class DriveState { PARKED, DRIVING, UNKNOWN }
