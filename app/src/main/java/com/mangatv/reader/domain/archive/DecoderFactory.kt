package com.mangatv.reader.domain.archive

import java.io.File

object DecoderFactory {

    val SUPPORTED_ARCHIVE_EXTENSIONS = setOf("cbz", "zip", "cbr", "rar")

    fun isSupportedFile(file: File): Boolean {
        if (!file.exists()) return false
        if (file.isDirectory) {
            val children = file.listFiles() ?: return false
            return children.any { ImageDecoderUtils.isImageFile(it.name) }
        }
        val ext = file.extension.lowercase()
        return SUPPORTED_ARCHIVE_EXTENSIONS.contains(ext)
    }

    fun createDecoder(file: File): ComicArchiveDecoder? {
        if (!file.exists()) return null

        if (file.isDirectory) {
            return FolderDecoder(file.absolutePath).apply { open() }
        }

        val ext = file.extension.lowercase()
        val decoder = when (ext) {
            "cbz", "zip" -> CbzDecoder(file.absolutePath)
            "cbr", "rar" -> CbrDecoder(file.absolutePath)
            else -> null
        }
        decoder?.open()
        return decoder
    }
}
