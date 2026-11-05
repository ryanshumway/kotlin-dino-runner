package com.ryanshumway.dinorunner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Every color in the scene. Day, sunset, and night are three instances, and
 * anything in between is a blend of two of them, so no color is hard-coded
 * for one time of day.
 *
 * Kotlin note: a plain class with `val` constructor parameters. It doesn't
 * need to be a `data class`, because colors are never compared or copied.
 */
class SceneColors(
    val skyTop: Color,
    val skyBottom: Color,
    /** The sun by day, the moon by night. */
    val sunOrMoon: Color,
    val cloud: Color,
    val mountainFar: Color,
    val mountainNear: Color,
    val hill: Color,
    val tree: Color,
    val groundFill: Color,
    /** Ground line and pebbles. */
    val ink: Color,
    /** Score and messages. Kept separate from [ink] so text stays readable on the sunset sky. */
    val hud: Color,
    val dino: Color,
    val dinoShade: Color,
    val dinoBelly: Color,
    val plates: Color,
    val cactus: Color,
    val cactusShade: Color,
    val spine: Color,
    val rock: Color,
    val rockShade: Color,
)

private val Day = SceneColors(
    skyTop = Color(0xFFCFE6EE), skyBottom = Color(0xFFF6EFE2), sunOrMoon = Color(0xFFF7C948),
    cloud = Color(0xFFFFFFFF), mountainFar = Color(0xFFD9D4E6), mountainNear = Color(0xFFBEC6DA),
    hill = Color(0xFFE4D6B8), tree = Color(0xFFC2AF85), groundFill = Color(0xFFEEDFC2),
    ink = Color(0xFF3A3530), hud = Color(0xFF3A3530),
    dino = Color(0xFF2F8F7A), dinoShade = Color(0xFF226B5B), dinoBelly = Color(0xFF74C8AB),
    plates = Color(0xFFEE8434), cactus = Color(0xFF5E8C45), cactusShade = Color(0xFF3F6B30),
    spine = Color(0xFFB5D98A), rock = Color(0xFFA1958A), rockShade = Color(0xFF766B61),
)

private val Sunset = SceneColors(
    skyTop = Color(0xFF6B5B95), skyBottom = Color(0xFFF5A26F), sunOrMoon = Color(0xFFF26B3A),
    cloud = Color(0xFFF4B6A6), mountainFar = Color(0xFFB07AA1), mountainNear = Color(0xFF8C5E8C),
    hill = Color(0xFFCF957B), tree = Color(0xFFA26A66), groundFill = Color(0xFFE3AE8A),
    ink = Color(0xFF3D2B33), hud = Color(0xFFF5EFE6),
    dino = Color(0xFF2C8273), dinoShade = Color(0xFF1F5F55), dinoBelly = Color(0xFF7CC0A0),
    plates = Color(0xFFF07A3A), cactus = Color(0xFF4F7A44), cactusShade = Color(0xFF365A33),
    spine = Color(0xFFA6C981), rock = Color(0xFF9B7F7A), rockShade = Color(0xFF6F5653),
)

private val Night = SceneColors(
    skyTop = Color(0xFF121726), skyBottom = Color(0xFF2A3350), sunOrMoon = Color(0xFFF4EED8),
    cloud = Color(0xFF3A4360), mountainFar = Color(0xFF2A3149), mountainNear = Color(0xFF222840),
    hill = Color(0xFF1E2436), tree = Color(0xFF2E3854), groundFill = Color(0xFF1B2030),
    ink = Color(0xFFE6E1D6), hud = Color(0xFFF5EFE6),
    dino = Color(0xFF5FC2A6), dinoShade = Color(0xFF3F9682), dinoBelly = Color(0xFF9BE0C8),
    plates = Color(0xFFF29A4A), cactus = Color(0xFF7FA866), cactusShade = Color(0xFF5A8048),
    spine = Color(0xFFC2E39E), rock = Color(0xFF8E8A9A), rockShade = Color(0xFF676477),
)

/**
 * The palette for a given [night] amount (0 = day, 1 = night, from the
 * engine). The middle of the fade holds on sunset for a moment, so the warm
 * sky has time to register instead of flashing by.
 */
fun sceneColors(night: Float): SceneColors = when {
    night <= 0f -> Day
    night < 0.4f -> blend(Day, Sunset, night / 0.4f)
    night <= 0.6f -> Sunset
    night < 1f -> blend(Sunset, Night, (night - 0.6f) / 0.4f)
    else -> Night
}

private fun blend(a: SceneColors, b: SceneColors, t: Float) = SceneColors(
    skyTop = lerp(a.skyTop, b.skyTop, t),
    skyBottom = lerp(a.skyBottom, b.skyBottom, t),
    sunOrMoon = lerp(a.sunOrMoon, b.sunOrMoon, t),
    cloud = lerp(a.cloud, b.cloud, t),
    mountainFar = lerp(a.mountainFar, b.mountainFar, t),
    mountainNear = lerp(a.mountainNear, b.mountainNear, t),
    hill = lerp(a.hill, b.hill, t),
    tree = lerp(a.tree, b.tree, t),
    groundFill = lerp(a.groundFill, b.groundFill, t),
    ink = lerp(a.ink, b.ink, t),
    hud = lerp(a.hud, b.hud, t),
    dino = lerp(a.dino, b.dino, t),
    dinoShade = lerp(a.dinoShade, b.dinoShade, t),
    dinoBelly = lerp(a.dinoBelly, b.dinoBelly, t),
    plates = lerp(a.plates, b.plates, t),
    cactus = lerp(a.cactus, b.cactus, t),
    cactusShade = lerp(a.cactusShade, b.cactusShade, t),
    spine = lerp(a.spine, b.spine, t),
    rock = lerp(a.rock, b.rock, t),
    rockShade = lerp(a.rockShade, b.rockShade, t),
)
