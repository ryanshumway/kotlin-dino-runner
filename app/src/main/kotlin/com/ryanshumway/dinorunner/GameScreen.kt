package com.ryanshumway.dinorunner

import android.provider.Settings
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ryanshumway.dinorunner.game.GameState
import kotlin.math.ceil

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val haptics = LocalHapticFeedback.current
    val motion = rememberAnimationsEnabled()

    // The game loop. `withFrameNanos` suspends until the screen is about to
    // draw its next frame, then hands us that frame's timestamp, much like
    // requestAnimationFrame on the web or CADisplayLink on iOS. We pass the
    // real elapsed time to the engine, so speed doesn't depend on refresh rate.
    // LaunchedEffect cancels the loop when the screen goes away.
    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                viewModel.onFrame((now - lastFrame) / 1_000_000_000f)
                lastFrame = now
            }
        }
    }

    // Leaving the app mid-run (a call, the home button) pauses instead of
    // letting the dino run into a cactus while you're away.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    // A firm buzz on a crash. Keyed on the phase, so it fires once per crash.
    val isGameOver by remember { derivedStateOf { viewModel.state is GameState.GameOver } }
    LaunchedEffect(isGameOver) {
        if (isGameOver) haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewModel.onViewportChanged(it.width, it.height) }
            .semantics { contentDescription = "Dino runner. Tap to jump, hold to jump higher." }
            // One gesture is one press and release. Reporting both edges is
            // what lets a long hold give a higher jump than a quick tap.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    // A light tick when the dino leaves the ground.
                    if (viewModel.onPress()) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    waitForUpOrCancellation()
                    viewModel.onRelease()
                }
            },
    ) {
        // `viewModel.state` is read inside the draw lambda, not in the
        // composable body, so a new frame only re-runs drawing. Compose skips
        // recomposition and layout, which keeps the per-frame cost low.
        Canvas(Modifier.fillMaxSize()) {
            drawGame(viewModel.state, viewModel.config, viewModel.nightAmount, viewModel.effects, motion)
        }

        // The overlay should only rebuild when what it shows changes: the score
        // ticks about 10 times a second and the message only on a phase change,
        // while the state itself changes every frame. `derivedStateOf` watches
        // the state but only notifies when its computed result is different.
        val score by remember { derivedStateOf { viewModel.score } }
        val message by remember { derivedStateOf { messageFor(viewModel.state, viewModel.score, viewModel.isNewHighScore) } }
        val ink by remember { derivedStateOf { sceneColors(viewModel.nightAmount).hud } }
        // At each 100-point milestone the score blinks, about 6 times a second.
        // Skipped when animations are off: blinking is motion too.
        val scoreHidden by remember {
            derivedStateOf {
                val flash = viewModel.effects.flash
                motion && flash > 0f && (flash * 6).toInt() % 2 == 0
            }
        }
        Hud(score, viewModel.highScore, scoreHidden, message, ink)
    }
}

/**
 * Text to show, and to announce to TalkBack, for the current phase. A data
 * class so two equal messages compare equal: `derivedStateOf` uses that to
 * skip updates when nothing changed.
 */
private data class Message(val shown: String, val spoken: String)

// Kotlin note: `when` is an expression, so it returns the message directly,
// and it must cover every GameState case. `Message?` means "a Message or null".
private fun messageFor(state: GameState, score: Int, isNewHighScore: Boolean): Message? = when (state) {
    is GameState.Ready -> Message("TAP TO RUN", "Tap to run.")
    is GameState.Playing -> null
    is GameState.Paused -> when (val left = state.resumeIn) {
        null -> Message("PAUSED · TAP TO RESUME", "Paused. Tap to resume.")
        else -> ceil(left).toInt().toString().let { Message(it, it) }
    }
    is GameState.GameOver ->
        if (isNewHighScore) Message("NEW HIGH SCORE · TAP TO RETRY", "New high score: $score. Tap to retry.")
        else Message("GAME OVER · TAP TO RETRY", "Game over. Score $score. Tap to retry.")
}

@Composable
private fun Hud(score: Int, highScore: Int, scoreHidden: Boolean, message: Message?, ink: Color) {
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = ink)
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        BasicText(
            text = "HI ${highScore.padded()}  ${score.padded()}",
            style = style,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(if (scoreHidden) 0.25f else 1f)
                .semantics { contentDescription = "Score $score. Best $highScore." },
        )
        // `?.let` runs only when message isn't null, like `if let` in Swift.
        message?.let {
            BasicText(
                text = it.shown,
                style = style,
                modifier = Modifier
                    .align(Alignment.Center)
                    // A live region is read aloud by TalkBack whenever it
                    // changes, so a screen-reader user hears "Game over"
                    // without having to find it.
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = it.spoken
                    },
            )
        }
    }
}

/** Arcade-style zero padding: 42 becomes "00042". */
private fun Int.padded(): String = toString().padStart(5, '0')

/**
 * False when the user has turned on "Remove animations" in Android's
 * accessibility settings. That setting scales system animations to zero.
 * Compose doesn't expose it directly, so we read it once from system settings.
 */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}
