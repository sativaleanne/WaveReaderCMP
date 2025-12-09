package com.maciel.wavereaderkmm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPointAnnotation
import platform.darwin.NSObject

/**
 * iOS implementation using MapKit
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    locationViewModel: LocationViewModel,
    modifier: Modifier
) {
    val coordinates by locationViewModel.coordinatesState.collectAsState()

    UIKitView(
        factory = {
            val mapView = MKMapView()
            mapView.delegate = object : NSObject(), MKMapViewDelegateProtocol {
                override fun mapView(
                    mapView: MKMapView,
                    didSelectAnnotationView: MKAnnotationView
                ) {
                    // Handle tap
                }
            }
            mapView
        },
        update = { mapView ->
            coordinates?.let { location ->
                val coordinate = CLLocationCoordinate2DMake(
                    location.latitude,
                    location.longitude
                )

                // Add annotation
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(coordinate)
                annotation.setTitle("Selected Location")
                mapView.addAnnotation(annotation)

                // Center map
                val region = MKCoordinateRegionMakeWithDistance(
                    coordinate,
                    10000.0, // meters
                    10000.0
                )
                mapView.setRegion(region, animated = true)
            }
        },
        modifier = modifier
    )
}