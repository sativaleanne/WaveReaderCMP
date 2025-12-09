package com.maciel.wavereaderkmm.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

actual class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()

    actual val userId: String?
        get() = auth.currentUser?.uid

    actual val isSignedIn: Boolean
        get() = auth.currentUser != null

    actual suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("No user"))

            Result.success(User(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                isEmailVerified = firebaseUser.isEmailVerified
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun createUserWithEmail(
        email: String,
        password: String,
        displayName: String?
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("No user"))

            // Update display name if provided
            displayName?.let {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(it)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            Result.success(User(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = displayName ?: firebaseUser.displayName,
                isEmailVerified = firebaseUser.isEmailVerified
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun signOut() {
        auth.signOut()
    }
}