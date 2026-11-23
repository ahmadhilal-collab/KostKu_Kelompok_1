package com.kostku.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kostku.app.push.PushNavigation
import com.kostku.app.ui.components.TourHost
import com.kostku.app.ui.navigation.AppNavHost
import com.kostku.app.ui.theme.KostKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Dibuka dengan mengetuk notifikasi → simpan tujuan layarnya.
        if (savedInstanceState == null) PushNavigation.handle(intent)
        setContent {
            KostKuTheme {
                // TourHost menggambar panduan interaktif di atas seluruh layar aplikasi.
                TourHost {
                    AppNavHost()
                }
            }
        }
    }

    /** Aplikasi sudah terbuka lalu notifikasi diketuk. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushNavigation.handle(intent)
    }
}
