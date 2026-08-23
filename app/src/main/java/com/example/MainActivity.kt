package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.ui.AppShell
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EdgeAITheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val edgeAI = EdgeAICore.getInstance(this)

    setContent {
      var currentThemeMode by remember { mutableStateOf(AppThemeMode.LIGHT) } // Google Light is default

      EdgeAITheme(
        themeMode = currentThemeMode,
        onThemeModeChange = { currentThemeMode = it }
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppShell(edgeAI = edgeAI)
        }
      }
    }
  }
}

