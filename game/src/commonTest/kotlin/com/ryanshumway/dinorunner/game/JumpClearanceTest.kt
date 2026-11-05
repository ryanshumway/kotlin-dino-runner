package com.ryanshumway.dinorunner.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the design intent behind the obstacle sizes: a quick tap clears a
 * rock, a tall cactus needs a held jump, and every kind is clearable at its
 * largest size.
 *
 * Rather than hand-picking one jump timing, each check tries every starting
 * distance and asks whether *any* timing works. "Clearable" then means a
 * player with perfect timing gets over it, and "not clearable" means no
 * timing does.
 */
class JumpClearanceTest {
    private val config = GameConfig()
    private val frame = 1f / 60f

    /** A quick tap: about 50 ms between finger down and up. */
    private val tapFrames = 3

    @Test
    fun aTapClearsTheBiggestRock() {
        assertTrue(canClear(biggest(ObstacleKind.Rock), releaseAfterFrames = tapFrames))
    }

    @Test
    fun aTapCannotClearATallCactus() {
        val shortestTall = Obstacle(0f, ObstacleKind.TallCactus.minWidth, ObstacleKind.TallCactus.minHeight)
        assertFalse(canClear(shortestTall, releaseAfterFrames = tapFrames))
    }

    @Test
    fun aHeldJumpClearsTheBiggestOfEveryKind() {
        for (kind in ObstacleKind.entries) {
            assertTrue(canClear(biggest(kind), releaseAfterFrames = null), "couldn't clear $kind")
        }
    }

    private fun biggest(kind: ObstacleKind) = Obstacle(0f, kind.maxWidth, kind.maxHeight, kind)

    /**
     * True if jumping at some distance from [obstacle] gets the player past it.
     * Tested at starting speed, where obstacles are hardest to clear: the
     * player covers the least ground per second of air time.
     */
    private fun canClear(obstacle: Obstacle, releaseAfterFrames: Int?): Boolean {
        val hitboxRight = config.playerX + config.playerWidth - config.hitboxInset
        return (0..300 step 2).any { lead ->
            survives(obstacle.copy(x = hitboxRight + lead), releaseAfterFrames)
        }
    }

    private fun survives(obstacle: Obstacle, releaseAfterFrames: Int?): Boolean {
        val engine = GameEngine(config, Random(seed = 0))
        // No other obstacles will spawn during the check.
        val world = World(obstacles = listOf(obstacle), untilNextObstacle = Float.MAX_VALUE)
        var state = engine.press(GameState.Playing(world))
        repeat(120) { frameIndex ->
            if (frameIndex == releaseAfterFrames) state = engine.release(state)
            state = engine.step(state, frame, viewWidth = 640f)
            if (state is GameState.GameOver) return false
            if (state.world.obstacles.isEmpty()) return true
        }
        return false
    }
}
