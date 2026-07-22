package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScienceDirectWebView(
    url: String,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf(title) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // WebView Top Control Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose, modifier = Modifier.testTag("webview_close_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            },
            actions = {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Maju"
                    )
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Muat Ulang")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        )

        // Progress Bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Transparent)
            )
        }

        // Web view
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            progress = 0.1f
                            url?.let {
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            progress = 1.0f
                            url?.let {
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                            view?.title?.let { t ->
                                if (t.isNotBlank() && !t.contains("http", ignoreCase = true)) {
                                    currentTitle = t
                                }
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress.toFloat() / 100f
                        }
                    }
                    loadUrl(url)
                    webView = this
                }
            },
            update = { view ->
                // Keep reference updated if URL changes
                if (view.url != url && webView == null) {
                    view.loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("science_direct_webview")
        )
    }
}

// Utility extension to mimic M3 surfaceColorAtElevation in a simple manner
@Composable
private fun androidx.compose.material3.ColorScheme.surfaceColorAtElevation(
    elevation: androidx.compose.ui.unit.Dp
): Color {
    return if (elevation == 0.dp) surface else {
        val alpha = ((4.5f * java.lang.Math.log(elevation.value.toDouble() + 1.0) + 2.0) / 100.0).toFloat()
        primary.copy(alpha = alpha).compositeOver(surface)
    }
}

private fun Color.compositeOver(background: Color): Color {
    val src = this
    val dst = background
    val a = src.alpha + dst.alpha * (1f - src.alpha)
    val r = (src.red * src.alpha + dst.red * dst.alpha * (1f - src.alpha)) / a
    val g = (src.green * src.alpha + dst.green * dst.alpha * (1f - src.alpha)) / a
    val b = (src.blue * src.alpha + dst.blue * dst.alpha * (1f - src.alpha)) / a
    return Color(r, g, b, a)
}
