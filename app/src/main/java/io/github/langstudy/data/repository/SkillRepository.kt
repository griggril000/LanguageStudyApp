package io.github.langstudy.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.github.langstudy.data.local.dao.SkillDao
import io.github.langstudy.data.local.entity.SkillEntity
import io.github.langstudy.data.local.entity.Subtask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SkillRepository(private val skillDao: SkillDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()
    val skillCount: Flow<Int> = skillDao.getSkillCount()

    suspend fun syncOneShot(userId: String) {
        if (userId.isBlank()) return
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("skills").get().await()

            val now = System.currentTimeMillis()
            val remoteSkillIds = mutableSetOf<String>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
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
                    progress = (data["progress"] as? Long)?.toInt() ?: 0,
                    status = (data["status"] as? String ?: "NOT_STARTED").uppercase(),
                    priority = (data["priority"] as? Long)?.toInt() ?: 0,
                    subtasks = subtasks,
                    lastUpdated = now
                )
                skillDao.insertSkill(skill)
                remoteSkillIds.add(doc.id)
            }

            // Full Sync: Remove local items that are not in remote
            val allLocal = skillDao.getAllSkills().first()
            for (local in allLocal) {
                if (!remoteSkillIds.contains(local.id)) {
                    skillDao.deleteSkill(local)
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Listen for real-time updates from Firestore.
     * This uses snapshots, which are optimized by Firestore to only send changes (deltas),
     * balancing resource usage with near-instant updates from the website.
     */
    fun startSync(userId: String): Flow<Unit> = callbackFlow {
        if (userId.isBlank()) {
            awaitClose { }
            return@callbackFlow
        }
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
                                val subtasksData =
                                    data["subtasks"] as? List<Map<String, Any>> ?: emptyList()
                                val subtasks = subtasksData.map { st ->
                                    Subtask(
                                        id = st["id"] as? String ?: "",
                                        text = st["text"] as? String ?: "",
                                        status = (st["status"] as? String
                                            ?: "NOT_STARTED").uppercase()
                                    )
                                }

                                val skill = SkillEntity(
                                    id = doc.id,
                                    name = data["name"] as? String ?: "",
                                    language = data["language"] as? String ?: "",
                                    progress = (data["progress"] as? Long)?.toInt() ?: 0,
                                    status = (data["status"] as? String
                                        ?: "NOT_STARTED").uppercase(),
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
        if (!userId.isNullOrBlank()) {
            pushToFirestore(userId, skill)
        }
    }

    suspend fun update(skill: SkillEntity, userId: String? = null) {
        skillDao.updateSkill(skill)
        if (!userId.isNullOrBlank()) {
            pushToFirestore(userId, skill)
        }
    }

    suspend fun delete(skill: SkillEntity, userId: String? = null) {
        skillDao.deleteSkill(skill)
        if (!userId.isNullOrBlank()) {
            firestore.collection("users").document(userId)
                .collection("skills").document(skill.id)
                .delete()
        }
    }

    private fun pushToFirestore(userId: String, skill: SkillEntity) {
        if (userId.isBlank()) return
        val skillData = mapOf(
            "name" to skill.name,
            "language" to skill.language,
            "status" to skill.status.lowercase(),
            "progress" to skill.progress,
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
