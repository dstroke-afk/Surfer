package com.example.surfer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surfer.data.RssFeed
import com.example.surfer.data.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class RssViewModel : ViewModel() {
    private val parser = RssParser()
    
    private val _feeds = MutableStateFlow<Map<String, RssFeed>>(emptyMap())
    val feeds: StateFlow<Map<String, RssFeed>> = _feeds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val predefinedFeeds = listOf(
        "TechCrunch" to "https://techcrunch.com/feed/",
        "The Verge" to "https://www.theverge.com/rss/index.xml",
        "Wired" to "https://www.wired.com/feed/rss",
        "Ars Technica" to "https://feeds.arstechnica.com/arstechnica/index",
        "Hacker News" to "https://news.ycombinator.com/rss",
        "Gadgets 360" to "https://www.gadgets360.com/rss/feeds",
        "The Hindu" to "https://www.thehindu.com/news/feeder/default.rss",
        "BBC News" to "http://feeds.bbci.co.uk/news/world/rss.xml",
        "Reuters" to "https://www.reutersagency.com/feed/",
        "Times of India" to "https://timesofindia.indiatimes.com/rssfeedstopstories.cms",
        "DinaMani" to "https://www.dinamani.com/rss",
        "IGN" to "https://feeds.feedburner.com/ign/all",
        "Engadget" to "https://www.engadget.com/rss.xml",
        "NASA" to "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss"
    )

    init {
        // Optionally fetch some feeds on init
    }

    fun fetchFeed(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val feed = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.getInputStream().use {
                        parser.parse(it)
                    }
                }
                _feeds.value = _feeds.value + (name to feed)
            } catch (e: Exception) {
                _error.value = "Failed to fetch $name: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
