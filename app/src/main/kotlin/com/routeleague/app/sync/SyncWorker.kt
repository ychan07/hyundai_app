package com.routeleague.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.routeleague.core.data.RecordRepository
import com.routeleague.core.data.remote.RemoteSyncApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** 오프라인으로 쌓인 요약 기록을 서버에 올리고, 30일 지난 원시 샘플을 삭제한다. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RecordRepository,
    private val remote: RemoteSyncApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var failed = false
        repository.unsyncedSessions().forEach { session ->
            remote.uploadSession(session)
                .onSuccess { repository.markSynced(session.id) }
                .onFailure { failed = true }
        }
        repository.purgeOldSamples(System.currentTimeMillis())
        return if (failed) Result.retry() else Result.success()
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
