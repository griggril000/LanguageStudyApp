package io.github.langstudy

import android.app.Application
import io.github.langstudy.data.local.AppDatabase
import io.github.langstudy.data.repository.AdminRepository
import io.github.langstudy.data.repository.FirestorePortfolioRepository
import io.github.langstudy.data.repository.GitHubService
import io.github.langstudy.data.repository.JournalRepository
import io.github.langstudy.data.repository.MentorRepository
import io.github.langstudy.data.repository.SampleDataSeeder
import io.github.langstudy.data.repository.SettingsRepository
import io.github.langstudy.data.repository.SkillRepository
import io.github.langstudy.data.repository.VocabRepository
import okhttp3.OkHttpClient
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
    
    val sampleDataSeeder by lazy {
        SampleDataSeeder(
            vocabRepository,
            skillRepository,
            settingsRepository,
            portfolioRepository,
            journalRepository
        )
    }

    val githubService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "LanguageStudyApp")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GitHubService::class.java)
    }
}
