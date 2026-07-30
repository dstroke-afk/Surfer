package com.example.surfer.data

import kotlinx.serialization.Serializable

@Serializable
data class RssFeed(
    val title: String,
    val link: String,
    val description: String,
    val items: List<RssItem> = emptyList()
)

@Serializable
data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String? = null,
    val thumbnailUrl: String? = null
)
