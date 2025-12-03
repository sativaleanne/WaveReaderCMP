package com.maciel.wavereaderkmm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WaveDataResponse (
    val latitude: Float,
    val longitude: Float,

    @SerialName("generationtime_ms")
    val generationtimeMS: Double,

    @SerialName("utc_offset_seconds")
    val utcOffsetSeconds: Long,

    val timezone: String,

    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String,

    val elevation: Float,

    @SerialName("current_units")
    val currentUnits: Units? = null,

    @SerialName("current")
    val current: Current? = null,

    @SerialName("hourly_units")
    val hourlyUnits: Units? = null,

    @SerialName("hourly")
    val hourly: Hourly? = null
)

@Serializable
data class Current(
    val time: String? = null,
    val interval: Long? = null,

    @SerialName("wave_height") val waveHeight: Float? = null,
    @SerialName("wave_direction") val waveDirection: Float? = null,
    @SerialName("wave_period") val wavePeriod: Float? = null,

    @SerialName("wind_wave_height") val windWaveHeight: Float? = null,
    @SerialName("wind_wave_direction") val windWaveDirection: Float? = null,
    @SerialName("wind_wave_period") val windWavePeriod: Float? = null,

    @SerialName("swell_wave_height") val swellWaveHeight: Float? = null,
    @SerialName("swell_wave_direction") val swellWaveDirection: Float? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: Float? = null,

    @SerialName("wind_speed") val windSpeed: Float? = null,
    @SerialName("air_temperature") val airTemperature: Float? = null,
    @SerialName("sea_surface_temperature") val seaSurfaceTemp: Float? = null
)

@Serializable
data class Units(
    val time: String,
    val interval: String? = null,

    @SerialName("wave_height") val waveHeight: String? = null,
    @SerialName("wave_direction") val waveDirection: String? = null,
    @SerialName("wave_period") val wavePeriod: String? = null,

    @SerialName("wind_wave_height") val windWaveHeight: String? = null,
    @SerialName("wind_wave_direction") val windWaveDirection: String? = null,
    @SerialName("wind_wave_period") val windWavePeriod: String? = null,

    @SerialName("swell_wave_height") val swellWaveHeight: String? = null,
    @SerialName("swell_wave_direction") val swellWaveDirection: String? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: String? = null,

    @SerialName("wind_speed") val windSpeed: String? = null,
    @SerialName("air_temperature") val airTemperature: String? = null,
    @SerialName("sea_surface_temperature") val seaSurfaceTemp: String? = null
)


@Serializable
data class Hourly (
    val time: List<String> = emptyList(),

    @SerialName("wave_height")
    val waveHeight: List<Float?>? = null,

    @SerialName("wave_direction")
    val waveDirection: List<Float?>? = null,

    @SerialName("wave_period")
    val wavePeriod: List<Float?>? = null,

    @SerialName("wind_wave_height")
    val windWaveHeight: List<Float?>? = null,

    @SerialName("wind_wave_direction")
    val windWaveDirection: List<Float?>? = null,

    @SerialName("wind_wave_period")
    val windWavePeriod: List<Float?>? = null,

    @SerialName("swell_wave_height")
    val swellWaveHeight: List<Float?>? = null,

    @SerialName("swell_wave_direction")
    val swellWaveDirection: List<Float?>? = null,

    @SerialName("swell_wave_period")
    val swellWavePeriod: List<Float?>? = null

)


