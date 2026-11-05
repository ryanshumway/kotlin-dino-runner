package com.ryanshumway.dinorunner.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObstacleSpawnerTest {
    private val config = GameConfig()

    @Test
    fun obstacleSizesStayInRange() {
        val spawner = ObstacleSpawner(config, Random(seed = 42))
        repeat(1_000) {
            val obstacle = spawner.spawn(x = 0f)
            assertTrue(obstacle.width in config.obstacleMinWidth..config.obstacleMaxWidth)
            assertTrue(obstacle.height in config.obstacleMinHeight..config.obstacleMaxHeight)
        }
    }

    @Test
    fun sizesActuallyVary() {
        val spawner = ObstacleSpawner(config, Random(seed = 42))
        val heights = List(50) { spawner.spawn(x = 0f).height }.toSet()
        assertTrue(heights.size > 40, "expected varied heights, got ${heights.size} distinct")
    }

    @Test
    fun gapsStayInRangeAtAnySpeed() {
        val spawner = ObstacleSpawner(config, Random(seed = 7))
        for (speed in listOf(config.runSpeed, 600f, config.maxSpeed)) {
            repeat(200) {
                val gap = spawner.nextGap(speed)
                assertTrue(gap in speed * config.minGapSeconds..speed * config.maxGapSeconds)
            }
        }
    }

    @Test
    fun gapsScaleWithSpeedSoReactionTimeIsTheSame() {
        // Same seed means the same random draw, so only the speed differs.
        val slow = ObstacleSpawner(config, Random(seed = 1)).nextGap(speed = 400f)
        val fast = ObstacleSpawner(config, Random(seed = 1)).nextGap(speed = 800f)
        assertEquals(slow * 2, fast, absoluteTolerance = 0.01f)
    }

    @Test
    fun sameSeedGivesTheSameObstacles() {
        val a = ObstacleSpawner(config, Random(seed = 99))
        val b = ObstacleSpawner(config, Random(seed = 99))
        assertEquals(List(10) { a.spawn(x = 0f) }, List(10) { b.spawn(x = 0f) })
    }

    @Test
    fun shortestGapLeavesRoomToLand() {
        // A full jump lasts 2v / g. If the shortest gap were shorter, two
        // obstacles could be impossible to clear back to back.
        val airTime = 2 * config.jumpVelocity / config.gravity
        assertTrue(config.minGapSeconds > airTime, "min gap ${config.minGapSeconds}s vs air time ${airTime}s")
    }
}
