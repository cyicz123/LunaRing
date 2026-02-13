package com.example.menstruation.update.model

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkName: String,
    val apkSizeBytes: Long
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
