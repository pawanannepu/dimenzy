package com.dimenzy.app.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

/**
 * The photo's journey across routes, told as one object: the capture screen's photo panel shrinks into the
 * generating card (the photo becomes its thumbnail), and the card grows into the 3D preview.
 * DimenzyNavHost provides both scopes; outside it (a preview, a test) these modifiers do nothing.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private const val PHOTO_CARD = "photo-card"
private const val PHOTO = "photo"
private val CardShape = RoundedCornerShape(24.dp)
private const val HOLD_MS = 160
private const val CROSSFADE_MS = 240

// A quick start with a long, soft settle and no overshoot: the card moves as one solid thing
private val BoundsSpring = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Rect.VisibilityThreshold)

/**
 * The surface the photo lives on: the photo panel, the generating card, the preview screen.
 * Each route's version crossfades inside the moving bounds.
 */
@Composable
fun Modifier.sharedPhotoCard(): Modifier {
  val shared = LocalSharedTransitionScope.current ?: return this
  val animated = LocalNavAnimatedScope.current ?: return this
  return with(shared) {
    this@sharedPhotoCard.sharedBounds(
      sharedContentState = rememberSharedContentState(PHOTO_CARD),
      animatedVisibilityScope = animated,
      // The leaving surface holds for a beat before the crossfade, so the card is seen growing (or shrinking);
      // crossfading at once, it dissolves into a background of nearly the same color and the move is lost
      enter = fadeIn(tween(CROSSFADE_MS, delayMillis = HOLD_MS)),
      exit = fadeOut(tween(CROSSFADE_MS, delayMillis = HOLD_MS)),
      boundsTransform = { _, _ -> BoundsSpring },
      clipInOverlayDuringTransition = OverlayClip(CardShape),
    )
  }
}

/**
 * True once this route's enter transition has finished (at once outside DimenzyNavHost), and true from then on,
 * so heavy setup (the 3D engine) can wait for the transition instead of stealing its frames. It latches: the
 * route's later exit transition never turns it back off.
 */
@Composable
fun rememberRouteSettled(): Boolean {
  val animated = LocalNavAnimatedScope.current
  var settled by remember { mutableStateOf(animated == null) }
  if (!settled && animated != null) {
    LaunchedEffect(animated) {
      val transition = animated.transition
      snapshotFlow { !transition.isRunning && transition.currentState == EnterExitState.Visible }.first { it }
      settled = true
    }
  }
  return settled
}

/** The photo itself: the panel's photo and the card's thumbnail are one element that changes size. */
@Composable
fun Modifier.sharedPhoto(): Modifier {
  val shared = LocalSharedTransitionScope.current ?: return this
  val animated = LocalNavAnimatedScope.current ?: return this
  return with(shared) {
    this@sharedPhoto.sharedElement(
      sharedContentState = rememberSharedContentState(PHOTO),
      animatedVisibilityScope = animated,
      boundsTransform = { _, _ -> BoundsSpring },
    )
  }
}
