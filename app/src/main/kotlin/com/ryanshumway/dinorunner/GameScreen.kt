package com.ryanshumway.dinorunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryanshumway.dinorunner.game.GameState

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    // The game loop. `withFrameNanos` suspends until the screen is about to
    // draw its next frame, then hands us that frame's timestamp, much like
    // requestAnimationFrame on the web or CADisplayLink on iOS. We pass the
    // real elapsed time to the engine, so speed doesn't depend on refresh rate.
    // The loop pauses on its own when the app is in the background (no frames
    // are drawn), and LaunchedEffect cancels it when the screen goes away.
    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                viewModel.onFrame((now - lastFrame) / 1_000_000_000f)
                lastFrame = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Dino runner. Tap to jump, hold to jump higher." }
            // One gesture is one press and release. Reporting both edges is
            // what lets a long hold give a higher jump than a quick tap.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    viewModel.onPress()
                    waitForUpOrCancellation()
                    viewModel.onRelease()
                }
            },
    ) {
        // `viewModel.state` is read inside the draw lambda, not in the
        // composable body, so a new frame only re-runs drawing. Compose skips
        // rebuilding the layout, which keeps the per-frame cost low.
        Canvas(Modifier.fillMaxSize()) { drawGame(viewModel.state) }

        Hud(viewModel.state, viewModel.score)
    }
}

@Composable
private fun Hud(state: GameState, score: Int) {
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = Palette.ink)
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        BasicText(
            text = score.toString().padStart(5, '0'),
            style = style,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        // Kotlin note: `when` is an expression, so it returns the message
        // directly, and it must cover every GameState case.
        val message = when (state) {
            is GameState.Ready -> "TAP TO RUN"
            is GameState.Playing -> null
            is GameState.GameOver -> "GAME OVER · TAP TO RETRY"
        }
        // `?.let` runs only when message isn't null, like `if let` in Swift.
        message?.let { BasicText(it, style = style, modifier = Modifier.align(Alignment.Center)) }
    }
}
