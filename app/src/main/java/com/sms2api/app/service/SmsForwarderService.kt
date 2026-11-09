package com.sms2api.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sms2api.app.R
import com.sms2api.app.SMS2ApiApplication
import com.sms2api.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SmsForwarderService : Service() {

    companion object {
        private const val TAG = "SmsForwarderService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.sms2api.app.action.START"
        const val ACTION_STOP = "com.sms2api.app.action.STOP"
        const val ACTION_DISPATCH = "com.sms2api.app.action.DISPATCH"

        fun start(context: Context) {
            val intent = Intent(context, SmsForwarderService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SmsForwarderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_DISPATCH -> {
                triggerBackgroundDispatch()
            }
            ACTION_START, null -> {
                if (!isRunning) {
                    startForegroundWithNotification()
                    observeQueueState()
                    isRunning = true
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(pendingCount = 0)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun observeQueueState() {
        val app = application as? SMS2ApiApplication ?: return
        serviceScope.launch {
            app.repository.pendingCountFlow.collectLatest { pendingCount ->
                val notification = buildNotification(pendingCount)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun triggerBackgroundDispatch() {
        val app = application as? SMS2ApiApplication ?: return
        serviceScope.launch(Dispatchers.IO) {
            app.repository.dispatchPendingRecords()
        }
    }

    private fun buildNotification(pendingCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (pendingCount > 0) {
            getString(R.string.service_messages_queued, getString(R.string.service_active_title), pendingCount)
        } else {
            getString(R.string.service_active_desc)
        }

        return NotificationCompat.Builder(this, SMS2ApiApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_active_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenApp)
            .build()
    }

    private fun stopForegroundService() {
        isRunning = false
        val app = application as? SMS2ApiApplication
        app?.preferencesManager?.isServiceEnabled = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isRunning = false
    }
}
