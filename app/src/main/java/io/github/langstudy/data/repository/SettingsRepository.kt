package io.github.langstudy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonObject
import io.github.langstudy.data.model.GitHubRelease
import io.github.langstudy.data.model.LanguageResource
import io.github.langstudy.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val githubService: GitHubService? = null
) {

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
                val learnedLanguages =
                    snapshot.get("learnedLanguages") as? List<String> ?: emptyList()
                val languageLearning = snapshot.getString("languageLearning") ?: ""
                val shareCode = snapshot.getString("shareCode") ?: ""
                val isPublic = snapshot.getBoolean("isPublic") ?: false
                val mentorCodeEnabled = snapshot.getBoolean("mentorCodeEnabled") ?: false
                val mentorAccessLevel = snapshot.getString("mentorAccessLevel") ?: "view"
                val mentorQuickReviewEnabled =
                    snapshot.getBoolean("mentorQuickReviewEnabled") ?: false
                val homepageTab = snapshot.getString("homepageTab") ?: "vocab"
                val theme = snapshot.getString("theme") ?: "system"
                val firstLogin = snapshot.getBoolean("firstLogin") ?: true

                trySend(
                    UserSettings(
                        learnedLanguages = learnedLanguages,
                        languageLearning = languageLearning,
                        shareCode = shareCode,
                        isPublic = isPublic,
                        mentorCodeEnabled = mentorCodeEnabled,
                        mentorAccessLevel = mentorAccessLevel,
                        mentorQuickReviewEnabled = mentorQuickReviewEnabled,
                        homepageTab = homepageTab,
                        theme = theme,
                        firstLogin = firstLogin
                    )
                )
            } else {
                // Document doesn't exist = brand new user
                // Initialize the settings document with firstLogin = true to ensure
                // partial updates (like language selection) don't lose the onboarding state.
                
                // ONLY initialize if the current user is the owner of these settings.
                val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId == currentAuthUid) {
                    docRef.set(mapOf("firstLogin" to true), SetOptions.merge())
                }
                trySend(UserSettings(firstLogin = userId == currentAuthUid))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getReleaseNotes(): List<GitHubRelease> {
        return try {
            githubService?.getReleases("Bearer ${io.github.langstudy.BuildConfig.GITHUB_TOKEN}")
                ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error fetching release notes", e)
            emptyList()
        }
    }

    suspend fun updateUserSettings(userId: String, settings: Map<String, Any>) {
        if (userId.isBlank()) return
        getSettingsDoc(userId).set(settings, SetOptions.merge()).await()
    }

    suspend fun submitContactRequest(
        userId: String,
        type: String,
        message: String,
        resourceName: String? = null,
        resourceLocation: String? = null,
        userEmail: String? = null,
        isMentorMode: Boolean = false,
        settings: UserSettings? = null
    ) {
        val client = OkHttpClient()
        val json = JsonObject().apply {
            addProperty("userId", userId)
            userEmail?.let { addProperty("email", it) }
            addProperty("type", type)
            addProperty("message", message)
            resourceName?.let { addProperty("resourceName", it) }
            resourceLocation?.let { addProperty("resourceLocation", it) }
            
            // Technical Metadata
            addProperty("appVersion", io.github.langstudy.BuildConfig.VERSION_NAME)
            addProperty("androidVersion", android.os.Build.VERSION.RELEASE)
            addProperty("device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            addProperty("isMentorMode", isMentorMode)
            
            settings?.let {
                addProperty("primaryLanguage", it.languageLearning)
                addProperty("learnedLanguages", it.learnedLanguages.joinToString(", "))
                addProperty("mentorEnabled", it.mentorCodeEnabled)
                addProperty("mentorAccessLevel", it.mentorAccessLevel)
                addProperty("theme", it.theme)
                addProperty("homepageTab", it.homepageTab)
                addProperty("isPublic", it.isPublic)
                addProperty("mentorQuickReview", it.mentorQuickReviewEnabled)
            }
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://formspree.io/f/xeebrgqb")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Submission failed: ${response.code}")
            }
        }
    }

    fun getAvailableLanguages(): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection("languageLinks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val languages = snapshot.documents.mapNotNull { it.id }
                        .filter { it.isNotBlank() }
                        .sorted()
                    trySend(languages)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
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
                val links = snapshot.get("links") as? List<Map<String, Any>> ?: emptyList()
                val resourceList = links.map {
                    LanguageResource(
                        name = it["name"] as? String ?: "",
                        url = it["url"] as? String ?: ""
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
