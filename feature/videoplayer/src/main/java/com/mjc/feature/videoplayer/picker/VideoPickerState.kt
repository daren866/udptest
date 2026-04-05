package com.mjc.feature.videoplayer.picker

import android.net.Uri

/**
 * 视频文件选择状态
 */
sealed class VideoPickerState {
    /**
     * 初始状态，未进行选择
     */
    object Idle : VideoPickerState()

    /**
     * 选择进行中
     */
    object Picking : VideoPickerState()

    /**
     * 选择成功
     * @param uri 选择的视频URI
     */
    data class Success(val uri: Uri) : VideoPickerState()

    /**
     * 选择错误
     * @param message 错误信息
     */
    data class Error(val message: String) : VideoPickerState()
}