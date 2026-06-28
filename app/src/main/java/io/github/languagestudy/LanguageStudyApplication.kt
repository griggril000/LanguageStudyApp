package io.github.languagestudy

import android.app.Application
import io.github.languagestudy.data.local.AppDatabase
import io.github.languagestudy.data.repository.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class LanguageStudyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
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
}
