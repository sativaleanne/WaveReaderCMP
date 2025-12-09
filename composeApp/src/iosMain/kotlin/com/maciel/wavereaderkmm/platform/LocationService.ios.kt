package com.maciel.wavereaderkmm.platform

import platform.CoreLocation.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlin.coroutines.resume

/**
 * iOS implementation using CoreLocation
 *
 * Provides location services, geocoding, and reverse geocoding for iOS
 *
 * FIXED: Matches exact GeocodedAddress structure with displayName field
 */
@OptIn(ExperimentalForeignApi::class)
actual class LocationService {

    private val locationManager = CLLocationManager()
    private val geocoder = CLGeocoder()
    private var locationDelegate: LocationDelegate? = null

    /**
     * Get current device location
     * Uses CLLocationManager with delegate pattern
     */
    actual suspend fun getCurrentLocation(): Result<LocationData> {
        // Check authorization status
        val authStatus = CLLocationManager.authorizationStatus()

        // Request permission if not determined
        if (authStatus == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
            // Note: On iOS, permission dialog is async, but we'll try anyway
        }

        // Check if we have permission
        val hasPermission = authStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                authStatus == kCLAuthorizationStatusAuthorizedAlways

        if (!hasPermission && authStatus != kCLAuthorizationStatusNotDetermined) {
            return Result.failure(Exception("Location permission denied"))
        }

        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationDelegate(
                onSuccess = { location ->
                    location.coordinate.useContents {
                        val locationData = LocationData(
                            latitude = latitude,
                            longitude = longitude
                        )
                        continuation.resume(Result.success(locationData))
                    }
                },
                onFailure = { error ->
                    continuation.resume(
                        Result.failure(Exception(error.localizedDescription))
                    )
                }
            )

            locationDelegate = delegate
            locationManager.delegate = delegate
            locationManager.requestLocation()

            // Cleanup on cancellation
            continuation.invokeOnCancellation {
                locationManager.stopUpdatingLocation()
                locationDelegate = null
            }
        }
    }

    /**
     * Forward geocoding: address string -> coordinates
     * Uses CLGeocoder
     */
    actual suspend fun geocodeAddress(address: String): Result<GeocodedAddress> {
        if (address.isBlank()) {
            return Result.failure(Exception("Address cannot be empty"))
        }

        return suspendCancellableCoroutine { continuation ->
            geocoder.geocodeAddressString(address) { placemarks, error ->
                when {
                    error != null -> {
                        continuation.resume(
                            Result.failure(Exception(error.localizedDescription))
                        )
                    }

                    placemarks.isNullOrEmpty() -> {
                        continuation.resume(
                            Result.failure(Exception("No results found for: $address"))
                        )
                    }

                    else -> {
                        val placemark = placemarks.first() as CLPlacemark
                        placemark.location?.coordinate?.useContents {
                            val geocodedAddress = GeocodedAddress(
                                latitude = latitude,
                                longitude = longitude,
                                displayName = formatPlacemark(placemark),
                                locality = placemark.locality,
                                adminArea = placemark.administrativeArea
                            )
                            continuation.resume(Result.success(geocodedAddress))
                        } ?: run {
                            continuation.resume(
                                Result.failure(Exception("No coordinates found"))
                            )
                        }
                    }
                }
            }

            // Cleanup on cancellation
            continuation.invokeOnCancellation {
                geocoder.cancelGeocode()
            }
        }
    }

    /**
     * Reverse geocoding: coordinates -> address string
     * Uses CLGeocoder
     */
    actual suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<String> {
        val location = CLLocation(
            latitude = latitude,
            longitude = longitude
        )

        return suspendCancellableCoroutine { continuation ->
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                when {
                    error != null -> {
                        // Fall back to formatted coordinates on error
                        continuation.resume(
                            Result.success(formatLatLong(latitude, longitude))
                        )
                    }

                    placemarks.isNullOrEmpty() -> {
                        continuation.resume(
                            Result.success(formatLatLong(latitude, longitude))
                        )
                    }

                    else -> {
                        val placemark = placemarks.first() as CLPlacemark
                        val formattedAddress = formatPlacemark(placemark)
                        continuation.resume(Result.success(formattedAddress))
                    }
                }
            }

            // Cleanup on cancellation
            continuation.invokeOnCancellation {
                geocoder.cancelGeocode()
            }
        }
    }

    /**
     * Format placemark into readable address string
     * Returns "City, State" format
     */
    private fun formatPlacemark(placemark: CLPlacemark): String {
        val components = listOfNotNull(
            placemark.locality,           // City (e.g., "Eugene")
            placemark.administrativeArea  // State/Province (e.g., "Oregon")
        )

        return if (components.isNotEmpty()) {
            components.joinToString(", ")  // "Eugene, Oregon"
        } else {
            // Fall back to coordinates if no address components
            placemark.location?.coordinate?.useContents {
                formatLatLong(latitude, longitude)
            } ?: "Unknown Location"
        }
    }

    /**
     * Format coordinates as string (uses multiplatform utility)
     * Returns format like "44.0521°N, 123.0868°W"
     */
    private fun formatLatLong(lat: Double, lon: Double): String {
        return com.maciel.wavereaderkmm.utils.formatLatLong(lat, lon)
    }
}

/**
 * CLLocationManager delegate implementation
 * Handles location updates and errors
 */
@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate(
    private val onSuccess: (CLLocation) -> Unit,
    private val onFailure: (NSError) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            onSuccess(location)
            manager.stopUpdatingLocation()
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        onFailure(didFailWithError)
        manager.stopUpdatingLocation()
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus
    ) {
        // Handle authorization status changes if needed
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                // Permission granted, location request will proceed
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                onFailure(
                    NSError.errorWithDomain(
                        domain = "LocationService",
                        code = -1,
                        userInfo = null
                    )
                )
            }
            else -> {
                // kCLAuthorizationStatusNotDetermined - wait for user response
            }
        }
    }
}