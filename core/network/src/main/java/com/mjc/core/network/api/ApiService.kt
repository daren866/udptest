package com.mjc.core.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Url

/**
 * 基础API服务接口
 * 提供通用的HTTP请求方法
 */
interface ApiService {

    /**
     * 通用的GET请求
     * @param url 完整的请求URL
     */
    @GET
    suspend fun get(@Url url: String): Response<ResponseBody>

    /**
     * 通用的HEAD请求（用于获取文件信息）
     * @param url 完整的请求URL
     */
    @HEAD
    suspend fun head(@Url url: String): Response<Void>
}