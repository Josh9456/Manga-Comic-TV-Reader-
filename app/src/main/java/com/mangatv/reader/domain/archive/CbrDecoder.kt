package com.mangatv.reader.domain.archive

import android.graphics.Bitmap
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.mangatv.reader.domain.model.ComicInfoMetadata
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class CbrDecoder(override val filePath: String) : ComicArchiveDecoder {

    private var archive: Archive? = null
    private var pageHeaders: List<FileHeader> = emptyList()
    private var comicInfoMetadata: ComicInfoMetadata? = null

    override fun open() {
        if (archive != null) return
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return

        val rar = Archive(file)
        this.archive = rar

        val naturalComparator = NaturalOrderComparator()
        val images = mutableListOf<FileHeader>()

        for (header in rar.fileHeaders) {
            if (header.isDirectory) continue

            val name = header.fileName ?: continue
            if (name.equals("ComicInfo.xml", ignoreCase = true) || name.endsWith("/ComicInfo.xml", ignoreCase = true) || name.endsWith("\\ComicInfo.xml", ignoreCase = true)) {
                try {
                    rar.getInputStream(header).use { stream ->
                        comicInfoMetadata = ComicInfoParser.parse(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ImageDecoderUtils.isImageFile(name)) {
                images.add(header)
            }
        }

        images.sortWith { a, b -> naturalComparator.compare(a.fileName, b.fileName) }
        this.pageHeaders = images
    }

    override fun getPageCount(): Int = pageHeaders.size

    override fun getPageNames(): List<String> = pageHeaders.map { it.fileName }

    override fun getPageStream(index: Int): InputStream? {
        val rar = archive ?: return null
        if (index < 0 || index >= pageHeaders.size) return null
        return try {
            rar.getInputStream(pageHeaders[index])
        } catch (e: Exception) {
            null
        }
    }

    override fun getPageBitmap(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        val rar = archive ?: return null
        if (index < 0 || index >= pageHeaders.size) return null
        val header = pageHeaders[index]

        // Junrar streams cannot always be reset, so cache to byte array for dimension sampling
        val bytes = try {
            rar.getInputStream(header).use { input ->
                val buffer = ByteArrayOutputStream()
                input.copyTo(buffer)
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            return null
        }

        return ImageDecoderUtils.decodeSampledBitmapFromStream(
            streamProvider = { ByteArrayInputStream(bytes) },
            reqWidth = targetWidth,
            reqHeight = targetHeight
        )
    }

    override fun getComicInfo(): ComicInfoMetadata? = comicInfoMetadata

    override fun close() {
        try {
            archive?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            archive = null
            pageHeaders = emptyList()
        }
    }
}
