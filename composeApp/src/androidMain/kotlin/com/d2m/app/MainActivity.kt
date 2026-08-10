package com.d2m.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Referenced by AndroidManifest.xml (.MainActivity) with the claim-link
 * deep-link intent filters already declared there. Koin/Settings are
 * initialized in D2MApplication.onCreate(), which always runs before any
 * Activity's onCreate() -- this class only has to host the Compose tree.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
