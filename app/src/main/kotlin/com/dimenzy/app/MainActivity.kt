package com.dimenzy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dimenzy.app.ui.capture.CaptureScreen
import com.dimenzy.app.ui.theme.DimenzyTheme

/** Route names from spec 0001: capture → generate → preview/{modelId} → ar/{modelId}. */
object Routes {
  const val CAPTURE = "capture"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DimenzyTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          DimenzyNavHost()
        }
      }
    }
  }
}

@Composable
private fun DimenzyNavHost() {
  val navController = rememberNavController()
  NavHost(navController = navController, startDestination = Routes.CAPTURE) {
    composable(Routes.CAPTURE) { CaptureScreen() }
  }
}
