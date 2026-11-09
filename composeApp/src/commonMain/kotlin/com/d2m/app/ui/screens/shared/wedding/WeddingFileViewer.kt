package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.d2m.app.data.model.WeddingAttachmentOut
import com.d2m.app.platform.openDocument
import com.d2m.app.ui.components.LinkText
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.rememberPhotoImageLoader
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.launch

/** What the in-app viewer renders itself. PDF / Word / Excel hand off to the platform viewer (Quick Look on iOS) instead. */
fun WeddingAttachmentOut.viewerKind(): String? {
    val n = filename.lowercase()
    return when {
        isImage -> "image"
        n.endsWith(".txt") -> "text"
        n.endsWith(".csv") -> "csv"
        else -> null
    }
}

private fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>(); var row = mutableListOf<String>(); val cell = StringBuilder(); var q = false; var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            q -> if (c == '"') { if (i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ } else q = false } else cell.append(c)
            c == '"' -> q = true
            c == ',' -> { row.add(cell.toString()); cell.clear() }
            c == '\n' || c == '\r' -> { if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++; row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
    return rows
}

/** Full-screen viewer for a task's images, text and CSV files, with previous / next across those files. */
@Composable
fun WeddingFileViewer(
    files: List<WeddingAttachmentOut>,
    startId: String,
    imageModel: (WeddingAttachmentOut) -> Any,
    download: suspend (WeddingAttachmentOut) -> ByteArray,
    onClose: () -> Unit,
) {
    val s = LocalWedding.current.s
    val scope = rememberCoroutineScope()
    val loader = rememberPhotoImageLoader()
    var index by remember { mutableStateOf(files.indexOfFirst { it.id == startId }.coerceAtLeast(0)) }
    val a = files[index]
    val kind = a.viewerKind()
    var text by remember(a.id) { mutableStateOf<String?>(null) }
    var failed by remember(a.id) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(a.id) {
        if (kind == "text" || kind == "csv") runCatching { download(a).decodeToString() }.onSuccess { text = it }.onFailure { failed = true }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xF2140F0A)).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(a.filename, style = MaterialTheme.typography.titleSmall, color = androidx.compose.ui.graphics.Color(0xFFF4ECE4), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (files.size > 1) MetaText("${index + 1} / ${files.size}")
                }
                if (files.size > 1) {
                    LinkText("‹", onClick = { index = (index - 1 + files.size) % files.size })
                    LinkText("›", onClick = { index = (index + 1) % files.size })
                }
                LinkText(if (busy) s.opening else s.openWith, enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        runCatching { download(a) }.onSuccess { openDocument(a.filename, a.contentType, it) }
                        busy = false
                    }
                })
                LinkText("✕", onClick = onClose)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                when {
                    failed -> MetaText(s.couldntOpen(a.filename))
                    kind == "image" -> AsyncImage(model = imageModel(a), imageLoader = loader, contentDescription = a.filename, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    text == null -> MetaText(s.opening)
                    kind == "csv" -> {
                        val rows = remember(text) { parseCsv(text!!).take(300) }
                        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(10.dp)) {
                            rows.forEachIndexed { i, r ->
                                Row {
                                    r.forEach { c ->
                                        Text(c, Modifier.width(120.dp).padding(horizontal = 4.dp, vertical = 3.dp), style = MaterialTheme.typography.bodySmall,
                                            color = if (i == 0) d2m.ink else d2m.textPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            fontWeight = if (i == 0) androidx.compose.ui.text.font.FontWeight.Bold else null)
                                    }
                                }
                            }
                        }
                    }
                    else -> Text(text!!, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium, color = d2m.textPrimary)
                }
            }
        }
    }
}
