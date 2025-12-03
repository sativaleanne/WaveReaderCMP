package com.maciel.wavereaderkmm.platform

/**
 * Location data result
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double
)

/**
 * Geocoded address result
 */
data class GeocodedAddress(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,  // e.g., "San Francisco, CA"
    val locality: String?,    // City name
    val adminArea: String?    // State/Province
)

/**
 * Platform-specific location service
 *
 * Android: Uses FusedLocationProviderClient + Geocoder
 * iOS: Uses CoreLocation + CLGeocoder
 */
expect class LocationService {
    /**
     * Get current device location
     * @return Result with LocationData or error
     */
    suspend fun getCurrentLocation(): Result<LocationData>

    /**
     * Convert address string to coordinates (forward geocoding)
     *
     * @param address Address string to geocode
     * @return Result with GeocodedAddress or error
     */
    suspend fun geocodeAddress(address: String): Result<GeocodedAddress>

    /**
     * Convert coordinates to address string (reverse geocoding)
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result with address string or error
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String>
}