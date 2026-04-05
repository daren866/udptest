package com.mjc.feature.download.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 下载进度组件
 * 显示下载进度条和进度信息
 */
@Composable
fun DownloadProgress(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSecond: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 进度条
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        // 进度文本
        Text(
            text = formatProgressText(progress, downloadedBytes, totalBytes, speedBytesPerSecond),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 格式化进度文本
 */
private fun formatProgressText(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSecond: Long
): String {
    val progressPercent = (progress * 100).toInt()
    val downloadedMB = downloadedBytes / (1024 * 1024)
    val totalMB = totalBytes / (1024 * 1024)
    val speedKB = speedBytesPerSecond / 1024

    return "$progressPercent% - ${downloadedMB}MB / ${totalMB}MB - ${speedKB}KB/s"
}

@Preview(showBackground = true)
@Composable
private fun DownloadProgressPreview() {
    DownloadProgress(
        progress = 0.65f,
        downloadedBytes = 65 * 1024 * 1024, // 65MB
        totalBytes = 100 * 1024 * 1024, // 100MB
        speedBytesPerSecond = 512 * 1024 // 512KB/s
    )
}