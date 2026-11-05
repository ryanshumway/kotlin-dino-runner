package com.ryanshumway.dinorunner.game

/** Everything on screen at one instant. */
data class World(
    val player: Player = Player(),
    /** Total distance scrolled this run. Drives the score and the ground pattern. */
    val distance: Float = 0f,
)

/**
 * Which phase the game is in. Each phase carries the [World] so the screen can
 * always draw it: the idle dino before a run, or the frozen scene after a crash.
 *
 * Kotlin note: a `sealed interface` is a closed set of cases, like a Swift
 * `enum` with associated values. A `when (state)` that covers all three needs
 * no `else`, and adding a fourth phase makes the compiler flag every `when`
 * that forgot to handle it.
 */
sealed interface GameState {
    val world: World

    /** Waiting for the first tap. */
    data class Ready(override val world: World = World()) : GameState

    /** The run is live. The world advances every frame. */
    data class Playing(override val world: World) : GameState

    /** The player crashed. The world is frozen until the next tap. */
    data class GameOver(override val world: World) : GameState
}
