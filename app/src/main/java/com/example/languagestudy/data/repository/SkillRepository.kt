package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.SkillDao
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.Subtask
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class SkillRepository(private val skillDao: SkillDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()

    /**
     * Listen for real-time updates from Firestore.
     * This uses snapshots, which are optimized by Firestore to only send changes (deltas),
     * balancing resource usage with near-instant updates from the website.
     */
    fun startSync(userId: String): Flow<Unit> = callbackFlow {
        listenerRegistration?.remove()
        
        val collectionRef = firestore.collection("users").document(userId)
            .collection("skills")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                launch {
                    val now = System.currentTimeMillis()
                    for (change in querySnapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val subtasksData = data["subtasks"] as? List<Map<String, Any>> ?: emptyList()
                                val subtasks = subtasksData.map { st ->
                                    Subtask(
                                        id = st["id"] as? String ?: "",
                                        text = st["text"] as? String ?: "",
                                        status = (st["status"] as? String ?: "NOT_STARTED").uppercase()
                                    )
                                }

                                val skill = SkillEntity(
                                    id = doc.id,
                                    name = data["name"] as? String ?: "",
                                    language = data["language"] as? String ?: "",
                                    status = (data["status"] as? String ?: "NOT_STARTED").uppercase(),
                                    priority = (data["priority"] as? Long)?.toInt() ?: 0,
                                    subtasks = subtasks,
                                    lastUpdated = now
                                )
                                skillDao.insertSkill(skill)
                            }
                            DocumentChange.Type.REMOVED -> {
                                skillDao.deleteSkillById(doc.id)
                            }
                        }
                    }
                }
            }
            trySend(Unit)
        }

        listenerRegistration = listener
        awaitClose { listener.remove() }
    }

    suspend fun insert(skill: SkillEntity, userId: String? = null) {
        skillDao.insertSkill(skill)
        userId?.let { uid ->
            pushToFirestore(uid, skill)
        }
    }

    suspend fun update(skill: SkillEntity, userId: String? = null) {
        skillDao.updateSkill(skill)
        userId?.let { uid ->
            pushToFirestore(uid, skill)
        }
    }

    suspend fun delete(skill: SkillEntity, userId: String? = null) {
        skillDao.deleteSkill(skill)
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("skills").document(skill.id)
                .delete()
        }
    }

    private fun pushToFirestore(userId: String, skill: SkillEntity) {
        val skillData = mapOf(
            "name" to skill.name,
            "language" to skill.language,
            "status" to skill.status.lowercase(),
            "priority" to skill.priority,
            "subtasks" to skill.subtasks.map { st ->
                mapOf(
                    "id" to st.id,
                    "text" to st.text,
                    "status" to st.status.lowercase()
                )
            },
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users").document(userId)
            .collection("skills").document(skill.id)
            .set(skillData, SetOptions.merge())
    }
}
