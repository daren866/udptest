package com.mjc.feature.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mjc.core.download.DownloadModule as CoreDownloadModule
import com.mjc.feature.download.controller.DownloadController
import com.mjc.feature.download.controller.DownloadControllerImpl
import com.mjc.feature.download.repository.DownloadRepository
import com.mjc.feature.download.utils.FileUtils

/**
 * 下载模块依赖注入
 * 提供下载相关的依赖实例
 */
object DownloadModule {

    /**
     * 创建FileUtils实例
     */
    fun createFileUtils(context: Context): FileUtils {
        return FileUtils(context)
    }

    /**
     * 创建DownloadService实例
     * 使用CoreDownloadModule创建下载服务（抽象接口，不依赖Retrofit）
     */
    fun createDownloadService() = CoreDownloadModule.createDownloadService()

    /**
     * 创建DownloadRepository实例
     */
    fun createDownloadRepository(
        context: Context
    ): DownloadRepository {
        val downloadService = createDownloadService()
        val fileUtils = createFileUtils(context)
        return DownloadRepository(downloadService, fileUtils)
    }

    /**
     * 创建DownloadController实例
     */
    fun createDownloadController(
        context: Context
    ): DownloadController {
        val downloadRepository = createDownloadRepository(context)
        return DownloadControllerImpl(downloadRepository)
    }

    /**
     * 创建DownloadViewModel工厂
     * 用于在Compose中使用viewModel()获取ViewModel实例
     */
    fun createViewModelFactory(context: Context): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                val downloadController = createDownloadController(context)
                DownloadViewModel(downloadController)
            }
        }
    }
}