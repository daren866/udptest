package com.mjc.feature.videoplayer.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * 视频文件选择工具类
 */
object VideoPickerUtils {
    /**
     * 检查是否支持Photo Picker API（Android 13+）
     */
    fun isPhotoPickerAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 验证URI是否为有效的视频URI
     */
    fun isValidVideoUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.getType(uri)?.startsWith("video/") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从URI中提取文件名
     */
    fun extractFileName(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                // 从ContentResolver查询
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else {
                        null
                    }
                }
            }
            "file" -> {
                // 从文件路径提取
                uri.lastPathSegment
            }
            else -> {
                // 其他scheme，尝试从路径提取
                uri.toString().substringAfterLast('/')
            }
        }
    }

    /**
     * 获取文件的Content URI（用于分享文件）
     */
    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}