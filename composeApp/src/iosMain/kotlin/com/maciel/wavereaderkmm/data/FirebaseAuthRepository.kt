package com.maciel.wavereaderkmm.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import platform.Foundation.*
import kotlinx.cinterop.*

actual class FirebaseAuthRepository {

    actual val userId: String?
        get() = null  // TODO: Implement after iOS build

    actual val isSignedIn: Boolean
        get() = false  // TODO: Implement after iOS build

    actual suspend fun signInWithEmail(email: String, password: String): Result<User> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }

    actual suspend fun createUserWithEmail(
        email: String,
        password: String,
        displayName: String?
    ): Result<User> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }

    actual suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }

    actual suspend fun sendEmailVerification(): Result<Unit> {
        // TODO: Implement after iOS build
        return Result.failure(Exception("Not implemented yet"))
    }

    actual suspend fun signOut() {
        // TODO: Implement after iOS build
    }
}