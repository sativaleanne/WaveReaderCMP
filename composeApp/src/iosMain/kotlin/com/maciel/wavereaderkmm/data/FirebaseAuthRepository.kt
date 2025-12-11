package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.platform.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FirebaseAuthRepository {

    actual val userId: String?
        get() = try {
            firebaseGetCurrentUserId()
        } catch (e: Exception) {
            null
        }

    actual val isSignedIn: Boolean
        get() = userId != null

    actual suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            firebaseSignInWithEmail(email, password) { authResult, error ->
                when {
                    error != null -> {
                        continuation.resume(Result.failure(Exception(error)))
                    }
                    authResult != null -> {
                        continuation.resume(Result.success(User(
                            uid = authResult.uid,
                            email = authResult.email,
                            displayName = authResult.displayName,
                            isEmailVerified = authResult.isEmailVerified
                        )))
                    }
                    else -> {
                        continuation.resume(Result.failure(Exception("Unknown error")))
                    }
                }
            }
        }
    }

    actual suspend fun createUserWithEmail(
        email: String,
        password: String,
        displayName: String?
    ): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            firebaseCreateUserWithEmail(email, password, displayName) { authResult, error ->
                when {
                    error != null -> {
                        continuation.resume(Result.failure(Exception(error)))
                    }
                    authResult != null -> {
                        continuation.resume(Result.success(User(
                            uid = authResult.uid,
                            email = authResult.email,
                            displayName = authResult.displayName,
                            isEmailVerified = authResult.isEmailVerified
                        )))
                    }
                    else -> {
                        continuation.resume(Result.failure(Exception("Unknown error")))
                    }
                }
            }
        }
    }

    actual suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            firebaseSendPasswordReset(email) { error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error)))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }
        }
    }

    actual suspend fun sendEmailVerification(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            firebaseSendEmailVerification { error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error)))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }
        }
    }

    actual suspend fun signOut() {
        try {
            firebaseSignOut()
        } catch (e: Exception) {
            println("Error signing out: ${e.message}")
        }
    }
}