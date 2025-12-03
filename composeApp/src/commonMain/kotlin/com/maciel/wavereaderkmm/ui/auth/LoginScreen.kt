package com.maciel.wavereaderkmm.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciel.wavereaderkmm.data.FirebaseAuthRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import wavereaderkmm.composeapp.generated.resources.Res
import wavereaderkmm.composeapp.generated.resources.wavereadericoncropped

/*
* Login Screen for returning users with active firebase account
 */
@Composable
fun LoginScreen(
    auth: FirebaseAuthRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onRegisterNavigate: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Image(
            painter = painterResource(Res.drawable.wavereadericoncropped),
            contentScale = ContentScale.Crop,
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Welcome Back!", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null  // Clear errors when typing
                resetMessage = null
            },
            label = { Text("Enter your email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
                resetMessage = null
            },
            label = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Forgot Password?",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable(enabled = !isLoading) {
                    if (email.isNotBlank()) {
                        scope.launch {
                            auth.sendPasswordResetEmail(email)
                                .onSuccess {
                                    resetMessage = "Reset email sent! Check your inbox."
                                    errorMessage = null
                                }
                                .onFailure { error ->
                                    errorMessage = error.message ?: "Failed to send reset email"
                                    resetMessage = null
                                }
                        }
                    } else {
                        errorMessage = "Please enter your email first."
                        resetMessage = null
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Success message for password reset
        if (resetMessage != null) {
            Text(
                text = resetMessage ?: "",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                // Validation
                when {
                    email.isBlank() -> {
                        errorMessage = "Please enter your email."
                        return@Button
                    }
                    password.isBlank() -> {
                        errorMessage = "Please enter your password."
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = null
                resetMessage = null

                scope.launch {
                    auth.signInWithEmail(email, password)
                        .onSuccess { user ->
                            isLoading = false
                            onSuccess()
                        }
                        .onFailure { error ->
                            isLoading = false
                            errorMessage = error.message ?: "Login failed"
                        }
                }
            },
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onRegisterNavigate() },
            enabled = !isLoading
        ) {
            Text("Don't have an account? Register Here")
        }
    }
}