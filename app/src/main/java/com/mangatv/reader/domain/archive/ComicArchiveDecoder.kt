package com.mangatv.reader.domain.archive

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mangatv.reader.domain.model.ComicInfoMetadata
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

interface ComicArchiveDecoder : Closeable {
    val filePath: String

    fun open()
    fun getPageCount(): Int
    fun getPageNames(): List<String>
    fun getPageStream(index: Int): InputStream?
    fun getPageBitmap(index: Int, targetWidth: Int = 1920, targetHeight: Int = 1080): Bitmap?
    fun getComicInfo(): ComicInfoMetadata?

    /**
     * Extracts cover image (first page or designated cover) into a local disk thumbnail file
     */
    fun extractCoverThumbnail(destFile: File, maxWidth: Int = 360, maxHeight: Int = 540): Boolean {
        return try {
            val bitmap = getPageBitmap(0, maxWidth, maxHeight) ?: return false
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

/**
 * Utility functions for image decoding and downsampling to prevent TV Out-Of-Memory errors
 */
object ImageDecoderUtils {
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")

    fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return IMAGE_EXTENSIONS.contains(ext) && !name.contains("__MACOSX") && !name.startsWith(".")
    }

    fun decodeSampledBitmapFromStream(
        streamProvider: () -> InputStream?,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            streamProvider()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            streamProvider()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }
}
