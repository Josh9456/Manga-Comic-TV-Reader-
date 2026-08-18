package com.mangatv.reader.domain.archive

import android.graphics.Bitmap
import com.mangatv.reader.domain.model.ComicInfoMetadata
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class FolderDecoder(override val filePath: String) : ComicArchiveDecoder {

    private var imageFiles: List<File> = emptyList()
    private var comicInfoMetadata: ComicInfoMetadata? = null

    override fun open() {
        val folder = File(filePath)
        if (!folder.exists() || !folder.isDirectory) return

        val naturalComparator = NaturalOrderComparator()
        val allFiles = folder.listFiles() ?: return

        val images = mutableListOf<File>()
        for (file in allFiles) {
            if (file.isFile) {
                if (file.name.equals("ComicInfo.xml", ignoreCase = true)) {
                    try {
                        FileInputStream(file).use { stream ->
                            comicInfoMetadata = ComicInfoParser.parse(stream)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (ImageDecoderUtils.isImageFile(file.name)) {
                    images.add(file)
                }
            }
        }

        images.sortWith { a, b -> naturalComparator.compare(a.name, b.name) }
        this.imageFiles = images
    }

    override fun getPageCount(): Int = imageFiles.size

    override fun getPageNames(): List<String> = imageFiles.map { it.name }

    override fun getPageStream(index: Int): InputStream? {
        if (index < 0 || index >= imageFiles.size) return null
        return try {
            FileInputStream(imageFiles[index])
        } catch (e: Exception) {
            null
        }
    }

    override fun getPageBitmap(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (index < 0 || index >= imageFiles.size) return null
        val file = imageFiles[index]

        return ImageDecoderUtils.decodeSampledBitmapFromStream(
            streamProvider = {
                try {
                    FileInputStream(file)
                } catch (e: Exception) {
                    null
                }
            },
            reqWidth = targetWidth,
            reqHeight = targetHeight
        )
    }

    override fun getComicInfo(): ComicInfoMetadata? = comicInfoMetadata

    override fun close() {
        imageFiles = emptyList()
    }
}
