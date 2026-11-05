package com.ryanshumway.dinorunner.game

import kotlin.random.Random

/**
 * A cactus-style block standing on the ground.
 *
 * [x] is its left edge in view coordinates (the same space as
 * [GameConfig.playerX]), so it starts past the right edge of the screen and
 * counts down toward the player as the world scrolls.
 */
data class Obstacle(val x: Float, val width: Float, val height: Float)

/**
 * Decides what the next obstacle looks like and how far away it is.
 *
 * The [Random] is passed in rather than created here. The game uses a
 * different sequence every run, and tests pass `Random(seed)` to get the same
 * "random" numbers every time, so results can be checked exactly. This is
 * dependency injection in its simplest form: a constructor parameter.
 */
class ObstacleSpawner(private val config: GameConfig, private val random: Random) {

    /** A new obstacle with its left edge at [x] and a random size. */
    fun spawn(x: Float): Obstacle = Obstacle(
        x = x,
        width = random.between(config.obstacleMinWidth, config.obstacleMaxWidth),
        height = random.between(config.obstacleMinHeight, config.obstacleMaxHeight),
    )

    /**
     * Distance from one obstacle to the next, at the current [speed]. A random
     * number of seconds times speed, so faster runs spread obstacles out in
     * distance but not in reaction time.
     */
    fun nextGap(speed: Float): Float =
        speed * random.between(config.minGapSeconds, config.maxGapSeconds)

    private fun Random.between(min: Float, max: Float): Float = min + nextFloat() * (max - min)
}
