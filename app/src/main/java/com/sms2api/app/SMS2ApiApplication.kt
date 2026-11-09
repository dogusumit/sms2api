package com.sms2api.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sms2api.app.data.local.AppDatabase
import com.sms2api.app.data.local.EncryptedPreferencesManager
import com.sms2api.app.data.network.ApiClient
import com.sms2api.app.data.repository.SmsRepository
import com.sms2api.app.service.SmsForwarderService
import com.sms2api.app.worker.SmsSyncWorker
import java.util.concurrent.TimeUnit

class SMS2ApiApplication : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ID = "sms2api_service_channel"
        private lateinit var instance: SMS2ApiApplication

        fun get(): SMS2ApiApplication = instance
    }

    val preferencesManager: EncryptedPreferencesManager by lazy {
        EncryptedPreferencesManager(this)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val apiClient: ApiClient by lazy {
        ApiClient(preferencesManager)
    }

    val repository: SmsRepository by lazy {
        SmsRepository(database.smsDao(), apiClient, preferencesManager)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        setupPeriodicSyncWorker()

        if (preferencesManager.isServiceEnabled) {
            SmsForwarderService.start(this)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun setupPeriodicSyncWorker() {
        if (preferencesManager.isPeriodicSyncEnabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            ).setConstraints(constraints)
             .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SmsSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
        } else {
            WorkManager.getInstance(this).cancelUniqueWork(SmsSyncWorker.WORK_NAME)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
