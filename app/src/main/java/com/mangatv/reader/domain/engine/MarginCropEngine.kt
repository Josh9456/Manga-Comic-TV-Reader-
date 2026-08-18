package com.mangatv.reader.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object MarginCropEngine {

    /**
     * Automatically trims white or dark solid margins around scanned manga/comic panels.
     * Returns a cropped sub-bitmap, or the original bitmap if no significant margin is found.
     */
    fun autoCropMargins(bitmap: Bitmap, tolerance: Int = 18): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 50 || height < 50) return bitmap

        // Sample corner pixels to determine border background color (white or black)
        val cornerColors = intArrayOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(width - 1, 0),
            bitmap.getPixel(0, height - 1),
            bitmap.getPixel(width - 1, height - 1)
        )

        // Calculate average background luminance
        var avgLum = 0
        for (c in cornerColors) {
            avgLum += (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000
        }
        avgLum /= 4

        val isLightBg = avgLum > 180
        val isDarkBg = avgLum < 75

        // If background is neither clearly light nor dark, don't crop
        if (!isLightBg && !isDarkBg) return bitmap

        val targetLum = if (isLightBg) 255 else 0

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        // Scan from top (sample every 4th pixel for speed)
        topLoop@ for (y in 0 until height / 4) {
            for (x in 0 until width step 4) {
                val pixel = bitmap.getPixel(x, y)
                val lum = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (abs(lum - targetLum) > tolerance) {
                    top = y
                    break@topLoop
                }
            }
        }

        // Scan from bottom
        bottomLoop@ for (y in (height - 1) downTo (height * 3 / 4)) {
            for (x in 0 until width step 4) {
                val pixel = bitmap.getPixel(x, y)
                val lum = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (abs(lum - targetLum) > tolerance) {
                    bottom = y
                    break@bottomLoop
                }
            }
        }

        // Scan from left
        leftLoop@ for (x in 0 until width / 4) {
            for (y in top..bottom step 4) {
                val pixel = bitmap.getPixel(x, y)
                val lum = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (abs(lum - targetLum) > tolerance) {
                    left = x
                    break@leftLoop
                }
            }
        }

        // Scan from right
        rightLoop@ for (x in (width - 1) downTo (width * 3 / 4)) {
            for (y in top..bottom step 4) {
                val pixel = bitmap.getPixel(x, y)
                val lum = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                if (abs(lum - targetLum) > tolerance) {
                    right = x
                    break@rightLoop
                }
            }
        }

        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1

        // Only crop if at least 2% of margin is shaved off and resulting image is valid
        if (cropWidth > width * 0.7 && cropHeight > height * 0.7 &&
            (cropWidth < width * 0.98 || cropHeight < height * 0.98)
        ) {
            return try {
                Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
            } catch (e: Exception) {
                bitmap
            }
        }

        return bitmap
    }
}
