package com.example.languagestudy

import android.app.Application
import com.example.languagestudy.data.local.AppDatabase
import com.example.languagestudy.data.repository.*

class LanguageStudyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val vocabRepository by lazy { VocabRepository(database.vocabDao(), database.categoryDao()) }
    val skillRepository by lazy { SkillRepository(database.skillDao()) }
    val journalRepository by lazy { JournalRepository(database.journalDao()) }
    val settingsRepository by lazy { SettingsRepository() }
    val mentorRepository by lazy { MentorRepository() }
}
