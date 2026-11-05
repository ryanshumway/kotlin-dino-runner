package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A small picture drawn from a text grid, one character per square. `.` is
 * empty, and every other character names a color role (for example `X` for
 * body, `S` for shade). A grid is easier to read and tweak than a list of
 * coordinates, and it's parsed once into cells, not on every frame.
 *
 * Kotlin note: `vararg` takes any number of arguments, like `String...` in
 * Swift. `flatMapIndexed` / `mapIndexedNotNull` turn the grid into its filled
 * cells and drop the blanks.
 */
class Sprite(vararg grid: String) {
    data class Cell(val col: Int, val row: Int, val kind: Char)

    val rows = grid.size
    val cells: List<Cell> = grid.flatMapIndexed { row, line ->
        line.mapIndexedNotNull { col, kind -> if (kind == '.') null else Cell(col, row, kind) }
    }
}

/**
 * Draws each filled cell of [sprite] as a [cell]-sized square, asking
 * [colorFor] which color each kind of cell gets.
 */
fun DrawScope.drawSprite(
    sprite: Sprite,
    left: Float,
    top: Float,
    cell: Float,
    alpha: Float = 1f,
    colorFor: (Char) -> Color,
) {
    for (c in sprite.cells) {
        drawRect(
            colorFor(c.kind),
            topLeft = Offset(left + c.col * cell, top + c.row * cell),
            // A hair of overlap hides seams between squares when scaled.
            size = Size(cell + 0.1f, cell + 0.1f),
            alpha = alpha,
        )
    }
}
