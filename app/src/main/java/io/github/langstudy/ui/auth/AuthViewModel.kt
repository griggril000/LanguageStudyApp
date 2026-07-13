package io.github.langstudy.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.PasswordCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import io.github.langstudy.R
import io.github.langstudy.data.repository.AdminRepository
import io.github.langstudy.data.repository.MentorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val adminRepository: AdminRepository) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified ?: false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _sessionId = MutableStateFlow(java.util.UUID.randomUUID().toString())
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private val _mentorUid = MutableStateFlow<String?>(null)
    val mentorUid: StateFlow<String?> = _mentorUid.asStateFlow()

    private val _mentorCode = MutableStateFlow<String?>(null)
    val mentorCode: StateFlow<String?> = _mentorCode.asStateFlow()

    val isMentorMode: StateFlow<Boolean> = _mentorUid.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val effectiveUserId: StateFlow<String> = combine(user, _mentorUid) { currentUser, mentor ->
        mentor ?: currentUser?.uid ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.currentUser?.uid ?: "")

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            _user.value = currentUser
            _isEmailVerified.value = currentUser?.isEmailVerified ?: false
            if (currentUser != null) {
                checkAdminStatus()
            } else {
                _isAdmin.value = false
            }
        }
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _isAdmin.value = false
                return@launch
            }
            try {
                val tokenResult = user.getIdToken(true).await()
                val adminClaim = tokenResult.claims["admin"]
                var isAdmin = adminClaim == true ||
                        adminClaim == "true" ||
                        adminClaim == 1 ||
                        adminClaim == 1L ||
                        (adminClaim as? Number)?.toInt() == 1

                if (!isAdmin) {
                    isAdmin = adminRepository.checkAdminDoc(user.uid)
                }

                _isAdmin.value = isAdmin
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking admin status", e)
                _isAdmin.value = false
            }
        }
    }

    fun enterMentorMode(context: Context, ownerUid: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUid = auth.currentUser?.uid
                if (currentUid != null) {
                    MentorRepository.createMentorSession(currentUid, ownerUid, code)
                }

                // Ensure database is cleared BEFORE updating state to prevent race conditions
                clearLocalDatabase(context)
                
                _mentorUid.value = ownerUid
                _mentorCode.value = code
                // Update sessionId to trigger ViewModel recreation across the app
                _sessionId.value = java.util.UUID.randomUUID().toString()
                
                Log.d("AuthViewModel", "Entered mentor mode for $ownerUid")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error entering mentor mode", e)
                _error.value = "Failed to switch to mentor view: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exitMentorMode(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUid = auth.currentUser?.uid
                val ownerUid = _mentorUid.value
                if (currentUid != null && ownerUid != null) {
                    MentorRepository.deleteMentorSession(currentUid, ownerUid)
                }

                clearLocalDatabase(context)
                
                _mentorUid.value = null
                _mentorCode.value = null
                _sessionId.value = java.util.UUID.randomUUID().toString()
                
                Log.d("AuthViewModel", "Exited mentor mode")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error exiting mentor mode", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun clearLocalDatabase(context: Context) {
        try {
            val app = context.applicationContext as io.github.langstudy.LanguageStudyApplication
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.database.clearAllTables()
            }
            Log.d("AuthViewModel", "Local database cleared")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error clearing local database", e)
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    fun signInWithGoogle(context: Context) {
        val activity = context.findActivity() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val serverClientId = context.getString(R.string.default_web_client_id)
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                val request =
                    GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential =
                        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    _sessionId.value = java.util.UUID.randomUUID().toString()
                    _user.value = auth.currentUser
                    _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                    checkAdminStatus()
                } else if (credential is PasswordCredential) {
                    auth.signInWithEmailAndPassword(credential.id, credential.password).await()
                    _user.value = auth.currentUser
                    _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                    checkAdminStatus()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed", e)
                _error.value = "Sign in failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _sessionId.value = java.util.UUID.randomUUID().toString()
                _user.value = auth.currentUser
                _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                checkAdminStatus()
            } catch (e: Exception) {
                _error.value = "Login failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _sessionId.value = java.util.UUID.randomUUID().toString()
                _user.value = auth.currentUser
                _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                sendEmailVerification()
                checkAdminStatus()
            } catch (e: Exception) {
                _error.value = "Sign up failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendEmailVerification() {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            viewModelScope.launch {
                try {
                    user.sendEmailVerification().await()
                    _error.value = "Verification email sent to ${user.email}"
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error sending verification email", e)
                    _error.value = "Failed to send verification email: ${e.message}"
                }
            }
        }
    }

    fun reloadUser() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                val user = auth.currentUser
                _user.value = user
                _isEmailVerified.value = user?.isEmailVerified ?: false
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error reloading user", e)
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _error.value = "Please enter your email to reset password"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.sendPasswordResetEmail(email).await()
                _error.value = "Password reset email sent to $email"
            } catch (e: Exception) {
                _error.value = "Reset failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEmail(newEmail: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false, "No user logged in")
            return
        }

        if (newEmail.isBlank()) {
            onComplete(false, "Email cannot be empty")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                user.verifyBeforeUpdateEmail(newEmail).await()
                onComplete(true, "A verification email has been sent to $newEmail. Please verify it to complete the update.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating email", e)
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    onComplete(false, "For security, please sign out and sign in again, then try updating your email.")
                } else {
                    onComplete(false, e.message ?: "Error updating email")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUid = auth.currentUser?.uid
                val ownerUid = _mentorUid.value
                if (currentUid != null && ownerUid != null) {
                    MentorRepository.deleteMentorSession(currentUid, ownerUid)
                }

                // Clear DB first
                clearLocalDatabase(context)
                
                // Clear mentor state
                _mentorUid.value = null
                _mentorCode.value = null
                
                // Clear user state
                _user.value = null
                _isAdmin.value = false
                
                // Reset session
                _sessionId.value = java.util.UUID.randomUUID().toString()
                
                // Firebase sign out
                auth.signOut()

                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                
                Log.d("AuthViewModel", "Signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during sign out", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(context: Context, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false, "No user logged in")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection("users").document(user.uid)

                // Delete subcollections
                val collections = listOf("vocabulary", "skills", "portfolio", "journal", "metadata")
                for (collectionName in collections) {
                    val snapshot = userDocRef.collection(collectionName).get().await()
                    if (!snapshot.isEmpty) {
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                    }
                }

                // Delete mentor code if exists
                val mentorCodeId = MentorRepository.getMentorCodeIdForUser(user.uid)
                if (mentorCodeId != null) {
                    MentorRepository.deleteMentorCode(mentorCodeId)
                }

                // Delete user document
                userDocRef.delete().await()

                // Delete auth user
                user.delete().await()

                // Sign out and clear local data
                signOut(context)
                onComplete(true, null)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error deleting account", e)
                when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        onComplete(
                            false,
                            "Please sign out and sign in again, then try deleting your account for security."
                        )
                    }

                    else -> {
                        onComplete(false, e.message ?: "Error deleting account")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AuthViewModelFactory(private val adminRepository: AdminRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(adminRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
