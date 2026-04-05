package com.mjc.core.download.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.regex.Pattern

/**
 * Range请求拦截器
 * 验证和处理HTTP Range请求头，支持断点续传
 */
class RangeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val rangeHeader = request.header("Range")

        // 如果有Range头，验证格式
        rangeHeader?.let { range ->
            validateRangeHeader(range)
        }

        // 验证条件请求头（如果存在）
        validateConditionalHeaders(request)

        // 继续请求
        val response = chain.proceed(request)

        // 检查响应状态码
        if (rangeHeader != null) {
            validateRangeResponse(response, rangeHeader)
        }

        return response
    }

    /**
     * 验证Range头格式
     * @param rangeHeader Range头值（如"bytes=1024-"或"bytes=1024-2048"）
     * @throws IllegalArgumentException 如果格式无效
     */
    private fun validateRangeHeader(rangeHeader: String) {
        val trimmed = rangeHeader.trim()
        if (!trimmed.startsWith("bytes=")) {
            throw IllegalArgumentException("无效的Range头格式: $rangeHeader。必须以'bytes='开头")
        }

        val rangesPart = trimmed.substring(6) // 去掉"bytes="
        if (rangesPart.isEmpty()) {
            throw IllegalArgumentException("Range头不能为空: $rangeHeader")
        }

        // 支持多Range：bytes=0-100,200-300,300-
        val ranges = rangesPart.split(",")
        ranges.forEach { range ->
            val matcher = SINGLE_RANGE_PATTERN.matcher(range.trim())
            if (!matcher.matches()) {
                throw IllegalArgumentException("无效的Range范围格式: '$range'。正确的格式应为: start-end 或 start-")
            }

            val startStr = matcher.group(1)
            val endStr = matcher.group(2)

            try {
                val start = startStr?.toLong()?: -1
                if (start < 0) {
                    throw IllegalArgumentException("Range起始位置不能为负数: $start")
                }

                // 如果有结束位置，验证结束位置大于等于起始位置
                if (!endStr.isNullOrEmpty()) {
                    val end = endStr.toLong()
                    if (end < start) {
                        throw IllegalArgumentException("Range结束位置不能小于起始位置: $end < $start")
                    }
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Range头包含无效的数字: '$range'", e)
            }
        }
    }

    /**
     * 验证条件请求头
     */
    private fun validateConditionalHeaders(request: Request) {
        // If-Range验证：应该是ETag或HTTP-date格式
        val ifRange = request.header("If-Range")
        ifRange?.let { value ->
            if (!isValidETag(value) && !isValidHttpDate(value)) {
                throw IllegalArgumentException("无效的If-Range头格式: $value。必须是ETag或HTTP-date格式")
            }
        }

        // If-Match验证：应该是ETag列表
        val ifMatch = request.header("If-Match")
        ifMatch?.let { value ->
            if (!isValidETagList(value)) {
                throw IllegalArgumentException("无效的If-Match头格式: $value。必须是ETag列表（逗号分隔）")
            }
        }

        // If-Modified-Since验证：应该是HTTP-date格式
        val ifModifiedSince = request.header("If-Modified-Since")
        ifModifiedSince?.let { value ->
            if (!isValidHttpDate(value)) {
                throw IllegalArgumentException("无效的If-Modified-Since头格式: $value。必须是HTTP-date格式")
            }
        }
    }

    /**
     * 验证Range响应
     */
    private fun validateRangeResponse(response: Response, rangeHeader: String) {
        when (response.code) {
            // 206 Partial Content - 期望的响应
            206 -> {
                // 验证Content-Range头
                val contentRange = response.header("Content-Range")
                    ?: throw IllegalStateException("服务器返回206状态码但缺少Content-Range头")
                if (!isValidContentRange(contentRange)) {
                    throw IllegalStateException("无效的Content-Range头格式: $contentRange")
                }
            }
            // 200 OK - 服务器可能忽略Range头，返回完整内容
            // 416 Range Not Satisfiable - 请求范围无效
            // 其他状态码 - 按正常响应处理
        }
    }

    /**
     * 检查是否为有效的ETag
     */
    private fun isValidETag(etag: String): Boolean {
        // ETag格式: W/"etag_value" 或 "etag_value"
        return ETAG_PATTERN.matcher(etag.trim()).matches()
    }

    /**
     * 检查是否为有效的ETag列表
     */
    private fun isValidETagList(etagList: String): Boolean {
        val etags = etagList.split(",").map { it.trim() }
        return etags.all { isValidETag(it) }
    }

    /**
     * 检查是否为有效的HTTP-date格式（简化验证）
     */
    private fun isValidHttpDate(date: String): Boolean {
        return date.isNotBlank()
    }

    /**
     * 检查是否为有效的Content-Range头
     */
    private fun isValidContentRange(contentRange: String): Boolean {
        return CONTENT_RANGE_PATTERN.matcher(contentRange.trim()).matches()
    }

    companion object {
        private val SINGLE_RANGE_PATTERN = Pattern.compile("^(\\d+)-(\\d*)$")
        private val ETAG_PATTERN = Pattern.compile("^(W/\")?\"[^\"]+\"$")
        private val CONTENT_RANGE_PATTERN = Pattern.compile("^bytes (\\d+)-(\\d*)/(\\d*|\\*)$")

        /**
         * 创建Range拦截器实例
         */
        fun create(): RangeInterceptor = RangeInterceptor()
    }
}
