package com.routeleague.core.domain.model

/** 위도/경도. NaviHelper 10/19 릴리즈부터 생성자 순서가 (latitude, longitude) 이므로 동일하게 맞춘다. */
data class GeoPoint(val latitude: Double, val longitude: Double)

enum class Powertrain { ICE, EV, UNKNOWN }

/** 주행 상태. 신호대기(속도 0)와 주차(P단)를 구분한다. 확인 불가면 UNKNOWN → 상호작용 화면 잠금. */
enum class DriveState { PARKED, DRIVING, UNKNOWN }

/**
 * 주행 중 수집하는 원시 샘플. 리스너 콜백의 마지막 값을 합쳐 일정 주기로 저장한다.
 * null 은 "해당 시점에 값이 없음"(미지원/결측)을 뜻한다.
 */
data class DriveSample(
    val timestampMs: Long,
    val speedKmh: Float?,
    val location: GeoPoint?,
    /** 차량이 보고한 "현재 주행 세션" 효율. [EnergyUnit] 로 정규화한 값. */
    val sessionEfficiency: Float?,
    val roadSpeedLimitKmh: Int?,
)

/** 소비량 중심 단위. 값이 작을수록 효율이 좋다. */
enum class EnergyUnit { LITER_PER_100KM, WH_PER_KM }

data class Route(
    val id: String,
    val name: String,
    /** 경로 중심선(폴리라인). 시작 → 끝 방향. */
    val path: List<GeoPoint>,
    /** 경로 일치 판정 허용 폭(m). */
    val corridorMeters: Double = 60.0,
)

data class DriveSession(
    val id: String,
    val routeId: String?,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val powertrain: Powertrain,
    val samples: List<DriveSample>,
)

/** 기록 자격 판정 결과. 제외 사유를 사용자에게 그대로 보여준다. */
sealed interface Qualification {
    data class Qualified(val consumption: Float, val unit: EnergyUnit) : Qualification
    data class Disqualified(val reasons: List<DisqualifyReason>) : Qualification
}

enum class DisqualifyReason(val label: String) {
    ROUTE_MISMATCH("등록 경로와 다른 길로 주행"),
    INSUFFICIENT_DATA("기록 데이터 부족"),
    DATA_GAP("기록 중간 누락"),
    SPEEDING("제한속도 초과"),
    NO_SPEED_LIMIT_INFO("제한속도 정보 없음"),
    NO_EFFICIENCY_DATA("차량 효율 데이터 없음"),
}

data class WeeklyEntry(val userId: String, val consumptions: List<Float>)

data class RankedEntry(val userId: String, val rank: Int, val medianConsumption: Float)
