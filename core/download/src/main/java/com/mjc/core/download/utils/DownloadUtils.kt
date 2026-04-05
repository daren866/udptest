package com.mjc.core.download.utils

import java.text.DecimalFormat
import kotlin.math.abs

/**
 * 下载工具类
 * 提供下载相关的实用函数，如进度计算、文件大小格式化、Range头解析等
 */
object DownloadUtils {

    /**
     * 格式化文件大小
     * @param bytes 文件大小（字节）
     * @return 格式化后的字符串（如 1.23 MB）
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()

        val unit = units[digitGroups.coerceAtMost(units.size - 1)]
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())

        return DecimalFormat("#,##0.#").format(value) + " $unit"
    }

    /**
     * 计算下载进度百分比
     * @param downloaded 已下载字节数
     * @param total 总字节数（如果未知则为null）
     * @return 进度百分比（0.0-1.0），如果总字节数未知则返回null
     */
    fun calculateProgress(downloaded: Long, total: Long?): Float? {
        return total?.takeIf { it > 0 }?.let { downloaded.toFloat() / it }
    }

    /**
     * 计算下载速度
     * @param bytesDownloaded 已下载字节数
     * @param elapsedTimeMs 经过的时间（毫秒）
     * @return 格式化后的下载速度字符串（如 1.23 MB/s）
     */
    fun calculateDownloadSpeed(bytesDownloaded: Long, elapsedTimeMs: Long): String {
        if (elapsedTimeMs <= 0) return "0 B/s"

        val bytesPerSecond = bytesDownloaded * 1000L / elapsedTimeMs
        return formatSpeed(bytesPerSecond)
    }

    /**
     * 格式化速度（字节/秒）
     * @param bytesPerSecond 每秒字节数
     * @return 格式化后的速度字符串（如 1.23 MB/s）
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "0 B/s"

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (Math.log10(bytesPerSecond.toDouble()) / Math.log10(1024.0)).toInt()

        val unit = units[digitGroups.coerceAtMost(units.size - 1)]
        val value = bytesPerSecond / Math.pow(1024.0, digitGroups.toDouble())

        return DecimalFormat("#,##0.#").format(value) + " $unit"
    }

    /**
     * 解析Range头字符串
     * @param rangeHeader Range头值（如 "bytes=0-100" 或 "bytes=0-100,200-300"）
     * @return 范围列表，每个范围是起始位置和可选的结束位置对
     * @throws IllegalArgumentException 如果格式无效
     */
    fun parseRangeHeader(rangeHeader: String): List<Pair<Long, Long?>> {
        val trimmed = rangeHeader.trim()
        if (!trimmed.startsWith("bytes=")) {
            throw IllegalArgumentException("无效的Range头格式: $rangeHeader。必须以'bytes='开头")
        }

        val rangesPart = trimmed.substring(6) // 去掉"bytes="
        if (rangesPart.isEmpty()) {
            throw IllegalArgumentException("Range头不能为空: $rangeHeader")
        }

        val ranges = rangesPart.split(",")
        return ranges.map { range ->
            val parts = range.trim().split("-")
            if (parts.size != 2) {
                throw IllegalArgumentException("无效的范围格式: '$range'。应为 'start-end' 或 'start-'")
            }

            val start = parts[0].toLongOrNull()
                ?: throw IllegalArgumentException("无效的起始位置: '${parts[0]}'")

            if (start < 0) {
                throw IllegalArgumentException("起始位置不能为负数: $start")
            }

            val end = parts[1].takeIf { it.isNotEmpty() }?.toLongOrNull()

            end?.let {
                if (it < start) {
                    throw IllegalArgumentException("结束位置不能小于起始位置: $it < $start")
                }
            }

            start to end
        }
    }

    /**
     * 生成Range头字符串
     * @param ranges 范围列表，每个范围是起始位置和可选的结束位置对
     * @return Range头字符串（如 "bytes=0-100,200-300"）
     */
    fun generateRangeHeader(ranges: List<Pair<Long, Long?>>): String {
        if (ranges.isEmpty()) {
            throw IllegalArgumentException("范围列表不能为空")
        }

        return buildString {
            append("bytes=")
            ranges.forEachIndexed { index, (start, end) ->
                if (index > 0) append(",")
                append(start)
                append("-")
                end?.let { append(it) }
            }
        }
    }

    /**
     * 构建断点续传Range头
     * @param downloadedBytes 已下载字节数
     * @return Range头字符串（如 "bytes=1024-"）
     */
    fun buildRangeHeader(downloadedBytes: Long): String {
        return "bytes=$downloadedBytes-"
    }

    /**
     * 验证文件大小是否足够剩余空间
     * @param fileSize 文件大小（字节）
     * @param freeSpace 可用空间（字节）
     * @param buffer 缓冲大小（字节，默认1MB）
     * @return 如果可用空间足够则返回true
     */
    fun hasEnoughSpace(fileSize: Long, freeSpace: Long, buffer: Long = 1024 * 1024): Boolean {
        return freeSpace >= fileSize + buffer
    }

    /**
     * 计算剩余下载时间
     * @param downloaded 已下载字节数
     * @param total 总字节数
     * @param elapsedTimeMs 已用时间（毫秒）
     * @return 剩余时间（毫秒），如果无法计算则返回null
     */
    fun calculateRemainingTime(downloaded: Long, total: Long, elapsedTimeMs: Long): Long? {
        if (downloaded <= 0 || elapsedTimeMs <= 0) return null
        if (downloaded >= total) return 0L

        val downloadSpeed = downloaded.toDouble() / elapsedTimeMs // 字节/毫秒
        val remainingBytes = total - downloaded
        return (remainingBytes / downloadSpeed).toLong()
    }

    /**
     * 从Content-Range头解析下载范围信息
     * @param contentRange Content-Range头值（如 "bytes 0-100/200"）
     * @return 三元组（起始位置，结束位置，总大小），如果总大小未知则为null
     * @throws IllegalArgumentException 如果格式无效
     */
    fun parseContentRange(contentRange: String): Triple<Long, Long, Long?> {
        val pattern = "^bytes (\\d+)-(\\d+)/(\\d+|\\*)$".toRegex()
        val match = pattern.find(contentRange.trim())
            ?: throw IllegalArgumentException("无效的Content-Range格式: $contentRange")

        val start = match.groupValues[1].toLong()
        val end = match.groupValues[2].toLong()
        val total = match.groupValues[3].takeIf { it != "*" }?.toLongOrNull()

        if (start > end) {
            throw IllegalArgumentException("起始位置不能大于结束位置: $start > $end")
        }

        return Triple(start, end, total)
    }

    /**
     * 从Content-Disposition头提取文件名
     * @param contentDisposition Content-Disposition头值（如 'attachment; filename="file.jpg"'）
     * @return 提取的文件名，如果不存在则返回null
     */
    fun extractFileName(contentDisposition: String): String? {
        val filenameRegex = "filename\\s*=\\s*\"?([^\";]+)\"?".toRegex()
        return filenameRegex.find(contentDisposition)?.groupValues?.getOrNull(1)
    }
}

/**
 * 进度监听器接口
 */
interface ProgressListener {
    /**
     * 进度更新回调
     * @param bytesRead 已读取字节数
     * @param contentLength 总字节数（如果未知则为-1）
     * @param done 是否完成
     */
    fun onProgressUpdate(bytesRead: Long, contentLength: Long, done: Boolean)
}

/**
 * 速度计算器
 * 平滑处理下载速度计算
 */
class SpeedCalculator {
    private val speedHistory = ArrayDeque<Long>()

    /**
     * 计算速度
     * @param bytesDownloaded 本次下载字节数
     * @param timeElapsedMs 本次耗时（毫秒）
     * @return 平滑后的速度（字节/秒）
     */
    fun calculateSpeed(bytesDownloaded: Long, timeElapsedMs: Long): Long {
        if (timeElapsedMs <= 0) return 0L

        val instantSpeed = (bytesDownloaded * 1000) / timeElapsedMs
        speedHistory.addLast(instantSpeed)

        // 保留最近10个速度值
        if (speedHistory.size > 10) {
            speedHistory.removeFirst()
        }

        return if (speedHistory.isEmpty()) 0L else {
            speedHistory.sum() / speedHistory.size.toLong()
        }
    }

    /**
     * 重置计算器
     */
    fun reset() {
        speedHistory.clear()
    }
}