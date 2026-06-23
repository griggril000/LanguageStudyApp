package io.github.languagestudy.data.repository

import io.github.languagestudy.data.local.entity.SkillEntity
import io.github.languagestudy.data.local.entity.Subtask
import io.github.languagestudy.data.local.entity.VocabEntity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SampleDataSeeder(
    private val vocabRepository: VocabRepository,
    private val skillRepository: SkillRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun seed(userId: String) = coroutineScope {
        android.util.Log.d("SampleDataSeeder", "Seeding data for user: $userId")
        // Check if data already exists to avoid duplicates
        val existingVocab = vocabRepository.allVocab.first()
        android.util.Log.d("SampleDataSeeder", "Existing vocab count: ${existingVocab.size}")
        if (existingVocab.isNotEmpty()) {
            android.util.Log.d("SampleDataSeeder", "Data already exists, skipping seed.")
            return@coroutineScope
        }

        launch {
            val samples = listOf(
                VocabEntity(id = "sample_v1", word = "Hola", translation = "Hello", category = "Greeting", language = "Spanish", status = "PROFICIENT"),
                VocabEntity(id = "sample_v2", word = "Gracias", translation = "Thank you", category = "Greeting", language = "Spanish", status = "IN_PROGRESS"),
                VocabEntity(id = "sample_v3", word = "Buenos días", translation = "Good morning", category = "Greeting", language = "Spanish", status = "NOT_STARTED"),
                VocabEntity(id = "sample_v4", word = "Bonjour", translation = "Hello", category = "Greeting", language = "French", status = "NOT_STARTED"),
                VocabEntity(id = "sample_v5", word = "S'il vous plaît", translation = "Please", category = "Politeness", language = "French", status = "PROFICIENT"),
                VocabEntity(id = "sample_v6", word = "Merci", translation = "Thank you", category = "Greeting", language = "French", status = "IN_PROGRESS"),
                VocabEntity(id = "sample_v7", word = "Konnichiwa", translation = "Hello", category = "Greeting", language = "Japanese", status = "IN_PROGRESS"),
                VocabEntity(id = "sample_v8", word = "Arigato", translation = "Thank you", category = "Greeting", language = "Japanese", status = "NOT_STARTED"),
                VocabEntity(id = "sample_v9", word = "Sumimasen", translation = "Excuse me / Sorry", category = "Politeness", language = "Japanese", status = "NOT_STARTED")
            )
            samples.forEach { vocabRepository.insert(it, userId) }
        }

        launch {
            val skills = listOf(
                SkillEntity(
                    id = "sample_s1",
                    name = "Spanish Basics",
                    language = "Spanish",
                    status = "IN_PROGRESS",
                    priority = 0,
                    subtasks = listOf(
                        Subtask(id = "s1", text = "Common greetings", status = "PROFICIENT"),
                        Subtask(id = "s2", text = "Numbers 1-10", status = "IN_PROGRESS"),
                        Subtask(id = "s3", text = "Basic introductions", status = "NOT_STARTED")
                    ),
                    progress = 33
                ),
                SkillEntity(
                    id = "sample_s2",
                    name = "French Pronunciation",
                    language = "French",
                    status = "NOT_STARTED",
                    priority = 1,
                    subtasks = listOf(
                        Subtask(id = "f1", text = "Vowel sounds"),
                        Subtask(id = "f2", text = "Nasal sounds"),
                        Subtask(id = "f3", text = "Liaisons")
                    )
                ),
                SkillEntity(
                    id = "sample_s3",
                    name = "Japanese Hiragana",
                    language = "Japanese",
                    status = "PROFICIENT",
                    priority = 2,
                    subtasks = listOf(
                        Subtask(id = "j1", text = "Vowels (a, i, u, e, o)", status = "PROFICIENT"),
                        Subtask(id = "j2", text = "K-row (ka, ki, ku, ke, ko)", status = "PROFICIENT"),
                        Subtask(id = "j3", text = "S-row (sa, shi, su, se, so)", status = "PROFICIENT")
                    ),
                    progress = 100
                )
            )
            skills.forEach { skillRepository.insert(it, userId) }
        }

        launch {
            settingsRepository.updateUserSettings(userId, mapOf(
                "learnedLanguages" to listOf("Spanish", "French", "Japanese"),
                "languageLearning" to "Spanish"
            ))
        }
    }
}
