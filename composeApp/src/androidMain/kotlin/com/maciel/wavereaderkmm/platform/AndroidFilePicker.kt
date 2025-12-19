package com.maciel.wavereaderkmm.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android-specific file picker implementation
 */

/**
 * File picker configuration for different export types
 */
data class ExportLauncher(
    val launchCsvExport: (filename: String) -> Unit,
    val launchJsonExport: (filename: String) -> Unit
)

/**
 * Creates and remembers file picker launchers for exporting data
 *
 * @param onCsvExport Called when user selects location for CSV file
 * @param onJsonExport Called when user selects location for JSON file
 * @return ExportLauncher with functions to trigger file pickers
 */
@Composable
fun rememberExportLauncher(
    onCsvExport: (Uri) -> Unit,
    onJsonExport: (Uri) -> Unit
): ExportLauncher {
    val context = LocalContext.current

    // CSV file picker launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { onCsvExport(it) }
    }

    // JSON file picker launcher
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onJsonExport(it) }
    }

    // Return a stable object with launch functions
    return remember(csvLauncher, jsonLauncher) {
        ExportLauncher(
            launchCsvExport = { filename -> csvLauncher.launch(filename) },
            launchJsonExport = { filename -> jsonLauncher.launch(filename) }
        )
    }
}

/**
 * Helper function to generate timestamped filenames
 */
fun generateExportFilename(prefix: String = "wave_data", extension: String): String {
    val timestamp = java.text.SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        java.util.Locale.getDefault()
    ).format(java.util.Date())
    return "${prefix}_${timestamp}.$extension"
}