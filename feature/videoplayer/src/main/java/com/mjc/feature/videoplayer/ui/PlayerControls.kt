package com.mjc.feature.videoplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mjc.feature.videoplayer.controller.PlayerState

/**
 * 播放控制组件
 */
@Composable
fun PlayerControls(
    playerState: PlayerState,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    isFullscreen: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 音量控制状态
    val isMuted = volume <= 0.01f

    // Slider本地状态，防止拖动时跳动
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // 同步外部位置到本地状态（仅在非拖动时）
    LaunchedEffect(currentPosition, duration) {
        if (!isDragging && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration.toFloat()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放/暂停按钮
        IconButton(onClick = onPlayPauseToggle) {
            Icon(
                imageVector = if (playerState is PlayerState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState is PlayerState.Playing) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 进度条
        Slider(
            value = sliderPosition,
            onValueChange = { ratio ->
                isDragging = true
                sliderPosition = ratio
            },
            onValueChangeFinished = {
                onSeek((sliderPosition * duration).toLong())
                isDragging = false
            },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )

        // 时间显示
        Text(
            text = formatTime(currentPosition) + " / " + formatTime(duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 音量控制
        IconButton(onClick = {
            // 切换静音/恢复
            if (isMuted) {
                // 恢复音量到默认值（1.0f）
                onVolumeChange(1.0f)
            } else {
                // 静音
                onVolumeChange(0.0f)
            }
        }) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "取消静音" else "静音",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 格式化时间（毫秒 -> MM:SS）
 */
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}