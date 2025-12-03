package com.maciel.wavereaderkmm.platform

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android implementation using FusedLocationProviderClient and Geocoder
 */
actual class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder by lazy {
        Geocoder(context, Locale.getDefault())
    }

    /**
     * Get current device location
     * Requires location permissions to be granted
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    actual suspend fun getCurrentLocation(): Result<LocationData> {
        return try {
            val location = fusedLocationClient.lastLocation.await()

            if (location != null) {
                Result.success(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            } else {
                Result.failure(Exception("Location not available"))
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get location: ${e.message}"))
        }
    }

    /**
     * Forward geocoding: address string -> coordinates
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    actual suspend fun geocodeAddress(address: String): Result<GeocodedAddress> {
        return try {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    address,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val firstAddress = addresses.firstOrNull()

                            if (firstAddress != null) {
                                val geocodedAddress = GeocodedAddress(
                                    latitude = firstAddress.latitude,
                                    longitude = firstAddress.longitude,
                                    displayName = formatAddressName(firstAddress),
                                    locality = firstAddress.locality,
                                    adminArea = firstAddress.adminArea
                                )
                                continuation.resume(Result.success(geocodedAddress))
                            } else {
                                continuation.resume(
                                    Result.failure(Exception("Address not found"))
                                )
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(
                                Result.failure(Exception("Geocoding error: $errorMessage"))
                            )
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Geocoding failed: ${e.message}"))
        }
    }

    /**
     * Reverse geocoding: coordinates -> address string
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    actual suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return try {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val address = addresses.firstOrNull()

                            if (address != null) {
                                val displayName = formatAddressName(address)
                                continuation.resume(Result.success(displayName))
                            } else {
                                // Fallback to coordinates
                                val coordString = formatLatLong(latitude, longitude)
                                continuation.resume(Result.success(coordString))
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            // Fallback to coordinates on error
                            val coordString = formatLatLong(latitude, longitude)
                            continuation.resume(Result.success(coordString))
                        }
                    }
                )
            }
        } catch (e: Exception) {
            // Fallback to coordinates
            val coordString = formatLatLong(latitude, longitude)
            Result.success(coordString)
        }
    }

    /**
     * Format address as "City, State" or best available format
     */
    private fun formatAddressName(address: Address): String {
        return listOfNotNull(address.locality, address.adminArea)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: formatLatLong(address.latitude, address.longitude)
    }

    /**
     * Format coordinates as "37.7749°N, 122.4194°W"
     * Uses multiplatform formatting utility
     */
    private fun formatLatLong(lat: Double, lon: Double): String {
        // Use the multiplatform formatting function
        return com.maciel.wavereaderkmm.utils.formatLatLong(lat, lon)
    }
}