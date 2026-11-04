package com.routeleague.feature.record

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.routeleague.core.vehicle.VehicleGateway
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 주행 중 백그라운드 기록을 유지하는 포그라운드 서비스.
 * TODO(정책 확인): Pleos 에서 허용하는 주행 중 백그라운드 동작 범위와 foregroundServiceType 을
 *  driving-restriction-development-guide / 앱 심사 가이드로 확인할 것. (현재 specialUse 로 선언)
 */
@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject lateinit var gateway: VehicleGateway
    @Inject lateinit var recorder: DriveRecorder

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        gateway.start()
        recorder.attach(lifecycleScope)
    }

    override fun onDestroy() {
        recorder.detach()
        gateway.stop()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "주행 기록", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("경로 기록 준비됨")
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, RecordingService::class.java))
        }
    }
}
