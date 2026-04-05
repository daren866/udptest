package com.mjc.core.network

import com.mjc.core.network.api.ApiService
import com.mjc.core.network.client.HttpClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

/**
 * 网络模块依赖注入
 * 提供通用网络相关的依赖实例
 */
object NetworkModule {

    private var retrofitCache: MutableMap<String, Retrofit> = mutableMapOf()
    private var httpClientCache: MutableMap<String, OkHttpClient> = mutableMapOf()

    /**
     * 创建Retrofit实例
     * @param baseUrl 基础URL
     * @param client OkHttpClient实例（可选，默认使用缓存的默认HTTP客户端）
     */
    fun createRetrofit(
        baseUrl: String,
        client: OkHttpClient = getOrCreateDefaultHttpClient()
    ): Retrofit {
        val cacheKey = generateRetrofitCacheKey(baseUrl, client)
        return retrofitCache.getOrPut(cacheKey) {
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
        }
    }

    /**
     * 生成HTTP客户端缓存键
     */
    private fun generateHttpClientCacheKey(
        type: String,
        cacheDir: File? = null,
        cacheSize: Long = 0
    ): String {
        val cacheDirPart = cacheDir?.absolutePath ?: "no-cache"
        return "$type:$cacheDirPart:$cacheSize"
    }

    /**
     * 获取或创建默认HTTP客户端（带缓存）
     * @param cacheDir 缓存目录（可选）
     * @param cacheSize 缓存大小（字节，默认10MB）
     */
    fun getOrCreateDefaultHttpClient(
        cacheDir: File? = null,
        cacheSize: Long = 10 * 1024 * 1024 // 10MB
    ): OkHttpClient {
        val cacheKey = generateHttpClientCacheKey("default", cacheDir, cacheSize)
        return httpClientCache.getOrPut(cacheKey) {
            HttpClient.createDefault(cacheDir, cacheSize)
        }
    }

    /**
     * 生成Retrofit缓存键
     */
    private fun generateRetrofitCacheKey(
        baseUrl: String,
        client: OkHttpClient
    ): String {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${normalizedBaseUrl}-${client.hashCode()}"
    }

    /**
     * 创建API服务实例
     * @param baseUrl 基础URL
     * @param serviceClass API服务接口类
     * @param client OkHttpClient实例（可选，默认使用createDefault）
     */
    fun <T> createApiService(
        baseUrl: String,
        serviceClass: Class<T>,
        client: OkHttpClient = getOrCreateDefaultHttpClient()
    ): T {
        val retrofit = createRetrofit(baseUrl, client)
        return retrofit.create(serviceClass)
    }

    /**
     * 创建API服务实例（使用reified类型参数）
     * @param baseUrl 基础URL
     * @param client OkHttpClient实例（可选，默认使用createDefault）
     */
    inline fun <reified T> createApiService(
        baseUrl: String,
        client: OkHttpClient = getOrCreateDefaultHttpClient()
    ): T {
        return createApiService(baseUrl, T::class.java, client)
    }

    /**
     * 创建默认的ApiService实例
     * @param baseUrl 基础URL
     * @param client OkHttpClient实例（可选，默认使用createDefault）
     */
    fun createDefaultApiService(
        baseUrl: String,
        client: OkHttpClient = getOrCreateDefaultHttpClient()
    ): ApiService {
        return createApiService(baseUrl, ApiService::class.java, client)
    }
}
