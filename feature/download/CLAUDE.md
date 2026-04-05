# 下载模块指南

## 模块概述

此模块提供了完整的文件下载功能，支持多任务下载、断点续传、进度显示、后台下载等功能。模块基于核心网络模块（core:network）构建，使用现代Android架构。

**模块信息：**
- **包名**: `com.mjc.feature.download`
- **依赖**: core:network, WorkManager, Compose
- **架构**: MVVM with Clean Architecture

## 模块结构

### 目录结构
```
feature/download/src/main/java/com/mjc/feature/download/
├── DownloadScreen.kt          # 主UI界面
├── DownloadViewModel.kt       # ViewModel
├── controller/               # 控制器层
│   ├── DownloadController.kt
│   ├── DownloadTaskManager.kt
│   └── DownloadState.kt
├── repository/               # 数据层
│   ├── DownloadRepository.kt
│   └── DownloadDatabase.kt
├── model/                    # 数据模型
│   ├── DownloadTask.kt
│   ├── DownloadConfig.kt
│   └── DownloadDestination.kt
├── ui/                      # UI组件
│   ├── DownloadProgress.kt
│   ├── DownloadList.kt
│   └── DownloadControls.kt
├── utils/                   # 工具类
│   ├── DownloadUtils.kt
│   ├── FileUtils.kt
│   └── NetworkUtils.kt
└── worker/                  # 后台任务
    └── DownloadWorker.kt
```

## 核心技术栈

### 核心依赖
1. **网络请求**: core:network模块（OkHttp + Retrofit）
2. **后台任务**: WorkManager
3. **数据库**: Room（可选，用于保存下载记录）
4. **UI框架**: Jetpack Compose
5. **异步处理**: Kotlin Coroutines + Flow
6. **文件操作**: AndroidX DocumentFile + MediaStore

### 主要功能
1. **多任务下载管理**: 支持并发下载、队列管理
2. **断点续传**: 支持暂停、继续、断点续传
3. **进度显示**: 实时下载进度和速度显示
4. **错误处理**: 网络错误、存储错误、权限错误处理
5. **后台下载**: 使用WorkManager支持后台下载
6. **存储管理**: 支持Android 10+分区存储

## 架构设计

### 架构图
```
DownloadScreen (UI层 - Compose)
    ↓
DownloadViewModel (ViewModel层 - 状态管理)
    ↓
DownloadController (控制器层 - 业务逻辑)
    ↓
DownloadRepository (数据层 - 数据访问)
    ↓
DownloadWorker (工作层 - 后台任务)
    ↓
core:network (网络层 - HTTP请求)
```

### 状态管理
使用Kotlin StateFlow和Sealed Class管理下载状态：
```kotlin
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Preparing : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Float
    ) : DownloadState()
    data class Completed(val fileUri: Uri) : DownloadState()
    data class Failed(val error: DownloadError) : DownloadState()
    data object Paused : DownloadState()
}
```

## 配置要求

### 权限配置
已在 `AndroidManifest.xml` 中声明：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
<!-- Android 10+ 使用分区存储，不需要WRITE_EXTERNAL_STORAGE -->
```

### 构建配置
模块使用以下依赖：
- core:network - 网络功能
- WorkManager - 后台任务
- Jetpack Compose - UI界面
- Kotlin Coroutines - 异步处理

## 使用示例

### 基本使用
```kotlin
// 在Compose界面中使用下载功能
@Composable
fun DownloadScreen(viewModel: DownloadViewModel = viewModel()) {
    val downloadState by viewModel.downloadState.collectAsState()

    Column {
        // 下载进度显示
        DownloadProgress(state = downloadState)

        // 下载控制按钮
        DownloadControls(
            onStartDownload = { url, destination ->
                viewModel.startDownload(url, destination)
            },
            onPauseDownload = viewModel::pauseDownload,
            onResumeDownload = viewModel::resumeDownload,
            onCancelDownload = viewModel::cancelDownload
        )

        // 下载任务列表
        DownloadList(tasks = viewModel.downloadTasks)
    }
}
```

### 启动下载任务
```kotlin
// 创建下载配置
val config = DownloadConfig(
    url = "https://example.com/file.zip",
    destination = DownloadDestination.Documents("files/file.zip"),
    title = "重要文件",
    description = "项目文档",
    allowMobileNetwork = false,
    enableNotifications = true
)

// 启动下载
viewModel.startDownload(config)
```

## 后台下载

### WorkManager配置
下载模块使用WorkManager处理后台下载任务：
```kotlin
class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 执行下载任务
        return try {
            val downloadResult = downloadFile()
            Result.success(downloadResult.toOutputData())
        } catch (e: Exception) {
            Result.failure(e.toOutputData())
        }
    }

    private suspend fun downloadFile(): DownloadResult {
        // 下载实现
    }
}
```

### 后台下载限制
- Android 8+：后台执行限制
- Android 10+：分区存储限制
- 电量优化：避免在低电量时下载大文件

## 错误处理

### 错误类型
```kotlin
sealed class DownloadError {
    data class NetworkError(val code: Int, val message: String) : DownloadError()
    data class StorageError(val reason: String) : DownloadError()
    data class PermissionError(val permission: String) : DownloadError()
    data class Cancelled(val byUser: Boolean) : DownloadError()
    data class UnknownError(val exception: Throwable) : DownloadError()
}
```

### 错误恢复策略
1. **网络错误**: 自动重试（最多3次）
2. **存储错误**: 检查存储空间，提示用户清理
3. **权限错误**: 引导用户授予必要权限
4. **取消下载**: 清理临时文件，更新状态

## 测试策略

### 单元测试
- 测试下载逻辑、状态管理
- 使用MockWebServer测试网络请求
- 测试错误处理逻辑

### 仪器测试
- 测试实际下载功能
- 测试权限处理
- 测试后台下载

### UI测试
- 测试Compose UI组件
- 测试用户交互

## 性能优化

### 下载优化
1. **分块下载**: 支持大文件分块下载
2. **断点续传**: 支持HTTP Range请求
3. **并发控制**: 限制同时下载任务数量
4. **速度限制**: 避免占用全部带宽

### 内存优化
1. **流式处理**: 使用OkHttp的流式响应，避免内存溢出
2. **缓冲区管理**: 合理设置缓冲区大小
3. **及时释放资源**: 下载完成后及时关闭连接

## 兼容性考虑

### Android版本兼容
1. **Android 10+**: 使用分区存储（Scoped Storage）
2. **Android 8-9**: 使用传统存储权限
3. **后台限制**: 适配不同Android版本的后台限制

### 网络兼容
1. **网络类型**: 区分Wi-Fi和移动网络
2. **网络状态**: 监听网络变化，自动暂停/恢复
3. **代理支持**: 支持HTTP代理

## 功能路线图

### 阶段1: 基础功能
- [ ] 单个文件下载
- [ ] 进度显示
- [ ] 基本错误处理
- [ ] 前台下载

### 阶段2: 增强功能
- [ ] 多任务下载
- [ ] 断点续传
- [ ] 后台下载
- [ ] 通知支持

### 阶段3: 高级功能
- [ ] 批量下载管理
- [ ] 下载计划调度
- [ ] 云存储集成
- [ ] 下载历史记录

## 常见问题

### Q1: 如何支持Android 10+的分区存储？
A: 使用MediaStore API或DocumentFile API访问文件，避免直接文件路径访问。

### Q2: 如何处理后台下载限制？
A: 使用WorkManager的约束条件，确保在合适的时机执行下载任务。

### Q3: 如何实现断点续传？
A: 使用HTTP Range头请求，保存已下载的字节位置，从断点处继续下载。

### Q4: 如何测试下载功能？
A: 使用MockWebServer模拟网络响应，测试各种场景（成功、失败、中断等）。

## 参考资料

1. [WorkManager官方文档](https://developer.android.com/topic/libraries/architecture/workmanager)
2. [OkHttp官方文档](https://square.github.io/okhttp/)
3. [Android存储最佳实践](https://developer.android.com/training/data-storage)
4. [Compose状态管理](https://developer.android.com/jetpack/compose/state)

---
**文档版本**: 1.0
**更新日期**: 2026-03-30
**适用模块**: feature/download
