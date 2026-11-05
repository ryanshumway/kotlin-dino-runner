package com.ryanshumway.dinorunner.game

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionTest {
    private val config = GameConfig()
    private val box = Box(left = 0f, bottom = 0f, width = 10f, height = 10f)

    @Test
    fun overlappingBoxesCollide() {
        assertTrue(box overlaps Box(left = 5f, bottom = 5f, width = 10f, height = 10f))
    }

    @Test
    fun boxInsideAnotherCollides() {
        assertTrue(box overlaps Box(left = 2f, bottom = 2f, width = 2f, height = 2f))
    }

    @Test
    fun boxesSideBySideDoNotCollide() {
        assertFalse(box overlaps Box(left = 20f, bottom = 0f, width = 10f, height = 10f))
    }

    @Test
    fun boxAboveDoesNotCollide() {
        // A player clearing the top of an obstacle.
        assertFalse(box overlaps Box(left = 0f, bottom = 15f, width = 10f, height = 10f))
    }

    @Test
    fun touchingEdgesDoNotCollide() {
        assertFalse(box overlaps Box(left = 10f, bottom = 0f, width = 10f, height = 10f))
        assertFalse(box overlaps Box(left = 0f, bottom = 10f, width = 10f, height = 10f))
    }

    @Test
    fun hitboxForgivesAGrazeAtTheDinosEdge() {
        // This obstacle overlaps the drawn dino by a few units, but not its
        // shrunken hitbox, so it isn't a crash.
        val graze = Obstacle(x = config.playerX + config.playerWidth - 3f, width = 20f, height = 30f)
        assertFalse(Player().hitbox(config) overlaps graze.box)
    }

    @Test
    fun obstacleUnderTheDinoIsAHit() {
        val obstacle = Obstacle(x = config.playerX + 10f, width = 20f, height = 30f)
        assertTrue(Player().hitbox(config) overlaps obstacle.box)
    }
}
