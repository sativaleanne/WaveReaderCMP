package com.maciel.wavereaderkmm.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel

/**
 * iOS implementation using MapKit
 *
 * TODO: Implement when setting up iOS
 * Will use platform.MapKit.MKMapView
 */
@Composable
actual fun MapView(
    locationViewModel: LocationViewModel,
    modifier: Modifier
) {
    val coordinates by locationViewModel.coordinatesState.collectAsState()
    val displayLocation by locationViewModel.displayLocationText.collectAsState()

    // Temporary placeholder until iOS implementation
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "iOS Map (Coming Soon)\n\n$displayLocation",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/*
 * Full iOS MapKit implementation (for when you're ready):
 *
 * @Composable
 * actual fun MapView(
 *     locationViewModel: LocationViewModel,
 *     modifier: Modifier
 * ) {
 *     val coordinates by locationViewModel.coordinatesState.collectAsState()
 *
 *     UIKitView(
 *         factory = {
 *             val mapView = MKMapView()
 *             mapView.delegate = object : NSObject(), MKMapViewDelegateProtocol {
 *                 override fun mapView(
 *                     mapView: MKMapView,
 *                     didSelectAnnotationView: MKAnnotationView
 *                 ) {
 *                     // Handle tap
 *                 }
 *             }
 *             mapView
 *         },
 *         update = { mapView ->
 *             coordinates?.let { location ->
 *                 val coordinate = CLLocationCoordinate2DMake(
 *                     location.latitude,
 *                     location.longitude
 *                 )
 *
 *                 // Add annotation
 *                 val annotation = MKPointAnnotation()
 *                 annotation.coordinate = coordinate
 *                 annotation.title = "Selected Location"
 *                 mapView.addAnnotation(annotation)
 *
 *                 // Center map
 *                 val region = MKCoordinateRegionMakeWithDistance(
 *                     coordinate,
 *                     10000.0, // meters
 *                     10000.0
 *                 )
 *                 mapView.setRegion(region, animated = true)
 *             }
 *         },
 *         modifier = modifier
 *     )
 * }
 */