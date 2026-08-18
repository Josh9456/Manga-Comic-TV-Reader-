package com.mangatv.reader.domain.archive

import android.util.Xml
import com.mangatv.reader.domain.model.ComicInfoMetadata
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object ComicInfoParser {

    fun parse(inputStream: InputStream): ComicInfoMetadata {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var title: String? = null
            var series: String? = null
            var number: String? = null
            var summary: String? = null
            var writer: String? = null
            var penciller: String? = null
            var coverArtist: String? = null
            var pageCount: Int? = null
            var manga: Boolean? = null
            var year: Int? = null
            var publisher: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name.lowercase()) {
                        "title" -> title = readText(parser)
                        "series" -> series = readText(parser)
                        "number" -> number = readText(parser)
                        "summary" -> summary = readText(parser)
                        "writer" -> writer = readText(parser)
                        "penciller" -> penciller = readText(parser)
                        "coverartist" -> coverArtist = readText(parser)
                        "pagecount" -> pageCount = readText(parser)?.toIntOrNull()
                        "manga" -> {
                            val mangaText = readText(parser)
                            manga = mangaText?.equals("yes", ignoreCase = true) == true ||
                                    mangaText?.equals("true", ignoreCase = true) == true ||
                                    mangaText?.equals("yesandrighttoleft", ignoreCase = true) == true
                        }
                        "year" -> year = readText(parser)?.toIntOrNull()
                        "publisher" -> publisher = readText(parser)
                    }
                }
                eventType = parser.next()
            }

            ComicInfoMetadata(
                title = title,
                series = series,
                number = number,
                summary = summary,
                writer = writer,
                penciller = penciller,
                coverArtist = coverArtist,
                pageCount = pageCount,
                manga = manga,
                year = year,
                publisher = publisher
            )
        } catch (e: Exception) {
            ComicInfoMetadata()
        }
    }

    private fun readText(parser: XmlPullParser): String? {
        var result: String? = null
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text?.trim()
            parser.nextTag()
        }
        return result
    }
}
