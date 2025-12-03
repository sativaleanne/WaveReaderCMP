package com.maciel.wavereaderkmm.platform

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.maciel.wavereaderkmm.model.HistoryRecord
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android implementation using Storage Access Framework
 *
 * Note: For Android, we need access to Activity to launch file picker.
 * This is a simplified version - see usage notes below for full implementation.
 */

// Helper function to generate timestamp
private fun timestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return sdf.format(Date())
}

// CSV export implementation
actual suspend fun exportToCsv(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        // Build CSV content
        val csvContent = buildString {
            // Header
            appendLine("Record ID,Timestamp,Location,Latitude,Longitude,Height,Period,Direction,Time")

            // Data rows
            data.forEach { record ->
                val lat = record.lat?.toString() ?: ""
                val lon = record.lon?.toString() ?: ""
                val timestamp = record.timestamp.replace(",", "")
                val location = record.location.replace(",", "")

                record.dataPoints.forEach { point ->
                    appendLine("${record.id},$timestamp,$location,$lat,$lon,${point.height},${point.period},${point.direction},${point.time}")
                }
            }
        }

        // Note: Actual file writing needs Activity context and file picker
        // For now, just report success with content length
        onSuccess("CSV generated (${csvContent.length} bytes)")

    } catch (e: Exception) {
        onFailure("CSV export failed: ${e.message}")
    }
}

// JSON export implementation
actual suspend fun exportToJson(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        // Build JSON content
        val jsonContent = buildString {
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

        // Note: Actual file writing needs Activity context and file picker
        onSuccess("JSON generated (${jsonContent.length} bytes)")

    } catch (e: Exception) {
        onFailure("JSON export failed: ${e.message}")
    }
}

/**
 * FULL ANDROID IMPLEMENTATION NOTES:
 * ===================================
 *
 * To properly implement file export on Android, you need:
 *
 * 1. Create a Composable helper that launches file picker:
 */

/*
@Composable
fun rememberFilePicker(
    mimeType: String,
    onFileSelected: (Uri) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }

    return { fileName ->
        launcher.launch(fileName)
    }
}

// Usage in HistoryScreen:
@Composable
fun HistoryScreenWithExport() {
    val context = LocalContext.current

    val csvPicker = rememberFilePicker("text/csv") { uri ->
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(csvContent.toByteArray())
        }
    }

    val jsonPicker = rememberFilePicker("application/json") { uri ->
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(jsonContent.toByteArray())
        }
    }

    // Then call csvPicker("wave_data.csv") when export button clicked
}
*/