package com.example.languagestudy

import android.app.Application
import com.example.languagestudy.data.local.AppDatabase
import com.example.languagestudy.data.repository.JournalRepository
import com.example.languagestudy.data.repository.SettingsRepository
import com.example.languagestudy.data.repository.SkillRepository
import com.example.languagestudy.data.repository.VocabRepository

class LanguageStudyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val vocabRepository by lazy { VocabRepository(database.vocabDao()) }
    val skillRepository by lazy { SkillRepository(database.skillDao()) }
    val journalRepository by lazy { JournalRepository(database.journalDao()) }
    val settingsRepository by lazy { SettingsRepository() }
}
