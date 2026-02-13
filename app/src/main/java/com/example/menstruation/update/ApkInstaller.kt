package com.example.menstruation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class InstallResult {
    data object StartedInstaller : InstallResult()
    data object NeedUnknownSourcePermission : InstallResult()
    data class Error(val message: String) : InstallResult()
}

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun installApk(apkFile: File): InstallResult {
        if (!apkFile.exists()) {
            return InstallResult.Error("安装包不存在")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return InstallResult.NeedUnknownSourcePermission
        }

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            InstallResult.StartedInstaller
        }.getOrElse { error ->
            InstallResult.Error(error.message ?: "拉起安装器失败")
        }
    }
}
