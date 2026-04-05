package com.mjc.feature.download.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mjc.core.download.model.DownloadStatus
import com.mjc.core.download.utils.DownloadUtils
import com.mjc.feature.download.controller.DownloadTaskInfo

/**
 * 下载任务列表组件
 * 显示所有下载任务的列表，包括状态、进度和速度
 */
@Composable
fun DownloadList(
    tasks: List<DownloadTaskInfo>,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        EmptyDownloadList(modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                DownloadListItem(task = task)
            }
        }
    }
}

/**
 * 单个下载任务项
 */
@Composable
fun DownloadListItem(
    task: DownloadTaskInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 文件名和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getStatusText(task.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = getStatusColor(task.status)
                    )
                }

                // 进度指示器或状态图标
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        CircularProgressIndicator(
                            progress = task.progress,
                            modifier = Modifier.width(24.dp).height(24.dp),
                            strokeWidth = 3.dp
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        // 其他状态不显示图标
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条和进度信息
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                LinearProgressWithLabel(
                    progress = task.progress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 下载详情：进度、速度、大小
                DownloadDetails(task)
            }

            // 文件大小信息（所有状态）
            if (task.totalBytes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DownloadUtils.formatFileSize(task.downloadedBytes) + " / " + DownloadUtils.formatFileSize(task.totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 下载速度（仅下载中状态）
            if (task.status == DownloadStatus.DOWNLOADING && task.speedBytesPerSecond > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "速度: ${DownloadUtils.formatSpeed(task.speedBytesPerSecond)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 线性进度条带标签
 */
@Composable
private fun LinearProgressWithLabel(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 下载详情信息
 */
@Composable
private fun DownloadDetails(task: DownloadTaskInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "进度: ${(task.progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (task.status == DownloadStatus.DOWNLOADING && task.speedBytesPerSecond > 0) {
            Text(
                text = "速度: ${DownloadUtils.formatSpeed(task.speedBytesPerSecond)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 空列表状态
 */
@Composable
private fun EmptyDownloadList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "没有下载任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮开始下载",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 获取状态文本
 */
private fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.PENDING -> "等待中"
        DownloadStatus.DOWNLOADING -> "下载中"
        DownloadStatus.PAUSED -> "已暂停"
        DownloadStatus.COMPLETED -> "已完成"
        DownloadStatus.FAILED -> "失败"
        DownloadStatus.CANCELLED -> "已取消"
    }
}

/**
 * 获取状态颜色
 */
@Composable
private fun getStatusColor(status: DownloadStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    }
}