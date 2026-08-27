# Cheacher

A chess opening teacher that believes names come first and words come last.

Cheacher teaches repertoires in two phases, both played out on a real board:

1. **Named Concept Walkthrough (Guided).** The prompt begins as the canonical *name* of the
   line the next move creates — "King's Pawn Opening" → 1. e4, "Sicilian Defence" →
   1...c5, "Open Sicilian" → 2. Nf3 — played from both sides. There is exactly one
   human-language allowance per move: a single sentence of *why* (the "idea"), revealed
   on a wrong attempt or on demand. You cannot brute-force past a name.
   As a move's clean-recall streak grows, the prompt fades from the full name to its
   first word and finally to the board alone; asking for the idea restores it in place.

2. **Branch Recall / Tree Pruning.** Only the destination name remains; no per-move names
   or hints. Play a line to its authored end and that branch closes out — green on the
   end-of-round tree, unplayable on the board — and you snap back to the nearest
   junction that still has an open door. The round ends when the whole tree is closed.
   Mistakes are governed by a policy: **Strict** (one miss fails the branch, red flash,
   snap back) or **One Allowance** (the first miss is forgiven in place). The tree is
   the scoreboard: "3 of 5 branches".

By default the shelf follows the **coach's plan**: the repertoire is conquered one
branch at a time, depth-first. A line is *mastered* when it has been named once (guided)
and recalled once (branch) — then the next line joins the syllabus, always the fork at
the deepest open junction, so every new line is "everything you already know, plus one
new turn". Landing a new fork is a small brass moment ("Unlocked: Ruy Lopez").

Mastered lines are never filed away. Every session mixes the new line with reviews:
each clean blind recall of a line grows its streak, and the streak sets how long the
line rests before the coach deals it again — roughly 1, 3, 7, 14, then 30 days, the
classic expanding-retrieval ladder. Miss a move and that move's own clock resets, so the
trouble position comes back without dragging every line through it along. The ladder is
a prioritiser, never a gate: nothing is withheld, no date is ever waved at you, and
"Full tree" — everything open at once —
is always one tap away. The shelf talks in counts and streaks instead: "Today: 1 new
line · 2 reviews", "3 days in a row", "Ruy Lopez: 4 clean recalls".

Moves that have actually caused trouble keep a smaller clock and recoverable severity
of their own. When one is due, Branch Recall deals a mastered line immediately before that move, so a shaky
...Nc6 is tested as ...Nc6 instead of charging the whole ten-move line again.

The recall lab adds two deliberately separate drills: **Blitz** deals unrelated board
formations at speed, while **Quiet** gives only a destination name and asks for the exact
root-to-leaf sequence. Every authored leaf also offers an optional play-out from its exact
position against the adaptive sparring engine without replacing the normal study path.

Cheacher also remembers. Every miss, every completed line, every review streak, every
session lands in a per-repertoire `TrainingRecord`, persisted with DataStore on both
platforms, and surfaces on the shelf as "trouble spots".

## Architecture

Two Gradle modules, the split Android Gradle Plugin 9 requires: `shared` is the
Kotlin Multiplatform library (everything below), and `androidApp` is a thin
`com.android.application` shell holding only `MainActivity` and the manifest. iOS
consumes `shared` directly as the `Shared` framework — `iosApp` is the Xcode project.

```
shared/src/commonMain/kotlin/com/cheacher/app/
├── chess/        Pure-Kotlin chess engine. Immutable Position, full legal move
│                 generation (castling, en passant, promotion), FEN, SAN
│                 (render + generate-and-match parsing). Zero dependencies.
├── domain/       Repertoire (the authored, serializable tree of SAN + name + idea),
│                 OpeningTree (resolved against real positions at load time — an
│                 illegal move in content fails at resolve, never at the board),
│                 and the authoring DSL.
├── training/     Study and drill modes as pure reducers: GuidedState, BranchState,
│                 BlitzState, QuietState, MoveDrillState, and SquareDrillState.
│                 Every transition is a value; no clocks, no coroutines, fully
│                 testable without UI. Progression derives the depth-first mastery
│                 ladder (mastered / unlocked / locked lines) from the tree and the
│                 TrainingRecord — computed, never stored. Scheduler composes on top:
│                 a pure (tree, record, now) → syllabus function that deals today's
│                 session — the frontier line plus whichever reviews earned a seat on
│                 the expanding 1/3/7/14/30-day ladder.
├── progress/     TrainingRecord (pure accumulation of misses/completions/sessions),
│                 ProgressStore (interface), DataStoreProgressStore (JSON blob per
│                 repertoire in one preferences file), expect/actual file paths.
├── data/         SampleRepertoires — a broad White shelf plus a French repertoire
│                 from Black's chair, all written with the DSL.
└── ui/           Compose Multiplatform. theme/ (warm wood & iron-gall ink identity —
                  a 60-30-10 palette with contrast-verified day and night schemes —
                  plus spring motion specs), board/ (ChessBoardView + PieceRenderer seam),
                  tree/ (VariationTreeView mini-DAG), screens/ (Home, Guided,
                  Branch + their ViewModels), App.kt (sealed-Screen navigation).
```

Design rules the code follows: immutable data, pure reducers, manual wiring (no DI
framework), and KDoc that explains *why* rather than *what*.

## Running it

Requirements: JDK 17+ (Android Studio's JBR works), Android SDK with platform 36,
Xcode for iOS. `local.properties` must point at your SDK (`sdk.dir=...`).

**Android**

```sh
./gradlew :androidApp:assembleDebug          # build the APK
./gradlew :androidApp:installDebug           # install on a connected device
```

**iOS**

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme, or:

```sh
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The Xcode project's "Compile Kotlin Framework" phase invokes
`:shared:embedAndSignAppleFrameworkForXcode`, so the Kotlin framework is rebuilt
automatically on every Xcode build.

## Tests

```sh
./gradlew :shared:testAndroidHostTest        # the full commonTest suite on the host JVM
./gradlew :shared:iosSimulatorArm64Test      # same suite on Kotlin/Native (slower)
```

The suite covers the engine (FEN round-trips, perft — 8,902 nodes at depth 3 from the
start, Kiwipete at depth 2 — castling edge cases, en passant, promotion, SAN
disambiguation and a render→parse round-trip over every legal move in a bag of
positions), tree resolution, both training reducers, the progression ladder and the
review scheduler (interval growth, lapses, interleaving, frozen session snapshots),
sample-content validity (every authored SAN must be legal or the test fails), and
TrainingRecord accumulation including legacy-blob decoding.

## Authoring a repertoire

With the DSL — indentation is the tree:

```kotlin
val mine = repertoire("scotch", "The Scotch Game", Color.WHITE) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees two pieces.") {
        move("e5", "Open Game", "Symmetry, contested immediately.") {
            move("Nf3", "King's Knight Opening", "Develops with a threat.") {
                move("Nc6", "Normal Variation", "Defends e5 without spending a pawn.") {
                    move("d4", "Scotch Game", "Breaks the centre open at once.")
                }
            }
        }
    }
}
```

Or as JSON — `Repertoire` is `@Serializable` with the same shape (`san`, `name`,
`idea`, `children`). Either way, `OpeningTree.resolve(...)` walks every move against a
real board and throws `RepertoireFormatException` on the first illegal SAN: content
bugs die at load time, not in front of a learner. Names should be canonical chess
vocabulary; ideas must fit in one sentence — that limit is the product, not a style
suggestion.

## Assets

Pieces are currently drawn as Unicode glyphs. The swap point is deliberate and small:
`ui/board/PieceRenderer.kt` defines the `PieceRenderer` interface — "draw this piece,
this big" — and `ChessBoardView` takes one as a parameter. To ship real vector pieces,
implement `PieceRenderer` against your assets (e.g. Compose `ImageVector`s generated
from SVGs) and pass it in; the board, animation and input code never change.

## Roadmap

Deferred on purpose, in rough order:

- **Foley & flair.** Piece sounds and comic-style streak callouts ("ANNIHILATED" after
  a clean sweep). A bigger endeavor than it looks — real sound design, not stock
  clicks — so it waits.
- **On-device trend analysis.** Let a local model (Gemini Nano via Android AICore
  where available; Apple Intelligence equivalents on iOS) read the persisted
  `TrainingRecord` and narrate repeat-mistake patterns — "you keep missing ...a6 in
  the Najdorf; the point is stopping Bb5+". The per-node `missCounts` map keyed by
  tree-node id is structured precisely as that feature's input.
