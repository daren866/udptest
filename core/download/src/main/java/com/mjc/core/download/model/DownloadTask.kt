package com.mjc.core.download.model

import com.mjc.core.download.utils.DownloadUtils
import java.io.File

/**
 * 下载任务数据模型
 * 包含下载任务的所有状态信息，支持断点续传
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val destinationPath: String,
    val status: DownloadStatus,
    val progress: Float,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedBytesPerSecond: Long,
    val resumeData: ResumeData? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 检查任务是否可恢复（有有效的断点数据）
     */
    fun canResume(): Boolean {
        return resumeData != null &&
                File(resumeData.tempFilePath).exists() &&
                resumeData.downloadedBytes == File(resumeData.tempFilePath).length()
    }

    /**
     * 构建Range请求头
     */
    fun buildRangeHeader(): String {
        return DownloadUtils.buildRangeHeader(downloadedBytes)
    }

    /**
     * 计算剩余字节
     */
    fun remainingBytes(): Long {
        return totalBytes - downloadedBytes
    }

    /**
     * 估算剩余时间（秒）
     */
    fun estimatedTimeRemaining(): Long {
        return if (speedBytesPerSecond > 0) {
            remainingBytes() / speedBytesPerSecond
        } else {
            -1L // 未知
        }
    }
}

/**
 * 断点续传数据
 * 保存断点信息，用于恢复下载
 */
data class ResumeData(
    val tempFilePath: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * 下载状态枚举
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 下载错误类型
 */
sealed class DownloadError {
    data class NetworkError(val code: Int, val message: String) : DownloadError()
    data class StorageError(val reason: String) : DownloadError()
    data class PermissionError(val permission: String) : DownloadError()
    data class FileError(val exception: Exception) : DownloadError()
    data class Cancelled(val byUser: Boolean) : DownloadError()
    data class UnknownError(val exception: Throwable) : DownloadError()

    /**
     * 获取用户友好的错误消息
     */
    fun getUserFriendlyMessage(): String {
        return when (this) {
            is NetworkError -> "网络错误: $message (代码: $code)"
            is StorageError -> "存储错误: $reason"
            is PermissionError -> "权限错误: 需要$permission 权限"
            is FileError -> "文件错误: ${exception.message ?: "未知错误"}"
            is Cancelled -> if (byUser) "下载已取消" else "下载被中断"
            is UnknownError -> "未知错误: ${exception.message ?: "请重试"}"
        }
    }
}
