package com.example.languagestudy.data.repository

import com.example.languagestudy.data.model.LanguageResource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun checkAdminDoc(uid: String): Boolean {
        return try {
            val doc = firestore.collection("admins").document(uid).get().await()
            doc.exists() && doc.getBoolean("active") == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addLanguage(languageName: String, userId: String) {
        val docRef = firestore.collection("languageLinks").document(languageName)
        val doc = docRef.get().await()
        if (doc.exists()) {
            throw Exception("Language already exists")
        }

        val data = hashMapOf(
            "links" to emptyList<Map<String, String>>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to userId,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedBy" to userId
        )
        docRef.set(data).await()
    }

    suspend fun deleteLanguage(languageName: String) {
        firestore.collection("languageLinks").document(languageName).delete().await()
    }

    suspend fun saveLanguageLinks(languageName: String, links: List<LanguageResource>, userId: String) {
        val cleanedLinks = links.map {
            mapOf("name" to it.name, "url" to it.url)
        }

        val data = hashMapOf(
            "links" to cleanedLinks,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedBy" to userId
        )
        firestore.collection("languageLinks").document(languageName).update(data).await()
    }
}
