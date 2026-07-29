package com.example.surfer.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {
    val allHistory: Flow<List<HistoryEntity>> = dao.getAllHistory()
    val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    suspend fun addHistory(url: String, title: String) {
        dao.insertHistory(HistoryEntity(url = url, title = title))
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(BookmarkEntity(url = url, title = title))
    }

    suspend fun removeBookmark(url: String, title: String) {
        dao.deleteBookmark(BookmarkEntity(url = url, title = title))
    }

    fun isBookmarked(url: String): Flow<Boolean> = dao.isBookmarked(url)
}
