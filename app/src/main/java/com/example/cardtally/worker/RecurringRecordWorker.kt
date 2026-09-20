package com.example.cardtally.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.RecurringRecordScheduler

class RecurringRecordWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val database = DatabaseHelper(applicationContext)
        return try {
            database.processDueRecurringRecordsForAllLedgers()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            database.close()
            RecurringRecordScheduler.schedule(applicationContext)
        }
    }
}
