package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.ryanshumway.dinorunner.game.Effects
import com.ryanshumway.dinorunner.game.GameConfig
import com.ryanshumway.dinorunner.game.GameState
import com.ryanshumway.dinorunner.game.Obstacle
import com.ryanshumway.dinorunner.game.ObstacleKind
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// Drawing only: this file reads a GameState and paints it. Nothing here
// changes state.
//
// The scene is drawn in world units and then scaled so the view is always
// VIEW_HEIGHT units tall. A taller screen gets a bigger dino, and a wider
// screen sees further ahead.

/** How many world units fit vertically on screen. */
const val VIEW_HEIGHT = 240f
/** World y = 0 (the ground) sits this many units below the top of the view. */
const val GROUND_LINE = 190f
/** Size of one square of the dino. 12 cells × 4 = the 48-unit player box. */
private const val CELL = 4f
private const val PEBBLE_SPACING = 26f

/**
 * Draws one frame. [motion] is false when the system's "Remove animations"
 * setting is on: the background holds still and the crash shake is skipped.
 */
fun DrawScope.drawGame(
    state: GameState,
    config: GameConfig,
    night: Float,
    effects: Effects,
    motion: Boolean,
) {
    val scale = size.height / VIEW_HEIGHT
    val viewWidth = size.width / scale
    val colors = sceneColors(night)
    val distance = state.world.distance

    // A quick side-to-side shake that dies down over its duration.
    val shakeX = if (motion && effects.shake > 0f) {
        sin(effects.shake * 70f) * 3f * (effects.shake / config.shakeSeconds)
    } else {
        0f
    }

    withTransform({
        scale(scale, scale, pivot = Offset.Zero)
        translate(left = shakeX)
    }) {
        drawBackground(distance, night, viewWidth, colors, motion)
        drawGround(distance, viewWidth, colors)
        state.world.obstacles.forEach { drawObstacle(it, colors) }
        drawDust(effects, distance, config, colors)
        drawDino(state, config, colors)
    }
}

/**
 * Each puff is four specks that spread out, rise a little, and fade. A puff
 * remembers where on the ground it happened, so it drifts back with the ground.
 */
private fun DrawScope.drawDust(effects: Effects, distance: Float, config: GameConfig, colors: SceneColors) {
    val dust = lerp(colors.groundFill, colors.ink, 0.35f)
    for (puff in effects.puffs) {
        val t = puff.age / config.puffSeconds // 0 when new, 1 when gone
        val x = puff.worldX - distance
        for (k in 0 until 4) {
            val spread = (k - 1.5f) * 6f * (0.5f + t)
            val rise = t * 8f + (k % 2) * 3f
            val speck = 1f + 3f * (1f - t)
            drawRect(dust, Offset(x + spread, GROUND_LINE - 2f - rise), Size(speck, speck), alpha = 0.7f * (1f - t))
        }
    }
}

/** Screen y (units down from the top) for a world height (units up from the ground). */
private fun screenY(worldY: Float) = GROUND_LINE - worldY

private fun DrawScope.drawGround(distance: Float, viewWidth: Float, colors: SceneColors) {
    drawRect(colors.groundFill, Offset(0f, GROUND_LINE), Size(viewWidth, VIEW_HEIGHT - GROUND_LINE))
    drawRect(colors.ink, Offset(0f, GROUND_LINE), Size(viewWidth, 2f))

    // Pebbles are placed on a fixed grid in world space, then shifted left by
    // the distance run, so they appear to scroll. Each slot's index picks its
    // look, so a pebble keeps its shape as it moves across the screen.
    val firstSlot = floor(distance / PEBBLE_SPACING).toInt()
    val slotsOnScreen = (viewWidth / PEBBLE_SPACING).toInt() + 2
    for (slot in firstSlot until firstSlot + slotsOnScreen) {
        val variant = (slot * 37 + 11) % 7
        if (variant == 0) continue // leave some gaps so it doesn't look tiled
        val x = slot * PEBBLE_SPACING - distance
        val y = GROUND_LINE + 6f + (variant % 4) * 5f
        drawRect(colors.ink, Offset(x, y), Size(2f + (variant % 3) * 2f, 2f), alpha = 0.6f)
    }
}

// Obstacles. Each is drawn to fill its collision box, so what you see
// matches what you can hit. Every surface has a lit left side and a shaded
// right side, as if the light came from behind the player.

private fun DrawScope.drawObstacle(obstacle: Obstacle, colors: SceneColors) {
    // Kotlin note: `when` on an enum must list every entry (or use `else`), so
    // adding a new ObstacleKind is a compile error here until it's drawn.
    when (obstacle.kind) {
        ObstacleKind.Rock -> drawRock(obstacle, colors)
        ObstacleKind.Cactus -> drawCactus(obstacle.x, obstacle.width, obstacle.height, colors, arms = true, tall = false)
        ObstacleKind.TallCactus -> drawCactus(obstacle.x, obstacle.width, obstacle.height, colors, arms = true, tall = true)
        ObstacleKind.Cluster -> drawCluster(obstacle, colors)
    }
}

/** A rounded boulder, built from stacked rows that narrow toward the top. */
private fun DrawScope.drawRock(rock: Obstacle, colors: SceneColors) {
    val top = screenY(rock.height)
    val rows = max(3, (rock.height / 4f).roundToInt())
    val rowHeight = rock.height / rows
    for (row in 0 until rows) {
        val fromBottom = (rows - 1 - row).toFloat() / rows
        val inset = fromBottom.pow(1.6f) * rock.width * 0.35f
        val x = rock.x + inset
        val width = rock.width - inset * 2
        val y = top + row * rowHeight
        drawRect(colors.rockShade, Offset(x, y), Size(width, rowHeight + 0.5f))
        // The bottom row is only half lit, which reads as a shadow on the ground.
        val litHeight = if (row == rows - 1) rowHeight * 0.5f else rowHeight
        drawRect(colors.rock, Offset(x, y), Size(width * 0.62f, litHeight + 0.5f))
    }
    // A couple of specks for texture.
    drawRect(colors.rockShade, Offset(rock.x + rock.width * 0.3f, top + rock.height * 0.45f), Size(2f, 2f))
    drawRect(colors.rock, Offset(rock.x + rock.width * 0.7f, top + rock.height * 0.7f), Size(2f, 2f))
}

/** Two or three cacti growing side by side, the middle one tallest. */
private fun DrawScope.drawCluster(cluster: Obstacle, colors: SceneColors) {
    val count = if (cluster.width >= 60f) 3 else 2
    val each = cluster.width / count
    for (i in 0 until count) {
        // The shorter ones vary in a way that's stable for this obstacle.
        val shorter = 0.62f + 0.09f * ((i * 7 + cluster.width.roundToInt()) % 3)
        val height = if (i == count / 2) cluster.height else cluster.height * shorter
        drawCactus(cluster.x + i * each + 1f, each - 2f, height, colors, arms = false, tall = false)
    }
}

/**
 * A blocky saguaro: a trunk with a rounded cap, rows of spines, and (if
 * [arms]) two arms that bend up at the elbow.
 */
private fun DrawScope.drawCactus(x: Float, width: Float, height: Float, colors: SceneColors, arms: Boolean, tall: Boolean) {
    val top = screenY(height)
    val trunk = if (arms) max(8f, width * 0.46f) else width * 0.6f
    val trunkX = x + (width - trunk) / 2

    // Kotlin note: `segment` is a local function, visible only inside
    // drawCactus. Each segment is shade, with its left 60% painted lit.
    fun segment(sx: Float, sy: Float, sw: Float, sh: Float) {
        drawRect(colors.cactusShade, Offset(sx, sy), Size(sw, sh))
        drawRect(colors.cactus, Offset(sx, sy), Size(sw * 0.6f, sh))
    }

    segment(trunkX, top + 2f, trunk, height - 2f)
    segment(trunkX + 2f, top, trunk - 4f, 2f) // rounded cap

    var spineY = top + 6f
    while (spineY < GROUND_LINE - 4f) {
        drawRect(colors.spine, Offset(trunkX + trunk * 0.25f, spineY), Size(1.5f, 1.5f))
        drawRect(colors.spine, Offset(trunkX + trunk * 0.75f, spineY + 3f), Size(1.5f, 1.5f))
        spineY += 7f
    }

    if (!arms) return
    val armWidth = max(4f, (width - trunk) / 2 * 0.75f)
    // Left arm: out from the trunk at the elbow, then straight up.
    val leftElbow = top + height * (if (tall) 0.42f else 0.4f)
    segment(x, leftElbow - height * 0.22f, armWidth, height * 0.22f + 4f)
    segment(x + armWidth - 1f, leftElbow, trunkX - (x + armWidth) + 2f, 4f)
    // Right arm: the same shape at a different height, so it isn't symmetrical.
    val rightElbow = top + height * (if (tall) 0.42f else 0.45f)
    val rightArmX = x + width - armWidth
    segment(rightArmX, rightElbow - height * 0.18f, armWidth, height * 0.18f + 4f)
    segment(trunkX + trunk - 1f, rightElbow, rightArmX - (trunkX + trunk) + 2f, 4f)
}

// The dino. X = body, S = shade, B = belly, P = back plates, E = eye.

private val BODY = Sprite(
    "........XXXX",
    "........XEXX",
    "...P.P..XXXX",
    "..PXPXP.XXSS",
    "X.XXXXXXXXX.",
    "XXXXXXXXXXX.",
    ".XXXXXXBBXX.",
    "..SXXXXBB.X.",
    "...SSSSSS...",
)
// The far leg is shaded, so the run cycle reads as two legs, not one.
private val LEGS_STAND = Sprite("...SS.XX....", "...SS.XX....", "...SSS.XXX..")
private val LEGS_RUN_A = Sprite("...SS.XX....", "...SS.......", "...SSS......")
private val LEGS_RUN_B = Sprite("...SS.XX....", "......XX....", "......XXX...")

private val EYE = Color(0xFFFDFBF5)
private val EYE_CRASHED = Color(0xFF2B2622)

private fun DrawScope.drawDino(state: GameState, config: GameConfig, colors: SceneColors) {
    val player = state.world.player
    val legsTop = screenY(player.y) - LEGS_STAND.rows * CELL
    // After a crash the eye goes dark. A small detail, but it reads instantly.
    val eye = if (state is GameState.GameOver) EYE_CRASHED else EYE
    val colorFor = { kind: Char ->
        when (kind) {
            'S' -> colors.dinoShade
            'B' -> colors.dinoBelly
            'P' -> colors.plates
            'E' -> eye
            else -> colors.dino
        }
    }
    drawSprite(BODY, config.playerX, legsTop - BODY.rows * CELL, CELL, colorFor = colorFor)
    drawSprite(legsFor(state), config.playerX, legsTop, CELL, colorFor = colorFor)
}

/** Two-frame run cycle while on the ground. Legs together otherwise. */
private fun legsFor(state: GameState): Sprite {
    if (state !is GameState.Playing || !state.world.player.isGrounded) return LEGS_STAND
    val frame = (state.world.distance / 18f).toInt() % 2
    return if (frame == 0) LEGS_RUN_A else LEGS_RUN_B
}
