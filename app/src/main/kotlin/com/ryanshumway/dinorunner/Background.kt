package com.ryanshumway.dinorunner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.floor
import kotlin.math.roundToInt

// The scenery behind the ground, back to front: sky, stars, sun or moon,
// clouds, two mountain ridges, then hills with trees.
//
// Parallax: each layer scrolls at a fraction of the ground's speed. Far
// things move slowly and near things faster, which is what makes a flat
// screen read as deep.
//
// Nothing here is stored. Every mountain height, cloud, and tree comes from
// hashing its position, so the same spot always looks the same, no matter
// how far you've run, with nothing to allocate or remember.

private const val CLOUD_SPEED = 0.06f
private const val FAR_RIDGE_SPEED = 0.12f
private const val NEAR_RIDGE_SPEED = 0.22f
private const val HILL_SPEED = 0.45f
private const val SUN_SPEED = 0.01f

/**
 * [motion] is false when the system's "Remove animations" setting is on.
 * The background then stays still, while the ground still scrolls so the
 * game remains playable.
 */
fun DrawScope.drawBackground(
    distance: Float,
    night: Float,
    viewWidth: Float,
    colors: SceneColors,
    motion: Boolean,
) {
    // When motion is off, every layer is drawn at distance 0.
    val d = if (motion) distance else 0f

    drawRect(
        Brush.verticalGradient(listOf(colors.skyTop, colors.skyBottom), startY = 0f, endY = GROUND_LINE),
        size = Size(viewWidth, VIEW_HEIGHT),
    )

    // Stars and moon fade in over the second half of dusk.
    val darkness = ((night - 0.5f) * 2f).coerceIn(0f, 1f)
    if (darkness > 0f) drawStars(viewWidth, colors.sunOrMoon, darkness)
    drawSunAndMoon(d, night, darkness, viewWidth, colors)

    drawClouds(d * CLOUD_SPEED, viewWidth, colors, alpha = 0.9f - 0.4f * night)
    drawRidge(d * FAR_RIDGE_SPEED, viewWidth, colors.mountainFar, column = 8f, peaksEvery = 18, minHeight = 40f, maxHeight = 92f, seed = 1)
    drawRidge(d * NEAR_RIDGE_SPEED, viewWidth, colors.mountainNear, column = 6f, peaksEvery = 11, minHeight = 22f, maxHeight = 60f, seed = 2)
    drawHills(d * HILL_SPEED, viewWidth, colors)
}

private fun DrawScope.drawStars(viewWidth: Float, color: Color, alpha: Float) {
    for (i in 0 until 22) {
        val x = hash(i * 3 + 1) * viewWidth
        val y = 8f + hash(i * 3 + 2) * 100f
        val starSize = if (hash(i * 3 + 3) > 0.8f) 3f else 2f
        // Each star gets its own brightness so the sky doesn't look stamped.
        drawRect(color, Offset(x, y), Size(starSize, starSize), alpha = alpha * (0.5f + hash(i * 7) * 0.5f))
    }
}

private val SUN = Sprite(".XXXXX.", "XSSXXXX", "XSXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.")
private val MOON = Sprite(".XXXX..", "XXSX...", "XXX....", "XXX....", "XSX....", "XXXX...", ".XXXX..")

/**
 * The sun sinks a little as it turns orange at sunset, then the moon takes
 * its place. Both drift slowly left and wrap around, so over a long run the
 * sky changes. They stay below the score so the two never overlap.
 */
private fun DrawScope.drawSunAndMoon(d: Float, night: Float, darkness: Float, viewWidth: Float, colors: SceneColors) {
    val span = viewWidth + 80f
    val x = positiveMod(viewWidth * 0.72f - d * SUN_SPEED + 40f, span) - 40f
    val highlight = lerp(colors.sunOrMoon, Color.White, 0.35f)
    if (darkness < 1f) {
        val sunY = 52f + night.coerceAtMost(0.5f) * 40f
        drawSprite(SUN, x, sunY, cell = 4f, alpha = 1f - darkness) { if (it == 'S') highlight else colors.sunOrMoon }
    }
    if (darkness > 0f) {
        val crater = lerp(colors.sunOrMoon, colors.skyTop, 0.25f)
        drawSprite(MOON, x, 44f, cell = 4f, alpha = darkness) { if (it == 'S') crater else colors.sunOrMoon }
    }
}

private val CLOUDS = listOf(
    Sprite("...XXX......", ".XXXXXXX.XX.", "XXXXXXXXXXXX", ".SSSSSSSSSS."),
    Sprite("..XXXX....", "XXXXXXXXX.", "SSSSSSSSSS"),
    Sprite("....XX.......", "..XXXXXX.....", ".XXXXXXXXXXX.", "SSSSSSSSSSSS."),
)

private fun DrawScope.drawClouds(offset: Float, viewWidth: Float, colors: SceneColors, alpha: Float) {
    val spacing = 150f
    val underside = lerp(colors.cloud, colors.skyTop, 0.3f)
    val first = floor(offset / spacing).toInt() - 1
    for (slot in first until first + (viewWidth / spacing).toInt() + 3) {
        if (hash(slot * 11) < 0.35f) continue // about a third of slots stay empty
        val x = slot * spacing - offset + hash(slot * 13) * 60f
        val y = 24f + hash(slot * 17) * 50f
        val cloud = CLOUDS[(hash(slot * 19) * CLOUDS.size).toInt()]
        drawSprite(cloud, x, y, cell = 3f, alpha = alpha) { if (it == 'S') underside else colors.cloud }
    }
}

/**
 * A stepped mountain ridge. Peak heights are picked at regular intervals and
 * eased between with smoothstep, then snapped to 4-unit steps for the blocky
 * look.
 */
private fun DrawScope.drawRidge(
    offset: Float,
    viewWidth: Float,
    color: Color,
    column: Float,
    peaksEvery: Int,
    minHeight: Float,
    maxHeight: Float,
    seed: Int,
) {
    val first = floor(offset / column).toInt()
    for (col in first until first + (viewWidth / column).toInt() + 2) {
        val h = smoothHeight(col, peaksEvery, minHeight, maxHeight, seed)
        val stepped = (h / 4f).roundToInt() * 4f
        drawRect(color, Offset(col * column - offset, GROUND_LINE - stepped), Size(column + 0.5f, stepped))
    }
}

private val SAGUARO = Sprite("..X..", "X.X..", "X.X.X", "XXX.X", "..XXX", "..X..", "..X..")
private val JOSHUA_TREE = Sprite("X...X", "XX.XX", ".X.X.", "..X..", "..X..", "..X..")

/** Low rolling hills, with faint tree silhouettes standing on them. */
private fun DrawScope.drawHills(offset: Float, viewWidth: Float, colors: SceneColors) {
    val column = 4f
    val first = floor(offset / column).toInt()
    for (col in first until first + (viewWidth / column).toInt() + 2) {
        val h = (smoothHeight(col, peaksEvery = 30, minHeight = 8f, maxHeight = 24f, seed = 3) / 2f).roundToInt() * 2f
        val x = col * column - offset
        drawRect(colors.hill, Offset(x, GROUND_LINE - h), Size(column + 0.5f, h))
        if (col % 11 == 0 && hash(col * 7) > 0.45f) {
            val tree = if (hash(col * 5) > 0.5f) JOSHUA_TREE else SAGUARO
            drawSprite(tree, x, GROUND_LINE - h - tree.rows * 3f + 2f, cell = 3f) { colors.tree }
        }
    }
}

/** Height at [col], eased between random peaks placed every [peaksEvery] columns. */
private fun smoothHeight(col: Int, peaksEvery: Int, minHeight: Float, maxHeight: Float, seed: Int): Float {
    val segment = floor(col.toFloat() / peaksEvery).toInt()
    val t = (col - segment * peaksEvery).toFloat() / peaksEvery
    val from = minHeight + hash(segment * 31 + seed) * (maxHeight - minHeight)
    val to = minHeight + hash((segment + 1) * 31 + seed) * (maxHeight - minHeight)
    val eased = t * t * (3 - 2 * t) // smoothstep: gentle at both ends
    return from + (to - from) * eased
}

/**
 * Turns any integer into a repeatable pseudo-random number from 0 to 1. The
 * same input always gives the same output, which is what keeps the scenery
 * stable as it scrolls. Int multiplication wraps on overflow in Kotlin, which
 * this mixing relies on.
 */
internal fun hash(i: Int): Float {
    var x = (i xor 0x9E3779B9.toInt()) * 0x85EBCA6B.toInt()
    x = x xor (x ushr 13)
    x *= 0xC2B2AE35.toInt()
    x = x xor (x ushr 16)
    return (x.toLong() and 0xFFFFFFFFL) / 4_294_967_296f
}

private fun positiveMod(a: Float, b: Float): Float = ((a % b) + b) % b
