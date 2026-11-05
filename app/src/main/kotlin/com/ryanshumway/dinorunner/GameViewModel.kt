package com.ryanshumway.dinorunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ryanshumway.dinorunner.game.GameConfig
import com.ryanshumway.dinorunner.game.GameEngine
import com.ryanshumway.dinorunner.game.GameState

/**
 * Holds the current [GameState] and turns UI events into engine calls.
 *
 * A ViewModel outlives configuration changes such as rotation or resizing on
 * a foldable, so a run in progress isn't lost. That's the role an
 * `@Observable` model owned by `@State` plays in SwiftUI.
 *
 * Kotlin note: `by mutableStateOf(...)` is property delegation. Reads and
 * writes of `state` go through a Compose State object, so any composable or
 * Canvas that reads `state` is redrawn when it changes, much like
 * `@Published`. `private set` means only this class can write it.
 */
class GameViewModel : ViewModel() {
    val config = GameConfig()
    private val engine = GameEngine(config)

    /** Screen width in world units. Updated when the screen is measured. */
    private var viewWidth = 640f

    var state: GameState by mutableStateOf(engine.newGame())
        private set

    val score: Int get() = engine.score(state)

    fun onPress() {
        state = engine.press(state)
    }

    fun onRelease() {
        state = engine.release(state)
    }

    fun onFrame(frameSeconds: Float) {
        state = engine.step(state, frameSeconds, viewWidth)
    }

    /** The renderer always shows VIEW_HEIGHT units vertically, so width follows the aspect ratio. */
    fun onViewportChanged(widthPx: Int, heightPx: Int) {
        if (heightPx > 0) viewWidth = widthPx.toFloat() / heightPx * VIEW_HEIGHT
    }
}
