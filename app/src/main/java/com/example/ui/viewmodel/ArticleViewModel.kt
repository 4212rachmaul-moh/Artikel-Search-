package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ArticleEntity
import com.example.data.model.ScopusEntry
import com.example.data.model.ScopusLink
import com.example.data.api.RetrofitClient
import com.example.data.repository.ArticleRepository
import com.example.data.repository.SamplePdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.Bitmap
import com.example.data.api.GeminiClient

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val articles: List<ScopusEntry>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface DownloadState {
    object Idle : DownloadState
    object Downloading : DownloadState
    data class Success(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

sealed interface TranslationUiState {
    object Idle : TranslationUiState
    object Loading : TranslationUiState
    data class Success(val markdownText: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

sealed interface PageTranslationUiState {
    object Idle : PageTranslationUiState
    object Loading : PageTranslationUiState
    data class Success(val pageNum: Int, val translatedText: String) : PageTranslationUiState
    data class Error(val message: String) : PageTranslationUiState
}

class ArticleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ArticleRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ArticleRepository(
            apiService = RetrofitClient.service,
            articleDao = database.articleDao()
        )
    }

    // Expose saved articles reactively
    val savedArticles: StateFlow<List<ArticleEntity>> = repository.savedArticles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _translationUiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val translationUiState: StateFlow<TranslationUiState> = _translationUiState.asStateFlow()

    private val _pageTranslationUiState = MutableStateFlow<PageTranslationUiState>(PageTranslationUiState.Idle)
    val pageTranslationUiState: StateFlow<PageTranslationUiState> = _pageTranslationUiState.asStateFlow()

    // Query inputs
    val searchQuery = MutableStateFlow("")
    val filterTitle = MutableStateFlow("")
    val filterAuthor = MutableStateFlow("")
    val filterJournal = MutableStateFlow("")
    val isAdvancedSearch = MutableStateFlow(false)

    // Sample Open Access papers that can be clicked and downloaded immediately
    val samplePapers = listOf(
        ScopusEntry(
            title = "Deep learning for digital pathology image analysis: A comprehensive tutorial with selected use cases",
            creator = "Madabhushi, Anant",
            publicationName = "Journal of Pathology Informatics",
            coverDate = "2016-03-01",
            doi = "10.4103/2153-3539.180918",
            volume = "7",
            issue = "1",
            pageRange = "12-25",
            identifier = "DOI:10.4103/2153-3539.180918",
            eid = "2-s2.0-84964654321",
            link = listOf(
                ScopusLink("https://www.sciencedirect.com/science/article/pii/S21533539180918X", "scopus")
            )
        ),
        ScopusEntry(
            title = "Machine learning in oncology: A review of algorithms and clinical applications",
            creator = "Kourou, Konstantina",
            publicationName = "European Journal of Cancer",
            coverDate = "2019-01-15",
            doi = "10.1016/j.ejca.2019.01.002",
            volume = "108",
            issue = "2",
            pageRange = "101-115",
            identifier = "DOI:10.1016/j.ejca.2019.01.002",
            eid = "2-s2.0-85059632481",
            link = listOf(
                ScopusLink("https://www.sciencedirect.com/science/article/pii/S095980491930002X", "scopus")
            )
        ),
        ScopusEntry(
            title = "Artificial Intelligence in Medicine: Review of Current Applications",
            creator = "Hamet, Pavel",
            publicationName = "Canadian Journal of Cardiology",
            coverDate = "2017-05-01",
            doi = "10.1016/j.cjca.2017.04.005",
            volume = "33",
            issue = "5",
            pageRange = "550-559",
            identifier = "DOI:10.1016/j.cjca.2017.04.005",
            eid = "2-s2.0-85018694032",
            link = listOf(
                ScopusLink("https://www.sciencedirect.com/science/article/pii/S0828282X1730164X", "scopus")
            )
        )
    )

    fun search() {
        val baseQuery = searchQuery.value.trim()
        if (!isAdvancedSearch.value && baseQuery.isEmpty()) {
            _searchUiState.value = SearchUiState.Error("Mohon masukkan kata kunci pencarian.")
            return
        }
        if (isAdvancedSearch.value && baseQuery.isEmpty() && filterTitle.value.isEmpty() && filterAuthor.value.isEmpty() && filterJournal.value.isEmpty()) {
            _searchUiState.value = SearchUiState.Error("Mohon masukkan minimal satu kata kunci pencarian.")
            return
        }

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                // Build a proper Scopus query
                val queryParts = mutableListOf<String>()
                
                if (isAdvancedSearch.value) {
                    if (filterTitle.value.isNotBlank()) {
                        queryParts.add("TITLE(${filterTitle.value.trim()})")
                    }
                    if (filterAuthor.value.isNotBlank()) {
                        queryParts.add("AUTHOR-NAME(${filterAuthor.value.trim()})")
                    }
                    if (filterJournal.value.isNotBlank()) {
                        queryParts.add("SRCTITLE(${filterJournal.value.trim()})")
                    }
                    if (baseQuery.isNotBlank()) {
                        queryParts.add("ALL(${baseQuery})")
                    }
                } else {
                    queryParts.add("ALL(${baseQuery})")
                }

                val scopusQuery = queryParts.joinToString(" AND ")
                
                val response = repository.searchScopus(scopusQuery)
                val entries = response.searchResults?.entry
                if (entries.isNullOrEmpty()) {
                    _searchUiState.value = SearchUiState.Success(emptyList())
                } else {
                    _searchUiState.value = SearchUiState.Success(entries)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchUiState.value = SearchUiState.Error(
                    e.localizedMessage ?: "Terjadi kesalahan koneksi ke Scopus API."
                )
            }
        }
    }

    fun downloadPdf(
        context: Context,
        doi: String,
        title: String,
        creator: String?,
        publicationName: String?,
        coverDate: String?,
        scopusUrl: String?
    ) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading
            val result = repository.downloadArticlePdf(
                context = context,
                doi = doi,
                title = title,
                creator = creator,
                publicationName = publicationName,
                coverDate = coverDate,
                scopusUrl = scopusUrl
            )
            result.fold(
                onSuccess = { file ->
                    _downloadState.value = DownloadState.Success(file)
                },
                onFailure = { error ->
                    error.printStackTrace()
                    _downloadState.value = DownloadState.Error(
                        error.localizedMessage ?: "Gagal mengunduh artikel dari Elsevier."
                    )
                }
            )
        }
    }

    fun getOrGenerateSamplePaper(context: Context): File {
        return SamplePdfGenerator.generateSamplePdf(context)
    }

    fun deleteDownloadedPdf(doi: String) {
        viewModelScope.launch {
            repository.deleteDownloadedPdf(doi)
        }
    }

    fun toggleBookmark(
        doi: String,
        title: String,
        creator: String?,
        publicationName: String?,
        coverDate: String?,
        scopusUrl: String?
    ) {
        viewModelScope.launch {
            repository.toggleBookmark(
                doi = doi,
                title = title,
                creator = creator,
                publicationName = publicationName,
                coverDate = coverDate,
                scopusUrl = scopusUrl
            )
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun translateAndReconstructPaper(
        title: String,
        author: String?,
        journal: String?,
        date: String?,
        doi: String?
    ) {
        viewModelScope.launch {
            _translationUiState.value = TranslationUiState.Loading
            try {
                val result = GeminiClient.reconstructAndTranslatePaper(title, author, journal, date, doi)
                _translationUiState.value = TranslationUiState.Success(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _translationUiState.value = TranslationUiState.Error(
                    e.localizedMessage ?: "Gagal menerjemahkan artikel dengan Gemini."
                )
            }
        }
    }

    fun translatePdfPage(bitmap: Bitmap, pageNum: Int) {
        viewModelScope.launch {
            _pageTranslationUiState.value = PageTranslationUiState.Loading
            try {
                val result = GeminiClient.translatePdfPage(bitmap, pageNum)
                _pageTranslationUiState.value = PageTranslationUiState.Success(pageNum, result)
            } catch (e: Exception) {
                e.printStackTrace()
                _pageTranslationUiState.value = PageTranslationUiState.Error(
                    e.localizedMessage ?: "Gagal menerjemahkan halaman PDF."
                )
            }
        }
    }

    fun clearTranslationState() {
        _translationUiState.value = TranslationUiState.Idle
    }

    fun clearPageTranslationState() {
        _pageTranslationUiState.value = PageTranslationUiState.Idle
    }
}
