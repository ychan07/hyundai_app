package com.routeleague.core.data.remote

import com.routeleague.core.data.local.SessionEntity
import com.routeleague.core.domain.model.RankedEntry

/**
 * 서버 연동 경계. 원시 위치·속도는 올리지 않고 요약(경로 ID·조건·소비량·판정 근거)만 올린다.
 * 구현 예정: Supabase PostgREST (backend/supabase/migrations 참조).
 */
interface RemoteSyncApi {
    /** session id 를 멱등 키로 사용 → 재시도해도 중복 저장되지 않는다. */
    suspend fun uploadSession(session: SessionEntity): Result<Unit>

    suspend fun weeklyLeaderboard(routeId: String, seasonId: String, week: Int): Result<List<RankedEntry>>
}

/** 서버 준비 전 사용하는 로컬 전용 구현. 업로드는 성공으로 처리하고 순위는 비어 있다. */
class OfflineSyncApi : RemoteSyncApi {
    override suspend fun uploadSession(session: SessionEntity): Result<Unit> = Result.success(Unit)

    override suspend fun weeklyLeaderboard(routeId: String, seasonId: String, week: Int): Result<List<RankedEntry>> =
        Result.success(emptyList())
}
