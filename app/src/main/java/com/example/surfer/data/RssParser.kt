package com.example.surfer.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class RssParser {
    fun parse(inputStream: InputStream): RssFeed {
        inputStream.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            return readRss(parser)
        }
    }

    private fun readRss(parser: XmlPullParser): RssFeed {
        parser.require(XmlPullParser.START_TAG, null, "rss")
        var title = ""
        var link = ""
        var description = ""
        val items = mutableListOf<RssItem>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "channel") {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    when (parser.name) {
                        "title" -> title = readText(parser, "title")
                        "link" -> link = readText(parser, "link")
                        "description" -> description = readText(parser, "description")
                        "item" -> items.add(readItem(parser))
                        else -> skip(parser)
                    }
                }
            } else {
                skip(parser)
            }
        }
        return RssFeed(title, link, description, items)
    }

    private fun readItem(parser: XmlPullParser): RssItem {
        parser.require(XmlPullParser.START_TAG, null, "item")
        var title = ""
        var link = ""
        var description = ""
        var pubDate: String? = null
        var thumbnailUrl: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser, "title")
                "link" -> link = readText(parser, "link")
                "description" -> description = readText(parser, "description")
                "pubDate" -> pubDate = readText(parser, "pubDate")
                "enclosure" -> {
                    val type = parser.getAttributeValue(null, "type")
                    if (type != null && type.startsWith("image/")) {
                        thumbnailUrl = parser.getAttributeValue(null, "url")
                    }
                    skip(parser)
                }
                "media:content" -> {
                    thumbnailUrl = parser.getAttributeValue(null, "url")
                    skip(parser)
                }
                else -> skip(parser)
            }
        }
        return RssItem(title, link, description, pubDate, thumbnailUrl)
    }

    private fun readText(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, null, tag)
        val result = if (parser.next() == XmlPullParser.TEXT) {
            parser.text
        } else {
            ""
        }
        if (parser.eventType != XmlPullParser.END_TAG) {
            parser.nextTag()
        }
        parser.require(XmlPullParser.END_TAG, null, tag)
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
