package com.example.cardtally.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.worker.RecurringRecordWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object RecurringRecordScheduler {
    private const val UNIQUE_WORK_NAME = "recurring-records-due"

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val database = DatabaseHelper(appContext)
        val earliestDue = database.getEarliestRecurringDueDate()
        database.close()

        val delay = earliestDue?.let(::delayUntilStartOfDate) ?: TimeUnit.DAYS.toMillis(1)
        val request = OneTimeWorkRequestBuilder<RecurringRecordWorker>()
            .setInitialDelay(delay.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun delayUntilStartOfDate(date: String): Long {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val target = runCatching { parser.parse(date)?.time }.getOrNull() ?: return 0L
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return target - todayStart
    }
}
