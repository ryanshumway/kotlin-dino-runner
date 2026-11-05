package com.ryanshumway.dinorunner.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameEngineTest {
    private val config = GameConfig()
    private val engine = GameEngine(config, Random(seed = 0))
    private val viewWidth = 640f
    private val frame = 1f / 60f

    /** A world with one obstacle and no more spawning, for predictable scenarios. */
    private fun worldWith(vararg obstacles: Obstacle) =
        World(obstacles = obstacles.toList(), untilNextObstacle = Float.MAX_VALUE)

    // Starting and scrolling

    @Test
    fun firstPressStartsTheRunWithAJump() {
        val state = engine.press(engine.newGame())
        assertIs<GameState.Playing>(state)
        assertTrue(state.world.player.velocityY > 0f)
    }

    @Test
    fun nothingMovesBeforeTheRunStarts() {
        val ready = engine.newGame()
        assertEquals(ready, engine.step(ready, 1f, viewWidth))
    }

    @Test
    fun worldScrollsAtRunSpeed() {
        val state = engine.step(GameState.Playing(World()), 0.05f, viewWidth)
        assertEquals(config.runSpeed * 0.05f, state.world.distance, absoluteTolerance = 0.001f)
    }

    @Test
    fun longFramesAreClampedSoNothingTeleports() {
        val state = engine.step(GameState.Playing(World()), 3f, viewWidth) // e.g. back from background
        assertEquals(config.runSpeed * config.maxFrameSeconds, state.world.distance, absoluteTolerance = 0.001f)
    }

    @Test
    fun scoreCountsDistance() {
        val state = GameState.Playing(World(distance = config.unitsPerPoint * 42.5f))
        assertEquals(42, engine.score(state))
    }

    // Speed ramp

    @Test
    fun speedStartsAtRunSpeed() {
        assertEquals(config.runSpeed, engine.speedAt(distance = 0f))
    }

    @Test
    fun speedRisesWithScore() {
        val at100 = engine.speedAt(distance = 100 * config.unitsPerPoint)
        assertEquals(config.runSpeed + 100 * config.speedGainPerPoint, at100, absoluteTolerance = 0.01f)
    }

    @Test
    fun speedIsCapped() {
        assertEquals(config.maxSpeed, engine.speedAt(distance = 1_000_000f))
    }

    // Obstacles

    @Test
    fun firstObstacleSpawnsJustPastTheRightEdge() {
        val state = engine.step(GameState.Playing(World()), frame, viewWidth)
        val obstacle = state.world.obstacles.single()
        assertTrue(obstacle.x <= viewWidth && obstacle.x > viewWidth - 10f, "spawned at ${obstacle.x}")
    }

    @Test
    fun obstaclesScrollTowardThePlayer() {
        val start = GameState.Playing(worldWith(Obstacle(x = 500f, width = 20f, height = 30f)))
        val state = engine.step(start, 0.05f, viewWidth)
        assertEquals(500f - config.runSpeed * 0.05f, state.world.obstacles.single().x, absoluteTolerance = 0.001f)
    }

    @Test
    fun obstaclesOffTheLeftEdgeAreRemoved() {
        val start = GameState.Playing(worldWith(Obstacle(x = -19f, width = 20f, height = 30f)))
        val state = engine.step(start, frame, viewWidth)
        assertTrue(state.world.obstacles.isEmpty())
    }

    @Test
    fun obstaclesKeepComingDuringALongRun() {
        var state: GameState = GameState.Playing(World())
        var spawned = 0
        repeat(600) { // ten seconds, with the player floating above everything
            val before = state.world.obstacles.size
            state = engine.step(state, frame, viewWidth)
            if (state.world.obstacles.size > before) spawned++
            state = GameState.Playing(state.world.copy(player = Player(y = 1_000f)))
        }
        assertTrue(spawned >= 5, "only $spawned obstacles in ten seconds")
    }

    // Collision

    @Test
    fun runningIntoAnObstacleEndsTheRun() {
        var state: GameState = GameState.Playing(worldWith(Obstacle(x = 140f, width = 30f, height = 40f)))
        repeat(60) { state = engine.step(state, frame, viewWidth) }
        assertIs<GameState.GameOver>(state)
    }

    @Test
    fun jumpingClearsAnObstacle() {
        var state = engine.press(GameState.Playing(worldWith(Obstacle(x = 140f, width = 30f, height = 40f))))
        repeat(30) { state = engine.step(state, frame, viewWidth) }
        assertIs<GameState.Playing>(state)
        assertTrue(state.world.obstacles.isEmpty(), "obstacle should be behind the player by now")
    }

    // Game over and restart

    @Test
    fun gameOverFreezesTheWorld() {
        val over = GameState.GameOver(World(distance = 500f))
        assertEquals(over.world, engine.step(over, frame, viewWidth).world)
    }

    @Test
    fun tapsRightAfterACrashAreIgnored() {
        val justCrashed = GameState.GameOver(World(distance = 500f))
        assertEquals(justCrashed, engine.press(justCrashed))
    }

    @Test
    fun pressAfterTheRestartDelayStartsAFreshRun() {
        var state: GameState = GameState.GameOver(World(distance = 500f))
        repeat(60) { state = engine.step(state, frame, viewWidth) } // one second
        val restarted = engine.press(state)
        assertIs<GameState.Playing>(restarted)
        assertEquals(World().copy(player = restarted.world.player), restarted.world)
    }
}
