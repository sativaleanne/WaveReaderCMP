package com.maciel.wavereaderkmm.platform

import android.content.Context
import android.net.Uri
import com.maciel.wavereaderkmm.model.HistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android implementation with Storage Access Framework support
 */

// Store context reference (set from MainActivity)
private var appContext: Context? = null

fun initializeExport(context: Context) {
    appContext = context.applicationContext
}

private fun timestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * Build CSV content from history records
 *
 * Separated into its own function so it can be reused by both export methods.
 */
fun buildCsvContent(data: List<HistoryRecord>): String {
    return buildString {
        appendLine("Record ID,Timestamp,Location,Latitude,Longitude,Height,Period,Direction,Time")

        data.forEach { record ->
            val lat = record.lat?.toString() ?: ""
            val lon = record.lon?.toString() ?: ""
            val timestamp = record.timestamp.replace(",", ";")
            val location = record.location.replace(",", ";")

            record.dataPoints.forEach { point ->
                appendLine("${record.id},$timestamp,$location,$lat,$lon,${point.height},${point.period},${point.direction},${point.time}")
            }
        }
    }
}

/**
 * Build JSON content from history records
 *
 * Separated into its own function so it can be reused by both export methods.
 */
fun buildJsonContent(data: List<HistoryRecord>): String {
    return buildString {
        appendLine("[")
        data.forEachIndexed { recordIndex, record ->
            record.dataPoints.forEachIndexed { pointIndex, point ->
                appendLine("  {")
                appendLine("    \"recordId\": \"${record.id}\",")
                appendLine("    \"timestamp\": \"${record.timestamp}\",")
                appendLine("    \"location\": \"${record.location}\",")
                if (record.lat != null) appendLine("    \"lat\": ${record.lat},")
                if (record.lon != null) appendLine("    \"lon\": ${record.lon},")
                appendLine("    \"height\": ${point.height},")
                appendLine("    \"period\": ${point.period},")
                appendLine("    \"direction\": ${point.direction},")
                appendLine("    \"time\": ${point.time}")

                val isLast = (recordIndex == data.lastIndex && pointIndex == record.dataPoints.lastIndex)
                appendLine(if (isLast) "  }" else "  },")
            }
        }
        appendLine("]")
    }
}

/**
 */
actual suspend fun exportToCsv(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val csvContent = buildCsvContent(data)

        val context = appContext ?: throw IllegalStateException("Context not initialized - call initializeExport() in MainActivity")
        val fileName = "wave_export_${timestamp()}.csv"
        val file = java.io.File(context.cacheDir, fileName)

        file.writeText(csvContent)
        onSuccess("Exported to cache: $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}

/**
 */
actual suspend fun exportToJson(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val jsonContent = buildJsonContent(data)

        val context = appContext ?: throw IllegalStateException("Context not initialized")
        val fileName = "wave_export_${timestamp()}.json"
        val file = java.io.File(context.cacheDir, fileName)

        file.writeText(jsonContent)
        onSuccess("Exported to cache: $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}

/**
 * NEW: Export CSV to user-selected location via SAF
 *
 * Call this from the URI callback in your launcher.
 *
 * USAGE:
 * ```
 * val csvLauncher = rememberLauncherForActivityResult(...) { uri ->
 *     uri?.let {
 *         exportToCsvWithUri(context, it, records,
 *             onSuccess = { msg -> showSuccess(msg) },
 *             onFailure = { err -> showError(err) }
 *         )
 *     }
 * }
 * ```
 */
fun exportToCsvWithUri(
    context: Context,
    uri: Uri,
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val csvContent = buildCsvContent(data)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(csvContent.toByteArray())
        } ?: throw IllegalStateException("Could not open output stream")

        // Extract filename from URI for success message
        val fileName = uri.lastPathSegment ?: "file"
        onSuccess("Exported ${data.size} records to $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}

/**
 * NEW: Export JSON to user-selected location via SAF
 */
fun exportToJsonWithUri(
    context: Context,
    uri: Uri,
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val jsonContent = buildJsonContent(data)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonContent.toByteArray())
        } ?: throw IllegalStateException("Could not open output stream")

        val fileName = uri.lastPathSegment ?: "file"
        onSuccess("Exported ${data.size} records to $fileName")

    } catch (e: Exception) {
        onFailure("Export failed: ${e.message}")
    }
}