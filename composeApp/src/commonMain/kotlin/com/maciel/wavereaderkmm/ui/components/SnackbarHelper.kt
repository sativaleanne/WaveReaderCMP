package com.maciel.wavereaderkmm.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Snackbar Helper
 */
class SnackbarHelper(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    /**
     * Show success message
     */
    fun showSuccess(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "✓ $message",
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Show error message
     */
    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "✗ $message",
                duration = SnackbarDuration.Long
            )
        }
    }

    /**
     * Show info message
     */
    fun showInfo(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Show message with action button
     * Returns true if action was clicked, false if dismissed
     */
    suspend fun showWithAction(
        message: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Long
    ): Boolean {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
        return result == SnackbarResult.ActionPerformed
    }
}

/**
 * Extension function for easy error display
 */
fun SnackbarHelper.showException(exception: Throwable) {
    showError(exception.message ?: "Unknown error")
}