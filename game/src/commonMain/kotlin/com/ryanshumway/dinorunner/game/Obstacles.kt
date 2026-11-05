package com.ryanshumway.dinorunner.game

import kotlin.random.Random

/**
 * The kinds of obstacle, each with its own size range, how often it shows up,
 * and the score it unlocks at.
 *
 * Kotlin note: enum entries can carry constructor values, like a Swift enum
 * with raw values, but with as many typed fields as you like.
 * `ObstacleKind.entries` lists them all.
 */
enum class ObstacleKind(
    val minWidth: Float,
    val maxWidth: Float,
    val minHeight: Float,
    val maxHeight: Float,
    /** Relative chance of being picked, once unlocked. */
    val weight: Int,
    /** Score at which this kind starts appearing, so a run eases you in. */
    val unlockAtPoints: Int,
) {
    /** Low and wide. A quick tap clears it. */
    Rock(22f, 34f, 14f, 22f, weight = 3, unlockAtPoints = 0),

    /** The standard obstacle. */
    Cactus(16f, 28f, 36f, 56f, weight = 4, unlockAtPoints = 0),

    /** Taller than a tap can reach: it needs a held jump. */
    TallCactus(20f, 28f, 70f, 84f, weight = 2, unlockAtPoints = 150),

    /** Two or three cacti side by side. Needs a long, held jump. */
    Cluster(48f, 72f, 36f, 50f, weight = 2, unlockAtPoints = 250),
}

/**
 * One obstacle standing on the ground.
 *
 * [x] is its left edge in view coordinates (the same space as
 * [GameConfig.playerX]), so it starts past the right edge of the screen and
 * counts down toward the player as the world scrolls.
 */
data class Obstacle(
    val x: Float,
    val width: Float,
    val height: Float,
    val kind: ObstacleKind = ObstacleKind.Cactus,
)

/**
 * Decides what the next obstacle looks like and how far away it is.
 *
 * The [Random] is passed in rather than created here. The game uses a
 * different sequence every run, and tests pass `Random(seed)` to get the same
 * "random" numbers every time, so results can be checked exactly. This is
 * dependency injection in its simplest form: a constructor parameter.
 */
class ObstacleSpawner(private val config: GameConfig, private val random: Random) {

    /** A new obstacle with its left edge at [x], of a kind unlocked at [points]. */
    fun spawn(x: Float, points: Int): Obstacle {
        val kind = pickKind(points)
        return Obstacle(
            x = x,
            width = random.between(kind.minWidth, kind.maxWidth),
            height = random.between(kind.minHeight, kind.maxHeight),
            kind = kind,
        )
    }

    /**
     * Clear distance between one obstacle's right edge and the next one's
     * left edge, at the current [speed]. A random number of seconds times
     * speed, so faster runs spread obstacles out in distance but not in
     * reaction time.
     */
    fun nextGap(speed: Float): Float =
        speed * random.between(config.minGapSeconds, config.maxGapSeconds)

    /** A weighted random pick: each unlocked kind gets `weight` tickets in the draw. */
    private fun pickKind(points: Int): ObstacleKind {
        val unlocked = ObstacleKind.entries.filter { points >= it.unlockAtPoints }
        var ticket = random.nextInt(unlocked.sumOf { it.weight })
        return unlocked.first { kind ->
            ticket -= kind.weight
            ticket < 0
        }
    }

    private fun Random.between(min: Float, max: Float): Float = min + nextFloat() * (max - min)
}
