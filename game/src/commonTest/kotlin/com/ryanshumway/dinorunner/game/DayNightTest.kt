package com.ryanshumway.dinorunner.game

import kotlin.test.Test
import kotlin.test.assertEquals

class DayNightTest {
    private val config = GameConfig()
    private val engine = GameEngine(config)

    /** A state scrolled far enough to have scored [points]. */
    private fun atPoints(points: Float) = GameState.Playing(World(distance = points * config.unitsPerPoint))

    @Test
    fun runStartsInDaylight() {
        assertEquals(0f, engine.nightAmount(atPoints(0f)))
        assertEquals(0f, engine.nightAmount(atPoints(499f)))
    }

    @Test
    fun skyFadesToNightAtTheFirstMilestone() {
        val halfway = config.dayNightEveryPoints + config.dayNightFadePoints / 2
        assertEquals(0.5f, engine.nightAmount(atPoints(halfway)), absoluteTolerance = 0.01f)
    }

    @Test
    fun fullNightOnceTheFadeIsDone() {
        val afterFade = config.dayNightEveryPoints + config.dayNightFadePoints + 1f
        assertEquals(1f, engine.nightAmount(atPoints(afterFade)))
    }

    @Test
    fun dayReturnsAtTheNextMilestone() {
        val nextDay = 2 * config.dayNightEveryPoints + config.dayNightFadePoints + 1f
        assertEquals(0f, engine.nightAmount(atPoints(nextDay)))
        // Halfway through the dawn fade.
        val dawn = 2 * config.dayNightEveryPoints + config.dayNightFadePoints / 2
        assertEquals(0.5f, engine.nightAmount(atPoints(dawn)), absoluteTolerance = 0.01f)
    }

    @Test
    fun skyStaysPutAfterACrash() {
        val night = config.dayNightEveryPoints + 100f
        val over = GameState.GameOver(atPoints(night).world)
        assertEquals(1f, engine.nightAmount(over))
    }
}
