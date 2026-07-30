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

    fun getLastVisit(url: String): Flow<HistoryEntity?> {
        val hostname = try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
        return dao.getLastVisit("%$hostname%")
    }

    fun getBookmarkByUrl(url: String): Flow<BookmarkEntity?> = dao.getBookmarkByUrl(url)

    fun getAllFolders(): Flow<List<String>> = dao.getAllFolders()

    suspend fun createFolder(name: String) {
        dao.insertFolder(BookmarkFolderEntity(name = name))
    }

    fun getCreatedFolders(): Flow<List<String>> = dao.getCreatedFolders()

    val allDownloads: Flow<List<DownloadEntity>> = dao.getAllDownloads()

    suspend fun addDownload(download: DownloadEntity) {
        dao.insertDownload(download)
    }

    suspend fun updateDownload(download: DownloadEntity) {
        dao.updateDownload(download)
    }

    suspend fun deleteDownload(id: String) {
        dao.deleteDownload(id)
    }
}
