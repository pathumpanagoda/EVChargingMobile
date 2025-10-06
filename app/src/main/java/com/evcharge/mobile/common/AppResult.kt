package com.evcharge.mobile.common

sealed class AppResult<out T> {
    data class Ok<T>(val data: T): AppResult<T>()
    data class Err(val error: Throwable): AppResult<Nothing>()
}

fun <T> AppResult<T>.isSuccess(): Boolean = this is AppResult.Ok<T>
fun <T> AppResult<T>.getDataOrNull(): T? = (this as? AppResult.Ok<T>)?.data
fun <T> AppResult<T>.getErrorOrNull(): Throwable? = (this as? AppResult.Err)?.error

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Ok) block(this.data); return this
}
inline fun <T> AppResult<T>.onFailure(block: (Throwable) -> Unit): AppResult<T> {
    if (this is AppResult.Err) block(this.error); return this
}
