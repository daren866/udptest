package com.mjc.feature.videoplayer

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.mjc.feature.videoplayer.controller.VideoPlayerController
import com.mjc.feature.videoplayer.controller.PlayerState
import com.mjc.feature.videoplayer.ui.VideoPlayer
import com.mjc.feature.videoplayer.ui.PlayerControls
import com.mjc.feature.videoplayer.ui.ErrorDisplay
import com.mjc.feature.videoplayer.ui.FullscreenToggle
import com.mjc.feature.videoplayer.ui.VideoPickerButton
import com.mjc.feature.videoplayer.picker.VideoPickerState
import com.mjc.feature.videoplayer.picker.rememberVideoPickerController

/**
 * 视频播放器主界面
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 创建视频播放控制器（与ViewModel共享）
    val videoPlayerController = remember {
        val coroutineScope = lifecycleOwner.lifecycleScope
        VideoPlayerController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            coroutineScope = coroutineScope
        )
    }

    // 视频播放ViewModel
    val viewModel: VideoPlayerViewModel = viewModel(
        factory = remember {
            VideoPlayerViewModelFactory(videoPlayerController)
        }
    )

    val playerState by viewModel.playerState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    val (pickerState, pickVideo) = rememberVideoPickerController()

    // 处理视频选择结果（仅在URI变化时触发）
    val selectedUri = remember(pickerState) {
        (pickerState as? VideoPickerState.Success)?.uri
    }

    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri -> viewModel.loadVideo(uri) }
    }

    // 加载初始视频（如果没有选择视频）
    LaunchedEffect(Unit) {
        pickVideo()
    }

    // 根据播放器状态渲染UI
    Box(modifier = modifier.fillMaxSize()) {
        when (val state = playerState) {
            is PlayerState.Initializing,
            is PlayerState.Buffering -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is PlayerState.Ready,
            is PlayerState.Playing,
            is PlayerState.Paused,
            is PlayerState.Ended -> {
                // 视频播放器组件
                VideoPlayer(
                    controller = videoPlayerController,
                    modifier = Modifier.fillMaxSize(),
                    showBuffering = true
                )

                // 播放控制组件
                PlayerControls(
                    playerState = state,
                    currentPosition = currentPosition,
                    duration = duration,
                    volume = volume,
                    isFullscreen = isFullscreen,
                    onPlayPauseToggle = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onVolumeChange = viewModel::setVolume,
                    onFullscreenToggle = viewModel::toggleFullscreen,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 全屏切换按钮
                FullscreenToggle(
                    isFullscreen = isFullscreen,
                    onToggle = viewModel::toggleFullscreen,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                // 视频选择按钮（左上角）
                VideoPickerButton(
                    pickerState = pickerState,
                    onPickVideo = { pickVideo() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            is PlayerState.Error -> {
                ErrorDisplay(
                    errorMessage = state.message,
                    onRetry = state.retryAction,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 视频选择按钮（错误状态下也显示）
                VideoPickerButton(
                    pickerState = pickerState,
                    onPickVideo = { pickVideo() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
        }
    }
}