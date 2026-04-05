package com.mjc.core.network.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 统一的API响应封装
 * 使用密封类表示不同的响应状态
 */
sealed class ApiResponse<out T> {

    /**
     * 请求成功
     * @param data 响应数据
     */
    data class Success<T>(val data: T) : ApiResponse<T>()

    /**
     * 请求失败（HTTP错误）
     * @param code HTTP状态码
     * @param message 错误信息
     */
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>()

    /**
     * 网络异常
     * @param throwable 异常对象
     */
    data class Exception(val throwable: Throwable) : ApiResponse<Nothing>()

    /**
     * 加载中
     */
    data object Loading : ApiResponse<Nothing>()

    companion object {
        /**
         * 将suspend函数包装为Flow<ApiResponse<T>>
         */
        fun <T> fromSuspend(suspendFunc: suspend () -> T): Flow<ApiResponse<T>> = flow {
            emit(Loading)
            try {
                val result = suspendFunc()
                emit(Success(result))
            } catch (e: retrofit2.HttpException) {
                emit(Error(e.code(), e.message()))
            } catch (e: kotlin.Exception) {
                emit(Exception(e))
            }
        }
    }
}

/**
 * 扩展函数：将ApiResponse转换为具体的值或处理错误
 */
fun <T> ApiResponse<T>.getOrThrow(): T {
    return when (this) {
        is ApiResponse.Success -> data
        is ApiResponse.Error -> throw RuntimeException("HTTP $code: $message")
        is ApiResponse.Exception -> throw throwable
        is ApiResponse.Loading -> throw IllegalStateException("Still loading")
    }
}

/**
 * 扩展函数：安全地获取数据，提供默认值
 */
fun <T> ApiResponse<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is ApiResponse.Success -> data
        else -> defaultValue
    }
}