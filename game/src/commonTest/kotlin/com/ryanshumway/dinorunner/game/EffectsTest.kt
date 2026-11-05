package com.ryanshumway.dinorunner.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectsTest {
    private val config = GameConfig()
    private val engine = GameEngine(config, Random(seed = 0))
    private val frame = 1f / 60f
    private val calm = World(untilNextObstacle = Float.MAX_VALUE)

    @Test
    fun jumpingKicksUpDust() {
        val before = GameState.Playing(calm)
        val after = engine.press(before)
        assertEquals(1, Effects().next(before, after, 0f, config).puffs.size)
    }

    @Test
    fun landingKicksUpDust() {
        val falling = GameState.Playing(calm.copy(player = Player(y = 1f, velocityY = -300f)))
        val landed = engine.step(falling, frame, viewWidth = 640f)
        assertEquals(1, Effects().next(falling, landed, frame, config).puffs.size)
    }

    @Test
    fun dustFadesAway() {
        var effects = Effects(puffs = listOf(Puff(worldX = 100f)))
        val state = GameState.Playing(calm)
        repeat(30) { effects = effects.next(state, state, frame, config) } // 0.5 s
        assertTrue(effects.puffs.isEmpty())
    }

    @Test
    fun crashingShakesTheScreenBriefly() {
        val playing = GameState.Playing(calm)
        val crashed = GameState.GameOver(calm)
        var effects = Effects().next(playing, crashed, frame, config)
        assertEquals(config.shakeSeconds, effects.shake)
        repeat(30) { effects = effects.next(crashed, crashed, frame, config) }
        assertEquals(0f, effects.shake)
    }

    @Test
    fun scoreBlinksAtEachMilestone() {
        val milestone = config.flashEveryPoints * config.unitsPerPoint
        val before = GameState.Playing(calm.copy(distance = milestone - 1f))
        val after = GameState.Playing(calm.copy(distance = milestone + 1f))
        assertEquals(config.flashSeconds, Effects().next(before, after, frame, config).flash)
    }

    @Test
    fun noBlinkBetweenMilestones() {
        val before = GameState.Playing(calm.copy(distance = 10f))
        val after = GameState.Playing(calm.copy(distance = 20f))
        assertEquals(0f, Effects().next(before, after, frame, config).flash)
    }

    @Test
    fun restartingClearsOldEffects() {
        val over = GameState.GameOver(calm.copy(distance = 5_000f), secondsSinceCrash = 1f)
        val old = Effects(puffs = listOf(Puff(worldX = 4_900f)), shake = 0.1f, flash = 0.5f)
        val restarted = engine.press(over)
        val effects = old.next(over, restarted, 0f, config)
        assertEquals(0f, effects.shake)
        assertEquals(0f, effects.flash)
        // Only the new jump's puff, not the old one.
        assertEquals(1, effects.puffs.size)
        assertTrue(effects.puffs.single().worldX < 1_000f)
    }
}
