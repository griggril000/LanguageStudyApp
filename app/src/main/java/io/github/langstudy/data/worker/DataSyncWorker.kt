package io.github.langstudy.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import androidx.glance.appwidget.updateAll
import io.github.langstudy.LanguageStudyApplication
import io.github.langstudy.ui.widget.LanguageWidget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

class DataSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val application = applicationContext as LanguageStudyApplication
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@coroutineScope Result.success()

        // Check last usage: Pull from firebase every 6-12 hours depending on app use
        // unless user hasn't used app recently.
        val lastUsage = application.preferenceManager.getLastUsage()
        val now = System.currentTimeMillis()

        // If app hasn't been used in 3 days, skip sync to save battery.
        if (now - lastUsage > TimeUnit.DAYS.toMillis(3)) {
            return@coroutineScope Result.success()
        }

        try {
            val vocabJob = async { application.vocabRepository.syncOneShot(userId) }
            val skillJob = async { application.skillRepository.syncOneShot(userId) }
            val journalJob = async { application.journalRepository.syncOneShot(userId) }
            
            awaitAll(vocabJob, skillJob, journalJob)
            
            LanguageWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
