package com.maciel.wavereaderkmm.viewmodels

/**
 * Generic UI state representation
 * Use for async operations - API calls, database queries
 */
sealed interface UiState<out T> {
    /** Initial or loading state */
    data object Loading : UiState<Nothing>

    /** Operation completed successfully */
    data class Success<T>(val data: T) : UiState<T>

    /** Operation failed with error */
    data class Error(
        val message: String? = null,
        val exception: Throwable? = null
    ) : UiState<Nothing>

    /** Empty state - lists/collections */
    data object Empty : UiState<Nothing>
}

/**
 * Helper extensions for working with UiState
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error
fun <T> UiState<T>.isEmpty(): Boolean = this is UiState.Empty

fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

fun <T> UiState<T>.getErrorOrNull(): String? = when (this) {
    is UiState.Error -> message
    else -> null
}