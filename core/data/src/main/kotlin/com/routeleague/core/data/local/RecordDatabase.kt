package com.routeleague.core.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Upsert
    suspend fun upsertRoute(route: RouteEntity)

    @Query("SELECT * FROM routes")
    fun observeRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun route(id: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startedAtMs DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status IS NOT NULL AND synced = 0")
    suspend fun unsyncedSessions(): List<SessionEntity>

    @Query("UPDATE sessions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Insert
    suspend fun insertSample(sample: SampleEntity)

    @Query("SELECT * FROM samples WHERE sessionId = :sessionId ORDER BY timestampMs")
    suspend fun samples(sessionId: String): List<SampleEntity>

    @Query("DELETE FROM samples WHERE timestampMs < :beforeMs")
    suspend fun deleteSamplesBefore(beforeMs: Long): Int
}

@Database(
    entities = [RouteEntity::class, SessionEntity::class, SampleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RecordDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}
