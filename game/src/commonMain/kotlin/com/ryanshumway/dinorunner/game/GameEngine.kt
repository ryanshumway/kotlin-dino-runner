package com.ryanshumway.dinorunner.game

import kotlin.random.Random

/**
 * The rules of the game, as functions: given a state and an input (a press, a
 * release, or time passing), return the next state.
 *
 * Nothing here knows about Android, Compose, or the clock, so the whole game
 * can be unit tested by calling these functions with made-up timings. This is
 * the same idea as a Redux reducer, or a SwiftUI model kept free of views. The
 * only outside input is [random], which tests replace with a seeded one.
 */
class GameEngine(
    private val config: GameConfig = GameConfig(),
    random: Random = Random.Default,
) {
    private val spawner = ObstacleSpawner(config, random)

    fun newGame(): GameState = GameState.Ready()

    /** Finger down / spacebar down. */
    fun press(state: GameState): GameState = when (state) {
        is GameState.Ready -> GameState.Playing(state.world.withPlayer { it.jump(config) })
        is GameState.Playing -> state.copy(world = state.world.withPlayer { it.jump(config) })
        is GameState.GameOver ->
            // One tap restarts straight into a new run, already jumping, but
            // only once the restart delay has passed.
            if (state.secondsSinceCrash < config.restartDelaySeconds) state
            else GameState.Playing(World().withPlayer { it.jump(config) })
    }

    /** Finger up / spacebar up. Only matters mid-jump. */
    fun release(state: GameState): GameState = when (state) {
        is GameState.Playing -> state.copy(world = state.world.withPlayer { it.releaseJump(config) })
        else -> state
    }

    /**
     * Advance the game by [frameSeconds], the real time since the last frame.
     * [viewWidth] is how many world units wide the screen is, so obstacles
     * appear just past its right edge on any screen size.
     *
     * Movement is speed × time, never "pixels per frame", so the game runs at
     * the same pace on a 60 Hz and a 120 Hz screen.
     */
    fun step(state: GameState, frameSeconds: Float, viewWidth: Float): GameState {
        val dt = frameSeconds.coerceIn(0f, config.maxFrameSeconds)
        return when (state) {
            is GameState.Ready -> state
            is GameState.GameOver -> state.copy(secondsSinceCrash = state.secondsSinceCrash + dt)
            is GameState.Playing -> stepPlaying(state.world, dt, viewWidth)
        }
    }

    /** Current scroll speed: ramps up with the score, then holds at the cap. */
    fun speedAt(distance: Float): Float =
        (config.runSpeed + config.speedGainPerPoint * distance / config.unitsPerPoint)
            .coerceAtMost(config.maxSpeed)

    fun score(state: GameState): Int = (state.world.distance / config.unitsPerPoint).toInt()

    /**
     * How dark the sky is: 0 is full day, 1 is full night. The sky flips at
     * every [GameConfig.dayNightEveryPoints] (night at 500, day again at
     * 1000, and so on), fading over [GameConfig.dayNightFadePoints] instead
     * of snapping.
     *
     * It's derived from distance rather than stored, so it needs no extra
     * state: a crash freezes the sky along with everything else, and a new
     * run starts in daylight.
     */
    fun nightAmount(state: GameState): Float {
        val points = state.world.distance / config.unitsPerPoint
        val period = (points / config.dayNightEveryPoints).toInt()
        if (period == 0) return 0f
        val pointsIntoPeriod = points - period * config.dayNightEveryPoints
        val fade = (pointsIntoPeriod / config.dayNightFadePoints).coerceIn(0f, 1f)
        // Odd periods are night (fading in), even periods are day (fading out).
        return if (period % 2 == 1) fade else 1f - fade
    }

    private fun stepPlaying(world: World, dt: Float, viewWidth: Float): GameState {
        val scrolled = speedAt(world.distance) * dt

        // Move every obstacle left, and forget the ones that are fully off
        // screen. Kotlin note: `map` and `filter` work like their JavaScript
        // and Swift counterparts and return new lists.
        var obstacles = world.obstacles
            .map { it.copy(x = it.x - scrolled) }
            .filter { it.x + it.width > 0f }

        // Spawn the next obstacle once enough distance has been covered. The
        // leftover (a negative remainder) is carried over so spacing doesn't
        // drift with frame timing. The next gap starts at the new obstacle's
        // right edge, so a wide cluster doesn't eat into your landing room.
        var untilNext = world.untilNextObstacle - scrolled
        if (untilNext <= 0f) {
            val points = (world.distance / config.unitsPerPoint).toInt()
            val spawned = spawner.spawn(x = viewWidth + untilNext, points = points)
            obstacles = obstacles + spawned
            untilNext += spawned.width + spawner.nextGap(speedAt(world.distance))
        }

        val next = world.copy(
            player = world.player.step(dt, config),
            obstacles = obstacles,
            distance = world.distance + scrolled,
            untilNextObstacle = untilNext,
        )

        val hitbox = next.player.hitbox(config)
        val crashed = next.obstacles.any { hitbox overlaps it.box }
        return if (crashed) GameState.GameOver(next) else GameState.Playing(next)
    }

    // Kotlin note: a private extension taking a lambda, so call sites read as
    // `world.withPlayer { it.jump(config) }`. `it` is the lambda's implicit
    // single parameter, like `$0` in a Swift closure.
    private inline fun World.withPlayer(change: (Player) -> Player): World =
        copy(player = change(player))
}
