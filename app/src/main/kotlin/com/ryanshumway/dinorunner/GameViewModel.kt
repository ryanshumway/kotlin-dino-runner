package com.ryanshumway.dinorunner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshumway.dinorunner.game.GameConfig
import com.ryanshumway.dinorunner.game.GameEngine
import com.ryanshumway.dinorunner.game.GameState
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

    fun onPress() {
        state = engine.press(state)
        if (state is GameState.Playing) isNewHighScore = false
    }

    fun onRelease() {
        state = engine.release(state)
    }

    fun onFrame(frameSeconds: Float) {
        val before = state
        state = engine.step(state, frameSeconds, viewWidth)
        // The engine just reports the crash. Deciding what to do about it,
        // here saving a score, is the app's job, which keeps :game free of
        // any storage code.
        if (before !is GameState.GameOver && state is GameState.GameOver) onRunEnded(score)
    }

    /** The renderer always shows VIEW_HEIGHT units vertically, so width follows the aspect ratio. */
    fun onViewportChanged(widthPx: Int, heightPx: Int) {
        if (heightPx > 0) viewWidth = widthPx.toFloat() / heightPx * VIEW_HEIGHT
    }

    private fun onRunEnded(finalScore: Int) {
        if (finalScore <= highScore) return
        highScore = finalScore
        isNewHighScore = true
        viewModelScope.launch { highScores.saveIfBest(finalScore) }
    }
}
