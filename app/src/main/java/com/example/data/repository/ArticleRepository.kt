package com.example.data.repository

import android.content.Context
import com.example.data.api.ElsevierService
import com.example.data.db.ArticleDao
import com.example.data.db.ArticleEntity
import com.example.data.model.ScopusSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ArticleRepository(
    private val apiService: ElsevierService,
    private val articleDao: ArticleDao
) {
    // Expose all saved/bookmarked articles reactively
    val savedArticles: Flow<List<ArticleEntity>> = articleDao.getAllArticles()

    suspend fun getArticleByDoi(doi: String): ArticleEntity? {
        return articleDao.getArticleByDoi(doi)
    }

    suspend fun searchScopus(query: String, start: Int = 0): ScopusSearchResponse {
        return withContext(Dispatchers.IO) {
            apiService.searchScopus(query = query, start = start)
        }
    }

    suspend fun toggleBookmark(
        doi: String,
        title: String,
        creator: String?,
        publicationName: String?,
        coverDate: String?,
        scopusUrl: String?
    ) {
        withContext(Dispatchers.IO) {
            val existing = articleDao.getArticleByDoi(doi)
            if (existing != null) {
                val updated = existing.copy(isBookmarked = !existing.isBookmarked)
                if (!updated.isBookmarked && updated.localPdfPath == null) {
                    articleDao.deleteArticle(existing)
                } else {
                    articleDao.updateArticle(updated)
                }
            } else {
                val newArticle = ArticleEntity(
                    doi = doi,
                    title = title,
                    creator = creator,
                    publicationName = publicationName,
                    coverDate = coverDate,
                    scopusUrl = scopusUrl,
                    localPdfPath = null,
                    isBookmarked = true
                )
                articleDao.insertArticle(newArticle)
            }
        }
    }

    suspend fun downloadArticlePdf(
        context: Context,
        doi: String,
        title: String,
        creator: String?,
        publicationName: String?,
        coverDate: String?,
        scopusUrl: String?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadPdfByDoi(doi)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                return@withContext Result.failure(
                    Exception("Gagal mengunduh PDF. Kode: ${response.code()}. Detail: $errorBody")
                )
            }

            val body = response.body() ?: return@withContext Result.failure(
                Exception("File PDF kosong atau tidak ditemukan.")
            )

            // Save PDF to internal storage with safe file name
            val safeFileName = "${doi.replace("/", "_")}.pdf"
            val file = File(context.filesDir, safeFileName)
            
            FileOutputStream(file).use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Save or update article in database
            val existing = articleDao.getArticleByDoi(doi)
            if (existing != null) {
                articleDao.updateArticle(existing.copy(localPdfPath = file.absolutePath))
            } else {
                val newArticle = ArticleEntity(
                    doi = doi,
                    title = title,
                    creator = creator,
                    publicationName = publicationName,
                    coverDate = coverDate,
                    scopusUrl = scopusUrl,
                    localPdfPath = file.absolutePath,
                    isBookmarked = false
                )
                articleDao.insertArticle(newArticle)
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDownloadedPdf(doi: String) {
        withContext(Dispatchers.IO) {
            val existing = articleDao.getArticleByDoi(doi)
            if (existing != null) {
                existing.localPdfPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                val updated = existing.copy(localPdfPath = null)
                if (!updated.isBookmarked) {
                    articleDao.deleteArticle(existing)
                } else {
                    articleDao.updateArticle(updated)
                }
            }
        }
    }
}
