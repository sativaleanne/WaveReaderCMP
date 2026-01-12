package com.maciel.wavereaderkmm.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Android implementation using Accompanist Permissions
 * Automatically shows permission dialog when needed
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var hasRequestedPermission by remember { mutableStateOf(false) }

    // Handle permission state changes
    LaunchedEffect(permissionState.status) {
        println("DEBUG PermissionHandler: Status = ${permissionState.status}")
        when (permissionState.status) {
            is PermissionStatus.Granted -> {
                println("DEBUG PermissionHandler: Permission GRANTED")
                onPermissionGranted()
            }
            is PermissionStatus.Denied -> {
                if (!hasRequestedPermission) {
                    // First time in this composable - request permission
                    println("DEBUG PermissionHandler: Launching permission request")
                    hasRequestedPermission = true
                    permissionState.launchPermissionRequest()
                } else {
                    // We already requested and user denied
                    println("DEBUG PermissionHandler: User denied permission")
                    onPermissionDenied()
                }
            }
        }
    }

    // Always show content (map works without permission, just no blue dot)
    content()
}

/**
 * Android implementation of LocationPermissionChecker
 */
actual object LocationPermissionChecker {
    /**
     * Check if location permission is granted
     */
    @Composable
    actual fun isGrantedComposable(): Boolean {
        val context = LocalContext.current
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}