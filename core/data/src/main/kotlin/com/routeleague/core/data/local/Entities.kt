package com.routeleague.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** "lat,lng;lat,lng;..." 직렬화 (외부 직렬화 라이브러리 없이) */
    val path: String,
    val corridorMeters: Double,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val routeId: String?,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val powertrain: String,
    /** QUALIFIED / DISQUALIFIED / null(진행 중) */
    val status: String?,
    val consumption: Float?,
    val unit: String?,
    /** 제외 사유 (DisqualifyReason.name 콤마 구분) */
    val reasons: String?,
    /** 서버 동기화 여부. 네트워크 복구 후 중복 없이 업로드 (session id = 멱등 키). */
    val synced: Boolean = false,
)

/** 원시 샘플. 차량 내 최대 30일 보관 후 삭제 (기획서 7장). 서버에는 요약만 올린다. */
@Entity(
    tableName = "samples",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val timestampMs: Long,
    val speedKmh: Float?,
    val latitude: Double?,
    val longitude: Double?,
    val sessionEfficiency: Float?,
    val roadSpeedLimitKmh: Int?,
)
