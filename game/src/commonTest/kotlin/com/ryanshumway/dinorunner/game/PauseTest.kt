package com.ryanshumway.dinorunner.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PauseTest {
    private val config = GameConfig()
    private val engine = GameEngine(config, Random(seed = 0))
    private val frame = 1f / 60f
    private val midJump = World(player = Player(y = 60f, velocityY = 300f), distance = 1_000f, untilNextObstacle = 500f)

    @Test
    fun pausingALiveRunFreezesIt() {
        val paused = engine.pause(GameState.Playing(midJump))
        assertIs<GameState.Paused>(paused)
        assertEquals(paused, engine.step(paused, frame, viewWidth = 640f))
    }

    @Test
    fun pausingOtherPhasesDoesNothing() {
        val ready = engine.newGame()
        val over = GameState.GameOver(midJump)
        assertEquals(ready, engine.pause(ready))
        assertEquals(over, engine.pause(over))
    }

    @Test
    fun tapStartsTheCountdownAndMoreTapsDontSkipIt() {
        val counting = engine.press(GameState.Paused(midJump))
        assertEquals(GameState.Paused(midJump, resumeIn = config.resumeCountdownSeconds), counting)
        assertEquals(counting, engine.press(counting))
    }

    @Test
    fun countdownTakesTheFullTime() {
        var state: GameState = engine.press(GameState.Paused(midJump))
        repeat(170) { state = engine.step(state, frame, viewWidth = 640f) } // 2.83 of 3 seconds
        assertIs<GameState.Paused>(state)
    }

    @Test
    fun runResumesExactlyWhereItLeftOff() {
        var state: GameState = engine.press(GameState.Paused(midJump))
        while (state is GameState.Paused) state = engine.step(state, frame, viewWidth = 640f)
        // Same position, same mid-air velocity, same obstacle timing: nothing
        // moved during the pause or the countdown.
        assertEquals(GameState.Playing(midJump), state)
    }
}
