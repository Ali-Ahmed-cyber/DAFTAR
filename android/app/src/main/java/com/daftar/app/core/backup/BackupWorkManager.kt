package com.daftar.app.core.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.daftar.app.R
import java.util.concurrent.TimeUnit

class LocalMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_daftar_logo)
            .setContentTitle("دفتر | فحص الصلاحية والنسخ الاحتياطي")
            .setContentText("تذكير محلي: تأكد من أخذ نسخة احتياطية مشفرة ومراجعة تنبيهات المخزون.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(1001, notification)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daftar Local Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "daftar_local_alerts"
        private const val WORK_NAME = "daftar_periodic_maintenance"

        fun schedulePeriodicChecks(context: Context, intervalHours: Long = 24L) {
            val request = PeriodicWorkRequestBuilder<LocalMaintenanceWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
