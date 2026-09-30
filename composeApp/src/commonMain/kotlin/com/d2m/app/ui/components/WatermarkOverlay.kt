package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.d2m.app.data.network.AuditApi
import com.d2m.app.data.network.ScreenViewAuditResponseDto
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject

private const val PING_INTERVAL_MS = 60_000L
private const val BLOCK_SIZE_DP = 10f
private const val ALPHA = 0.01f
private const val PAYLOAD_BITS = 32

/**
 * Whole-screen forensic watermark -- the mobile counterpart to d2m_web's
 * WatermarkOverlay.jsx/blockWatermark.js, using the exact same encoding
 * (see that file's doc comment for the full reasoning): a 32-bit payload
 * -- the first 8 hex characters of watermark_id -- spread across many
 * small blocks via a hash-shuffled assignment, each "1" bit painted as a
 * single-direction (lighten-only) fill at 1% alpha, "0" bits left
 * untouched. Not meant to be read by eye at all -- see
 * scripts/reveal_watermark.py (backend repo) for the decode side: an
 * analyst runs a suspect leaked screenshot/photo through that script to
 * recover the watermark_id prefix, then looks it up in
 * screen_view_audits (app/routers/audit.py) for the exact viewer, screen,
 * IP, and time that produced it. Verified (on the web side, sharing this
 * same math) against a synthetic worst-case render: invisible at normal
 * viewing, decodes correctly even after JPEG re-encoding at quality 65.
 *
 * Mounted once at App()'s root, above the whole nav graph, so it sits
 * over every screen. Baked into the rendered pixels (a real Canvas draw,
 * not an accessibility label or metadata), so it survives a screenshot, a
 * screen recording, or someone photographing the device with a second
 * camera -- none of which FLAG_SECURE (Android) or the iOS capture
 * notifications can actually prevent on their own, see those integration
 * points for where real blocking is/isn't possible.
 *
 * Pings d2m_core_engine's POST /audit/screen-view (AuditApi.kt) once on
 * mount/screen change and every PING_INTERVAL_MS after.
 */
// MUST stay bit-for-bit identical to d2m_web's blockWatermark.js mixHash
// and d2m_core_engine's scripts/reveal_watermark.py _mix -- all three
// encode/decode the same payload via this exact hash, see this file's
// own doc comment. Kotlin Int multiplication already wraps on overflow
// the same way Python's `& 0xffffffff` masking does (just interpreted as
// signed vs. unsigned -- same 32 bits either way), so no explicit masking
// is needed here the way the Python version has it.
private fun mixHash(input: Int): Int {
    var x = input
    x = (x xor (x ushr 16)) * 2146121005 // 0x7feb352d
    x = (x xor (x ushr 15)) * -2073254261 // 0x846ca68b as a signed 32-bit Int
    return x xor (x ushr 16)
}

private fun bitFor(row: Int, col: Int): Int {
    val h = mixHash(row * 92821 + col * 68917 + 12345)
    // Kotlin's Int.mod (not rem/%) always returns a non-negative result
    // for a positive divisor, matching Python's `%` -- java.lang.Math
    // isn't available on the iOS/Native target, this is the common-stdlib
    // equivalent.
    return h.mod(PAYLOAD_BITS)
}

private fun watermarkIdToPayload(watermarkId: String): Int =
    watermarkId.take(8).toLong(16).toInt()

@Composable
fun WatermarkOverlay(screen: String, modifier: Modifier = Modifier) {
    val identityStore: IdentityStore = koinInject()
    val auditApi: AuditApi = koinInject()
    val identity by identityStore.identity.collectAsState()
    val userId = if (identity.role == D2MRole.PARENT) identity.sponsorId else identity.primaryId

    var stamp by remember { mutableStateOf<ScreenViewAuditResponseDto?>(null) }
    val currentScreen by rememberUpdatedState(screen)

    LaunchedEffect(userId) {
        if (userId == null) {
            stamp = null
            return@LaunchedEffect
        }
        while (isActive) {
            stamp = auditApi.recordScreenView(currentScreen)
            delay(PING_INTERVAL_MS)
        }
    }

    val currentStamp = stamp
    if (userId != null && currentStamp != null) {
        val payload = remember(currentStamp.watermarkId) { watermarkIdToPayload(currentStamp.watermarkId) }

        Canvas(modifier = modifier.fillMaxSize()) {
            val blockPx = BLOCK_SIZE_DP * density
            val cols = kotlin.math.ceil(size.width / blockPx).toInt()
            val rows = kotlin.math.ceil(size.height / blockPx).toInt()
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val bitIdx = bitFor(row, col)
                    val bit = (payload ushr bitIdx) and 1
                    if (bit == 1) {
                        drawRect(
                            color = Color.White.copy(alpha = ALPHA),
                            topLeft = Offset(col * blockPx, row * blockPx),
                            size = Size(blockPx, blockPx),
                        )
                    }
                }
            }
        }
    }
}
