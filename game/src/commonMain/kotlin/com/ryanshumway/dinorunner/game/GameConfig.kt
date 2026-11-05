package com.ryanshumway.dinorunner.game

/**
 * Every tunable number in one place, so tuning the game feel never means
 * hunting through logic.
 *
 * Units: distances are "world units" (the renderer maps 1 unit to roughly
 * 1 dp), time is seconds, and y is measured upward from the ground line.
 *
 * Kotlin note: a `data class` with default arguments acts like a Swift struct
 * with default member values. `GameConfig()` gives the defaults, and a test
 * can write `GameConfig(gravity = 1000f)` to change just one.
 */
data class GameConfig(
    // Player
    /** The player's fixed left edge. The world scrolls past it. */
    val playerX: Float = 48f,
    val playerWidth: Float = 48f,
    val playerHeight: Float = 48f,
    /**
     * How far the collision box is shrunk inside the drawn dino on the left,
     * right and top. The dino isn't a rectangle, so a full-size box would
     * count "hits" that look like clear misses.
     */
    val hitboxInset: Float = 6f,

    // Jumping
    /** Downward acceleration while airborne, in units/s². */
    val gravity: Float = 2_600f,
    /** Upward speed at the moment of a jump. Peak height is v² / 2g, about 129 units. */
    val jumpVelocity: Float = 820f,
    /**
     * On release, upward speed is capped to this, so a quick tap gives a short
     * hop (a 50 ms tap peaks around 45 units, enough for a rock) while a held
     * jump goes the full height (enough for a tall cactus).
     */
    val jumpCutVelocity: Float = 200f,

    // Speed
    /** Horizontal scroll speed at the start of a run, in units/s. */
    val runSpeed: Float = 360f,
    /** Speed added for each point scored, so the run gets harder gradually. */
    val speedGainPerPoint: Float = 0.6f,
    /** Speed never goes above this. Reached at about 900 points. */
    val maxSpeed: Float = 900f,

    // Obstacles (sizes live on ObstacleKind)
    /**
     * Clear gaps between obstacles are measured in seconds of running, not in
     * distance, so the time you get to react stays fair as speed rises. The
     * minimum must be longer than a full jump (about 0.63 s) so there's always
     * room to land before the next one.
     */
    val minGapSeconds: Float = 0.8f,
    val maxGapSeconds: Float = 1.8f,

    // Scoring and timing
    /** Distance that earns one point. 360 / 36 is 10 points per second at the start. */
    val unitsPerPoint: Float = 36f,

    // Day and night
    /** The sky flips between day and night every this many points. */
    val dayNightEveryPoints: Float = 500f,
    /**
     * How many points the fade between day and night lasts (about 4 seconds
     * at that speed). Long enough for the sunset in the middle to register.
     */
    val dayNightFadePoints: Float = 60f,

    /**
     * After a crash, taps are ignored this long, so a jump pressed a moment
     * too late doesn't instantly restart the game and hide the final score.
     */
    val restartDelaySeconds: Float = 0.5f,
    /** After pausing, a tap starts a countdown this long before the run resumes. */
    val resumeCountdownSeconds: Float = 3f,

    // Effects (purely visual, but timed here so they're testable)
    /** How long a dust puff lasts. */
    val puffSeconds: Float = 0.35f,
    /** How long the screen shakes after a crash. */
    val shakeSeconds: Float = 0.3f,
    /** The score blinks for this long at each milestone. */
    val flashSeconds: Float = 0.9f,
    /** A milestone every this many points. */
    val flashEveryPoints: Int = 100,
    /**
     * Longest frame we will simulate in one step. If the app stalls (a GC
     * pause, or coming back from the background), a huge delta would teleport
     * the player through an obstacle, so we clamp it.
     */
    val maxFrameSeconds: Float = 1f / 20f,
)
