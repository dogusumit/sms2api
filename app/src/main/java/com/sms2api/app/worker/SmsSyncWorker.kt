package com.sms2api.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sms2api.app.SMS2ApiApplication

class SmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "sms2api_periodic_sync"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? SMS2ApiApplication ?: SMS2ApiApplication.get()
        val repository = app.repository

        return try {
            val pendingCount = repository.getPendingCount()
            if (pendingCount > 0) {
                repository.dispatchPendingRecords(maxBatchSize = 100)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
