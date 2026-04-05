# 核心网络模块指南

## 模块概述

此模块提供了统一的网络请求功能，基于OkHttp和Retrofit构建，支持HTTP/HTTPS请求、拦截器、错误处理等功能。作为核心模块，为其他功能模块提供网络能力。

**模块信息：**
- **包名**: `com.mjc.core.network`
- **依赖**: OkHttp, Retrofit, Moshi, Kotlin Coroutines
- **架构**: 基于Retrofit的声明式API

## 模块结构

### 目录结构
模块主要包含以下目录和文件：
- **api/**: API接口定义（ApiService.kt, ApiEndpoints.kt, DownloadApiService.kt）
- **client/**: HTTP客户端配置（HttpClient.kt, RetrofitClient.kt, NetworkConfig.kt, DownloadHttpClient.kt）
- **config/**: 配置类（DownloadConfig.kt）
- **interceptor/**: 拦截器（AuthInterceptor.kt, LoggingInterceptor.kt, RetryInterceptor.kt, RangeInterceptor.kt）
- **model/**: 数据模型（ApiResponse.kt, ApiError.kt, NetworkResult.kt）
- **utils/**: 工具类（NetworkUtils.kt, MoshiUtils.kt, CoroutineUtils.kt, DownloadUtils.kt）
- **ext/**: 扩展函数（ResponseExt.kt）
- **NetworkModule.kt**: 依赖注入模块

## 核心技术栈

### 核心依赖
1. **HTTP客户端**: OkHttp 4.12.0
2. **REST客户端**: Retrofit 2.11.0
3. **JSON解析**: Moshi 1.16.0
4. **异步处理**: Kotlin Coroutines 1.8.0
5. **日志**: OkHttp Logging Interceptor

### 主要功能
1. **统一HTTP客户端**: 配置超时、缓存、拦截器等
2. **声明式API**: 基于Retrofit的接口定义
3. **错误处理**: 统一的网络错误处理机制
4. **拦截器**: 认证、日志、重试等拦截器
5. **协程支持**: 使用Suspend函数进行异步调用
6. **文件下载**: 完整的文件下载功能，支持断点续传和进度监听
7. **灵活配置**: 通过DownloadConfig类提供可定制的下载参数

## 架构设计

### 网络层架构
网络层采用分层架构：功能模块调用ApiService（Retrofit接口），经过RetrofitClient、HttpClient和拦截器链（认证、日志、重试），最终发送网络请求。

### 响应封装
使用统一的ApiResponse密封类封装API响应，包含Success、Error和Exception三种状态，分别表示成功响应、API错误和网络异常。

## 配置要求

### 权限配置
已在 `AndroidManifest.xml` 中声明以下权限：`android.permission.INTERNET` 和 `android.permission.ACCESS_NETWORK_STATE`。

### 构建配置
模块使用以下依赖：
- OkHttp: HTTP客户端
- Retrofit: REST客户端
- Moshi: JSON解析
- Kotlin Coroutines: 异步处理

## 使用指南

### 基本使用流程
1. 使用DownloadApiService接口进行下载操作，该接口支持断点续传、条件请求和文件信息获取
2. 通过NetworkModule.createDownloadApiService()获取下载API服务实例
3. 调用下载方法发起请求，处理响应结果

### 统一网络结果处理
使用safeApiCall函数封装网络请求，返回统一的ApiResponse结果类型，包含Success、Error和Exception三种状态，简化错误处理和状态管理。

## 下载功能

网络模块提供完整的文件下载功能，支持断点续传、进度监听和灵活的配置选项。

### 核心组件

1. **DownloadApiService** - 下载API接口，支持Range请求和条件请求头
2. **DownloadHttpClient** - 下载专用HTTP客户端，集成RangeInterceptor和重试逻辑
3. **DownloadConfig** - 下载配置类，提供超时、重试、缓存等配置
4. **RangeInterceptor** - Range请求拦截器，支持多范围请求和条件验证
5. **DownloadUtils** - 下载工具类，提供进度计算、文件大小格式化等功能
6. **ResponseExt** - 响应扩展函数，简化文件写入和进度监听

### 主要功能

#### 断点续传
支持HTTP Range请求，可从断点处继续下载。通过ETag或最后修改时间验证文件一致性，确保断点续传时文件未更改。

#### 进度监听
提供ProgressListener接口和ProgressInterceptor拦截器，支持实时下载进度和速度显示。可通过扩展函数简化进度监听实现。

#### 灵活配置
通过DownloadConfig类提供超时时间、重试次数、日志级别等配置选项，支持创建自定义的HTTP客户端和下载服务。

#### 错误处理与重试
定义DownloadError密封类表示各种下载错误类型，提供安全下载函数封装错误处理逻辑，支持自动重试机制。

#### 实用工具
提供文件大小格式化、Range头解析与生成、内容范围解析等实用工具函数。

### 性能优化建议

1. **缓冲区大小**: 根据文件大小调整缓冲区（默认8KB）
2. **连接复用**: 使用相同的HTTP客户端进行多次下载
3. **并行下载**: 对大文件使用多范围并行下载
4. **内存管理**: 使用流式写入，避免将大文件加载到内存
5. **缓存策略**: 对频繁访问的文件启用HTTP缓存

### 注意事项

1. **Range请求支持**: 不是所有服务器都支持Range请求，使用前检查`Accept-Ranges`头
2. **ETag验证**: 使用ETag确保断点续传时文件未更改
3. **存储权限**: Android 10+需要适当的存储权限
4. **后台下载**: 长时间下载应考虑使用WorkManager或Foreground Service
5. **网络状态**: 下载前检查网络连接状态

## 客户端配置

### OkHttp客户端配置
通过HttpClient.create()方法创建OkHttpClient实例，支持连接超时、读取超时、写入超时配置，可添加日志拦截器、认证拦截器、重试拦截器，支持缓存配置。

### Retrofit客户端配置
通过RetrofitClient.create()方法创建Retrofit实例，支持基础URL配置、自定义HTTP客户端、Moshi JSON转换器。

## 拦截器

### 日志拦截器
LoggingInterceptor记录HTTP请求和响应的详细信息，便于调试和监控。

### 认证拦截器
AuthInterceptor自动为请求添加Authorization头部，支持Bearer Token认证。

### 重试拦截器
RetryInterceptor在网络错误或服务器错误时自动重试请求，支持配置最大重试次数。

## 错误处理

### 网络错误类型
使用NetworkError密封类定义各种网络错误类型，包括HTTP错误、网络异常、超时错误、序列化错误和未知错误。

### 错误处理工具
提供Throwable.toNetworkError()扩展函数将异常转换为统一的NetworkError类型，简化错误类型判断和处理。

## 依赖注入

### 网络模块
NetworkModule提供API服务实例的创建和管理，支持缓存Retrofit实例以提高性能，提供泛型方法简化服务创建。

## 测试策略

### 单元测试
使用MockWebServer测试API请求，测试拦截器逻辑和错误处理。

### 集成测试
测试实际网络请求、超时和重试逻辑、缓存机制。

### MockWebServer
使用MockWebServer模拟服务器响应，测试各种网络场景。

## 性能优化

### 连接池优化
配置ConnectionPool参数优化连接复用，设置最大空闲连接数和连接保持时间。

### 缓存优化
配置HTTP缓存大小和存储位置，减少重复网络请求。

### 超时设置
根据网络类型（Wi-Fi、移动网络、慢速网络）设置不同的超时时间。

## 安全考虑

### HTTPS证书验证
- 使用系统信任的证书
- 支持证书锁定（Certificate Pinning）

### 敏感信息保护
- API密钥通过拦截器添加，避免硬编码
- 日志中过滤敏感信息
- 使用安全的存储方式保存凭证

### 网络安全性
- 强制使用HTTPS
- 支持TLS 1.2+
- 验证主机名

## 兼容性考虑

### Android版本兼容
- 支持Android 10+（minSdk=29）
- 适配不同Android版本的网络行为

### 网络类型兼容
- Wi-Fi和移动网络
- 代理服务器支持
- VPN网络支持

## 使用建议

### 最佳实践
1. **使用协程**: 优先使用`suspend`函数进行网络调用
2. **统一错误处理**: 使用`ApiResponse`封装网络结果
3. **合理配置超时**: 根据业务需求设置合适的超时时间
4. **启用日志**: 开发阶段启用日志拦截器，生产环境关闭

### 注意事项
1. **主线程安全**: 网络请求不要在主线程执行
2. **内存管理**: 及时关闭响应体，避免内存泄漏
3. **电池优化**: 避免频繁的网络请求，合并请求

## 功能路线图

### 阶段1: 基础功能（当前）
- [x] HTTP/HTTPS请求支持
- [x] 统一错误处理
- [x] 基本拦截器（日志、认证、重试）

### 阶段2: 增强功能
- [x] 文件上传/下载支持（已完成）
- [ ] WebSocket支持
- [ ] 更复杂的缓存策略
- [ ] 网络状态监听

### 阶段3: 高级功能
- [ ] 请求优先级管理
- [ ] 自适应网络质量
- [ ] 请求合并与去重
- [ ] 离线缓存支持

## 常见问题

### Q1: 如何处理网络超时？
A: 使用OkHttp的超时设置，并根据网络类型动态调整超时时间。

### Q2: 如何实现文件下载？
A: 网络模块提供了完整的下载功能，包括：
1. **使用DownloadApiService**进行下载请求，支持断点续传和条件请求头
2. **通过DownloadConfig配置**超时、重试、缓存等参数
3. **使用ResponseExt扩展函数**将响应体写入文件，支持进度监听
4. **实现断点续传**通过Range请求和ETag验证
5. **进度监听**通过ProgressListener接口实时获取下载进度
详细信息请参阅"下载功能"章节。

### Q3: 如何测试网络模块？
A: 使用MockWebServer模拟服务器响应，测试各种网络场景。

### Q4: 如何优化网络性能？
A: 使用连接池、合理设置缓存、启用HTTP/2、压缩响应体等。

## 参考资料

1. [OkHttp官方文档](https://square.github.io/okhttp/)
2. [Retrofit官方文档](https://square.github.io/retrofit/)
3. [Moshi官方文档](https://github.com/square/moshi)
4. [Android网络最佳实践](https://developer.android.com/training/basics/network-ops)

---
**文档版本**: 1.2
**更新日期**: 2026-04-01
**适用模块**: core:network
