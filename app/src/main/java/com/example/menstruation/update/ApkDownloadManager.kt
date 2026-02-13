package com.example.menstruation.update

import android.content.Context
import android.os.Environment
import com.example.menstruation.update.model.ReleaseInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    fun downloadApk(
        releaseInfo: ReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> {
        return runCatching {
            val request = Request.Builder()
                .url(releaseInfo.apkDownloadUrl)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("下载失败: HTTP ${response.code}")
                }

                val body = response.body ?: error("下载失败: 响应体为空")
                val totalBytes = body.contentLength()
                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val safeName = releaseInfo.apkName.ifBlank { "LunaRing-${releaseInfo.tagName}.apk" }
                val targetFile = File(targetDir, safeName)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read: Int
                        var downloadedBytes = 0L
                        var lastProgress = -1
                        while (input.read(buffer).also { read = it } >= 0) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0L) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (progress != lastProgress) {
                                    onProgress(progress)
                                    lastProgress = progress
                                }
                            }
                        }
                        output.flush()
                    }
                }

                onProgress(100)
                targetFile
            }
        }
    }
}
