package io.github.langstudy.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.langstudy.ui.theme.SuccessGreen

@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Email,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Verify your email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "A verification email has been sent to ${user?.email}. Please check your inbox and click the link to verify your account.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.reloadUser() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("I've Verified")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = { viewModel.sendEmailVerification() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Resend Email")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }

        error?.let {
            val isSuccess = it.contains("verified", ignoreCase = true) || it.contains("sent", ignoreCase = true)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
