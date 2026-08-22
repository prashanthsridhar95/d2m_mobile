package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.ui.theme.mutedText
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// GIPHY's response is snake_case, which auto-maps to these camelCase field
// names via ApiClient's already-installed ContentNegotiation
// (JsonNamingStrategy.SnakeCase) -- no extra @SerialName annotations needed,
// same as every other DTO in this codebase.
@Serializable
private data class GiphyImageVariant(val url: String? = null, val width: String? = null, val height: String? = null)

@Serializable
private data class GiphyImages(
    val fixedWidthSmall: GiphyImageVariant? = null,
    val fixedWidth: GiphyImageVariant? = null,
    val downsized: GiphyImageVariant? = null,
    val original: GiphyImageVariant? = null,
)

@Serializable
private data class GiphyGif(val id: String, val images: GiphyImages)

@Serializable
private data class GiphySearchResponse(val data: List<GiphyGif> = emptyList())

/** One browsable GIF result -- [previewUrl] for the grid thumbnail, [sendUrl] fetched (as bytes) only once actually picked. */
data class GifResult(val id: String, val previewUrl: String, val sendUrl: String, val width: Int, val height: Int)

private const val GIPHY_BASE = "https://api.giphy.com/v1/gifs"

private fun GiphyGif.toResult(): GifResult? {
    val preview = images.fixedWidthSmall ?: images.fixedWidth
    val send = images.downsized ?: images.fixedWidth ?: images.original
    val previewUrl = preview?.url ?: return null
    val sendUrl = send?.url ?: return null
    return GifResult(id, previewUrl, sendUrl, send.width?.toIntOrNull() ?: 200, send.height?.toIntOrNull() ?: 200)
}

private suspend fun searchGifs(client: HttpClient, apiKey: String, query: String): List<GifResult> {
    val response: GiphySearchResponse = if (query.isNotBlank()) {
        client.get(GIPHY_BASE + "/search") {
            parameter("api_key", apiKey)
            parameter("q", query)
            parameter("limit", "24")
            parameter("rating", "g")
        }.body()
    } else {
        client.get(GIPHY_BASE + "/trending") {
            parameter("api_key", apiKey)
            parameter("limit", "24")
            parameter("rating", "g")
        }.body()
    }
    return response.data.mapNotNull { it.toResult() }
}

/**
 * GIF tab of EmojiGifPanel.kt -- search (debounced) or trending by default,
 * tap a result to send it. Direct port of d2m_web's GifPicker.jsx UX
 * (debounce/request-id-guarded exactly the same way, so a fast typer never
 * has an old query's results race a newer one onto screen).
 */
@Composable
fun GifPickerPanel(client: HttpClient, onPick: (GifResult) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GifResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val apiKey = remember { giphyApiKey() }

    // Debounced the same way as the web version -- 450ms of no typing before
    // firing a new search, with a plain "did a newer query already start"
    // guard (via LaunchedEffect's own cancel-on-key-change semantics, which
    // does the same job as GifPicker.jsx's manual reqId counter) so a slow
    // response for an old query can never overwrite a newer one's results.
    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(450)
        loading = true
        error = null
        try {
            results = searchGifs(client, apiKey, query)
        } catch (e: Exception) {
            error = e.message ?: "Could not load GIFs"
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            placeholder = { Text("Search GIFs", color = mutedText(0.4f)) },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = mutedText(0.15f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 220.dp)) {
            when {
                loading && results.isEmpty() -> CenteredHint("Loading GIFs…")
                error != null -> CenteredHint(error!!)
                results.isEmpty() -> CenteredHint("No GIFs found.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                ) {
                    items(results, key = { it.id }) { gif ->
                        AsyncImage(
                            model = gif.previewUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(gif.width.toFloat() / gif.height.toFloat())
                                .background(mutedText(0.06f), RoundedCornerShape(8.dp))
                                .clickable { onPick(gif) },
                        )
                    }
                }
            }
        }
        // Required by GIPHY's API terms whenever their content/search is used.
        Text(
            "Powered by GIPHY",
            style = MaterialTheme.typography.labelSmall,
            color = mutedText(0.5f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = mutedText(0.5f))
    }
}

/** Fetches a picked GIF's actual bytes (the search/trending endpoints only ever return metadata + a preview URL) -- called once, right before handing off to MessagingRepository.sendMedia. */
suspend fun fetchGifBytes(client: HttpClient, gif: GifResult): ByteArray = client.get(gif.sendUrl).body()
