package com.mjc.feature.videoplayer.picker

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoPickerController {
    private val _pickerState = MutableStateFlow<VideoPickerState>(VideoPickerState.Idle)
    val pickerState: StateFlow<VideoPickerState> = _pickerState.asStateFlow()

    /**
     * 触发视频选择
     */
    fun pickVideo(pickVideoLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>) {
        _pickerState.update { VideoPickerState.Picking }
        val pickMedia = PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
            .build()
        pickVideoLauncher.launch(pickMedia)
    }

    fun handlePickResult(result: Result<Uri>) {
        result.fold(
            onSuccess = { uri ->
                _pickerState.update { VideoPickerState.Success(uri) }
            },
            onFailure = { error ->
                _pickerState.update {
                    VideoPickerState.Error(
                        message = error.message ?: "选择视频失败"
                    )
                }
            }
        )
    }

    /**
     * 重置为Idle状态（用户取消选择时调用）
     */
    fun resetToIdle() {
        _pickerState.update { VideoPickerState.Idle }
    }
}

@Composable
fun rememberVideoPickerController(): Pair<VideoPickerState, () -> Unit> {

    val videoPickerController = remember {
        VideoPickerController()
    }

    val state by videoPickerController.pickerState.collectAsState()

    val pickVideoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                videoPickerController.handlePickResult(Result.success(uri))
            } else {
                videoPickerController.resetToIdle()
            }
        }



    return state to { videoPickerController.pickVideo(pickVideoLauncher) }
}