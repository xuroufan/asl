package com.hackfuture.core.util

/**
 * 统一异步结果封装，遵循 Kotlin 惯用模式
 * 替代传统回调，支持函数式链式调用
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (Throwable, String?) -> Unit): Result<T> {
        if (this is Error) action(exception, message)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable, message: String? = null): Result<Nothing> = Error(exception, message)

        /**
         * 将可能抛出异常的代码块包裹为 Result
         */
        fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Throwable) {
            Error(e, e.message)
        }
    }
}

/**
 * Flow 中用结果封装简化错误处理
 */
suspend fun <T> Result<T>.requireValue(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw exception
}
