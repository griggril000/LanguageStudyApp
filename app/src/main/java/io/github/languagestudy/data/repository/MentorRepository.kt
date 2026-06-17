package io.github.languagestudy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object MentorRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val codesCollection = firestore.collection("mentorCodes")

    suspend fun getMentorCodeIdForUser(userId: String): String? {
        return try {
            val snapshot = codesCollection.whereEqualTo("uid", userId).get().await()
            snapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            android.util.Log.e("MentorRepository", "Error getting mentor code", e)
            null
        }
    }

    suspend fun generateUniqueMentorCode(userId: String): String {
        // Try to find existing
        val existing = getMentorCodeIdForUser(userId)
        if (existing != null) return existing

        // Generate a new 5-character code
        var code: String
        var isUnique = false
        var attempts = 0
        do {
            code = (1..5).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
            val exists = try {
                val doc = codesCollection.document(code).get().await()
                doc.exists()
            } catch (e: Exception) {
                // If we can't even read to check uniqueness, we have a bigger problem
                android.util.Log.e("MentorRepository", "Error checking code uniqueness", e)
                true // Assume it exists to prevent overwriting if read fails
            }
            if (!exists) isUnique = true
            attempts++
        } while (!isUnique && attempts < 10)

        if (!isUnique) throw Exception("Failed to generate a unique code")

        val data = hashMapOf(
            "uid" to userId,
            "enabled" to true,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        try {
            codesCollection.document(code).set(data).await()
        } catch (e: Exception) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            android.util.Log.e("MentorRepository", "Error setting mentor code for user $userId (Auth UID: $uid)", e)
            throw e // Rethrow so the UI knows it failed, but handled by viewModel scope
        }
        
        return code
    }

    suspend fun updateMentorCodeStatus(code: String, enabled: Boolean) {
        try {
            codesCollection.document(code).update("enabled", enabled).await()
        } catch (e: Exception) {
            android.util.Log.e("MentorRepository", "Error updating mentor code status", e)
        }
    }

    suspend fun deleteMentorCode(code: String) {
        try {
            codesCollection.document(code).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("MentorRepository", "Error deleting mentor code", e)
        }
    }

    suspend fun validateMentorCode(code: String): String? {
        return try {
            val doc = codesCollection.document(code).get().await()
            if (doc.exists() && doc.getBoolean("enabled") == true) {
                doc.getString("uid")
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MentorRepository", "Error validating code", e)
            null
        }
    }
}
