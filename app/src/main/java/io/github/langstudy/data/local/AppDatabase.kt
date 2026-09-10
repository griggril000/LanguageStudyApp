package io.github.langstudy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.langstudy.data.local.dao.CategoryDao
import io.github.langstudy.data.local.dao.JournalDao
import io.github.langstudy.data.local.dao.SkillDao
import io.github.langstudy.data.local.dao.VocabDao
import io.github.langstudy.data.local.entity.CategoryEntity
import io.github.langstudy.data.local.entity.JournalEntryEntity
import io.github.langstudy.data.local.entity.JournalTypeConverters
import io.github.langstudy.data.local.entity.SkillEntity
import io.github.langstudy.data.local.entity.SkillTypeConverters
import io.github.langstudy.data.local.entity.VocabEntity

@Database(
    entities = [VocabEntity::class, SkillEntity::class, JournalEntryEntity::class, CategoryEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(SkillTypeConverters::class, JournalTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun skillDao(): SkillDao
    abstract fun journalDao(): JournalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "language_study_db"
                ).addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
