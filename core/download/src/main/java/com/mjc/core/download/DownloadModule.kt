package com.mjc.core.download

import com.mjc.core.download.api.DownloadApiService
import com.mjc.core.download.client.DownloadHttpClient
import com.mjc.core.download.config.DownloadConfig
import com.mjc.core.download.service.DownloadService
import com.mjc.core.download.service.RetrofitDownloadService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

/**
 * 下载模块依赖注入
 * 提供下载相关的依赖实例
 *
 * HTTP客户端缓存由 DownloadHttpClient 管理，
 * 本模块只负责 Retrofit 和 Service 的创建与缓存。
 */
object DownloadModule {
    /** 下载Retrofit的默认基础URL（虚拟URL，实际使用@Url完整URL） */
    private const val DEFAULT_DOWNLOAD_BASE_URL = "https://download.invalid/"

    private val downloadRetrofitCache = mutableMapOf<String, Retrofit>()
    private val downloadServiceCache = mutableMapOf<String, DownloadService>()

    // region HTTP客户端创建（委托DownloadHttpClient）

    /**
     * 获取或创建下载HTTP客户端
     * 委托给 DownloadHttpClient，由其内部缓存
     */
    fun getOrCreateDownloadHttpClient(
        cacheDir: File? = null,
        cacheSize: Long = 20 * 1024 * 1024,
        enableRangeSupport: Boolean = true
    ): OkHttpClient = DownloadHttpClient.create(cacheDir, cacheSize, enableRangeSupport)

    /**
     * 获取或创建带重试的下载HTTP客户端
     */
    fun getOrCreateDownloadHttpClientWithRetry(
        cacheDir: File? = null,
        maxRetries: Int = 3,
        enableRangeSupport: Boolean = true
    ): OkHttpClient = DownloadHttpClient.createWithRetry(cacheDir, maxRetries, enableRangeSupport)

    /**
     * 根据配置创建下载HTTP客户端
     */
    fun createHttpClient(config: DownloadConfig): OkHttpClient = DownloadHttpClient.create(config)

    // endregion

    // region Retrofit创建

    /**
     * 创建下载专用的Retrofit实例
     */
    fun createDownloadRetrofit(
        baseUrl: String = DEFAULT_DOWNLOAD_BASE_URL,
        client: OkHttpClient = getOrCreateDownloadHttpClient()
    ): Retrofit {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cacheKey = "$normalizedBaseUrl-${System.identityHashCode(client)}"
        return downloadRetrofitCache.getOrPut(cacheKey) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
        }
    }

    // endregion

    // region Service创建

    /**
     * 创建DownloadApiService实例
     */
    fun createDownloadApiService(
        baseUrl: String = DEFAULT_DOWNLOAD_BASE_URL,
        client: OkHttpClient = getOrCreateDownloadHttpClient()
    ): DownloadApiService {
        val retrofit = createDownloadRetrofit(baseUrl, client)
        return retrofit.create(DownloadApiService::class.java)
    }

    /**
     * 创建带缓存的DownloadApiService实例
     */
    fun createDownloadApiServiceWithCache(
        baseUrl: String = DEFAULT_DOWNLOAD_BASE_URL,
        cacheDir: File,
        cacheSize: Long = 20 * 1024 * 1024,
        enableRangeSupport: Boolean = true
    ): DownloadApiService {
        val client = getOrCreateDownloadHttpClient(cacheDir, cacheSize, enableRangeSupport)
        return createDownloadApiService(baseUrl, client)
    }

    /**
     * 创建DownloadService实例
     */
    fun createDownloadService(
        baseUrl: String = DEFAULT_DOWNLOAD_BASE_URL,
        client: OkHttpClient = getOrCreateDownloadHttpClient()
    ): DownloadService {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cacheKey = "$normalizedBaseUrl-${System.identityHashCode(client)}"
        return downloadServiceCache.getOrPut(cacheKey) {
            val apiService = createDownloadApiService(baseUrl, client)
            RetrofitDownloadService(apiService)
        }
    }

    /**
     * 创建带缓存的DownloadService实例
     */
    fun createDownloadServiceWithCache(
        baseUrl: String = DEFAULT_DOWNLOAD_BASE_URL,
        cacheDir: File,
        cacheSize: Long = 20 * 1024 * 1024,
        enableRangeSupport: Boolean = true
    ): DownloadService {
        val client = getOrCreateDownloadHttpClient(cacheDir, cacheSize, enableRangeSupport)
        return createDownloadService(baseUrl, client)
    }

    // endregion
}
