package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable

@Composable
expect fun RequestLocationPermission(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    content: @Composable () -> Unit
)

expect object LocationPermissionChecker {
    @Composable
    fun isGrantedComposable(): Boolean
}