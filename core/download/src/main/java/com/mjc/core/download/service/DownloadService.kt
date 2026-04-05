package com.mjc.core.download.service

import com.mjc.core.download.utils.DownloadUtils
import java.io.InputStream

/**
 * 下载服务抽象接口
 * 不依赖具体的HTTP客户端库（如Retrofit、OkHttp）
 * 用于功能模块与网络层的解耦
 */
interface DownloadService {

    /**
     * 下载文件（支持断点续传）
     * @param url 文件URL
     * @param range Range请求头（格式：bytes=start-end，如bytes=1024-）
     * @param ifRange 条件请求头，确保资源未改变（ETag或HTTP-date）
     * @param ifMatch 条件请求头，仅当资源匹配给定ETag时返回
     * @param ifModifiedSince 条件请求头，仅当资源在给定时间后修改时返回
     * @return 下载响应结果
     */
    suspend fun downloadFile(
        url: String,
        range: String? = null,
        ifRange: String? = null,
        ifMatch: String? = null,
        ifModifiedSince: String? = null
    ): DownloadResponse

    /**
     * 获取文件信息（HEAD请求）
     * @param url 文件URL
     * @return 文件信息响应
     */
    suspend fun getFileInfo(url: String): FileInfoResponse

    /**
     * 获取详细的文件信息（HEAD请求）
     * @param url 文件URL
     * @return 包含文件详细信息的对象
     */
    suspend fun getFileInfoWithDetails(url: String): FileDetailsResponse {
        val response = getFileInfo(url)
        return FileDetailsResponse(
            contentLength = response.headers["Content-Length"]?.toLongOrNull(),
            contentType = response.headers["Content-Type"],
            lastModified = response.headers["Last-Modified"],
            eTag = response.headers["ETag"],
            acceptRanges = response.headers["Accept-Ranges"],
            contentEncoding = response.headers["Content-Encoding"],
            fileName = response.headers["Content-Disposition"]?.let { DownloadUtils.extractFileName(it) }
        )
    }
}

/**
 * 下载响应
 * 包装HTTP响应，不依赖具体的HTTP客户端库
 */
data class DownloadResponse(
    /** HTTP状态码 */
    val statusCode: Int,
    /** 是否成功（2xx状态码） */
    val isSuccessful: Boolean,
    /** 响应头 */
    val headers: Map<String, String>,
    /** 响应体输入流 */
    val bodyStream: InputStream?,
    /** 响应体内容长度（如果知道） */
    val contentLength: Long = -1L
)

/**
 * 文件信息响应（HEAD请求响应）
 * 不依赖具体的HTTP客户端库
 */
data class FileInfoResponse(
    /** HTTP状态码 */
    val statusCode: Int,
    /** 是否成功（2xx状态码） */
    val isSuccessful: Boolean,
    /** 响应头 */
    val headers: Map<String, String>
)

/**
 * 文件详细信息响应
 * 包含常见的文件元数据（用于getFileInfoWithDetails方法）
 */
data class FileDetailsResponse(
    /** 文件大小（字节） */
    val contentLength: Long?,
    /** 内容类型 */
    val contentType: String?,
    /** 最后修改时间（HTTP-date格式） */
    val lastModified: String?,
    /** ETag（实体标签） */
    val eTag: String?,
    /** 是否支持Range请求 */
    val acceptRanges: String?,
    /** 内容编码 */
    val contentEncoding: String?,
    /** 文件名（从Content-Disposition头提取） */
    val fileName: String?
)
