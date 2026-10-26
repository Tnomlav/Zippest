package com.decompress.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.decompress.app.ui.ZippestApp

class MainActivity : ComponentActivity() {
    private val incomingArchiveUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingArchiveUri.value = intent?.data
        enableEdgeToEdge()
        setContent {
            ZippestApp(incomingArchiveUri = incomingArchiveUri.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingArchiveUri.value = intent.data
    }
}
