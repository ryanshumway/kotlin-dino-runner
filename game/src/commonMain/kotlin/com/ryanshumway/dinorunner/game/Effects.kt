package com.ryanshumway.dinorunner.game

import kotlin.math.max

/**
 * A puff of dust kicked up by a jump or a landing.
 *
 * [worldX] is measured along the whole run (distance + screen x), so the puff
 * stays on the spot of ground where it happened and scrolls away with it.
 */
data class Puff(val worldX: Float, val age: Float = 0f)

/**
 * Short visual reactions to game events: dust, a crash shake, and a blinking
 * score at milestones. None of this changes the rules, so it lives apart from
 * [GameState]. The *timing* is still decided here, in plain Kotlin, so it can
 * be unit tested like everything else.
 */
data class Effects(
    val puffs: List<Puff> = emptyList(),
    /** Seconds of screen shake left. */
    val shake: Float = 0f,
    /** Seconds of score blinking left. */
    val flash: Float = 0f,
)

/**
 * The effects after the game went from [before] to [after] over [dt] seconds.
 * Called after every press and every frame, it compares the two states to
 * spot events: leaving the ground, landing, crashing, passing a milestone.
 */
fun Effects.next(before: GameState, after: GameState, dt: Float, config: GameConfig): Effects {
    // A fresh run (distance went back down) starts with a clean slate.
    val restarted = after.world.distance < before.world.distance
    val base = if (restarted) Effects() else this

    var puffs = base.puffs
        .map { it.copy(age = it.age + dt) }
        .filter { it.age < config.puffSeconds }
    var shake = max(0f, base.shake - dt)
    var flash = max(0f, base.flash - dt)

    val wasGrounded = before.world.player.isGrounded || restarted
    val isGrounded = after.world.player.isGrounded
    if (after is GameState.Playing && wasGrounded != isGrounded) {
        // Took off or touched down: dust at the dino's feet.
        val feet = after.world.distance + config.playerX + config.playerWidth * 0.35f
        puffs = puffs + Puff(worldX = feet)
    }

    if (before !is GameState.GameOver && after is GameState.GameOver) shake = config.shakeSeconds

    val milestone = config.flashEveryPoints * config.unitsPerPoint
    val passedMilestone = (after.world.distance / milestone).toInt() > (before.world.distance / milestone).toInt()
    if (after is GameState.Playing && !restarted && passedMilestone) flash = config.flashSeconds

    return Effects(puffs, shake, flash)
}
