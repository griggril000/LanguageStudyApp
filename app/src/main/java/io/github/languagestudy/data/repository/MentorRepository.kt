package io.github.languagestudy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object MentorRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val codesCollection = firestore.collection("mentor_codes")

    suspend fun getMentorCodeIdForUser(userId: String): String? {
        val snapshot = codesCollection.whereEqualTo("ownerUid", userId).get().await()
        return snapshot.documents.firstOrNull()?.id
    }

    suspend fun generateUniqueMentorCode(userId: String): String {
        // Try to find existing
        val existing = getMentorCodeIdForUser(userId)
        if (existing != null) return existing

        // Generate a new 5-character code
        var code: String
        var isUnique = false
        do {
            code = (1..5).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
            val doc = codesCollection.document(code).get().await()
            if (!doc.exists()) isUnique = true
        } while (!isUnique)

        val data = hashMapOf(
            "ownerUid" to userId,
            "active" to true,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        codesCollection.document(code).set(data).await()
        return code
    }

    suspend fun updateMentorCodeStatus(code: String, enabled: Boolean) {
        codesCollection.document(code).update("active", enabled).await()
    }

    suspend fun deleteMentorCode(code: String) {
        codesCollection.document(code).delete().await()
    }

    suspend fun validateMentorCode(code: String): String? {
        val doc = codesCollection.document(code).get().await()
        return if (doc.exists() && doc.getBoolean("active") == true) {
            doc.getString("ownerUid")
        } else {
            null
        }
    }
}
