# Visual Ticks

RuneLite external plugin that draws a customisable tick metronome overlay: up to three
independent tick sets, each with its own shape, colours, layout and optional tab restriction.

The source documents itself: class javadoc and why-comments carry the architecture, thread
map and design decisions, and tests enforce the conventions. Read them before changing code.

## Commands

```bash
./gradlew test           # unit tests (JUnit 4 + Mockito), no game client needed
./gradlew build          # compile + test
./gradlew runTestClient  # launch RuneLite with this plugin side-loaded (--developer-mode)
```

`runTestClient` opens the real game client; use it for visual checks, not automated
verification.

The RuneLite dependency resolves `latest.release` from `repo.runelite.net` on every clean
build, so a build needs network access and can pick up a new client version. Source level is
Java 8 (`sourceCompatibility = '1.8'`) though the toolchain is JDK 11: keep `src/main` to
Java 8 language features.

## Persisted identifiers

Some names in the source are stored user data, and renaming one silently resets every user's
settings or overlay positions while every test still passes. The warnings sit on the things
they protect: the class javadoc of `VisualTicksConfig` (config keys, enum constant names) and
of `BaseVisualTicksOverlay` (overlay class names), and `migrate()` in `VisualTicksPlugin`
(legacy keys).

## Code style

Indentation differs by area; keep each file as it is: `src/main` uses 4 spaces, `src/test`
and `build.gradle` use tabs.

## Git

General branching/commit/PR rules live in the user-level `CLAUDE.md`. Project specifics:

- The default branch is **`master`**, not `main` — rebase with
  `git fetch origin && git rebase origin/master`.
- Branch names carry the issue number when one exists:
  `fix/5-tick-columns-misalign-when-labels-diff`. Older defect branches used `bug/`; use
  `fix/` for new ones.
- Squash-merged commits keep the PR number in the subject:
  `test: add unit test suite for plugin, overlay and settings (#9)`.
- This repo is worked on through **git worktrees** sharing one `.git`. Check
  `git worktree list` before assuming which branch is checked out where, and keep off bare
  `git stash`/`git stash pop` — the stash stack is shared.
- Build output from `./gradlew test` appears in every checkout and is gitignored; stage
  source only.

## User-facing text

Two files describe behaviour to users and go stale when it changes; update them in the same
PR as the code:

- `README.md` — feature overview plus the demo gifs in `readme/`. A change that alters what
  those gifs show needs the gifs regenerated, not just the prose edited.
- `runelite-plugin.properties` — the Plugin Hub manifest (`displayName`, `description`,
  `tags`). This is what users read before installing.

## Plugin Hub packaging

`runelite-plugin.properties` carries `build=standard`, which the hub packager requires.
Standard means the hub **replaces `build.gradle` and `settings.gradle` with its own** when it
builds a submission, so nothing in this repo's `build.gradle` reaches the shipped plugin: a
runtime dependency added there compiles here and is missing in the hub build. The hub's set
covers the RuneLite client, Lombok and JetBrains annotations; it never compiles `src/test`,
so JUnit/Mockito and `runTestClient` are local-only. A real dependency means switching to
`build=gradle` and getting it onto the hub's verification allow-list.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on mcscoobe/visual-ticks (via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context. No `GLOSSARY.md` or `docs/adr/` exists yet; `/domain-modeling` creates them when a term or decision is first resolved. See `docs/agents/domain.md`.
