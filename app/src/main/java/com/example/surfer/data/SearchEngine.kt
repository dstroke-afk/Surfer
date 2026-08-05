package com.example.surfer.data

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    YAHOO_INDIA("Yahoo! India", "https://in.search.yahoo.com/search?p="),
    BING("Microsoft Bing", "https://www.bing.com/search?q="),
    YANDEX("Yandex", "https://yandex.com/search/?text="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=")
}
