package com.maciel.wavereaderkmm.platform

import com.maciel.wavereaderkmm.model.HistoryRecord


/**
 * Platform-specific data export
 *
 * Android: Uses Storage Access Framework (SAF)
 * iOS: Uses UIDocumentPickerViewController
 * Desktop: Uses file chooser dialog
 * Web: Downloads file
 */

/**
 * Export history data to CSV format
 *
 * @param data List of history records to export
 * @param onSuccess Called when export succeeds with file path/name
 * @param onFailure Called when export fails with error message
 */
expect suspend fun exportToCsv(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
)

/**
 * Export history data to JSON format
 *
 * @param data List of history records to export
 * @param onSuccess Called when export succeeds with file path/name
 * @param onFailure Called when export fails with error message
 */
expect suspend fun exportToJson(
    data: List<HistoryRecord>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
)