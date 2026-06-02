package com.rock.pixelplay.ai.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object ModelsManager {
    private const val MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
    const val MODEL_FILENAME = "ggml-tiny.bin"

    @Volatile
    private var isCancelled = false

    fun getModelFile(context: Context): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelDownloaded(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > 10 * 1024 * 1024
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    fun cancelDownload() {
        isCancelled = true
    }

    fun downloadModel(context: Context, callback: DownloadCallback) {
        isCancelled = false
        val targetFile = getModelFile(context)
        val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

        thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(MODEL_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val existingLength = if (tempFile.exists()) tempFile.length() else 0L
                if (existingLength > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingLength-")
                }

                connection.connect()

                val responseCode = connection.responseCode
                val append = responseCode == HttpURLConnection.HTTP_PARTIAL

                val fileLength = if (append) {
                    connection.contentLength.toLong() + existingLength
                } else {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    connection.contentLength.toLong()
                }

                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw Exception("Server returned code $responseCode")
                }

                val input = connection.inputStream
                val output = FileOutputStream(tempFile, append)

                val data = ByteArray(4096)
                var total: Long = if (append) existingLength else 0L
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    if (isCancelled) {
                        output.close()
                        input.close()
                        return@thread
                    }
                    total += count
                    output.write(data, 0, count)

                    callback.onProgress(total, fileLength)
                }

                output.close()
                input.close()

                if (isCancelled) {
                    return@thread
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)

                callback.onComplete(targetFile)
            } catch (e: Exception) {
                callback.onError(e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    interface DownloadCallback {
        fun onProgress(downloaded: Long, total: Long)
        fun onComplete(file: File)
        fun onError(e: Exception)
    }
}