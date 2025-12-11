package com.maciel.wavereaderkmm.model

data class AuthResult(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean
)

data class FirestoreResult(
    val success: Boolean,
    val documentId: String? = null,
    val error: String? = null
)
