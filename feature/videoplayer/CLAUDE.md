# 视频播放器模块指南

## 模块概述

此模块提供了基于Android Media3架构的视频播放功能实现。Media3是Android媒体播放的现代架构，提供了统一的API来处理音频和视频播放，支持本地文件、网络流媒体和自适应流（如DASH、HLS、SmoothStreaming）。

**模块信息：**
- **包名**: `com.mjc.feature.videoplayer`
- **依赖**: Android Media3 (ExoPlayer), Compose, Core模块
- **架构**: MVVM with Media3 Player and MediaSession
- **目标版本**: Media3 1.10.0 (2026年最新稳定版本)

## Media3 架构

### 核心组件

1. **ExoPlayer** - 核心播放器引擎，支持多种媒体格式和流协议
2. **Player** - 统一的播放器接口，抽象了ExoPlayer的实现细节
3. **MediaSession** - 媒体会话管理，支持后台播放和媒体控制集成
4. **MediaController** - 控制MediaSession的前端组件
5. **MediaItem** - 媒体项表示，包含媒体元数据和播放配置
6. **MediaSource** - 媒体源抽象，支持多种流媒体协议

### 架构图
```
VideoPlayerScreen (UI层，Compose)
    ↓
VideoPlayerViewModel (ViewModel层，状态管理)
    ↓
VideoPlayerController (控制器层，业务逻辑)
    ↓
MediaSession (媒体会话层，后台播放)
    ↓
ExoPlayer (播放引擎层，核心播放)
    ↓
MediaSource (媒体源层，DASH/HLS/SmoothStreaming)
```

## Media3 1.10.0 新特性 (2026)

### 1. Material3播放控件
- **Material3主题集成**：全新的Material3风格播放控件，支持动态颜色
- **Compose原生组件**：`PlayerViewCompose`完全替代传统`PlayerView`
- **自适应布局**：自动适应不同屏幕尺寸和设备方向

### 2. 增强的播放能力
- **预缓存支持**：`PreCacheHelper`允许应用预缓存指定起始位置和时长的媒体
- **VobSub轨道支持**：MP4文件中的VobSub字幕轨道支持
- **改进的擦洗模式**：修复了播放列表中擦洗模式下的多个边界情况问题

### 3. 性能优化
- **智能缓冲算法**：改进的自适应比特率选择和缓冲策略
- **内存优化**：减少播放器实例的内存占用
- **网络效率**：优化的数据源管理和连接复用

### 4. 架构推荐
```kotlin
// 基于Media3 1.10.0的现代架构
VideoPlayerScreen (Compose UI with PlayerViewCompose)
    ↓
VideoPlayerViewModel (使用Player.State进行状态管理)
    ↓
VideoPlayerSession (MediaSession管理，支持后台播放)
    ↓
ExoPlayer (配置了最新扩展和编解码器)
    ↓
MediaSourceFactory (统一媒体源工厂，支持多种协议)
```

## 代码结构

```
feature/videoplayer/src/main/java/com/mjc/feature/videoplayer/
├── VideoPlayerScreen.kt          # 主视频播放UI界面
├── VideoPlayerViewModel.kt       # 播放器状态管理
├── controller/                  # 播放器控制器
│   ├── VideoPlayerController.kt # 播放器控制逻辑
│   ├── MediaSessionController.kt # 媒体会话控制
│   └── PlayerState.kt           # 播放器状态定义
├── usercase/                   # 播放器用例
│   ├── LocalPlaybackUseCase.kt # 本地文件播放
│   ├── StreamingUseCase.kt     # 流媒体播放
│   ├── PlaylistUseCase.kt      # 播放列表管理
│   └── DownloadUseCase.kt      # 媒体下载管理 (使用PreCacheHelper)
├── ui/                         # 播放器UI组件
│   ├── VideoPlayer.kt          # 视频播放组件 (使用PlayerViewCompose)
│   ├── PlayerControls.kt       # 播放控制组件 (Material3风格)
│   ├── QualitySelector.kt      # 画质选择器
│   └── PlayerOverlay.kt        # 播放器叠加层
├── model/                      # 数据模型
│   ├── MediaItem.kt            # 媒体项模型
│   ├── Playlist.kt             # 播放列表模型
│   └── PlayerConfig.kt         # 播放器配置
└── utils/                      # 工具类
    ├── MediaUriParser.kt       # 媒体URI解析
    ├── SubtitleLoader.kt       # 字幕加载器 (支持VobSub)
    ├── NetworkMonitor.kt       # 网络状态监控
    └── PreCacheManager.kt      # 预缓存管理器
```

## 实现指南

### 1. 依赖配置

**gradle/libs.versions.toml 版本配置：**
```toml
[versions]
media3 = "1.10.0"  # 2026年最新稳定版本

[libraries]
# Media3核心库
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-exoplayer-dash = { group = "androidx.media3", name = "media3-exoplayer-dash", version.ref = "media3" }
media3-exoplayer-hls = { group = "androidx.media3", name = "media3-exoplayer-hls", version.ref = "media3" }
media3-exoplayer-smoothstreaming = { group = "androidx.media3", name = "media3-exoplayer-smoothstreaming", version.ref = "media3" }

# Media3 UI库
media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
media3-ui-compose = { group = "androidx.media3", name = "media3-ui-compose", version.ref = "media3" }

# Media3扩展库
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }
```

**feature/videoplayer/build.gradle.kts 关键依赖：**
```kotlin
dependencies {
    // Media3核心
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.smoothstreaming)

    // Media3 UI (Compose集成)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)

    // Media3扩展
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    // 项目标准依赖 (与camera模块保持一致)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
```

### 2. 播放器初始化

**VideoPlayerController 核心逻辑：**
- 使用`ExoPlayer.Builder`创建播放器实例
- 配置`TrackSelector`和`LoadControl`优化播放性能
- 创建`MediaSession`支持后台播放和系统媒体控制
- 实现播放器状态监听和错误处理

**关键配置示例：**
```kotlin
val player = ExoPlayer.Builder(context)
    .setTrackSelector(DefaultTrackSelector(context))
    .setLoadControl(DefaultLoadControl.Builder()
        .setBufferDurationsMs(15000, 30000, 2500, 5000)
        .build())
    .build()
```

### 3. Compose UI集成

**使用PlayerViewCompose：**
```kotlin
@Composable
fun VideoPlayerScreen(player: ExoPlayer) {
    PlayerViewCompose(
        player = player,
        modifier = Modifier.fillMaxSize(),
        useController = true,
        showBuffering = true
    )
}
```

**自定义控制组件：**
- 基于`Player`状态实现播放/暂停控制
- 使用`Slider`实现进度条和跳转功能
- 集成Material3主题和动态颜色

### 4. 媒体下载管理 (DownloadUseCase)

Media3 1.10.0引入了`PreCacheHelper`，为媒体下载和离线播放提供了更好的支持：

**核心功能：**
- **预缓存指定片段**：可以预缓存媒体的特定起始位置和时长
- **后台下载管理**：支持后台下载任务的管理和监控
- **下载状态跟踪**：实时跟踪下载进度和状态
- **存储空间管理**：智能管理本地存储空间

**实现要点：**
1. 使用`DownloadManager`管理下载任务
2. 集成`PreCacheHelper`进行智能预缓存
3. 实现下载状态监听和错误恢复
4. 提供下载队列管理和优先级设置

### 5. 媒体源处理

支持多种媒体源类型：
- **本地文件**：`file://`协议，直接文件路径
- **HTTP/HTTPS流**：渐进式下载流媒体
- **自适应流**：HLS (.m3u8)、DASH (.mpd)、SmoothStreaming
- **内容URI**：MediaStore内容提供程序

### 6. 播放列表管理

**PlaylistUseCase核心功能：**
- 播放列表的添加、删除和重新排序
- 顺序播放、随机播放和单曲循环模式
- 播放历史记录和继续播放功能
- 跨会话的播放状态持久化

## 最佳实践

### 1. 性能优化

**播放器实例管理：**
- 复用播放器实例避免重复创建开销
- 及时释放不再使用的播放器资源
- 使用适当的缓冲区大小平衡内存使用和播放流畅性

**网络优化：**
- 配置合适的自适应比特率策略
- 使用`OkHttpDataSource`提供更好的网络控制和缓存
- 实现网络状态监听和自动重试机制

### 2. 错误处理

**错误分类处理：**
- **网络错误**：提供重试选项和离线模式
- **解码错误**：提示不支持的格式并提供转码选项
- **权限错误**：引导用户授予必要的存储或网络权限
- **资源错误**：处理无效的媒体URI或损坏的文件

### 3. 用户体验

**播放控制：**
- 提供直观的播放/暂停、快进/快退控制
- 实现手势控制（滑动调节亮度、音量、进度）
- 支持画中画（PiP）模式

**状态反馈：**
- 清晰的缓冲状态指示
- 播放错误时的友好提示
- 网络状态变化的及时通知

### 4. 测试策略

**单元测试：**
- 测试`VideoPlayerViewModel`的状态管理逻辑
- 验证媒体URI解析和播放列表管理功能
- 模拟网络错误和播放异常场景

**仪器测试：**
- 测试实际设备上的播放性能
- 验证不同网络条件下的自适应流媒体播放
- 测试后台播放和媒体会话集成

**UI测试：**
- 测试Compose UI组件的交互
- 验证播放控制的功能完整性
- 测试不同屏幕尺寸和方向的布局适配

## 功能路线图

### 阶段1：基础播放功能
- [ ] 本地视频文件播放
- [ ] 基本播放控制（播放/暂停、进度条、音量）
- [ ] 全屏播放支持
- [ ] 基础错误处理

### 阶段2：增强播放功能
- [ ] 网络视频流播放（HTTP/HTTPS）
- [ ] 自适应流媒体支持（HLS、DASH）
- [ ] 字幕支持（SRT、VTT、VobSub格式）
- [ ] 多音轨选择
- [ ] 播放速度控制
- [ ] 画中画模式（PiP）

### 阶段3：高级功能
- [ ] 播放列表管理
- [ ] 后台播放支持（MediaSession集成）
- [ ] 媒体下载和离线播放（PreCacheHelper）
- [ ] 播放历史记录和继续播放
- [ ] Chromecast投屏支持

### 阶段4：用户体验优化
- [ ] 智能缓冲和预加载
- [ ] 手势控制（亮度、音量、快进/快退）
- [ ] 视频质量选择器
- [ ] 主题和界面自定义
- [ ] 无障碍功能支持

## 常见问题

### Q1: Media3 1.10.0与之前版本的主要区别？
A: 主要区别包括：Material3风格播放控件、PreCacheHelper预缓存支持、VobSub字幕轨道支持、改进的擦洗模式处理。

### Q2: 如何处理不同视频格式的兼容性问题？
A: Media3支持广泛的视频格式，对于不支持的格式，可以尝试使用软件解码器或提示用户安装相应的编解码器扩展。

### Q3: 如何优化视频播放的电池消耗？
A: 使用合适的缓冲策略、及时释放播放器实例、在后台播放时优化网络使用、使用硬件解码器。

### Q4: 如何实现安全的DRM保护内容播放？
A: Media3支持Widevine、PlayReady等DRM方案，需要配置相应的许可证服务器和DRM会话管理器。

### Q5: 媒体下载功能的最佳实践是什么？
A: 使用DownloadManager管理下载任务，集成PreCacheHelper进行智能预缓存，实现下载状态监听和错误恢复，提供存储空间管理。

## 参考资料

1. [Media3官方文档](https://developer.android.com/media/media3)
2. [Media3 1.10.0发布说明](https://developer.android.com/jetpack/androidx/releases/media3)
3. [Media3 GitHub仓库](https://github.com/androidx/media)
4. [Compose与Media3集成指南](https://developer.android.com/media/media3/ui/compose)
5. [自适应流媒体实现指南](https://developer.android.com/guide/topics/media/adaptive-streaming)
6. [Android媒体播放最佳实践](https://developer.android.com/media/playback)

---
**文档版本**: 2.0
**更新日期**: 2026-03-31
**适用模块**: feature/videoplayer
**Media3版本**: 1.10.0
**架构规范**: 与项目现有架构保持一致，使用MVVM模式，遵循模块化设计原则