package com.ryanshumway.dinorunner.game

/**
 * The rules of the game, as pure functions: given a state and an input (a
 * press, a release, or time passing), return the next state.
 *
 * Nothing here knows about Android, Compose, or the clock, so the whole game
 * can be unit tested by calling these functions with made-up timings. This is
 * the same idea as a Redux reducer, or a SwiftUI model kept free of views.
 */
class GameEngine(private val config: GameConfig = GameConfig()) {

    fun newGame(): GameState = GameState.Ready()

    /** Finger down / spacebar down. */
    fun press(state: GameState): GameState = when (state) {
        is GameState.Ready -> GameState.Playing(state.world.withPlayer { it.jump(config) })
        is GameState.Playing -> state.copy(world = state.world.withPlayer { it.jump(config) })
        // One tap restarts straight into a new run, already jumping.
        is GameState.GameOver -> GameState.Playing(World().withPlayer { it.jump(config) })
    }

    /** Finger up / spacebar up. Only matters mid-jump. */
    fun release(state: GameState): GameState = when (state) {
        is GameState.Playing -> state.copy(world = state.world.withPlayer { it.releaseJump(config) })
        else -> state
    }

    /**
     * Advance the game by [frameSeconds], the real time since the last frame.
     * Movement is speed × time, never "pixels per frame", so the game runs at
     * the same pace on a 60 Hz and a 120 Hz screen.
     */
    fun step(state: GameState, frameSeconds: Float): GameState {
        if (state !is GameState.Playing) return state
        // Kotlin note: after the `!is` check above, the compiler "smart casts"
        // `state` to Playing, so `state.copy` below needs no cast.
        val dt = frameSeconds.coerceIn(0f, config.maxFrameSeconds)
        val world = state.world
        return state.copy(
            world = world.copy(
                player = world.player.step(dt, config),
                distance = world.distance + config.runSpeed * dt,
            ),
        )
    }

    fun score(state: GameState): Int = (state.world.distance / config.unitsPerPoint).toInt()

    // Kotlin note: a private extension taking a lambda, so call sites read as
    // `world.withPlayer { it.jump(config) }`. `it` is the lambda's implicit
    // single parameter, like `$0` in a Swift closure.
    private inline fun World.withPlayer(change: (Player) -> Player): World =
        copy(player = change(player))
}
