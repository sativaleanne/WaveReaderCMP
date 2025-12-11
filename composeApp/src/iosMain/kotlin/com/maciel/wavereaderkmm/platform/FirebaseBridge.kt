package com.maciel.wavereaderkmm.platform

import com.maciel.wavereaderkmm.model.AuthResult
import com.maciel.wavereaderkmm.model.FirestoreResult

// Auth callbacks
lateinit var firebaseGetCurrentUserId: () -> String?

lateinit var firebaseSignInWithEmail: (
    email: String,
    password: String,
    completion: (AuthResult?, String?) -> Unit
) -> Unit

lateinit var firebaseCreateUserWithEmail: (
    email: String,
    password: String,
    displayName: String?,
    completion: (AuthResult?, String?) -> Unit
) -> Unit

lateinit var firebaseSendPasswordReset: (
    email: String,
    completion: (String?) -> Unit
) -> Unit
lateinit var firebaseSendEmailVerification: (
    completion: (String?) -> Unit
) -> Unit

lateinit var firebaseSignOut: () -> Unit

// Firestore callbacks
lateinit var firestoreSaveSession: (
    userId: String,
    locationName: String,
    latitude: Double,
    longitude: Double,
    dataPoints: List<Map<String, Any>>,
    completion: (FirestoreResult) -> Unit
) -> Unit

lateinit var firestoreFetchHistory: (
    userId: String,
    sortDescending: Boolean,
    startDateMillis: Long?,
    endDateMillis: Long?,
    completion: (List<Map<String, Any>>?, String?) -> Unit
) -> Unit

lateinit var firestoreDeleteSession: (
    userId: String,
    sessionId: String,
    completion: (FirestoreResult) -> Unit
) -> Unit
