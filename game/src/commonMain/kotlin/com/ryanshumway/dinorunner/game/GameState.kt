package com.ryanshumway.dinorunner.game

/** Everything on screen at one instant. */
data class World(
    val player: Player = Player(),
    val obstacles: List<Obstacle> = emptyList(),
    /** Total distance scrolled this run. Drives the score, the speed and the ground pattern. */
    val distance: Float = 0f,
    /**
     * Distance left to scroll before the next obstacle spawns at the right
     * edge. Zero means the first obstacle spawns on the first frame, just off
     * screen, so it takes a couple of seconds to arrive.
     */
    val untilNextObstacle: Float = 0f,
)

/**
 * Which phase the game is in. Each phase carries the [World] so the screen can
 * always draw it: the idle dino before a run, or the frozen scene after a crash.
 *
 * Kotlin note: a `sealed interface` is a closed set of cases, like a Swift
 * `enum` with associated values. A `when (state)` that covers every case needs
 * no `else`, and adding a phase makes the compiler flag every `when` that
 * forgot to handle it. (That's exactly what happened when Paused was added.)
 * Each case can also carry its own data, like GameOver's timer below.
 */
sealed interface GameState {
    val world: World

    /** Waiting for the first tap. */
    data class Ready(override val world: World = World()) : GameState

    /** The run is live. The world advances every frame. */
    data class Playing(override val world: World) : GameState

    /**
     * The app went to the background mid-run. The world is frozen until a
     * tap, which starts a countdown ([resumeIn] seconds left) so the player
     * has time to get ready. `null` means waiting for that tap.
     */
    data class Paused(override val world: World, val resumeIn: Float? = null) : GameState

    /** The player crashed. The world is frozen until a tap after the restart delay. */
    data class GameOver(override val world: World, val secondsSinceCrash: Float = 0f) : GameState
}
