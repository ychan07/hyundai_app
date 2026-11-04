package com.routeleague.core.vehicle

/**
 * SDK 가 주는 효율값(Float)을 소비량 단위로 변환한다. 변환은 이 파일 한 곳에서만 한다.
 *
 * TODO(실측 확인): SDK 문서에 단위가 명시돼 있지 않다. 에뮬레이터 Virtual Console 과
 *  차량 표시 단위(READ_CAR_DISPLAY_UNITS)로 확인한 뒤 아래 가정을 확정할 것.
 *  - 연비: km/L 로 가정 → L/100km = 100 / (km/L)
 *  - 전비: km/kWh 로 가정 → Wh/km = 1000 / (km/kWh)
 */
object EfficiencyNormalizer {
    fun fuelToLitersPer100Km(kmPerLiter: Float?): Float? =
        kmPerLiter?.takeIf { it > 0f }?.let { 100f / it }

    fun evToWhPerKm(kmPerKwh: Float?): Float? =
        kmPerKwh?.takeIf { it > 0f }?.let { 1000f / it }
}
