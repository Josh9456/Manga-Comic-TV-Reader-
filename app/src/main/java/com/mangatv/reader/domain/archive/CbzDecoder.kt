package com.mangatv.reader.domain.archive

import android.graphics.Bitmap
import com.mangatv.reader.domain.model.ComicInfoMetadata
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class CbzDecoder(override val filePath: String) : ComicArchiveDecoder {

    private var zipFile: ZipFile? = null
    private var pageEntries: List<ZipEntry> = emptyList()
    private var comicInfoMetadata: ComicInfoMetadata? = null

    override fun open() {
        if (zipFile != null) return
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return

        val zip = ZipFile(file)
        this.zipFile = zip

        val naturalComparator = NaturalOrderComparator()
        val images = mutableListOf<ZipEntry>()

        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory) continue

            val name = entry.name
            if (name.equals("ComicInfo.xml", ignoreCase = true) || name.endsWith("/ComicInfo.xml", ignoreCase = true)) {
                try {
                    zip.getInputStream(entry).use { stream ->
                        comicInfoMetadata = ComicInfoParser.parse(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ImageDecoderUtils.isImageFile(name)) {
                images.add(entry)
            }
        }

        images.sortWith { a, b -> naturalComparator.compare(a.name, b.name) }
        this.pageEntries = images
    }

    override fun getPageCount(): Int = pageEntries.size

    override fun getPageNames(): List<String> = pageEntries.map { it.name }

    override fun getPageStream(index: Int): InputStream? {
        val zip = zipFile ?: return null
        if (index < 0 || index >= pageEntries.size) return null
        return try {
            zip.getInputStream(pageEntries[index])
        } catch (e: Exception) {
            null
        }
    }

    override fun getPageBitmap(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        val zip = zipFile ?: return null
        if (index < 0 || index >= pageEntries.size) return null
        val entry = pageEntries[index]

        return ImageDecoderUtils.decodeSampledBitmapFromStream(
            streamProvider = {
                try {
                    zip.getInputStream(entry)
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
        try {
            zipFile?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            zipFile = null
            pageEntries = emptyList()
        }
    }
}
