package com.example.menstruation.update

import com.example.menstruation.update.model.ReleaseInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubReleaseService @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchLatestRelease(owner: String, repo: String): Result<ReleaseInfo> {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("GitHub API 请求失败: HTTP ${response.code}")
                }
                val rawBody = response.body?.string().orEmpty()
                if (rawBody.isBlank()) {
                    error("GitHub API 返回为空")
                }

                val root = json.parseToJsonElement(rawBody).jsonObject
                val tagName = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
                val body = root["body"]?.jsonPrimitive?.content.orEmpty()
                val htmlUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty()
                val publishedAt = root["published_at"]?.jsonPrimitive?.content.orEmpty()
                val assets = root["assets"]?.jsonArray.orEmpty()

                val apkAsset = assets.firstOrNull { assetElement ->
                    val name = assetElement.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
                    name.endsWith(".apk", ignoreCase = true)
                }?.jsonObject ?: error("Release 中未找到 APK 资产")

                val apkName = apkAsset["name"]?.jsonPrimitive?.content.orEmpty()
                val apkUrl = apkAsset["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
                val apkSize = apkAsset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                if (tagName.isBlank() || apkUrl.isBlank()) {
                    error("Release 信息不完整")
                }

                ReleaseInfo(
                    tagName = tagName,
                    versionName = tagName.removePrefix("v"),
                    releaseNotes = body,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt,
                    apkDownloadUrl = apkUrl,
                    apkName = apkName,
                    apkSizeBytes = apkSize
                )
            }
        }
    }
}
