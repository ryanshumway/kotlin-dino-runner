package com.ryanshumway.dinorunner.game

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhysicsTest {
    private val config = GameConfig()

    @Test
    fun jumpFromGroundLaunchesUpward() {
        val player = Player().jump(config)
        assertEquals(config.jumpVelocity, player.velocityY)
    }

    @Test
    fun cannotJumpAgainInTheAir() {
        val airborne = Player().jump(config).step(0.1f, config)
        assertEquals(airborne, airborne.jump(config))
    }

    @Test
    fun fullJumpPeaksAtTheExpectedHeight() {
        // Peak of a projectile is v² / 2g.
        val expected = config.jumpVelocity * config.jumpVelocity / (2 * config.gravity)
        assertEquals(expected, peakHeight(fps = 60), absoluteTolerance = 1f)
    }

    @Test
    fun jumpArcIsTheSameAtAnyFrameRate() {
        val at30 = peakHeight(fps = 30)
        val at120 = peakHeight(fps = 120)
        assertTrue(abs(at30 - at120) < 1f, "30fps peak $at30 vs 120fps peak $at120")
    }

    @Test
    fun playerLandsAndIsGroundedAgain() {
        var player = Player().jump(config)
        repeat(120) { player = player.step(1f / 60f, config) } // two seconds is plenty
        assertEquals(Player(), player)
        assertTrue(player.isGrounded)
    }

    @Test
    fun releasingEarlyGivesAShorterHop() {
        val full = peakHeight(fps = 60)
        val tap = peakHeight(fps = 60, releaseAfterFrames = 2)
        assertTrue(tap < full * 0.5f, "tap peak $tap should be well under full peak $full")
    }

    @Test
    fun releasingWhileFallingChangesNothing() {
        val falling = Player(y = 50f, velocityY = -200f)
        assertEquals(falling, falling.releaseJump(config))
    }

    /** Simulates one jump at a fixed frame rate and returns the highest y reached. */
    private fun peakHeight(fps: Int, releaseAfterFrames: Int? = null): Float {
        val dt = 1f / fps
        var player = Player().jump(config)
        var peak = 0f
        var frame = 0
        while (!player.isGrounded) {
            if (frame == releaseAfterFrames) player = player.releaseJump(config)
            player = player.step(dt, config)
            peak = maxOf(peak, player.y)
            frame++
        }
        return peak
    }
}
