package com.dimenzy.app.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dimenzy.app.AppContainer
import com.dimenzy.app.ui.ar.ArScreen
import com.dimenzy.app.ui.ar.ArViewModel
import com.dimenzy.app.ui.capture.CaptureScreen
import com.dimenzy.app.ui.common.LocalNavAnimatedScope
import com.dimenzy.app.ui.common.LocalSharedTransitionScope
import com.dimenzy.app.ui.generate.GenerateScreen
import com.dimenzy.app.ui.preview.PreviewScreen
import com.dimenzy.app.ui.preview.PreviewViewModel

/** Route names from spec 0001: capture → generate → preview/{modelId} → ar/{modelId}. Only ids cross routes. */
object Routes {
  const val CAPTURE = "capture"
  const val GENERATE = "generate"
  const val PREVIEW = "preview/{${PreviewViewModel.ARG_MODEL_ID}}"
  const val AR = "ar/{${ArViewModel.ARG_MODEL_ID}}"

  fun preview(modelId: String) = "preview/$modelId"

  fun ar(modelId: String) = "ar/$modelId"
}

/** How long routes without a shared element crossfade; the shared photo card moves on its own spring. */
private const val ROUTE_FADE_MS = 350

@Composable
fun DimenzyNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
  // The photo panel, the generating card and the preview share one surface across routes (ui/common/SharedTransitions.kt)
  SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
      NavHost(
        navController = navController,
        startDestination = Routes.CAPTURE,
        enterTransition = { fadeIn(tween(ROUTE_FADE_MS)) },
        exitTransition = { fadeOut(tween(ROUTE_FADE_MS)) },
        popEnterTransition = { fadeIn(tween(ROUTE_FADE_MS)) },
        popExitTransition = { fadeOut(tween(ROUTE_FADE_MS)) },
      ) {
        composable(Routes.CAPTURE) { entry ->
          WithRouteScope {
            CaptureScreen(
              viewModel = viewModel(factory = container.captureViewModelFactory),
              // dropUnlessResumed ignores the second tap of a double tap, once navigation has begun.
              onGenerate = dropUnlessResumed { navController.navigate(Routes.GENERATE) { launchSingleTop = true } },
              onTrySample = { modelId ->
                entry.ifResumed { navController.navigate(Routes.preview(modelId)) { launchSingleTop = true } }
              },
            )
          }
        }
        composable(Routes.GENERATE) { entry ->
          WithRouteScope {
            GenerateScreen(
              viewModel = viewModel(factory = container.generateViewModelFactory),
              onDone = { modelId -> navController.openPreviewFromGenerate(modelId) },
              onBack = dropUnlessResumed { navController.popBackStack() },
              onTrySample = { modelId -> entry.ifResumed { navController.openPreviewFromGenerate(modelId) } },
            )
          }
        }
        composable(
          Routes.PREVIEW,
          arguments = listOf(navArgument(PreviewViewModel.ARG_MODEL_ID) { type = NavType.StringType }),
        ) { entry ->
          val modelId = entry.arguments?.getString(PreviewViewModel.ARG_MODEL_ID).orEmpty()
          WithRouteScope {
            PreviewScreen(
              viewModel = viewModel(factory = container.previewViewModelFactory),
              onNewPhoto = dropUnlessResumed { navController.popBackStack(Routes.CAPTURE, inclusive = false) },
              onPlaceInAr = dropUnlessResumed { navController.navigate(Routes.ar(modelId)) { launchSingleTop = true } },
            )
          }
        }
        composable(
          Routes.AR,
          arguments = listOf(navArgument(ArViewModel.ARG_MODEL_ID) { type = NavType.StringType }),
        ) {
          ArScreen(
            viewModel = viewModel(factory = container.arViewModelFactory),
            arCoreCheck = container.arCoreCheck,
            onBack = dropUnlessResumed { navController.popBackStack() },
          )
        }
      }
    }
  }
}

/** Gives a route's screen the route's own enter/exit scope, which its shared elements animate against. */
@Composable
private fun AnimatedContentScope.WithRouteScope(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalNavAnimatedScope provides this, content = content)
}

/** [dropUnlessResumed] for a callback that takes an argument: ignores a call once navigation has begun. */
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
  if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

/** Generate is removed from the back stack, so Back from the preview lands on capture. */
private fun NavHostController.openPreviewFromGenerate(modelId: String) {
  if (currentDestination?.route != Routes.GENERATE) return
  navigate(Routes.preview(modelId)) { popUpTo(Routes.GENERATE) { inclusive = true } }
}
