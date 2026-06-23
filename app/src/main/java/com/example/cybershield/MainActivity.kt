package com.example.cybershield

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cybershield.theme.CyberShieldTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Request notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
    }

    enableEdgeToEdge()
    setContent {
      CyberShieldTheme { 
        Surface(
            modifier = Modifier.fillMaxSize(), 
            color = MaterialTheme.colorScheme.background
        ) { 
            var showSplash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            if (showSplash) {
                com.example.cybershield.ui.auth.SplashScreen(onSplashFinished = { showSplash = false })
            } else {
                val session by com.example.cybershield.data.SupabaseManager.getInstance()
                    .currentUserFlow
                    .collectAsState(initial = null)

                if (session != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val settings = androidx.compose.runtime.remember { com.example.cybershield.data.AppSettings.getInstance(context) }
                    var isSetupCompleted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(settings.isSetupCompleted) }

                    if (isSetupCompleted) {
                        MainNavigation()
                    } else {
                        SetupNavigation(onSetupFinished = {
                            settings.isSetupCompleted = true
                            isSetupCompleted = true
                        })
                    }
                } else {
                    AuthNavigation()
                }
            }
        } 
      }
    }
  }
}
