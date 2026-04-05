package com.mjc.core.download.client

import com.mjc.core.download.config.DownloadConfig
import com.mjc.core.download.interceptor.RangeInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 下载专用HTTP客户端
 * 针对文件下载场景优化：
 * - 更长的超时时间（大文件下载）
 * - 支持Range请求（断点续传）
 * - 可选的缓存配置
 */
object DownloadHttpClient {
    private val clientCache = mutableMapOf<String, OkHttpClient>()

    /**
     * 生成下载客户端缓存键
     */
    private fun generateCacheKey(cacheDir: File?, cacheSize: Long, enableRangeSupport: Boolean): String {
        val cacheDirPart = cacheDir?.absolutePath ?: "no-cache"
        return "download:$cacheDirPart:$cacheSize:$enableRangeSupport"
    }

    /**
     * 生成带重试的下载客户端缓存键
     */
    private fun generateRetryCacheKey(cacheDir: File?, maxRetries: Int, enableRangeSupport: Boolean): String {
        val cacheDirPart = cacheDir?.absolutePath ?: "no-cache"
        return "download-retry:$cacheDirPart:$maxRetries:$enableRangeSupport"
    }

    /**
     * 创建下载专用的HTTP客户端（带缓存）
     * @param cacheDir 缓存目录（可选）
     * @param cacheSize 缓存大小（字节，默认20MB）
     * @param enableRangeSupport 是否启用Range请求支持（默认启用）
     */
    fun create(
        cacheDir: File? = null,
        cacheSize: Long = 20 * 1024 * 1024, // 20MB
        enableRangeSupport: Boolean = true
    ): OkHttpClient {
        val cacheKey = generateCacheKey(cacheDir, cacheSize, enableRangeSupport)
        return clientCache.getOrPut(cacheKey) {
            val builder = OkHttpClient.Builder()
                // 下载大文件需要更长的超时时间
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS) // 5分钟读取超时
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)

            // 添加Range拦截器（支持断点续传）
            if (enableRangeSupport) {
                builder.addInterceptor(RangeInterceptor())
            }

            builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })

            // 添加缓存（如果提供了缓存目录）
            cacheDir?.let {
                val cache = Cache(File(it, "download-cache"), cacheSize)
                builder.cache(cache)
            }

            builder.build()
        }
    }

    /**
     * 创建支持重试的下载客户端（带缓存）
     * @param maxRetries 最大重试次数（默认3次）
     */
    fun createWithRetry(
        cacheDir: File? = null,
        maxRetries: Int = 3,
        enableRangeSupport: Boolean = true
    ): OkHttpClient {
        val cacheKey = generateRetryCacheKey(cacheDir, maxRetries, enableRangeSupport)
        return clientCache.getOrPut(cacheKey) {
            create(cacheDir, enableRangeSupport = enableRangeSupport)
                .newBuilder()
                .addInterceptor(RetryInterceptor(maxRetries))
                .build()
        }
    }

    /**
     * 根据DownloadConfig创建下载客户端
     * @param config 下载配置
     */
    fun create(config: DownloadConfig): OkHttpClient {
        val cacheKey = generateConfigCacheKey(config)
        return clientCache.getOrPut(cacheKey) {
            val builder = OkHttpClient.Builder()
                .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(config.followRedirects)
                .followSslRedirects(config.followSslRedirects)

            // Range拦截器
            if (config.enableRangeSupport) {
                builder.addInterceptor(RangeInterceptor())
            }

            // 重试拦截器
            if (config.enableRetry) {
                builder.addInterceptor(RetryInterceptor(config.maxRetries, config.retryDelayMs))
            }

            // 缓存
            if (config.enableCache && config.cacheDir != null) {
                val cache = Cache(config.cacheDir, config.cacheSize)
                builder.cache(cache)
            }

            // 日志拦截器
            if (config.enableLogging) {
                val loggingLevel = when (config.loggingLevel) {
                    com.mjc.core.download.config.LoggingLevel.BASIC -> HttpLoggingInterceptor.Level.BASIC
                    com.mjc.core.download.config.LoggingLevel.HEADERS -> HttpLoggingInterceptor.Level.HEADERS
                    com.mjc.core.download.config.LoggingLevel.BODY -> HttpLoggingInterceptor.Level.BODY
                    else -> HttpLoggingInterceptor.Level.NONE
                }
                if (loggingLevel != HttpLoggingInterceptor.Level.NONE) {
                    builder.addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
                }
            }

            // SSL验证
            if (!config.sslVerification) {
                builder.hostnameVerifier { _, _ -> true }
            }

            builder.build()
        }
    }

    /**
     * 根据DownloadConfig生成缓存键
     */
    private fun generateConfigCacheKey(config: DownloadConfig): String {
        val cacheDirPart = config.cacheDir?.absolutePath ?: "no-cache"
        return "download-config:$cacheDirPart:${config.cacheSize}:${config.enableRangeSupport}:${config.maxRetries}:${config.enableLogging}"
    }
}

/**
 * 重试拦截器（专门针对下载场景优化）
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000 // 初始重试延迟1秒
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response
        var retryCount = 0

        while (true) {
            try {
                response = chain.proceed(request)

                // 对于下载，我们主要关心服务器错误和网络错误
                when {
                    // 成功响应（包括部分内容206）
                    response.isSuccessful || response.code == 206 -> {
                        return response
                    }
                    // 服务器错误（5xx）可以重试
                    response.code in 500..599 && retryCount < maxRetries -> {
                        retryCount++
                        response.close()
                        Thread.sleep(calculateExponentialBackoff(retryCount))
                        continue
                    }
                    // 客户端错误（4xx）通常不应该重试（除非是429 Too Many Requests）
                    response.code == 429 && retryCount < maxRetries -> {
                        retryCount++
                        response.close()
                        val retryAfter = response.header("Retry-After")?.toIntOrNull() ?: 5
                        Thread.sleep(retryAfter * 1000L)
                        continue
                    }
                    else -> {
                        return response
                    }
                }
            } catch (e: Exception) {
                // 网络异常（连接超时、读取超时等）可以重试
                if (retryCount < maxRetries && isRetryableException(e)) {
                    retryCount++
                    Thread.sleep(calculateExponentialBackoff(retryCount))
                    continue
                }
                throw e
            }
        }
    }

    /**
     * 判断异常是否可重试
     */
    private fun isRetryableException(e: Exception): Boolean {
        return when (e) {
            // 网络超时异常 - 可重试
            is java.net.SocketTimeoutException -> true // 连接或读取超时
            // 连接相关异常 - 可重试
            is java.net.ConnectException -> true      // 连接被拒绝
            is java.net.NoRouteToHostException -> true // 无法路由到主机
            is java.net.PortUnreachableException -> true // 端口不可达
            // 主机解析异常 - 可重试
            is java.net.UnknownHostException -> true  // 域名解析失败
            is java.net.UnknownServiceException -> true // 未知服务异常
            // Socket异常 - 可重试
            is java.net.SocketException -> true       // Socket错误
            // HTTP相关异常 - 可重试
            is java.net.HttpRetryException -> true    // HTTP重试
            // 协议错误 - 不可重试（需要修复代码）
            is java.net.ProtocolException -> false    // 协议错误通常不应该重试
            // SSL相关异常 - 不可重试（通常需要用户干预）
            is javax.net.ssl.SSLException -> false    // SSL错误通常不应该重试
            is javax.net.ssl.SSLHandshakeException -> false // SSL握手失败
            // 其他IO异常 - 可重试
            else -> e is java.io.IOException          // 其他IO异常
        }
    }

    /**
     * 计算指数退避延迟时间
     */
    private fun calculateExponentialBackoff(retryCount: Int): Long {
        return retryDelayMs * (1L shl (retryCount - 1)) // 指数退避：1, 2, 4, 8秒...
    }
}
