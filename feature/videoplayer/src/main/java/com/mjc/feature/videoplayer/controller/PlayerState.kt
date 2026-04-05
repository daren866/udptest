package com.mjc.feature.videoplayer.controller

/**
 * 播放器状态密封类
 */
sealed class PlayerState {
    object Initializing : PlayerState()
    object Ready : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    data class Buffering(val bufferedPercentage: Int) : PlayerState()
    data class Error(val message: String, val retryAction: () -> Unit = {}) : PlayerState()
    object Ended : PlayerState()
}