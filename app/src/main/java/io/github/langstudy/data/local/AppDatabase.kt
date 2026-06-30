package io.github.langstudy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.langstudy.data.local.dao.CategoryDao
import io.github.langstudy.data.local.dao.JournalDao
import io.github.langstudy.data.local.dao.SkillDao
import io.github.langstudy.data.local.dao.VocabDao
import io.github.langstudy.data.local.entity.CategoryEntity
import io.github.langstudy.data.local.entity.JournalEntryEntity
import io.github.langstudy.data.local.entity.SkillEntity
import io.github.langstudy.data.local.entity.SkillTypeConverters
import io.github.langstudy.data.local.entity.VocabEntity

@Database(
    entities = [VocabEntity::class, SkillEntity::class, JournalEntryEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(SkillTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun skillDao(): SkillDao
    abstract fun journalDao(): JournalDao
    abstract fun categoryDao(): CategoryDao

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
