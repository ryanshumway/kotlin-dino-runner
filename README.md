# Kotlin Dino Runner

A small endless runner for Android, built with Kotlin and Jetpack Compose. A
blocky dinosaur runs on scrolling ground and jumps over cacti that come faster
as the score climbs: tap to jump, hold to jump higher.
Everything is drawn with simple Canvas shapes, with no sprite sheets or image
assets.

<!-- TODO: replace with a GIF recorded from the emulator -->
![Gameplay screenshot placeholder](docs/screenshot.png)

I'm a product designer who usually works in React and SwiftUI. I built this to
learn idiomatic Kotlin and Compose, so the code is commented where a Kotlin
idiom differs from what a Swift or React developer would expect.

## Status

- [x] Dino runs and jumps on scrolling ground (variable-height jump, two-frame run cycle)
- [x] Obstacles at random sizes and intervals, collision, game over and restart
- [x] Speed ramp
- [x] High score saved with DataStore
- [x] Day/night cycle every 500 points, through a sunset
- [x] Rocks (tap), tall cacti (hold), and cactus clusters
- [x] Parallax scenery: mountains, hills, trees, clouds, sun and moon
- [x] Dust, crash shake, milestone blink, haptics
- [x] Pauses in the background, resumes with a countdown
- [x] Accessibility: respects "Remove animations", TalkBack announcements
- [ ] Web build (Compose Multiplatform, Wasm)

## Build and run

You'll need [Android Studio](https://developer.android.com/studio) (any
recent stable version; it bundles the JDK and Android SDK).

1. Clone the repo and open the folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Pick an emulator or a connected phone and press **Run**.

From the command line:

```sh
./gradlew :game:allTests      # game logic tests (no Android SDK needed)
./gradlew :app:installDebug   # build and install on a connected device
```

## Architecture

```
game/   Pure Kotlin. The rules: physics, state, scoring. No Android imports.
app/    Android shell. Draws the game, runs the frame loop, forwards input.
```

The split is the main design decision. `:game` is a Kotlin Multiplatform
module, so the same rules can later power a web build, and it can be unit
tested on a plain JVM without an emulator.

**Data flow** is one-way, like a Redux store or a SwiftUI model:

```
tap / frame tick ──▶ GameViewModel ──▶ GameEngine.press / release / step
                          │                         │
                          │◀──── new GameState ─────┘
                          ▼
              Canvas redraws from GameState
```

| File | Role |
|---|---|
| `game/.../GameConfig.kt` | Every tunable number: gravity, jump speed, run speed. |
| `game/.../Physics.kt` | `Player` and its jump, release, and per-frame step. |
| `game/.../Obstacles.kt` | `Obstacle`, and the spawner that picks sizes and gaps. |
| `game/.../Collision.kt` | Rectangle overlap, and the player's forgiving hitbox. |
| `game/.../GameState.kt` | `sealed interface GameState`: `Ready`, `Playing`, `Paused`, `GameOver`. |
| `game/.../Effects.kt` | When to show dust, shake the screen, or blink the score. |
| `game/.../GameEngine.kt` | Pure functions from (state, input) to the next state. |
| `app/.../GameViewModel.kt` | Holds the state across rotation; turns events into engine calls. |
| `app/.../HighScoreStore.kt` | Saves the best score with DataStore. |
| `app/.../GameScreen.kt` | Frame loop (`withFrameNanos`), touch input, score overlay. |
| `app/.../GameRenderer.kt` | Draws the ground, rocks, cacti, and the dino. |
| `app/.../Background.kt` | Parallax scenery: sky, sun and moon, clouds, mountains, hills, trees. |
| `app/.../Palette.kt` | Day, sunset, and night palettes, and the blend between them. |
| `app/.../Sprite.kt` | Turns text grids into blocky pictures. |

### A few choices worth calling out

- **Frame-rate independence.** The frame loop passes real elapsed seconds to
  the engine, and all movement is speed × time. The jump uses the exact
  constant-acceleration formula, so its arc is identical at 30, 60, or 120 fps.
  There's a test for that.
- **Immutable state.** `Player`, `World`, and each `GameState` are data
  classes. Every update returns a new value with `copy(...)`. Nothing is
  mutated in place, which is what makes the engine easy to test.
- **Hold to jump higher.** A press sets full jump speed. Releasing early caps
  the upward speed, so a quick tap is a short hop.
- **Fair obstacles.** Gaps between obstacles are measured in seconds of
  running, not distance, so reaction time stays the same as speed rises. The
  shortest gap is longer than a full jump, so there's always room to land.
- **Forgiving collision.** The hitbox is a little smaller than the drawn dino,
  so a graze that looks like a miss is a miss.
- **Testable randomness.** The spawner takes a `Random` as a constructor
  parameter. Tests pass `Random(seed)` so every "random" run is repeatable.
- **No accidental restarts.** Taps are ignored for half a second after a crash,
  so a late jump doesn't skip past your final score.
- **Day and night without extra state.** How dark the sky is gets calculated
  from the distance run, so a crash freezes the sky with everything else and
  a new run starts in daylight. The renderer blends two palettes, so no color
  is hard-coded for one mode.
- **High score stays in the app layer.** The engine only reports the crash.
  The ViewModel notices the switch to `GameOver` and saves the score, so
  `:game` stays free of storage code. The save compares against the stored best
  inside a DataStore transaction, so a lower score can never overwrite a higher
  one.
- **Parallax from math, not assets.** Each background layer scrolls at a
  fraction of the ground's speed. Mountain heights, clouds, and trees come
  from hashing their position, so the scenery is endless, stable as it
  scrolls, and needs no images or stored state.
- **Accessibility is part of the design.** With Android's "Remove
  animations" setting on, the scenery holds still and the shake and blink
  are skipped, while the ground still scrolls so the game stays playable.
  Game over, new high scores, and the resume countdown are announced to
  TalkBack through a live region.
- **Effects are tested too.** Dust, shake, and the milestone blink are
  visual, but *when* they happen is decided in `:game` by comparing the
  state before and after each change, so it's covered by tests like the
  rules are.
- **Cheap redraws.** The Canvas reads the game state inside its draw lambda,
  so each frame only re-runs drawing, not layout. The score overlay uses
  `derivedStateOf`, so it only rebuilds when the number on screen changes.

## Tests

`game/src/commonTest` has 60 tests covering:

- **Jump physics:** peak height, no double jump, landing, variable jump
  height, frame-rate independence.
- **Collision:** overlap, near misses, touching edges, the forgiving hitbox.
- **Obstacle spawning:** sizes and gaps stay in range, gaps scale with speed,
  seeded runs repeat exactly, the shortest gap is always clearable.
- **Engine rules:** state transitions, scrolling, spawning at the screen edge,
  speed ramp and cap, crashing, clearing an obstacle with a jump, restart delay.
- **Jump clearance:** a tap clears the biggest rock, no tap clears a tall
  cactus, and a held jump clears the biggest of every kind (checked across
  every jump timing).
- **Pause:** freezing, the countdown, and resuming exactly where you left off.
- **Effects:** dust on takeoff and landing, crash shake, milestone blink,
  and a clean slate on restart.
- **Day/night:** daylight at the start, the fade at each milestone, full
  night, dawn, and the sky holding still after a crash.

They use `kotlin.test`, Kotlin's own test library, so they'll also run on the
web target later.
