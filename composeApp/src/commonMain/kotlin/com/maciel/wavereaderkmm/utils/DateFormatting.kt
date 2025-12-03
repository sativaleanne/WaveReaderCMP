package com.maciel.wavereaderkmm.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Date and time formatting utilities
 */

/**
 * Format ISO 8601 datetime string to hour only
 *
 * @param isoString ISO 8601 formatted datetime string
 * @return Hour as string (0-23)
 */
@OptIn(ExperimentalTime::class)
fun formatTime(isoString: String): String {
    return try {
        // Parse ISO 8601 string
        val instant = Instant.parse(isoString)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        // Return hour as string
        dateTime.hour.toString()
    } catch (e: Exception) {
        // Fallback: try to parse as LocalDateTime directly
        try {
            val dateTime = LocalDateTime.parse(isoString)
            dateTime.hour.toString()
        } catch (e2: Exception) {
            // Last resort: return the original string
            isoString
        }
    }
}

/**
 * Format ISO 8601 datetime string to hour:minute
 *
 * @param isoString ISO 8601 formatted datetime string
 * @return Time as string (HH:mm)
 */
@OptIn(ExperimentalTime::class)
fun formatTimeWithMinutes(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')

        "$hour:$minute"
    } catch (e: Exception) {
        try {
            val dateTime = LocalDateTime.parse(isoString)
            val hour = dateTime.hour.toString().padStart(2, '0')
            val minute = dateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        } catch (e2: Exception) {
            isoString
        }
    }
}

/**
 * Format milliseconds timestamp to date string
 *
 * @param millis Milliseconds since epoch
 * @return Formatted date string (MM/dd/yyyy)
 */
@OptIn(ExperimentalTime::class)
fun formatDate(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val year = dateTime.year

    return "$month/$day/$year"
}

/**
 * Format milliseconds timestamp to date and time string
 *
 * @param millis Milliseconds since epoch
 * @return Formatted datetime string (MMM dd, yyyy HH:mm)
 */
@OptIn(ExperimentalTime::class)
fun formatDateTime(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val month = monthNames[dateTime.monthNumber - 1]
    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val year = dateTime.year
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')

    return "$month $day, $year $hour:$minute"
}

/**
 * Get current timestamp in milliseconds
 *
 * @return Current time in milliseconds since epoch
 */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

/**
 * Generate timestamp string for file names
 *
 * @return Timestamp string (yyyyMMdd_HHmmss)
 */
@OptIn(ExperimentalTime::class)
fun generateFileTimestamp(): String {
    val instant = Clock.System.now()
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val year = dateTime.year
    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    val second = dateTime.second.toString().padStart(2, '0')

    return "${year}${month}${day}_${hour}${minute}${second}"
}