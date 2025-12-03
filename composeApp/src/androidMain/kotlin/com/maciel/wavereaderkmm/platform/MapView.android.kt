package com.maciel.wavereaderkmm.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel

/**
 * Android implementation using Google Maps Compose
 *
 * Features:
 * - Interactive Google Maps
 * - Shows user's current location (blue dot)
 * - Marker at selected position
 * - Tap to select location
 * - Camera animation
 * - Zoom controls
 *
 * Requirements:
 * - Google Maps API key in AndroidManifest.xml
 * - Location permissions (handled by RequestLocationPermission)
 * - Maps Compose dependency
 */
@Composable
actual fun MapView(
    locationViewModel: LocationViewModel,
    modifier: Modifier
) {
    val coordinates by locationViewModel.coordinatesState.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val markerState = rememberMarkerState(position = LatLng(0.0, 0.0))

    // Check if permission is granted
    val isPermissionGranted = LocationPermissionChecker.isGrantedComposable()

    // Request location permission and fetch initial location
    RequestLocationPermission(
        onPermissionGranted = {
            // Permission granted - fetch location will happen in LaunchedEffect
        },
        onPermissionDenied = {
            // Permission denied - map still works, just no blue dot
        }
    ) {
        // Fetch user location when permission is granted
        LaunchedEffect(isPermissionGranted) {
            if (isPermissionGranted) {
                locationViewModel.fetchUserLocation()
            }
        }

        // Animate camera to selected location
        LaunchedEffect(coordinates) {
            coordinates?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                markerState.position = latLng
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 12f)
                )
            }
        }

        // Google Maps
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = isPermissionGranted  // Only show blue dot if permitted
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,  // +/- buttons
                myLocationButtonEnabled = isPermissionGranted,  // "Go to my location" button
                compassEnabled = true,  // Compass when rotated
                scrollGesturesEnabled = true,  // Pan
                zoomGesturesEnabled = true,  // Pinch to zoom
                tiltGesturesEnabled = false,  // 3D tilt (disabled)
                rotationGesturesEnabled = false  // Rotation (disabled)
            ),
            onMapClick = { latLng ->
                // User tapped on map - update selected location
                locationViewModel.setLocationFromMap(
                    lat = latLng.latitude,
                    lon = latLng.longitude
                )
            }
        ) {
            // Marker at selected location
            coordinates?.let { location ->
                Marker(
                    state = markerState,
                    title = "Selected Location",
                    snippet = "${location.latitude}, ${location.longitude}"
                )
            }
        }
    }
}