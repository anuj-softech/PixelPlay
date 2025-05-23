package com.rock.pixelplay.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

object DiskBitmapCache {
    private lateinit var cacheDir: File

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "thumbCache").apply { mkdirs() }
    }

    fun get(key: String): Bitmap? {
        val file = File(cacheDir, key.md5())
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun put(key: String, bitmap: Bitmap?) {
        if (bitmap == null || bitmap.byteCount >= 200_000) return
        val file = File(cacheDir, key.md5())
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
        }
    }

    private fun String.md5(): String {
        return MessageDigest.getInstance("MD5").digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
