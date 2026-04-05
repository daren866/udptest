package com.mjc.core.network.ext

import retrofit2.Response as RetrofitResponse

/**
 * Response扩展函数
 * 提供通用HTTP响应处理的便捷函数
 */

// region Retrofit Response 扩展

/**
 * 获取ETag头
 */
fun <T> RetrofitResponse<T>.eTag(): String? = headers()["ETag"]

/**
 * 获取最后修改时间
 */
fun <T> RetrofitResponse<T>.lastModified(): String? = headers()["Last-Modified"]

/**
 * 获取文件名
 */
fun <T> RetrofitResponse<T>.fileName(): String? {
    val contentDisposition = headers()["Content-Disposition"] ?: return null
    val filenameRegex = "filename\\s*=\\s*\"?([^\";]+)\"?".toRegex()
    return filenameRegex.find(contentDisposition)?.groupValues?.getOrNull(1)
}

// endregion
