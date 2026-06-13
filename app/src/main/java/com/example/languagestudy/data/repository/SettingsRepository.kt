package com.example.languagestudy.data.repository

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
                // If we get an error (like PERMISSION_DENIED on logout), just close the flow normally
                close()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val learnedLanguages = snapshot.get("learnedLanguages") as? List<String> ?: emptyList()
                val languageLearning = snapshot.getString("languageLearning") ?: ""
                val shareCode = snapshot.getString("shareCode") ?: ""
                val isPublic = snapshot.getBoolean("isPublic") ?: false
                trySend(UserSettings(learnedLanguages, languageLearning, shareCode, isPublic))
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
}
