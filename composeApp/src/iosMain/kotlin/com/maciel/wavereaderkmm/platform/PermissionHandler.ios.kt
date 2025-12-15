package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted

/**
 * iOS implementation of RequestLocationPermission
 *
 * TODO: Implement using CoreLocation
 */
@Composable
actual fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    var permissionStatus by remember {
        mutableStateOf(CLLocationManager.authorizationStatus())
    }

    LaunchedEffect(Unit) {
        when (permissionStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                onPermissionGranted()
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                onPermissionDenied()
            }
            kCLAuthorizationStatusNotDetermined -> {
                val manager = CLLocationManager()
                manager.requestWhenInUseAuthorization()
            }
        }
    }
    content()
}

/**
 * iOS implementation of LocationPermissionChecker
 *
 * TODO: Implement using CLLocationManager.authorizationStatus()
 */
actual object LocationPermissionChecker {
    @Composable
    actual fun isGrantedComposable(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }
}
