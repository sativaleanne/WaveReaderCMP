package com.maciel.wavereaderkmm.ui.auth

import androidx.compose.foundation.Image
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

/**
 * Register Screen for new users to start a Firebase account
 *
 * Uses GitLive Firebase SDK with coroutines - works on Android & iOS!
 */
@Composable
fun RegisterScreen(
    auth: FirebaseAuthRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onLoginNavigate: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
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

        Text("Welcome! Register to get Started", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Enter your name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Enter your email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Re-enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                // Validation
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match."
                    return@Button
                }

                if (name.isBlank()) {
                    errorMessage = "Please enter your name."
                    return@Button
                }

                if (email.isBlank()) {
                    errorMessage = "Please enter your email."
                    return@Button
                }

                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                // Create new user with GitLive Firebase
                scope.launch {
                    auth.createUserWithEmail(email, password, name)
                        .onSuccess { user ->
                            // Send email verification (optional, doesn't block success)
                            auth.sendEmailVerification()

                            isLoading = false
                            onSuccess()
                        }
                        .onFailure { error ->
                            isLoading = false
                            errorMessage = error.message ?: "Registration failed"
                        }
                }
            },
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onLoginNavigate() }) {
            Text("Have an account? Login Here")
        }
    }
}
