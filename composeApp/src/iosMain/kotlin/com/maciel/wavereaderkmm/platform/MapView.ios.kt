package com.maciel.wavereaderkmm.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.maciel.wavereaderkmm.viewmodels.LocationViewModel
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject

/**
 * iOS implementation using MapKit embedded via UIKitView.
 *
 * - Blue dot for user location
 * - Marker at selected position
 * - Tap anywhere to call locationViewModel.setLocation()
 * - Camera animates to new coordinate
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapView(
    locationViewModel: LocationViewModel,
    coordinates: LocationData?,
    modifier: Modifier
) {

    val tapHandler = remember { MapTapHandler(locationViewModel) }

    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = {
            MKMapView().apply {
                setTranslatesAutoresizingMaskIntoConstraints(false)
                setShowsUserLocation(true)
                setZoomEnabled(true)
                setScrollEnabled(true)
                setPitchEnabled(false)
                setRotateEnabled(false)

                tapHandler.attach(this)
            }
        },
        update = { mapView ->
            coordinates?.let { location ->
                val targetCoordinate = CLLocationCoordinate2DMake(
                    location.latitude,
                    location.longitude
                )

                val existing = mapView.annotations
                    .filterIsInstance<MKPointAnnotation>()
                mapView.removeAnnotations(existing)

                val annotation = MKPointAnnotation().apply {
                    setCoordinate(targetCoordinate)
                    setTitle("Selected Location")
                    setSubtitle("${location.latitude}, ${location.longitude}")
                }
                mapView.addAnnotation(annotation)

                // Animate camera
                val region = MKCoordinateRegionMakeWithDistance(
                    targetCoordinate,
                    10_000.0,
                    10_000.0
                )
                mapView.setRegion(region, animated = true)
            }
        }
    )
}

/**
 * Handles tap-to-select coordinate conversion.
 */
@OptIn(ExperimentalForeignApi::class)
class MapTapHandler(
    private val locationViewModel: LocationViewModel
) : NSObject() {

    private var mapView: MKMapView? = null

    fun attach(mapView: MKMapView) {
        this.mapView = mapView
        val recognizer = UITapGestureRecognizer(
            target = this,
            action = platform.objc.sel_registerName("handleTap:")
        )
        mapView.addGestureRecognizer(recognizer)
    }

    /**
     * @ObjCAction exposes this as an Obj-C selector ("handleTap:").
     * Calls locationViewModel.setLocation()
     * onMapClick calls
     */
    @OptIn(BetaInteropApi::class)
    @Suppress("unused")
    @ObjCAction
    fun handleTap(recognizer: UITapGestureRecognizer) {
        val mapView = mapView ?: return
        val point = recognizer.locationInView(mapView)
        val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)

        coordinate.useContents {
            locationViewModel.setLocation(
                latitude = latitude,
                longitude = longitude
            )
        }
    }
}
