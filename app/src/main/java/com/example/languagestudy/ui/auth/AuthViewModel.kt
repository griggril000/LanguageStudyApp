package com.example.languagestudy.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

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
                // Force a refresh (true) to ensure we have the latest claims
                val tokenResult = user.getIdToken(true).await()
                
                // Allow "true" (String), true (Boolean), or 1 (Integer) for flexibility
                val adminClaim = tokenResult.claims["admin"]
                val isAdmin = adminClaim == true || 
                             adminClaim == "true" || 
                             adminClaim == 1 || 
                             adminClaim == 1L ||
                             (adminClaim as? Number)?.toInt() == 1

                _isAdmin.value = isAdmin
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking admin status", e)
                _isAdmin.value = false
            }
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
        val activity = context.findActivity()
        if (activity == null) {
            _error.value = "Activity not found"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Hardcoding the ID temporarily to rule out resource issues
                val serverClientId = "47054764584-toogfgpr6rrqu8u23352s0qpk3glpgll.apps.googleusercontent.com"
                Log.d("AuthViewModel", "Requesting sign-in for client: $serverClientId")

                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    
                    auth.signInWithCredential(firebaseCredential).await()
                    _user.value = auth.currentUser
                    checkAdminStatus()
                } else {
                    _error.value = "Unexpected credential type"
                }
            } catch (e: NoCredentialException) {
                _error.value = "No Google accounts found on this device."
            } catch (e: GetCredentialCancellationException) {
                _error.value = "Sign in was cancelled."
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential Manager Error", e)
                _error.value = "Sign in failed: ${e.message}"
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unknown Auth Error", e)
                _error.value = "An error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
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
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
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

    fun signOut(context: Context) {
        viewModelScope.launch {
            auth.signOut()
            _user.value = null
            _isAdmin.value = false
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
