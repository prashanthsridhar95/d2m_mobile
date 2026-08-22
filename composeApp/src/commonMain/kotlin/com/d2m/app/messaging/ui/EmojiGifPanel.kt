package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import io.ktor.client.HttpClient

private enum class EmojiPanelTab { EMOJI, STICKER, GIF }

/**
 * The Emoji/Stickers/GIFs tabbed panel that opens above ChatPane's composer
 * -- direct port of MessageComposer.jsx's `emojiOpen` panel (same three
 * tabs, same behavior split): tapping an emoji APPENDS it to the draft and
 * keeps the panel open (so someone can pick several in a row), tapping a
 * sticker SENDS it immediately as its own message (large, standalone --
 * matches web's onSend(e, ...) call), tapping a GIF result fetches its bytes
 * and sends it as media.
 */
@Composable
fun EmojiGifPanel(
    client: HttpClient,
    onPickEmoji: (String) -> Unit,
    onSendSticker: (String) -> Unit,
    onPickGif: (GifResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(EmojiPanelTab.EMOJI) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D2MRadius.md))
            .border(1.dp, mutedText(0.12f), RoundedCornerShape(D2MRadius.md)),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            EmojiTabButton("Emoji", tab == EmojiPanelTab.EMOJI, Modifier.weight(1f)) { tab = EmojiPanelTab.EMOJI }
            EmojiTabButton("Stickers", tab == EmojiPanelTab.STICKER, Modifier.weight(1f)) { tab = EmojiPanelTab.STICKER }
            EmojiTabButton("GIFs", tab == EmojiPanelTab.GIF, Modifier.weight(1f)) { tab = EmojiPanelTab.GIF }
        }
        when (tab) {
            EmojiPanelTab.GIF -> GifPickerPanel(client = client, onPick = onPickGif)
            EmojiPanelTab.EMOJI, EmojiPanelTab.STICKER -> {
                val items = if (tab == EmojiPanelTab.EMOJI) EMOJIS else STICKERS
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(8.dp),
                ) {
                    items(items) { emoji ->
                        Box(
                            modifier = Modifier.padding(4.dp)
                                .clickable {
                                    if (tab == EmojiPanelTab.EMOJI) onPickEmoji(emoji) else onSendSticker(emoji)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = if (tab == EmojiPanelTab.EMOJI) 22.sp else 30.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Bottom-underline tab indicator (a thin colored Box, not a full border) -- matches web's `boxShadow: inset 0 -2px 0 var(--accent)` on the selected tab. */
@Composable
private fun EmojiTabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else mutedText(0.55f),
        )
        Box(
            modifier = Modifier.padding(top = 6.dp).fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}
