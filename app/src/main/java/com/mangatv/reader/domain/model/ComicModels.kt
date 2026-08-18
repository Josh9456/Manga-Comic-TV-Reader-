package com.mangatv.reader.domain.model

data class ComicInfoMetadata(
    val title: String? = null,
    val series: String? = null,
    val number: String? = null,
    val summary: String? = null,
    val writer: String? = null,
    val penciller: String? = null,
    val coverArtist: String? = null,
    val pageCount: Int? = null,
    val manga: Boolean? = null, // true: RTL, false: LTR
    val year: Int? = null,
    val publisher: String? = null
)

enum class ReadingMode(val displayName: String) {
    RTL("Manga (RTL)"),
    LTR("Comic (LTR)"),
    WEBTOON("Webtoon (Scroll)");

    companion object {
        fun fromString(value: String?): ReadingMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: RTL
        }
    }
}

enum class AspectRatioMode(val displayName: String) {
    FIT_SCREEN("Fit Screen"),
    FIT_WIDTH("Fit Width"),
    FIT_HEIGHT("Fit Height"),
    ORIGINAL("Original 1:1"),
    STRETCH("Stretch");

    companion object {
        fun fromString(value: String?): AspectRatioMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: FIT_SCREEN
        }
    }
}

enum class PageSpreadMode(val displayName: String) {
    DUAL_PAGE("Dual Page"),
    SINGLE_PAGE("Single Page");

    companion object {
        fun fromString(value: String?): PageSpreadMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DUAL_PAGE
        }
    }
}

data class ComicItem(
    val path: String,
    val name: String,
    val parentDirectory: String,
    val extension: String,
    val isDirectory: Boolean = false,
    val fileSize: Long = 0L,
    val lastModified: Long = 0L,
    val coverPath: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false,
    val metadata: ComicInfoMetadata? = null,
    val readingMode: ReadingMode = ReadingMode.RTL,
    val aspectMode: AspectRatioMode = AspectRatioMode.FIT_SCREEN,
    val spreadMode: PageSpreadMode = PageSpreadMode.DUAL_PAGE
)
