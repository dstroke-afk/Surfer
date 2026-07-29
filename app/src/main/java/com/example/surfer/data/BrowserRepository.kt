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

    suspend fun addBookmark(url: String, title: String, folder: String = "Mobile Bookmarks") {
        dao.insertBookmark(BookmarkEntity(url = url, title = title, folder = folder))
    }

    suspend fun removeBookmark(url: String, title: String) {
        dao.deleteBookmark(BookmarkEntity(url = url, title = title))
    }

    fun isBookmarked(url: String): Flow<Boolean> = dao.isBookmarked(url)

    fun getBookmarkByUrl(url: String): Flow<BookmarkEntity?> = dao.getBookmarkByUrl(url)

    fun getAllFolders(): Flow<List<String>> = dao.getAllFolders()

    suspend fun createFolder(name: String) {
        dao.insertFolder(BookmarkFolderEntity(name = name))
    }

    fun getCreatedFolders(): Flow<List<String>> = dao.getCreatedFolders()
}
