package com.routeleague.core.domain.rule

import com.routeleague.core.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)
        val h = sinLat * sinLat +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinLon * sinLon
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /** 점 p 에서 선분 ab 까지의 거리(m). 짧은 구간이므로 평면 근사(equirectangular)를 쓴다. */
    fun distanceToSegmentMeters(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val cosLat = cos(Math.toRadians(p.latitude))
        fun x(g: GeoPoint) = Math.toRadians(g.longitude) * cosLat * EARTH_RADIUS_M
        fun y(g: GeoPoint) = Math.toRadians(g.latitude) * EARTH_RADIUS_M
        val px = x(p); val py = y(p)
        val ax = x(a); val ay = y(a)
        val dx = x(b) - ax
        val dy = y(b) - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / len2))
        val cx = ax + t * dx
        val cy = ay + t * dy
        return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
    }
}
