package com.sms2api.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sms2api.app.SMS2ApiApplication

class HistoricalImportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val UNIQUE_WORK_NAME = "historical_import_worker"
        const val KEY_SCANNED = "scanned"
        const val KEY_MATCHED = "matched"
        const val KEY_QUEUED = "queued"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<HistoricalImportWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? SMS2ApiApplication ?: SMS2ApiApplication.get()
        val repository = app.repository

        val importResult = repository.importHistoricalInbox(applicationContext)
        if (!importResult.success) {
            return Result.failure()
        }

        // Trigger batch dispatch of all queued messages
        if (importResult.newlyQueued > 0) {
            SmsForwardWorker.enqueue(applicationContext)
        }

        val output = workDataOf(
            KEY_SCANNED to importResult.totalScanned,
            KEY_MATCHED to importResult.matched,
            KEY_QUEUED to importResult.newlyQueued
        )

        return Result.success(output)
    }
}
