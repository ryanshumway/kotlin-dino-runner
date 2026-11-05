package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.ryanshumway.dinorunner.game.GameState
import com.ryanshumway.dinorunner.game.Player

// Drawing only: this file reads a GameState and paints it. Nothing here
// changes state.
//
// The scene is drawn in world units and then scaled so the view is always
// VIEW_HEIGHT units tall. A taller screen gets a bigger dino, and a wider
// screen sees further ahead.

private const val VIEW_HEIGHT = 240f
/** World y = 0 (the ground) sits this many units below the top of the view. */
private const val GROUND_LINE = 190f
/** The player's fixed left edge. The world scrolls past it. */
private const val PLAYER_X = 48f
/** Size of one "pixel" of the blocky dino. */
private const val CELL = 4f
private const val PEBBLE_SPACING = 26f

object Palette {
    val sky = Color(0xFFF4EFE6)
    val ink = Color(0xFF3A3530)
    val dino = Color(0xFF2F8F7A)
    val plates = Color(0xFFEE8434)
}

fun DrawScope.drawGame(state: GameState) {
    val scale = size.height / VIEW_HEIGHT
    val viewWidth = size.width / scale

    drawRect(Palette.sky)
    withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
        drawGround(state.world.distance, viewWidth)
        drawDino(state.world.player, legsFor(state))
    }
}

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

/** Two-frame run cycle while on the ground. Legs together otherwise. */
private fun legsFor(state: GameState): Sprite {
    val player = state.world.player
    if (state !is GameState.Playing || !player.isGrounded) return LEGS_STAND
    val frame = (state.world.distance / 18f).toInt() % 2
    return if (frame == 0) LEGS_RUN_A else LEGS_RUN_B
}

private fun DrawScope.drawDino(player: Player, legs: Sprite) {
    val feetY = GROUND_LINE - player.y
    val legsTop = feetY - legs.rows * CELL
    drawSprite(BODY, left = PLAYER_X, top = legsTop - BODY.rows * CELL)
    drawSprite(legs, left = PLAYER_X, top = legsTop)
}

private fun DrawScope.drawSprite(sprite: Sprite, left: Float, top: Float) {
    for (cell in sprite.cells) {
        val color = when (cell.kind) {
            'P' -> Palette.plates
            'E' -> Palette.sky
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
