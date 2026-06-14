package com.example.languagestudy.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.languagestudy.R
import com.example.languagestudy.data.repository.AdminRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val adminRepository: AdminRepository) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

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
    
    init {
        checkAdminStatus()
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

    fun enterMentorMode(ownerUid: String, code: String) {
        _mentorUid.value = ownerUid
        _mentorCode.value = code
    }

    fun exitMentorMode() {
        _mentorUid.value = null
        _mentorCode.value = null
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
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    _user.value = auth.currentUser
                    checkAdminStatus()
                } else if (credential is PasswordCredential) {
                    auth.signInWithEmailAndPassword(credential.id, credential.password).await()
                    _user.value = auth.currentUser
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
                _user.value = auth.currentUser
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
                _user.value = auth.currentUser
                checkAdminStatus()
            } catch (e: Exception) {
                _error.value = "Sign up failed: ${e.message}"
            } finally {
                _isLoading.value = false
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
                _error.value = "Password reset email sent"
            } catch (e: Exception) {
                _error.value = "Reset failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            exitMentorMode()
            _user.value = null
            _isAdmin.value = false
            auth.signOut()
            
            try {
                val app = context.applicationContext as com.example.languagestudy.LanguageStudyApplication
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    app.database.clearAllTables()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error clearing database", e)
            }

            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

class AuthViewModelFactory(private val adminRepository: AdminRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(adminRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
