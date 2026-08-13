# Cheacher

A chess opening teacher that believes names come first and words come last.

Cheacher teaches repertoires in two phases, both played out on a real board:

1. **Named Concept Walkthrough (Guided).** The prompt is the canonical *name* of the
   line the next move creates — "King's Pawn Opening" → 1. e4, "Sicilian Defence" →
   1...c5, "Open Sicilian" → 2. Nf3 — played from both sides. There is exactly one
   human-language allowance per move: a single sentence of *why* (the "idea"), revealed
   on a wrong attempt or on demand. You cannot brute-force past a name.

2. **Branch Recall / Tree Pruning.** No names, no hints. A compact tree diagram sits
   beside the board. Play a line to its authored end and that branch closes out — dims
   green on the diagram, unplayable on the board — and you snap back to the nearest
   junction that still has an open door. The round ends when the whole tree is closed.
   Mistakes are governed by a policy: **Strict** (one miss fails the branch, red flash,
   snap back) or **One Allowance** (the first miss is forgiven in place). The tree is
   the scoreboard: "3 of 5 branches".

Cheacher also remembers. Every miss, every completed line, every session lands in a
per-repertoire `TrainingRecord`, persisted with DataStore on both platforms, and
surfaces on the shelf as "trouble spots".

## Architecture

```
composeApp/src/commonMain/kotlin/com/cheacher/opening/
├── chess/        Pure-Kotlin chess engine. Immutable Position, full legal move
│                 generation (castling, en passant, promotion), FEN, SAN
│                 (render + generate-and-match parsing). Zero dependencies.
├── domain/       Repertoire (the authored, serializable tree of SAN + name + idea),
│                 OpeningTree (resolved against real positions at load time — an
│                 illegal move in content fails at resolve, never at the board),
│                 and the authoring DSL.
├── training/     The two study modes as pure reducers: GuidedState and BranchState.
│                 Every transition is a value; no clocks, no coroutines, fully
│                 testable without UI.
├── progress/     TrainingRecord (pure accumulation of misses/completions/sessions),
│                 ProgressStore (interface), DataStoreProgressStore (JSON blob per
│                 repertoire in one preferences file), expect/actual file paths.
├── data/         SampleRepertoires — the built-in Italian Game and Sicilian
│                 Crossroads content, written with the DSL.
└── ui/           Compose Multiplatform. theme/ (warm wood & ink identity, spring
                  motion specs), board/ (ChessBoardView + PieceRenderer seam),
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
./gradlew :composeApp:assembleDebug          # build the APK
./gradlew :composeApp:installDebug           # install on a connected device
```

**iOS**

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme, or:

```sh
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The Xcode project's "Compile Kotlin Framework" phase invokes
`:composeApp:embedAndSignAppleFrameworkForXcode`, so the Kotlin framework is rebuilt
automatically on every Xcode build.

## Tests

```sh
./gradlew :composeApp:testDebugUnitTest      # the full commonTest suite on JVM
./gradlew :composeApp:iosSimulatorArm64Test  # same suite on Kotlin/Native (slower)
```

The suite covers the engine (FEN round-trips, perft — 8,902 nodes at depth 3 from the
start, Kiwipete at depth 2 — castling edge cases, en passant, promotion, SAN
disambiguation and a render→parse round-trip over every legal move in a bag of
positions), tree resolution, both training reducers, sample-content validity (every
authored SAN must be legal or the test fails), and TrainingRecord accumulation.

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
- **Dynamic intent fading.** Guided prompts that gradually drop the name as recall
  strengthens, converging on Phase 2 naturally.
