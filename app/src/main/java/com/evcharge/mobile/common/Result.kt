package com.evcharge.mobile.common

/**
 * A generic class that holds a value or an error
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Returns true if this Result represents a successful outcome
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Returns true if this Result represents an error outcome
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Returns true if this Result represents a loading state
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

/**
 * Returns the data if this is a Success, null otherwise
 */
fun <T> Result<T>.getDataOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Returns the exception if this is an Error, null otherwise
 */
fun <T> Result<T>.getErrorOrNull(): Throwable? = when (this) {
    is Result.Error -> exception
    else -> null
}

/**
 * Maps the data if this is a Success, otherwise returns the same Result type
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}

/**
 * Maps the data if this is a Success, otherwise returns the same Result type
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}
