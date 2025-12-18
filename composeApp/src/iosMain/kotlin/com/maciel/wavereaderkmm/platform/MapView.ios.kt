package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

/**
 * iOS implementation using MapKit
 *
 * Features:
 * - Interactive MapKit map
 * - Shows user's current location (blue dot via showsUserLocation)
 * - Marker at selected position
 * - Tap to select location
 * - Camera animation
 *
 * Requirements:
 * - Location permissions (NSLocationWhenInUseUsageDescription in Info.plist)
 * - MapKit framework (automatically available on iOS)
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    locationViewModel: LocationViewModel,
    coordinates: LocationData?,
    modifier: Modifier
) {
    val currentAnnotation = remember { mutableListOf<MKPointAnnotation>() }

    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().apply {
                setShowsUserLocation(true)
                setZoomEnabled(true)
                setScrollEnabled(true)
                setPitchEnabled(false)
                setRotateEnabled(false)
            }
        },
        update = { mapView ->
            coordinates?.let { location ->
                val coordinate = CLLocationCoordinate2DMake(
                    location.latitude,
                    location.longitude
                )

                // Remove old annotations
                if (currentAnnotation.isNotEmpty()) {
                    mapView.removeAnnotations(currentAnnotation)
                    currentAnnotation.clear()
                }

                // Add new annotation
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(coordinate)
                annotation.setTitle("Selected Location")

                currentAnnotation.add(annotation)
                mapView.addAnnotation(annotation)

                // Animate camera
                val region = MKCoordinateRegionMakeWithDistance(
                    coordinate,
                    10000.0,
                    10000.0
                )
                mapView.setRegion(region, animated = true)
            }
        }
    )
}