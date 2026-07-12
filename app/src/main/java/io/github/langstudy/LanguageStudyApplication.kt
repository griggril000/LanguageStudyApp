package io.github.langstudy

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.langstudy.data.local.AppDatabase
import io.github.langstudy.data.repository.AdminRepository
import io.github.langstudy.data.repository.FirestorePortfolioRepository
import io.github.langstudy.data.repository.GitHubService
import io.github.langstudy.data.repository.JournalRepository
import io.github.langstudy.data.repository.MentorRepository
import io.github.langstudy.data.repository.SettingsRepository
import io.github.langstudy.data.repository.SkillRepository
import io.github.langstudy.data.repository.VocabRepository
import io.github.langstudy.data.worker.DataSyncWorker
import io.github.langstudy.utils.PreferenceManager
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class LanguageStudyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val preferenceManager by lazy { PreferenceManager(this) }
    val vocabRepository by lazy { VocabRepository(database.vocabDao(), database.categoryDao()) }
    val skillRepository by lazy { SkillRepository(database.skillDao()) }
    val portfolioRepository by lazy { FirestorePortfolioRepository() }
    val journalRepository by lazy { JournalRepository(database.journalDao()) }
    val settingsRepository by lazy { SettingsRepository(githubService = githubService) }
    val mentorRepository by lazy { MentorRepository }
    val adminRepository by lazy { AdminRepository() }

    val githubService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GitHubService::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleDataSync()
    }

    private fun scheduleDataSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(8, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DataSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
