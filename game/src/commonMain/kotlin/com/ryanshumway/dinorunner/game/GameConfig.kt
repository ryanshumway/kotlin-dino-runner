package com.ryanshumway.dinorunner.game

/**
 * Every tunable number in one place, so tuning the game feel never means
 * hunting through logic.
 *
 * Units: distances are "world units" (the renderer maps 1 unit to roughly
 * 1 dp), time is seconds, and y is measured upward from the ground line.
 *
 * Kotlin note: a `data class` with default arguments acts like a Swift struct
 * with default member values. `GameConfig()` gives the defaults, and a test
 * can write `GameConfig(gravity = 1000f)` to change just one.
 */
data class GameConfig(
    /** Downward acceleration while airborne, in units/s². */
    val gravity: Float = 2_600f,
    /** Upward speed at the moment of a jump. Peak height is v² / 2g, about 129 units. */
    val jumpVelocity: Float = 820f,
    /** On release, upward speed is capped to this, so a quick tap gives a short hop. */
    val jumpCutVelocity: Float = 320f,
    /** Horizontal scroll speed at the start of a run, in units/s. */
    val runSpeed: Float = 360f,
    /** Distance that earns one point. 360 / 36 is 10 points per second at the start. */
    val unitsPerPoint: Float = 36f,
    /**
     * Longest frame we will simulate in one step. If the app stalls (a GC
     * pause, or coming back from the background), a huge delta would teleport
     * the player, so we clamp it.
     */
    val maxFrameSeconds: Float = 1f / 20f,
)
