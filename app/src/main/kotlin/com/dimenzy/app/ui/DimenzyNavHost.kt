package com.dimenzy.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dimenzy.app.AppContainer
import com.dimenzy.app.data.ModelStore
import com.dimenzy.app.ui.capture.CaptureScreen
import com.dimenzy.app.ui.generate.GenerateScreen
import com.dimenzy.app.ui.preview.PreviewScreen
import com.dimenzy.app.ui.preview.PreviewViewModel

/** Route names from spec 0001: capture → generate → preview/{modelId} (→ ar/{modelId} in feature 4). Only ids cross routes. */
object Routes {
  const val CAPTURE = "capture"
  const val GENERATE = "generate"
  const val PREVIEW = "preview/{${PreviewViewModel.ARG_MODEL_ID}}"

  fun preview(modelId: String) = "preview/$modelId"
}

@Composable
fun DimenzyNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
  NavHost(navController = navController, startDestination = Routes.CAPTURE) {
    composable(Routes.CAPTURE) {
      CaptureScreen(
        viewModel = viewModel(factory = container.captureViewModelFactory),
        // dropUnlessResumed ignores the second tap of a double tap, once navigation has begun.
        onGenerate = dropUnlessResumed { navController.navigate(Routes.GENERATE) { launchSingleTop = true } },
        onTrySample = dropUnlessResumed {
          navController.navigate(Routes.preview(ModelStore.SAMPLE_ID)) { launchSingleTop = true }
        },
      )
    }
    composable(Routes.GENERATE) {
      GenerateScreen(
        viewModel = viewModel(factory = container.generateViewModelFactory),
        onDone = { modelId -> navController.openPreviewFromGenerate(modelId) },
        onBack = dropUnlessResumed { navController.popBackStack() },
        onTrySample = dropUnlessResumed { navController.openPreviewFromGenerate(ModelStore.SAMPLE_ID) },
      )
    }
    composable(
      Routes.PREVIEW,
      arguments = listOf(navArgument(PreviewViewModel.ARG_MODEL_ID) { type = NavType.StringType }),
    ) {
      PreviewScreen(
        viewModel = viewModel(factory = container.previewViewModelFactory),
        onNewPhoto = dropUnlessResumed { navController.popBackStack(Routes.CAPTURE, inclusive = false) },
      )
    }
  }
}

/** Generate is removed from the back stack, so Back from the preview lands on capture. */
private fun NavHostController.openPreviewFromGenerate(modelId: String) {
  if (currentDestination?.route != Routes.GENERATE) return
  navigate(Routes.preview(modelId)) { popUpTo(Routes.GENERATE) { inclusive = true } }
}
