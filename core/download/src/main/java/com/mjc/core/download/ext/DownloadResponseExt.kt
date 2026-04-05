package com.mjc.core.download.ext

import com.mjc.core.download.utils.ProgressListener
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import retrofit2.Response as RetrofitResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 下载相关Response扩展函数
 * 提供下载响应处理的便捷函数
 */

// region Retrofit Response 扩展

/**
 * 检查是否为部分内容响应（206 Partial Content）
 */
fun <T> RetrofitResponse<T>.isPartialContent(): Boolean = code() == 206

/**
 * 检查响应是否支持Range请求
 */
fun <T> RetrofitResponse<T>.supportsRangeRequests(): Boolean {
    return headers()["Accept-Ranges"] == "bytes" || headers()["Content-Range"] != null
}

/**
 * 获取Content-Range头信息
 */
fun <T> RetrofitResponse<T>.contentRange(): Triple<Long, Long, Long?>? {
    val contentRange = headers()["Content-Range"] ?: return null
    return try {
        com.mjc.core.download.utils.DownloadUtils.parseContentRange(contentRange)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * 获取文件大小
 */
fun <T> RetrofitResponse<T>.fileSize(): Long? {
    headers()["Content-Length"]?.toLongOrNull()?.let { return it }
    contentRange()?.third?.let { return it }
    return null
}

// endregion

// region ResponseBody 扩展

/**
 * 将ResponseBody写入文件，支持进度监听
 * @param file 目标文件
 * @param progressListener 进度监听器（可选）
 * @throws IOException 写入失败时抛出
 */
@Throws(IOException::class)
fun ResponseBody.writeToFile(file: File, progressListener: ProgressListener? = null) {
    val totalBytes = contentLength()
    var bytesRead = 0L

    // 确保父目录存在
    file.parentFile?.mkdirs()

    byteStream().use { inputStream ->
        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesRead += read

                // 更新进度
                progressListener?.onProgressUpdate(bytesRead, totalBytes, false)
            }

            // 完成
            progressListener?.onProgressUpdate(bytesRead, totalBytes, true)
        }
    }
}

/**
 * 将ResponseBody写入文件（简化版本）
 */
@Throws(IOException::class)
fun ResponseBody.writeToFile(filePath: String, progressListener: ProgressListener? = null) {
    writeToFile(File(filePath), progressListener)
}

/**
 * 将ResponseBody转换为字节数组，支持进度监听
 */
@Throws(IOException::class)
fun ResponseBody.toByteArray(progressListener: ProgressListener? = null): ByteArray {
    val totalBytes = contentLength()
    var bytesRead = 0L

    return byteStream().use { inputStream ->
        val outputStream = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int

        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
            bytesRead += read
            progressListener?.onProgressUpdate(bytesRead, totalBytes, false)
        }

        progressListener?.onProgressUpdate(bytesRead, totalBytes, true)
        outputStream.toByteArray()
    }
}

// endregion

// region 进度监听拦截器（简化版）

/**
 * 进度监听拦截器
 * 用于监听下载进度
 */
class ProgressInterceptor(private val listener: ProgressListener) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalResponse = chain.proceed(chain.request())

        // 如果响应体存在，包装它
        val responseBody = originalResponse.body ?: return originalResponse

        val progressResponseBody = ProgressResponseBody(responseBody, listener)
        return originalResponse.newBuilder()
            .body(progressResponseBody)
            .build()
    }
}

/**
 * 带进度监听的ResponseBody
 */
private class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val listener: ProgressListener
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentType() = responseBody.contentType()

    override fun contentLength() = responseBody.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: okio.Source): okio.Source {
        return object : ForwardingSource(source) {
            private var totalBytesRead = 0L
            private val totalBytes = contentLength()

            override fun read(sink: okio.Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // 读取完成时返回-1
                if (bytesRead != -1L) {
                    totalBytesRead += bytesRead
                }
                listener.onProgressUpdate(totalBytesRead, totalBytes, bytesRead == -1L)
                return bytesRead
            }
        }
    }
}

// endregion

private const val DEFAULT_BUFFER_SIZE = 8192
