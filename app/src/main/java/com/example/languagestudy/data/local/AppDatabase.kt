package com.example.languagestudy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.languagestudy.data.local.dao.JournalDao
import com.example.languagestudy.data.local.dao.SkillDao
import com.example.languagestudy.data.local.dao.VocabDao
import com.example.languagestudy.data.local.entity.JournalEntryEntity
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.SkillTypeConverters
import com.example.languagestudy.data.local.entity.VocabEntity

@Database(
    entities = [VocabEntity::class, SkillEntity::class, JournalEntryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(SkillTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun skillDao(): SkillDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "language_study_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
