package com.mjc.core.network.client

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * HTTP客户端配置
 * 提供统一的OkHttpClient实例
 */
object HttpClient {
    private val clientCache = mutableMapOf<String, OkHttpClient>()

    /**
     * 生成HTTP客户端缓存键
     */
    private fun generateCacheKey(cacheDir: File?, cacheSize: Long): String {
        val cacheDirPart = cacheDir?.absolutePath ?: "no-cache"
        return "default:$cacheDirPart:$cacheSize"
    }

    /**
     * 创建默认的HTTP客户端（带缓存）
     * @param cacheDir 缓存目录（可选）
     * @param cacheSize 缓存大小（字节，默认10MB）
     */
    fun createDefault(
        cacheDir: File? = null,
        cacheSize: Long = 10 * 1024 * 1024 // 10MB
    ): OkHttpClient {
        val cacheKey = generateCacheKey(cacheDir, cacheSize)
        return clientCache.getOrPut(cacheKey) {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

            // 添加缓存（如果提供了缓存目录）
            cacheDir?.let {
                val cache = Cache(File(it, "http-cache"), cacheSize)
                builder.cache(cache)
            }

            builder.build()
        }
    }

    /**
     * 创建带日志拦截器的HTTP客户端（用于调试）
     */
    fun createWithLogging(
        cacheDir: File? = null
    ): OkHttpClient {
        val cacheKey = "with-logging:${cacheDir?.absolutePath ?: "no-cache"}"
        return clientCache.getOrPut(cacheKey) {
            createDefault(cacheDir)
                .newBuilder()
                // 在实际项目中，这里可以添加日志拦截器
                // .addInterceptor(HttpLoggingInterceptor().apply {
                //     level = HttpLoggingInterceptor.Level.BASIC
                // })
                .build()
        }
    }
}