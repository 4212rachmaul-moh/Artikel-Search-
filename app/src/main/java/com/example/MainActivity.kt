package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ArticleEntity
import com.example.data.model.ScopusEntry
import com.example.data.model.ScopusLink
import com.example.ui.components.PdfViewer
import com.example.ui.components.ScienceDirectWebView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ArticleViewModel
import com.example.ui.viewmodel.DownloadState
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.TranslationUiState
import java.io.File

enum class ActiveView {
    Dashboard,
    NativePdfReader,
    WebViewReader,
    GeminiTranslationReader
}

class MainActivity : ComponentActivity() {
    private val viewModel: ArticleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ArticleViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Navigation and detail overlay states
    var currentView by remember { mutableStateOf(ActiveView.Dashboard) }
    var selectedArticle: ScopusEntry? by remember { mutableStateOf(null) }
    var activePdfFile: File? by remember { mutableStateOf(null) }
    var activeUrl by remember { mutableStateOf("") }
    var activeReaderTitle by remember { mutableStateOf("") }

    // State flows from ViewModel
    val savedArticles by viewModel.savedArticles.collectAsState()
    val searchUiState by viewModel.searchUiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterTitle by viewModel.filterTitle.collectAsState()
    val filterAuthor by viewModel.filterAuthor.collectAsState()
    val filterJournal by viewModel.filterJournal.collectAsState()
    val isAdvancedSearch by viewModel.isAdvancedSearch.collectAsState()

    // Handle PDF download transitions
    LaunchedEffect(downloadState) {
        val state = downloadState
        when (state) {
            is DownloadState.Success -> {
                activePdfFile = state.file
                activeReaderTitle = selectedArticle?.title ?: "Dokumen PDF"
                currentView = ActiveView.NativePdfReader
                selectedArticle = null // Close details overlay
                viewModel.resetDownloadState()
            }
            is DownloadState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetDownloadState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentView) {
                ActiveView.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        filterTitle = filterTitle,
                        filterAuthor = filterAuthor,
                        filterJournal = filterJournal,
                        isAdvancedSearch = isAdvancedSearch,
                        searchUiState = searchUiState,
                        savedArticles = savedArticles,
                        onArticleClick = { article ->
                            selectedArticle = article
                        },
                        onOfflineArticleClick = { savedEntity ->
                            // Open cached native PDF or fallback webview
                            if (savedEntity.localPdfPath != null) {
                                val file = File(savedEntity.localPdfPath)
                                if (file.exists()) {
                                    activePdfFile = file
                                    activeReaderTitle = savedEntity.title
                                    currentView = ActiveView.NativePdfReader
                                } else {
                                    Toast.makeText(context, "File PDF lokal tidak ditemukan pada penyimpanan.", Toast.LENGTH_SHORT).show()
                                }
                            } else if (!savedEntity.scopusUrl.isNullOrEmpty()) {
                                activeUrl = savedEntity.scopusUrl
                                activeReaderTitle = savedEntity.title
                                currentView = ActiveView.WebViewReader
                            } else {
                                Toast.makeText(context, "Tautan artikel tidak valid.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLaunchDemo = {
                            val demoFile = viewModel.getOrGenerateSamplePaper(context)
                            activePdfFile = demoFile
                            activeReaderTitle = "Akselerasi Mobile PDF Rendering - Elsevier Sample"
                            currentView = ActiveView.NativePdfReader
                        }
                    )
                }
                ActiveView.NativePdfReader -> {
                    activePdfFile?.let { file ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header with beautiful Close and fallback buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF386B01))
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentView = ActiveView.Dashboard },
                                    modifier = Modifier.testTag("pdf_reader_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Kembali ke Dashboard",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = activeReaderTitle,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                            PdfViewer(
                                file = file,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f),
                                onClose = { currentView = ActiveView.Dashboard }
                            )
                        }
                    }
                }
                ActiveView.WebViewReader -> {
                    ScienceDirectWebView(
                        url = activeUrl,
                        title = activeReaderTitle,
                        onClose = { currentView = ActiveView.Dashboard }
                    )
                }
                ActiveView.GeminiTranslationReader -> {
                    val translationState by viewModel.translationUiState.collectAsState()
                    val clipboardManager = LocalClipboardManager.current

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFCFDF6))
                            .statusBarsPadding()
                    ) {
                        // Top Navigation Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF386B01))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    currentView = ActiveView.Dashboard
                                    viewModel.clearTranslationState()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali ke Dashboard",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Terjemahan Artikel Gemini AI 🌟",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            if (translationState is TranslationUiState.Success) {
                                val textToCopy = (translationState as TranslationUiState.Success).markdownText
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                        Toast.makeText(context, "Seluruh naskah terjemahan disalin!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin Terjemahan",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val state = translationState) {
                                is TranslationUiState.Loading -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF386B01),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "Gemini AI sedang menganalisis, menerjemahkan, dan menyusun kembali naskah artikel ini ke Bahasa Indonesia...",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp,
                                                lineHeight = 22.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF386B01)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Metode ini tidak mengunduh naskah PDF asli sehingga menghemat kuota internet Anda.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                is TranslationUiState.Success -> {
                                    val scrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFDDE6D3), RoundedCornerShape(12.dp))
                                            .verticalScroll(scrollState)
                                            .padding(20.dp)
                                    ) {
                                        state.markdownText.split("\n").forEach { line ->
                                            val trimmedLine = line.trim()
                                            when {
                                                trimmedLine.startsWith("# ") -> {
                                                    val headerText = trimmedLine.substringAfter("# ")
                                                    Text(
                                                        text = headerText,
                                                        style = MaterialTheme.typography.headlineMedium.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            lineHeight = 32.sp
                                                        ),
                                                        color = Color(0xFF386B01),
                                                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                                                    )
                                                }
                                                trimmedLine.startsWith("## ") -> {
                                                    val headerText = trimmedLine.substringAfter("## ")
                                                    Text(
                                                        text = headerText,
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            lineHeight = 26.sp
                                                        ),
                                                        color = Color(0xFF386B01),
                                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                                    )
                                                }
                                                trimmedLine.startsWith("### ") -> {
                                                    val headerText = trimmedLine.substringAfter("### ")
                                                    Text(
                                                        text = headerText,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            lineHeight = 22.sp
                                                        ),
                                                        color = Color(0xFF93A387),
                                                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                                    )
                                                }
                                                trimmedLine.startsWith("---") -> {
                                                    HorizontalDivider(
                                                        color = Color(0xFFDDE6D3),
                                                        modifier = Modifier.padding(vertical = 12.dp)
                                                    )
                                                }
                                                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("• ") -> {
                                                    val bulletText = if (trimmedLine.startsWith("- ")) trimmedLine.substring(2) else trimmedLine.substring(2)
                                                    Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                                                        Text("• ", fontWeight = FontWeight.Bold, color = Color(0xFF386B01))
                                                        Text(
                                                            text = bulletText,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                lineHeight = 20.sp
                                                            ),
                                                            color = Color(0xFF43493E)
                                                        )
                                                    }
                                                }
                                                trimmedLine.isEmpty() -> {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                                else -> {
                                                    Text(
                                                        text = trimmedLine,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            lineHeight = 22.sp
                                                        ),
                                                        color = Color(0xFF222222),
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))

                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(state.markdownText))
                                                Toast.makeText(context, "Seluruh naskah terjemahan disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B01)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Salin Seluruh Naskah Terjemahan")
                                        }
                                    }
                                }
                                is TranslationUiState.Error -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text(
                                            text = state.message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                val article = selectedArticle ?: return@Button
                                                viewModel.translateAndReconstructPaper(
                                                    title = article.title ?: "Tanpa Judul",
                                                    author = article.creator,
                                                    journal = article.publicationName,
                                                    date = article.coverDate,
                                                    doi = article.cleanDoi
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B01))
                                        ) {
                                            Text("Coba Lagi")
                                        }
                                    }
                                }
                                else -> {
                                    Text("Memuat naskah terjemahan...")
                                }
                            }
                        }
                    }
                }
            }

            // Article Details Modal Bottom Sheet
            if (selectedArticle != null) {
                val article = selectedArticle!!
                val cleanDoi = article.cleanDoi
                val isSaved = savedArticles.any { it.doi == cleanDoi }
                val localSavedEntity = savedArticles.firstOrNull { it.doi == cleanDoi }
                val hasLocalPdf = localSavedEntity?.localPdfPath != null

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { selectedArticle = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        // Title
                        Text(
                            text = article.title ?: "Tanpa Judul",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF386B01)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Authors
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Penulis",
                                tint = Color(0xFF386B01),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = article.creator ?: "Penulis tidak diketahui",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Journal publication details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Jurnal",
                                tint = Color(0xFF93A387),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildString {
                                    append(article.publicationName ?: "Jurnal tidak diketahui")
                                    if (!article.volume.isNullOrEmpty()) append(", Vol. ${article.volume}")
                                    if (!article.issue.isNullOrEmpty()) append(", No. ${article.issue}")
                                    if (!article.coverDate.isNullOrEmpty()) append(" (${article.coverDate})")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        // DOI info and copy action
                        if (!cleanDoi.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFFF0F4E9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(cleanDoi))
                                        Toast
                                            .makeText(
                                                context,
                                                "DOI disalin ke papan klip!",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DOI (Digital Object Identifier)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF43493E)
                                    )
                                    Text(
                                        text = cleanDoi,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF386B01),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Salin DOI",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Action Buttons: Read, Bookmark, WebView
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bookmark toggle button
                            IconButton(
                                onClick = {
                                    viewModel.toggleBookmark(
                                        doi = cleanDoi ?: "DEMO_DOI_${article.title.hashCode()}",
                                        title = article.title ?: "Tanpa Judul",
                                        creator = article.creator,
                                        publicationName = article.publicationName,
                                        coverDate = article.coverDate,
                                        scopusUrl = article.scopusUrl
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSaved) Color(0xFF386B01) else Color(0xFFDDE6D3),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (isSaved) Color(0xFF386B01) else Color(0xFF43493E)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Simpan Artikel"
                                )
                            }

                            // Secondary actions: WebView ScienceDirect
                            OutlinedButton(
                                onClick = {
                                    val url = article.scopusUrl ?: "https://www.sciencedirect.com"
                                    activeUrl = url
                                    activeReaderTitle = article.title ?: "Portal ScienceDirect"
                                    currentView = ActiveView.WebViewReader
                                    selectedArticle = null
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF386B01)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(Color(0xFF386B01), Color(0xFF386B01)))
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "WebView"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "ScienceDirect", fontSize = 13.sp)
                            }

                            // Main Action: Read Native PDF
                            Button(
                                onClick = {
                                    if (hasLocalPdf) {
                                        val file = File(localSavedEntity!!.localPdfPath!!)
                                        if (file.exists()) {
                                            activePdfFile = file
                                            activeReaderTitle = article.title ?: "Dokumen PDF"
                                            currentView = ActiveView.NativePdfReader
                                            selectedArticle = null
                                        } else {
                                            Toast.makeText(context, "Mengunduh ulang dokumen PDF...", Toast.LENGTH_SHORT).show()
                                            viewModel.downloadPdf(
                                                context = context,
                                                doi = cleanDoi ?: "",
                                                title = article.title ?: "",
                                                creator = article.creator,
                                                publicationName = article.publicationName,
                                                coverDate = article.coverDate,
                                                scopusUrl = article.scopusUrl
                                            )
                                        }
                                    } else {
                                        if (cleanDoi.isNullOrEmpty()) {
                                            Toast.makeText(context, "Dokumen ini tidak memiliki tautan DOI PDF valid.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.downloadPdf(
                                                context = context,
                                                doi = cleanDoi,
                                                title = article.title ?: "",
                                                creator = article.creator,
                                                publicationName = article.publicationName,
                                                coverDate = article.coverDate,
                                                scopusUrl = article.scopusUrl
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1.2f)
                                    .testTag("read_pdf_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF386B01),
                                    contentColor = Color.White
                                )
                            ) {
                                if (downloadState is DownloadState.Downloading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (hasLocalPdf) Icons.Default.Book else Icons.Default.Download,
                                        contentDescription = "Unduh & Baca"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasLocalPdf) "Baca PDF" else "Unduh PDF",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Translate with Gemini Button (No download required!)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.translateAndReconstructPaper(
                                    title = article.title ?: "Tanpa Judul",
                                    author = article.creator,
                                    journal = article.publicationName,
                                    date = article.coverDate,
                                    doi = cleanDoi
                                )
                                currentView = ActiveView.GeminiTranslationReader
                                activeReaderTitle = article.title ?: "Terjemahan Artikel"
                                selectedArticle = null // Close bottom sheet
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("gemini_translate_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF386B01)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF386B01), Color(0xFF386B01)))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Terjemahkan dengan Gemini AI",
                                tint = Color(0xFF386B01)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Baca Versi Terjemahan (Gemini AI 🌟)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF386B01)
                            )
                        }

                        // Entitlement warning helper
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF0F4E9),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, Color(0xFFDDE6D3), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info Hak Akses",
                                tint = Color(0xFF386B01),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Beberapa PDF Elsevier dilindungi hak cipta langganan institusi. Jika gagal diunduh langsung, gunakan tombol 'ScienceDirect' untuk login institusi dan membaca artikel lengkap.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = Color(0xFF43493E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: ArticleViewModel,
    searchQuery: String,
    filterTitle: String,
    filterAuthor: String,
    filterJournal: String,
    isAdvancedSearch: Boolean,
    searchUiState: SearchUiState,
    savedArticles: List<ArticleEntity>,
    onArticleClick: (ScopusEntry) -> Unit,
    onOfflineArticleClick: (ArticleEntity) -> Unit,
    onLaunchDemo: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFDF6)),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App Custom Brand Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF386B01), Color(0xFF2C5501))
                        )
                        drawRect(brush = brush)
                    }
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFC2EBA2), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Logo",
                                tint = Color(0xFF1A1C18),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Elsevier Search",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pencarian literatur ilmiah Scopus global & pembaca PDF tangguh dalam satu aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Search Form Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFDDE6D3)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Standard search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Cari topik ilmiah (misal: deep learning)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color(0xFF43493E)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.isAdvancedSearch.value = !isAdvancedSearch },
                                modifier = Modifier.testTag("advanced_filter_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter Lanjutan",
                                    tint = if (isAdvancedSearch) Color(0xFF386B01) else Color(0xFF43493E)
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.search() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF386B01),
                            unfocusedBorderColor = Color(0xFFDDE6D3),
                            focusedLabelColor = Color(0xFF386B01),
                            unfocusedLabelColor = Color(0xFF43493E)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Advanced Filters
                    AnimatedVisibility(
                        visible = isAdvancedSearch,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Title Filter
                            OutlinedTextField(
                                value = filterTitle,
                                onValueChange = { viewModel.filterTitle.value = it },
                                label = { Text("Batasi Judul Artikel (TITLE)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("filter_title_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Author Filter
                            OutlinedTextField(
                                value = filterAuthor,
                                onValueChange = { viewModel.filterAuthor.value = it },
                                label = { Text("Nama Penulis (AUTHOR)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("filter_author_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Journal Filter
                            OutlinedTextField(
                                value = filterJournal,
                                onValueChange = { viewModel.filterJournal.value = it },
                                label = { Text("Nama Jurnal / Penerbit (SRCTITLE)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("filter_journal_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Search Submit Button
                    Button(
                        onClick = { viewModel.search() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_search_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF386B01),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mulai Cari Scopus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Results List
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (searchUiState) {
            is SearchUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF386B01))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menghubungi Scopus API...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF43493E)
                            )
                        }
                    }
                }
            }
            is SearchUiState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .background(
                                Color(0xFFFFECEB),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color(0xFFFFA39E), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = Color(0xFFF5222D),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = searchUiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCF1322),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            is SearchUiState.Success -> {
                val articles = searchUiState.articles
                if (articles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tidak Ada Hasil Ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Coba gunakan kata kunci umum lain atau periksa konfigurasi filter Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Hasil Pencarian Scopus (${articles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF386B01),
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .padding(bottom = 10.dp)
                        )
                    }

                    items(articles) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onArticleClick(article) }
                        )
                    }
                }
            }
            is SearchUiState.Idle -> {
                // Horizontal Carousel of Sample Papers
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Artikel Sampel (Akses Terbuka)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF386B01),
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Offline demo shortcut paper card
                            Card(
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(130.dp)
                                    .clickable { onLaunchDemo() },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4E9)),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFDDE6D3)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFF386B01),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "DEMO OFFLINE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.ChromeReaderMode,
                                            contentDescription = "Demo",
                                            tint = Color(0xFF386B01),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Optimasi Rendering PDF Ilmiah Pada Android",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1C18),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Uji Coba Pembaca PDF Instan (Tanpa Internet)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF386B01),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // High-quality Open Access Papers
                            viewModel.samplePapers.forEach { entry ->
                                Card(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(130.dp)
                                        .clickable { onArticleClick(entry) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        Color(0xFFE6F7ED),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "OPEN ACCESS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF1D8F49),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = entry.title ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C1B1F),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = entry.creator ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bookmark & Offline Downloads section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Artikel Tersimpan & PDF Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF386B01),
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 10.dp)
            )
        }

        if (savedArticles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Simpan Offline",
                            tint = Color.LightGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Artikel Tersimpan",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Artikel yang disimpan atau diunduh PDF-nya akan tersusun rapi di sini untuk dibaca offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedArticles) { savedEntity ->
                SavedArticleCard(
                    article = savedEntity,
                    onClick = { onOfflineArticleClick(savedEntity) },
                    onDelete = { viewModel.deleteDownloadedPdf(savedEntity.doi) }
                )
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: ScopusEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("article_card_${article.cleanDoi}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFDDE6D3)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = article.title ?: "Tanpa Judul",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C18),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Penulis",
                    tint = Color(0xFF43493E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = article.creator ?: "Penulis tidak diketahui",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF43493E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Jurnal",
                    tint = Color(0xFF43493E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append(article.publicationName ?: "Jurnal tidak diketahui")
                        if (!article.coverDate.isNullOrEmpty()) append(" (${article.coverDate})")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF43493E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // PDF Available indicators
                if (!article.cleanDoi.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFC2EBA2), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF386B01),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedArticleCard(
    article: ArticleEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("saved_article_card_${article.doi}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFDDE6D3)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C18),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Penulis",
                        tint = Color(0xFF43493E),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = article.creator ?: "Penulis tidak diketahui",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF43493E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Offline / Bookmark
                    if (article.localPdfPath != null) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFC2EBA2), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "PDF Tersimpan Offline",
                                    tint = Color(0xFF386B01),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PDF OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF386B01),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.6.sp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF0F4E9), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TERSIMPAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF386B01),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.6.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Delete cached PDF icon button if saved offline
            if (article.localPdfPath != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_pdf_button_${article.doi}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus PDF Offline",
                        tint = Color(0xFF93A387)
                    )
                }
            }
        }
    }
}
