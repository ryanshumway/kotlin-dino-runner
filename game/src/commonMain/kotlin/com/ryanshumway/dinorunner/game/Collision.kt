package com.ryanshumway.dinorunner.game

/**
 * An axis-aligned rectangle in world units, with y pointing up from the ground.
 */
data class Box(val left: Float, val bottom: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val top: Float get() = bottom + height

    /**
     * True when the two boxes share any area. Edges that only touch don't
     * count, so landing exactly on the ground line or brushing the edge of an
     * obstacle isn't a hit.
     *
     * Kotlin note: `infix` lets this be called as `a overlaps b`, which reads
     * like a sentence. It's still an ordinary method call.
     */
    infix fun overlaps(other: Box): Boolean =
        left < other.right && other.left < right &&
            bottom < other.top && other.bottom < top
}

/** The player's collision box: the drawn dino, shrunk by the hitbox inset. */
fun Player.hitbox(config: GameConfig): Box = Box(
    left = config.playerX + config.hitboxInset,
    bottom = y,
    width = config.playerWidth - 2 * config.hitboxInset,
    height = config.playerHeight - config.hitboxInset,
)

/** An obstacle's collision box. Obstacles always sit on the ground. */
val Obstacle.box: Box get() = Box(left = x, bottom = 0f, width = width, height = height)
