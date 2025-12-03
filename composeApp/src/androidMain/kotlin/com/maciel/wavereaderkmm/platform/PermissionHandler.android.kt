package com.maciel.wavereaderkmm.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // Handle permission state changes
    LaunchedEffect(permissionState.status) {
        when (permissionState.status) {
            is PermissionStatus.Granted -> {
                onPermissionGranted()
            }
            is PermissionStatus.Denied -> {
                val denied = permissionState.status as PermissionStatus.Denied

                if (!denied.shouldShowRationale) {
                    // First time asking - launch permission request
                    permissionState.launchPermissionRequest()
                } else {
                    // User denied previously - call denied callback
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
     * Check if location permission is granted (for use in Composables)
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