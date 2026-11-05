package com.ryanshumway.dinorunner.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameEngineTest {
    private val config = GameConfig()
    private val engine = GameEngine(config)

    @Test
    fun firstPressStartsTheRunWithAJump() {
        val state = engine.press(engine.newGame())
        assertIs<GameState.Playing>(state)
        assertTrue(state.world.player.velocityY > 0f)
    }

    @Test
    fun nothingMovesBeforeTheRunStarts() {
        val ready = engine.newGame()
        assertEquals(ready, engine.step(ready, 1f))
    }

    @Test
    fun worldScrollsAtRunSpeed() {
        val state = engine.step(GameState.Playing(World()), 0.05f)
        assertEquals(config.runSpeed * 0.05f, state.world.distance, absoluteTolerance = 0.001f)
    }

    @Test
    fun longFramesAreClampedSoNothingTeleports() {
        val state = engine.step(GameState.Playing(World()), 3f) // e.g. back from background
        assertEquals(config.runSpeed * config.maxFrameSeconds, state.world.distance, absoluteTolerance = 0.001f)
    }

    @Test
    fun scoreCountsDistance() {
        val state = GameState.Playing(World(distance = config.unitsPerPoint * 42.5f))
        assertEquals(42, engine.score(state))
    }

    @Test
    fun gameOverIsFrozen() {
        val over = GameState.GameOver(World(distance = 500f))
        assertEquals(over, engine.step(over, 0.016f))
    }

    @Test
    fun pressAfterGameOverStartsAFreshRun() {
        val restarted = engine.press(GameState.GameOver(World(distance = 500f)))
        assertIs<GameState.Playing>(restarted)
        assertEquals(0f, restarted.world.distance)
    }
}
