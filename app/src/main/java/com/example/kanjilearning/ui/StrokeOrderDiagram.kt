package com.example.kanjilearning.ui

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kanjilearning.data.KanjiStrokeDiagram

@Composable
fun StrokeOrderDiagram(
    kanji: String,
    strokeOrderHint: String,
    strokeCount: Int,
    modifier: Modifier = Modifier
) {
    val imageUrl = remember(kanji) { KanjiStrokeDiagram.svgUrlFor(kanji) }
    val htmlDocument = remember(imageUrl) {
        imageUrl?.let {
            """
            <html>
            <body style="margin:0;display:flex;align-items:center;justify-content:center;background:transparent;">
                <img src="$it" style="max-width:100%;max-height:100%;object-fit:contain;" />
            </body>
            </html>
            """.trimIndent()
        }
    }
    var loadFailed by remember(kanji) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Stroke order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl == null || htmlDocument == null || loadFailed) {
                DiagramFallback(
                    kanji = kanji,
                    strokeCount = strokeCount,
                    strokeOrderHint = strokeOrderHint
                )
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            settings.javaScriptEnabled = false
                            settings.loadsImagesAutomatically = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    loadFailed = true
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    loadFailed = true
                                }
                            }
                            loadDataWithBaseURL(imageUrl, htmlDocument, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        val loadedUrl = webView.getTag() as? String
                        if (loadedUrl != imageUrl) {
                            webView.setTag(imageUrl)
                            webView.loadDataWithBaseURL(imageUrl, htmlDocument, "text/html", "utf-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text("Expected strokes: $strokeCount", style = MaterialTheme.typography.bodyMedium)
        Text(strokeOrderHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DiagramFallback(
    kanji: String,
    strokeCount: Int,
    strokeOrderHint: String
) {
    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(kanji, style = MaterialTheme.typography.displayLarge)
        Text("Stroke diagram unavailable", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Text(
            "Strokes: $strokeCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            strokeOrderHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
