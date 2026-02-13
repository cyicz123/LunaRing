package com.example.menstruation.update

import com.example.menstruation.BuildConfig
import com.example.menstruation.update.model.ReleaseInfo
import com.example.menstruation.update.model.UpdateCheckResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    private val gitHubReleaseService: GitHubReleaseService,
    private val apkDownloadManager: ApkDownloadManager
) {
    fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        val latestResult = gitHubReleaseService.fetchLatestRelease(
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO
        )

        return latestResult.fold(
            onSuccess = { releaseInfo ->
                if (isVersionNewer(currentVersionName, releaseInfo.versionName)) {
                    UpdateCheckResult.UpdateAvailable(releaseInfo)
                } else {
                    UpdateCheckResult.UpToDate
                }
            },
            onFailure = { error ->
                UpdateCheckResult.Error(error.message ?: "检查更新失败")
            }
        )
    }

    fun downloadReleaseApk(
        releaseInfo: ReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> {
        return apkDownloadManager.downloadApk(releaseInfo, onProgress)
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.normalizeVersionParts()
        val latestParts = latest.normalizeVersionParts()
        val maxSize = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxSize) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    private fun String.normalizeVersionParts(): List<Int> {
        return trim()
            .removePrefix("v")
            .split(".")
            .map { token ->
                token.takeWhile { it.isDigit() }
                    .toIntOrNull() ?: 0
            }
    }
}
