package com.maciel.wavereaderkmm.data

expect class FirebaseAuthRepository() {
    val userId: String?
    val isSignedIn: Boolean

    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun createUserWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun signOut()
}

// Simple user model (no Firebase dependency)
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean
)