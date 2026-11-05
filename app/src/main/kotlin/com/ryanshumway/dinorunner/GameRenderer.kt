package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
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

object Palette {
    val sky = Color(0xFFF4EFE6)
    val ink = Color(0xFF3A3530)
    val dino = Color(0xFF2F8F7A)
    val plates = Color(0xFFEE8434)
    val cactus = Color(0xFF557A3E)
}

fun DrawScope.drawGame(state: GameState, config: GameConfig) {
    val scale = size.height / VIEW_HEIGHT
    val viewWidth = size.width / scale

    drawRect(Palette.sky)
    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
        drawGround(state.world.distance, viewWidth)
        state.world.obstacles.forEach { drawCactus(it) }
        drawDino(state, config)
    }
}

/** Screen y (units down from the top) for a world height (units up from the ground). */
private fun screenY(worldY: Float) = GROUND_LINE - worldY

private fun DrawScope.drawGround(distance: Float, viewWidth: Float) {
    drawRect(Palette.ink, topLeft = Offset(0f, GROUND_LINE), size = Size(viewWidth, 2f))

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
        drawRect(Palette.ink, topLeft = Offset(x, y), size = Size(2f + (variant % 3) * 2f, 2f))
    }
}

/**
 * A blocky cactus filling the obstacle's box: a trunk down the middle, plus
 * two arms at different heights when it's wide enough. The arms reach the
 * edges of the box so what you see matches what you can hit.
 */
private fun DrawScope.drawCactus(obstacle: Obstacle) {
    val top = screenY(obstacle.height)
    val h = obstacle.height
    if (obstacle.width < 24f) {
        drawRect(Palette.cactus, Offset(obstacle.x, top), Size(obstacle.width, h))
        return
    }
    val trunk = obstacle.width * 0.5f
    val arm = (obstacle.width - trunk) / 2
    drawRect(Palette.cactus, Offset(obstacle.x + arm, top), Size(trunk, h))
    drawRect(Palette.cactus, Offset(obstacle.x, top + h * 0.3f), Size(arm, h * 0.35f))
    drawRect(Palette.cactus, Offset(obstacle.x + arm + trunk, top + h * 0.15f), Size(arm, h * 0.35f))
}

// The dino is drawn from text grids: X = body, P = back plates, E = eye. A
// grid is easier to read and tweak than a list of coordinates. Each sprite is
// parsed once into a list of cells, not on every frame.

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

private fun DrawScope.drawDino(state: GameState, config: GameConfig) {
    val player = state.world.player
    val left = config.playerX
    val legsTop = screenY(player.y) - LEGS_STAND.rows * CELL
    // After a crash the eye goes dark. A small detail, but it reads instantly.
    val eye = if (state is GameState.GameOver) Palette.ink else Palette.sky
    drawSprite(BODY, left, top = legsTop - BODY.rows * CELL, eye)
    drawSprite(legsFor(state), left, top = legsTop, eye)
}

/** Two-frame run cycle while on the ground. Legs together otherwise. */
private fun legsFor(state: GameState): Sprite {
    if (state !is GameState.Playing || !state.world.player.isGrounded) return LEGS_STAND
    val frame = (state.world.distance / 18f).toInt() % 2
    return if (frame == 0) LEGS_RUN_A else LEGS_RUN_B
}

private fun DrawScope.drawSprite(sprite: Sprite, left: Float, top: Float, eye: Color) {
    for (cell in sprite.cells) {
        val color = when (cell.kind) {
            'P' -> Palette.plates
            'E' -> eye
            else -> Palette.dino
        }
        drawRect(
            color,
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
