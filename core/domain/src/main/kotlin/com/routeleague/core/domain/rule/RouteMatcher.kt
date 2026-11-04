package com.routeleague.core.domain.rule

import com.routeleague.core.domain.model.GeoPoint
import com.routeleague.core.domain.model.Route

/**
 * 주행 궤적이 등록 경로와 일치하는지 판정한다.
 * - 궤적 점의 [minInsideRatio] 이상이 경로 허용 폭 안에 있어야 한다.
 * - 시작/끝 점이 경로 시작/끝 근처여야 한다 (= 진행 방향 포함).
 * 목표: 정답 라벨 기록 50건에서 정확도 95% 이상 (기획서 5장).
 */
class RouteMatcher(
    private val minInsideRatio: Double = 0.9,
    private val endpointToleranceMeters: Double = 150.0,
) {
    fun matches(route: Route, track: List<GeoPoint>): Boolean {
        if (route.path.size < 2 || track.size < 2) return false
        if (Geo.distanceMeters(track.first(), route.path.first()) > endpointToleranceMeters) return false
        if (Geo.distanceMeters(track.last(), route.path.last()) > endpointToleranceMeters) return false

        val inside = track.count { distanceToPath(it, route.path) <= route.corridorMeters }
        return inside.toDouble() / track.size >= minInsideRatio
    }

    private fun distanceToPath(p: GeoPoint, path: List<GeoPoint>): Double =
        path.zipWithNext().minOf { (a, b) -> Geo.distanceToSegmentMeters(p, a, b) }
}
