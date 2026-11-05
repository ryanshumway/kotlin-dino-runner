package com.ryanshumway.dinorunner.game

/**
 * The runner's vertical state. It never moves horizontally: the world scrolls
 * past it instead.
 *
 * Kotlin note: properties are `val` (immutable), so a change makes a new
 * value via `copy(...)`, like mutating a Swift struct `var`. Every function
 * below returns a new Player rather than changing one in place, which keeps
 * them trivial to unit test.
 */
data class Player(
    /** Height of the player's feet above the ground line. 0 means standing. */
    val y: Float = 0f,
    /** Vertical speed in units/s. Positive is upward. */
    val velocityY: Float = 0f,
) {
    /** On the ground and not already launching upward. */
    val isGrounded: Boolean get() = y <= 0f && velocityY <= 0f
}

// Kotlin note: these are extension functions, the equivalent of
// `extension Player { func jump(...) }` in Swift. They read like methods but
// live outside the class, next to the rules that use them.

/** Start a jump. Ignored in the air, so there is no double jump. */
fun Player.jump(config: GameConfig): Player =
    if (isGrounded) copy(velocityY = config.jumpVelocity) else this

/**
 * Called when the finger lifts. Cutting upward speed early is what makes
 * "hold for a higher jump" work: a tap peaks low, a long press rides the full
 * jump. Falling is never affected.
 */
fun Player.releaseJump(config: GameConfig): Player =
    if (velocityY > config.jumpCutVelocity) copy(velocityY = config.jumpCutVelocity) else this

/**
 * Advance the player by [dt] seconds.
 *
 * Uses the exact constant-acceleration formula (y += v·t − ½·g·t²) instead of
 * the simpler y += v·t. With constant gravity that is exact for any step
 * size, so the jump arc is the same at 30, 60 or 120 fps. Landing snaps to
 * the ground and clears velocity.
 */
fun Player.step(dt: Float, config: GameConfig): Player {
    if (isGrounded) return this
    val nextY = y + velocityY * dt - 0.5f * config.gravity * dt * dt
    val nextVelocity = velocityY - config.gravity * dt
    return if (nextY <= 0f) Player() else Player(y = nextY, velocityY = nextVelocity)
}
