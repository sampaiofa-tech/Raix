package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker responsible for synchronizing messages in the background
 * upon receiving a blind push notification.
 */
class SyncMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SyncMessageWorker started. Performing blind-push sync.")
            // TODO: In a complete implementation, this would trigger the actual KMP network 
            // request to pull the encrypted payload and save it in the local Room database,
            // before showing the local notification (or replacing the generic one).
            
            Log.d(TAG, "SyncMessageWorker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SyncMessageWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncMessageWorker"

        fun enqueueSync(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncMessageWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "SyncMessageWorker enqueued.")
        }
    }
}
