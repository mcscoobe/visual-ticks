# Visual Ticks

RuneLite external plugin that draws a customisable tick metronome overlay. Up to three
independent tick counters run at once, each with its own shape, colours, layout and
optional tab restriction.

## Commands

```bash
./gradlew test           # unit tests (JUnit 4 + Mockito) — 72 tests, no game client needed
./gradlew build          # compile + test
./gradlew runTestClient  # launch RuneLite with this plugin side-loaded (--developer-mode)
```

`runTestClient` runs `VisualTicksPluginTest.main`, which calls
`ExternalPluginManager.loadBuiltin` before `RuneLite.main`. It opens the real game client;
use it only for visual checks, not for automated verification.

The RuneLite dependency is pinned to `latest.release` from `repo.runelite.net`, so a clean
build needs network access. Source level is Java 8 (`sourceCompatibility = '1.8'`) even
though the toolchain here is JDK 11 — don't use post-8 language features in `src/main`.

## Architecture

**`VisualTicksPlugin`** owns all mutable state: `public final int[] ticks = new int[3]`,
one counter per tick set. `onGameTick` advances each enabled counter modulo its own
`numberOfTicksN`. Everything else is stateless rendering.

**Three overlays, one base.** `BaseVisualTicksOverlay` holds all layout and drawing.
`VisualTicksOverlayOne/Two/Three` are thin subclasses that supply only two things — the
counter index (`plugin.ticks[N]`) and the settings factory (`TickSettings.one/two/three`).
Adding a fourth tick set means a new subclass, a new factory, a new config block, and
widening the `ticks` array. `OverlayWiringTest` exists specifically to catch a copy-paste
slip that points an overlay at the wrong counter.

**`TickSettings`** is a plain-field snapshot of one tick set's config, rebuilt only when
config changes so `render` doesn't call proxy getters per frame. The three near-identical
factories are deliberate and documented in the class javadoc: RuneLite resolves
`@ConfigItem` defaults only through the config proxy, so reading keys generically by
suffix via `ConfigManager.getConfiguration` returns null for anything the user never
touched, losing every default. Don't "de-duplicate" them into a reflective loop.

**Layout invariant (issue #5).** `calculateSizes` computes one cell shared by every tick, so
every column and row sits on a single pitch — sizing each cell on its own label gives each
column its own pitch (`"10"` is wider than `"9"`) and breaks the grid. Width and height are
tracked separately on purpose: `cellWidth` takes the widest label across `1..numberOfTicks`,
while `cellHeight` considers only the shape size and font ascent, because label width is a
horizontal measurement and must not push rows apart. Because the cell is sized for the widest
label, `shapeInsetX/Y` re-centre the shape inside it so shape and label stay concentric
instead of the shape drifting to the corner. The reported `Dimension` deliberately excludes
trailing spacing after the last row/column.

**Refresh path.** `updateOverlays` is the only place overlays are added to or removed from
`OverlayManager`; it runs on `startUp` and on any `ConfigChanged` in group `visualticks`,
removing every overlay first and re-adding the enabled ones, then flagging each for
recalculation. Overlays set `configChanged = true` again if `readSettings`/`calculateSizes`
throws, so a transient failure retries next frame instead of latching.

**Threading.** `ticks` is touched by `onGameTick` and overlay rendering (client thread) and
by `keyPressed` (AWT thread). The reset hotkey therefore hands its `Arrays.fill` to
`clientThread.invoke` rather than writing directly. The adjust hotkeys need no such hop for
their config writes, which `onConfigChanged` already handles off the client thread, but
`hiddenByHotkey` is written from AWT and cleared from the client thread, so it is volatile.

**Tick adjust hotkeys (issue #3).** `tickAdjustHotkeyMode` picks between one global
increase/decrease pair and a pair per tick set; RuneLite's `@ConfigItem` has no conditional
hide or disable, so both sets of keybinds are always editable and the mode decides which
ones `keyPressed` honours. Adjustments are written back through `ConfigManager` and reach
the overlays as an ordinary `ConfigChanged`. Decreasing a set at 2 disables it and records
that in `hiddenByHotkey`, which exists so a *global* increase revives only what the global
decrease hid instead of switching on sets the user deliberately turned off; a per-set
increase names its target and revives it either way. That claim is session-only and is
cleared on `startUp` and `ProfileChanged` — a profile carries its own enabled flags, so a
claim must never cross between profiles.

`keyPressed` skips adjustments while the player is typing, and consumes the key event of a
matched adjustment. `KeyManager.shouldProcess` withholds key events only on the login
screen, so without that guard a printable key bound to an adjustment would rewrite stored
settings mid-sentence. The reset hotkey is deliberately outside the guard: clearing a
counter is cheap enough to survive a stray keystroke.

**Config migration.** `migrate()` runs on `startUp` and on `ProfileChanged`. It splits the
legacy single-padding keys (`paddingBetweenTicksOne`, `tickPaddingTwo`, `tickPaddingThree`)
into the current `horizontalSpacingN`/`verticalSpacingN` pair, then unsets the old key.
`ProfileChanged` matters because a profile switch brings in a different set of stored keys.

## Config

`VisualTicksConfig` is a single ~720-line interface, config group `visualticks`, organised
into four sections: `hotkeySettings`, then `tickSettings` / `tickSettingsTwo` /
`tickSettingsThree`. Keys are suffixed `One`/`Two`/`Three` and every set has the same 18
items. When adding a setting you must touch all three blocks plus the matching
`TickSettings` factory — `TickSettingsTest` asserts each factory reads only its own
suffixed getters and maps every getter to its matching field. The exception is the per-set
`increaseHotkeyN`/`decreaseHotkeyN` pair: hotkeys are read in `keyPressed`, not while
rendering, so they stay out of `TickSettings`.

## Code style

- Indentation is inconsistent by area and each file should stay as it is: `src/main` uses
  4 spaces, `src/test` and `build.gradle` use tabs.
- The user-level why-not-what comment rule has real teeth here: the pitch invariant, the
  duplicated `TickSettings` factories, and the client-thread hop all carry comments whose
  only job is to stop a future reader "simplifying" them. `TickSettings`'s class javadoc is
  the model to follow when a deliberate deviation needs explaining.

## Git

General branching/commit/PR rules live in the user-level `CLAUDE.md`. Project specifics:

- The default branch is **`master`**, not `main` — rebase with
  `git fetch origin && git rebase origin/master`.
- Branch names carry the issue number when one exists:
  `bug/5-tick-columns-misalign-when-labels-diff`. Note this repo has used `bug/` for
  defect branches where the global convention says `fix/`; prefer `fix/` for new ones.
- Squash-merged commits keep the PR number in the subject:
  `test: add unit test suite for plugin, overlay and settings (#9)`.
- This repo is worked on through **git worktrees** (`visual-ticks` and `visual-ticks-wt`
  share one `.git`). Check `git worktree list` before assuming which branch is checked out
  where, and never use bare `git stash`/`git stash pop` — the stash stack is shared.
- `.gitignore` already covers `.gradle`, `build`, and editor config. Build output appears
  in both checkouts after `./gradlew test`; it should never be staged.

## Docs

`README.md` and this file are the current doc set. The `CONTRIBUTING.md`, `CHANGELOG.md`,
and `docs/adr/` files the user-level standard calls for don't exist here yet.

Two files hold user-facing text and go stale when behavior changes; update them in the
same PR as the code:

- `README.md` — feature overview plus the demo gifs in `readme/`. A change that alters what
  those gifs show needs the gifs regenerated, not just the prose edited.
- `runelite-plugin.properties` — the Plugin Hub manifest (`displayName`, `description`,
  `tags`). This is what users read before installing.
