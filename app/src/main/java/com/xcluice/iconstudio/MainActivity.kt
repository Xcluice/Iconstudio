package com.xcluice.iconstudio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            PinQueue.onPinned(this@MainActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(Pinner.ACTION), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setContent {
            StudioTheme {
                StudioRoot(remember { StudioState() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PinQueue.foreground = true
        if (PinQueue.pendingPin) {
            PinQueue.pendingPin = false
            PinQueue.pinCurrent(this)
        }
    }

    override fun onPause() {
        PinQueue.foreground = false
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}
