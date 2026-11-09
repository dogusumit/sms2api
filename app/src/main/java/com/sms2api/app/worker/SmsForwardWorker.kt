package com.sms2api.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sms2api.app.SMS2ApiApplication
import com.sms2api.app.util.UrlValidator
import java.util.concurrent.TimeUnit

class SmsForwardWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val UNIQUE_WORK_NAME = "sms_forward_worker"
        const val KEY_MESSAGE_ID = "key_message_id"

        fun enqueue(context: Context, messageId: String? = null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val builder = OneTimeWorkRequestBuilder<SmsForwardWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )

            if (messageId != null) {
                builder.setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
            }

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                builder.build()
            )
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? SMS2ApiApplication ?: SMS2ApiApplication.get()
        val repository = app.repository
        val targetMessageId = inputData.getString(KEY_MESSAGE_ID)

        val endpoint = app.preferencesManager.apiEndpoint.trim()
        if (!UrlValidator.isValidUrl(endpoint)) {
            if (targetMessageId != null) {
                repository.dispatchSingleRecord(targetMessageId)
            }
            repository.dispatchPendingRecords(maxBatchSize = 50)
            return Result.failure()
        }

        return try {
            if (targetMessageId != null) {
                repository.dispatchSingleRecord(targetMessageId)
            }
            val summary = repository.dispatchPendingRecords(maxBatchSize = 50)
            if (summary.failed > 0 && runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (_: Exception) {
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
