package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.ryanshumway.dinorunner.game.GameConfig
import com.ryanshumway.dinorunner.game.GameState
import com.ryanshumway.dinorunner.game.Obstacle

// Drawing only: this file reads a GameState and paints it. Nothing here
// changes state.
//
// The scene is drawn in world units and then scaled so the view is always
// VIEW_HEIGHT units tall. A taller screen gets a bigger dino, and a wider
// screen sees further ahead.

/** How many world units fit vertically on screen. */
const val VIEW_HEIGHT = 240f
/** World y = 0 (the ground) sits this many units below the top of the view. */
private const val GROUND_LINE = 190f
/** Size of one "pixel" of the blocky dino. 12 cells × 4 = the 48-unit player box. */
private const val CELL = 4f
private const val PEBBLE_SPACING = 26f

/** Every color in the scene, so day and night are just two instances. */
class SceneColors(
    val sky: Color,
    val ink: Color,
    val dino: Color,
    val plates: Color,
    val cactus: Color,
    val moon: Color,
)

private val Day = SceneColors(
    sky = Color(0xFFF4EFE6),
    ink = Color(0xFF3A3530),
    dino = Color(0xFF2F8F7A),
    plates = Color(0xFFEE8434),
    cactus = Color(0xFF557A3E),
    moon = Color(0xFFF4EFE6),
)

private val Night = SceneColors(
    sky = Color(0xFF1F2433),
    ink = Color(0xFFE6E1D6),
    dino = Color(0xFF5FC2A6),
    plates = Color(0xFFF29A4A),
    cactus = Color(0xFF86AE6A),
    moon = Color(0xFFF7F1DC),
)

/**
 * Blends day into night. [night] runs from 0 (day) to 1 (night) and comes
 * from the engine, so the fade is the same on every device.
 */
fun sceneColors(night: Float): SceneColors = when {
    night <= 0f -> Day
    night >= 1f -> Night
    else -> SceneColors(
        sky = lerp(Day.sky, Night.sky, night),
        ink = lerp(Day.ink, Night.ink, night),
        dino = lerp(Day.dino, Night.dino, night),
        plates = lerp(Day.plates, Night.plates, night),
        cactus = lerp(Day.cactus, Night.cactus, night),
        moon = Night.moon,
    )
}

fun DrawScope.drawGame(state: GameState, config: GameConfig, night: Float) {
    val scale = size.height / VIEW_HEIGHT
    val viewWidth = size.width / scale
    val colors = sceneColors(night)

    drawRect(colors.sky)
    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
        if (night > 0f) drawNightSky(viewWidth, colors.moon, alpha = night)
        drawGround(state.world.distance, viewWidth, colors.ink)
        state.world.obstacles.forEach { drawCactus(it, colors.cactus) }
        drawDino(state, config, colors)
    }
}

// Stars sit at fixed fractions of the screen width, so they spread out on
// wider screens instead of bunching at the left.
private val STARS = listOf(
    0.08f to 34f, 0.19f to 72f, 0.31f to 22f, 0.44f to 58f,
    0.57f to 30f, 0.66f to 88f, 0.86f to 64f, 0.94f to 26f,
)
private val MOON = Sprite(".XXX.", "XXX..", "XX...", "XXX..", ".XXX.")

/** Stars and a blocky crescent moon, faded in with [alpha]. They don't scroll. */
private fun DrawScope.drawNightSky(viewWidth: Float, color: Color, alpha: Float) {
    val faded = color.copy(alpha = alpha)
    // Kotlin note: `(fraction, y)` unpacks each pair, like a Swift tuple pattern.
    for ((fraction, y) in STARS) {
        drawRect(faded, topLeft = Offset(fraction * viewWidth, y), size = Size(2f, 2f))
    }
    // Kotlin note: when a function's last parameter is a lambda, it can go
    // outside the parentheses, the same as a Swift trailing closure. Here the
    // lambda gives every cell the same color.
    drawSprite(MOON, left = viewWidth * 0.75f, top = 28f) { faded }
}

/** Screen y (units down from the top) for a world height (units up from the ground). */
private fun screenY(worldY: Float) = GROUND_LINE - worldY

private fun DrawScope.drawGround(distance: Float, viewWidth: Float, ink: Color) {
    drawRect(ink, topLeft = Offset(0f, GROUND_LINE), size = Size(viewWidth, 2f))

    // Pebbles are placed on a fixed grid in world space, then shifted left by
    // the distance run, so they appear to scroll. Each slot's index picks its
    // look, so a pebble keeps its shape as it moves across the screen.
    val firstSlot = (distance / PEBBLE_SPACING).toInt()
    val slotsOnScreen = (viewWidth / PEBBLE_SPACING).toInt() + 2
    for (slot in firstSlot until firstSlot + slotsOnScreen) {
        val variant = (slot * 37 + 11) % 7
        if (variant == 0) continue // leave some gaps so it doesn't look tiled
        val x = slot * PEBBLE_SPACING - distance
        val y = GROUND_LINE + 6f + (variant % 4) * 5f
        drawRect(ink, topLeft = Offset(x, y), size = Size(2f + (variant % 3) * 2f, 2f))
    }
}

/**
 * A blocky cactus filling the obstacle's box: a trunk down the middle, plus
 * two arms at different heights when it's wide enough. The arms reach the
 * edges of the box so what you see matches what you can hit.
 */
private fun DrawScope.drawCactus(obstacle: Obstacle, color: Color) {
    val top = screenY(obstacle.height)
    val h = obstacle.height
    if (obstacle.width < 24f) {
        drawRect(color, Offset(obstacle.x, top), Size(obstacle.width, h))
        return
    }
    val trunk = obstacle.width * 0.5f
    val arm = (obstacle.width - trunk) / 2
    drawRect(color, Offset(obstacle.x + arm, top), Size(trunk, h))
    drawRect(color, Offset(obstacle.x, top + h * 0.3f), Size(arm, h * 0.35f))
    drawRect(color, Offset(obstacle.x + arm + trunk, top + h * 0.15f), Size(arm, h * 0.35f))
}

// The dino (and the moon) are drawn from text grids: X = body, P = back
// plates, E = eye. A
// grid is easier to read and tweak than a list of coordinates. Each sprite
// is parsed once into a list of cells, not on every frame.

private val BODY = Sprite(
    "........XXXX",
    "........XEXX",
    "...P.P..XXXX",
    "..PXPXP.XXX.",
    "X.XXXXXXXXX.",
    "XXXXXXXXXXX.",
    ".XXXXXXXXXX.",
    "..XXXXXXX.X.",
    "...XXXXXX...",
)
private val LEGS_STAND = Sprite("...XX.XX....", "...XX.XX....", "...XXX.XXX..")
private val LEGS_RUN_A = Sprite("...XX.XX....", "...XX.......", "...XXX......")
private val LEGS_RUN_B = Sprite("...XX.XX....", "......XX....", "......XXX...")

private fun DrawScope.drawDino(state: GameState, config: GameConfig, colors: SceneColors) {
    val player = state.world.player
    val left = config.playerX
    val legsTop = screenY(player.y) - LEGS_STAND.rows * CELL
    // After a crash the eye goes dark. A small detail, but it reads instantly.
    val eye = if (state is GameState.GameOver) colors.ink else colors.sky
    val colorFor = { kind: Char ->
        when (kind) {
            'P' -> colors.plates
            'E' -> eye
            else -> colors.dino
        }
    }
    drawSprite(BODY, left, top = legsTop - BODY.rows * CELL, colorFor)
    drawSprite(legsFor(state), left, top = legsTop, colorFor)
}

/** Two-frame run cycle while on the ground. Legs together otherwise. */
private fun legsFor(state: GameState): Sprite {
    if (state !is GameState.Playing || !state.world.player.isGrounded) return LEGS_STAND
    val frame = (state.world.distance / 18f).toInt() % 2
    return if (frame == 0) LEGS_RUN_A else LEGS_RUN_B
}

/** Draws each filled cell of [sprite], asking [colorFor] which color each kind of cell gets. */
private fun DrawScope.drawSprite(sprite: Sprite, left: Float, top: Float, colorFor: (Char) -> Color) {
    for (cell in sprite.cells) {
        drawRect(
            colorFor(cell.kind),
            topLeft = Offset(left + cell.col * CELL, top + cell.row * CELL),
            size = Size(CELL, CELL),
        )
    }
}

/**
 * Kotlin note: `vararg` takes any number of arguments, like `String...` in
 * Swift. `flatMapIndexed` / `mapIndexedNotNull` turn the grid into its filled
 * cells and drop the blanks.
 */
private class Sprite(vararg grid: String) {
    data class Cell(val col: Int, val row: Int, val kind: Char)

    val rows = grid.size
    val cells: List<Cell> = grid.flatMapIndexed { row, line ->
        line.mapIndexedNotNull { col, kind -> if (kind == '.') null else Cell(col, row, kind) }
    }
}
