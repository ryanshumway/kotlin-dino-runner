package com.ryanshumway.dinorunner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshumway.dinorunner.game.Effects
import com.ryanshumway.dinorunner.game.GameConfig
import com.ryanshumway.dinorunner.game.GameEngine
import com.ryanshumway.dinorunner.game.GameState
import com.ryanshumway.dinorunner.game.next
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the current [GameState] and turns UI events into engine calls.
 *
 * A ViewModel outlives configuration changes such as rotation or resizing on
 * a foldable, so a run in progress isn't lost. That's the role an
 * `@Observable` model owned by `@State` plays in SwiftUI.
 *
 * It's an `AndroidViewModel` only so it can reach the app's Context for
 * [HighScoreStore]. In a larger app you'd pass the store in through a
 * ViewModel factory or a dependency-injection library instead.
 *
 * Kotlin note: `by mutableStateOf(...)` is property delegation. Reads and
 * writes of `state` go through a Compose State object, so any composable or
 * Canvas that reads `state` is redrawn when it changes, much like
 * `@Published`. `private set` means only this class can write it.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    val config = GameConfig()
    private val engine = GameEngine(config)
    private val highScores = HighScoreStore(application)

    /** Screen width in world units. Updated when the screen is measured. */
    private var viewWidth = 640f

    var state: GameState by mutableStateOf(engine.newGame())
        private set

    var highScore: Int by mutableIntStateOf(0)
        private set

    /** True on the game-over screen when the run that just ended set a new best. */
    var isNewHighScore: Boolean by mutableStateOf(false)
        private set

    /** Dust, shake, and score blink. Purely visual. */
    var effects: Effects by mutableStateOf(Effects())
        private set

    val score: Int get() = engine.score(state)
    val nightAmount: Float get() = engine.nightAmount(state)

    init {
        // Load the saved best once at startup. DataStore reads the file on a
        // background thread; this coroutine just waits for the result without
        // blocking the UI. `viewModelScope` cancels it if the ViewModel is
        // destroyed. `maxOf` covers the unlikely case that a run already beat
        // the best before the load finished.
        viewModelScope.launch {
            highScore = maxOf(highScore, highScores.highScore.first())
        }
    }

    /** Returns true if this press started a jump, so the screen can play a haptic tick. */
    fun onPress(): Boolean {
        val before = state
        advance(engine.press(state), dt = 0f)
        return before.world.player.isGrounded && !state.world.player.isGrounded
    }

    fun onRelease() {
        advance(engine.release(state), dt = 0f)
    }

    fun onFrame(frameSeconds: Float) {
        advance(engine.step(state, frameSeconds, viewWidth), frameSeconds)
    }

    /** The app went to the background. */
    fun onPause() {
        advance(engine.pause(state), dt = 0f)
    }

    /** The renderer always shows VIEW_HEIGHT units vertically, so width follows the aspect ratio. */
    fun onViewportChanged(widthPx: Int, heightPx: Int) {
        if (heightPx > 0) viewWidth = widthPx.toFloat() / heightPx * VIEW_HEIGHT
    }

    /**
     * Every state change goes through here, so effects and the high score
     * can react to it in one place, whatever caused it.
     */
    private fun advance(next: GameState, dt: Float) {
        val before = state
        effects = effects.next(before, next, dt, config)
        state = next
        // The engine just reports the crash. Deciding what to do about it,
        // here saving a score, is the app's job, which keeps :game free of
        // any storage code.
        if (before !is GameState.GameOver && next is GameState.GameOver) onRunEnded(score)
        if (before is GameState.GameOver && next is GameState.Playing) isNewHighScore = false
    }

    private fun onRunEnded(finalScore: Int) {
        if (finalScore <= highScore) return
        highScore = finalScore
        isNewHighScore = true
        viewModelScope.launch { highScores.saveIfBest(finalScore) }
    }
}
