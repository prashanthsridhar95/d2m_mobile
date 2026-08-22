package com.d2m.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.d2m.app.messaging.call.PendingCallAccept
import com.d2m.app.push.EXTRA_ACCEPT_CALL_ID

/**
 * Referenced by AndroidManifest.xml (.MainActivity) with the claim-link
 * deep-link intent filters already declared there. Koin/Settings are
 * initialized in D2MApplication.onCreate(), which always runs before any
 * Activity's onCreate() -- this class only has to host the Compose tree.
 *
 * consumeAcceptExtra: the incoming-call notification's Accept action now
 * launches THIS activity directly (PendingIntent.getActivity, carrying
 * EXTRA_ACCEPT_CALL_ID) instead of going through a BroadcastReceiver --
 * see PendingCallAccept.kt's doc comment for why. onNewIntent is needed
 * alongside onCreate because MainActivity has no special launchMode
 * declared: if a task already has an instance of it (app was opened before,
 * not fully killed), Android reuses that instance and delivers here instead
 * of a fresh onCreate.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeAcceptExtra(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAcceptExtra(intent)
    }

    private fun consumeAcceptExtra(intent: Intent?) {
        intent?.getStringExtra(EXTRA_ACCEPT_CALL_ID)?.let { callId ->
            PendingCallAccept.set(callId)
        }
    }
}
