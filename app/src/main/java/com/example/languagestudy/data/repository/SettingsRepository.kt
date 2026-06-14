package com.example.languagestudy.data.repository

import com.example.languagestudy.data.model.LanguageResource
import com.example.languagestudy.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun getSettingsDoc(userId: String) = 
        firestore.collection("users").document(userId).collection("metadata").document("settings")

    fun getUserSettings(userId: String): Flow<UserSettings> = callbackFlow {
        if (userId.isBlank()) {
            trySend(UserSettings())
            awaitClose { }
            return@callbackFlow
        }
        val docRef = getSettingsDoc(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val learnedLanguages = snapshot.get("learnedLanguages") as? List<String> ?: emptyList()
                val languageLearning = snapshot.getString("languageLearning") ?: ""
                val shareCode = snapshot.getString("shareCode") ?: ""
                val isPublic = snapshot.getBoolean("isPublic") ?: false
                val mentorCodeEnabled = snapshot.getBoolean("mentorCodeEnabled") ?: false
                val mentorAccessLevel = snapshot.getString("mentorAccessLevel") ?: "view"
                val mentorQuickReviewEnabled = snapshot.getBoolean("mentorQuickReviewEnabled") ?: false
                
                trySend(UserSettings(
                    learnedLanguages = learnedLanguages,
                    languageLearning = languageLearning,
                    shareCode = shareCode,
                    isPublic = isPublic,
                    mentorCodeEnabled = mentorCodeEnabled,
                    mentorAccessLevel = mentorAccessLevel,
                    mentorQuickReviewEnabled = mentorQuickReviewEnabled
                ))
            } else {
                trySend(UserSettings())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserSettings(userId: String, settings: Map<String, Any>) {
        if (userId.isBlank()) return
        getSettingsDoc(userId).set(settings, SetOptions.merge()).await()
    }

    suspend fun getAvailableLanguages(): List<String> {
        val snapshot = firestore.collection("languageLinks").get().await()
        return snapshot.documents.mapNotNull { it.id }.filter { it.isNotBlank() }.sorted()
    }

    fun getLanguageResources(language: String): Flow<List<LanguageResource>> = callbackFlow {
        if (language.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val docRef = firestore.collection("languageLinks").document(language)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val links = snapshot.get("links") as? List<Map<String, String>> ?: emptyList()
                val resourceList = links.map { 
                    LanguageResource(
                        name = it["name"] ?: "",
                        url = it["url"] ?: ""
                    )
                }
                trySend(resourceList)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }
}
