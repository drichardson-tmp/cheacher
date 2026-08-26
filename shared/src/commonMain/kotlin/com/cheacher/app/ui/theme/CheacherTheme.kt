package com.cheacher.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cheacher's visual identity: **lagoon and ember**.
 *
 * The app should feel like clear water over a reef — cool, deep, and lit from above,
 * with one hot spark of coral. The palette is a true complementary scheme:
 *
 * - **60/30: the teal field.** Foam, shallow, and shoal sit on one short arc of
 *   blue-green; they are the dominant field and never compete with content. The board's
 *   two woods are the same arc, pushed apart in value.
 * - **The complement: ember.** Teal's opposite across the wheel is a warm coral-red.
 *   It is the *only* warm hue in the app, which is what makes it read as heat — used
 *   for streaks, unlocks, the selection ring, and nothing else.
 * - **The split: sea green and wash blue.** Verdict green sits just off the teal arc so
 *   "correct" never blends into the field, and the review wash leans blue so a reviewed
 *   line is legibly not-new without shouting.
 * - **Verdicts by value, not just hue.** Correct is a clear step *lighter* than miss, so
 *   the scoreboard survives deuteranopia — the one rule inherited unchanged.
 *
 * Every text-bearing pairing here is enforced by `PaletteContrastTest`: the palette is
 * verified content, exactly like the repertoires.
 */
object Lagoon {
    // The teal field: one analogous arc from foam to abyss.
    val foam = Color(0xFFF4FBF9)
    val shallow = Color(0xFFE7F4F1)
    val shoal = Color(0xFFD2E8E3)
    val tealPale = Color(0xFFB6DED8)
    val teal = Color(0xFF0F6E6A)
    val tealDeep = Color(0xFF0A4B48)
    val tealBright = Color(0xFF5FC8BE)
    val ruleLine = Color(0xFF6B8C87)

    // The deep end: text-grade blue-greens, near-black but never neutral.
    val abyss = Color(0xFF0D2E30)
    val abyssFaded = Color(0xFF47605F)

    // Ember, the single warm accent and teal's complement. `emberDeep` is the
    // text-grade cut; `emberBright` is for glows and rings, never for words.
    val ember = Color(0xFFB2442A)
    val emberDeep = Color(0xFF8F3320)
    val emberBright = Color(0xFFE0764F)

    // Board: the same teal arc, pushed apart in value so pieces read on both squares.
    val boardLight = Color(0xFFCFE6E0)
    val boardDark = Color(0xFF35766F)

    // Verdicts. Sea green sits off the teal arc; madder-coral is ember pushed dark.
    val seaGreen = Color(0xFF1C6B4F)
    val seaGreenFill = Color(0xFF47915F)
    val seaGreenBright = Color(0xFF6FD39A)
    val crimson = Color(0xFFB3241C)
    val crimsonFill = Color(0xFF93291E)
    val crimsonSoft = Color(0xFFF08A7A)

    // The review wash: the cool blue neighbour, for "you have seen this before".
    val wash = Color(0xFF3D6E86)
    val washNight = Color(0xFF8FB6D8)

    // Pieces: cream and abyss, each with the edge that carries it on its same-tone square.
    val pieceCream = Color(0xFFFAF6EC)
    val pieceCreamEdge = Color(0xFF123033)
    val pieceInk = Color(0xFF0E2225)
    val pieceInkEdgeDay = Color(0xFF05131A)
    val pieceInkEdgeNight = Color(0xFFC8DAD6)

    // Night dive: same water, deeper — the field drops to near-black teal and the
    // pale roles flip up, while every hue stays on the same arc.
    val nightDeep = Color(0xFF0B1E20)
    val nightCard = Color(0xFF12292B)
    val nightVellum = Color(0xFF1B383A)
    val nightInk = Color(0xFFD6EAE7)
    val nightInkFaded = Color(0xFF9DB5B3)
    val nightRuleLine = Color(0xFF6E908C)
    val nightBoardLight = Color(0xFF9DC4BE)
    val nightBoardDark = Color(0xFF2E5E5A)
    val nightSeaGreenFill = Color(0xFF4A9462)
    val nightCrimsonFill = Color(0xFF7E241A)
    val nightInProgress = Color(0xFF21474A)
    val lockedGhostDay = Color(0xFFDCEDE9)
    val lockedGhostNight = Color(0xFF16302F)
}

/**
 * The app-specific colour roles Material3's scheme has no words for: board water,
 * move annotations, verdict pigments, the ember moments, and the tree's four states.
 *
 * One immutable value per scheme; both are derived from the same hue relationships in
 * [Lagoon], so day and night are the same reef at two depths. Reach it as
 * `CheacherTheme.colors` — the Material slots keep carrying the standard chrome.
 */
@Immutable
data class CheacherColors(
    val boardLight: Color,
    val boardDark: Color,
    val lastMoveGlow: Color,
    val selectedGlow: Color,
    val targetDot: Color,
    val checkGlow: Color,
    val pieceCream: Color,
    val pieceCreamEdge: Color,
    val pieceInk: Color,
    val pieceInkEdge: Color,
    val verdictCorrect: Color,
    val verdictMiss: Color,
    val onVerdict: Color,
    val reviewTint: Color,
    val lockedGhost: Color,
    val streakAccent: Color,
    val treeUnvisited: Color,
    val treeInProgress: Color,
    val treeCompleted: Color,
    val treeFailed: Color,
    val treeOpenText: Color,
)

internal val DayCheacherColors = CheacherColors(
    boardLight = Lagoon.boardLight,
    boardDark = Lagoon.boardDark,
    lastMoveGlow = Lagoon.emberBright.copy(alpha = 0.40f),
    selectedGlow = Lagoon.ember.copy(alpha = 0.55f),
    targetDot = Lagoon.abyss.copy(alpha = 0.35f),
    checkGlow = Lagoon.crimson.copy(alpha = 0.50f),
    pieceCream = Lagoon.pieceCream,
    pieceCreamEdge = Lagoon.pieceCreamEdge,
    pieceInk = Lagoon.pieceInk,
    pieceInkEdge = Lagoon.pieceInkEdgeDay,
    verdictCorrect = Lagoon.seaGreenFill,
    verdictMiss = Lagoon.crimsonFill,
    onVerdict = Lagoon.foam,
    reviewTint = Lagoon.wash,
    lockedGhost = Lagoon.lockedGhostDay,
    streakAccent = Lagoon.emberDeep,
    treeUnvisited = Lagoon.shoal,
    treeInProgress = Lagoon.boardLight,
    treeCompleted = Lagoon.seaGreenFill,
    treeFailed = Lagoon.crimsonFill,
    treeOpenText = Lagoon.abyss,
)

internal val NightCheacherColors = CheacherColors(
    boardLight = Lagoon.nightBoardLight,
    boardDark = Lagoon.nightBoardDark,
    lastMoveGlow = Lagoon.emberBright.copy(alpha = 0.35f),
    selectedGlow = Lagoon.emberBright.copy(alpha = 0.50f),
    // A dark dot vanishes on dark water, so at night the annotation ink goes pale.
    targetDot = Lagoon.pieceCream.copy(alpha = 0.40f),
    checkGlow = Lagoon.crimsonSoft.copy(alpha = 0.50f),
    pieceCream = Lagoon.pieceCream,
    pieceCreamEdge = Lagoon.pieceCreamEdge,
    pieceInk = Lagoon.pieceInk,
    // Lamp off: the dark pieces are carried by rim light instead of shadow.
    pieceInkEdge = Lagoon.pieceInkEdgeNight,
    verdictCorrect = Lagoon.nightSeaGreenFill,
    verdictMiss = Lagoon.nightCrimsonFill,
    onVerdict = Lagoon.foam,
    reviewTint = Lagoon.washNight,
    lockedGhost = Lagoon.lockedGhostNight,
    streakAccent = Lagoon.emberBright,
    treeUnvisited = Lagoon.nightVellum,
    treeInProgress = Lagoon.nightInProgress,
    treeCompleted = Lagoon.nightSeaGreenFill,
    treeFailed = Lagoon.nightCrimsonFill,
    treeOpenText = Lagoon.nightInk,
)

internal val LightColors = lightColorScheme(
    primary = Lagoon.teal,
    onPrimary = Lagoon.foam,
    primaryContainer = Lagoon.tealPale,
    onPrimaryContainer = Lagoon.tealDeep,
    secondary = Lagoon.ember,
    onSecondary = Lagoon.foam,
    tertiary = Lagoon.seaGreen,
    onTertiary = Lagoon.foam,
    background = Lagoon.shallow,
    onBackground = Lagoon.abyss,
    surface = Lagoon.foam,
    onSurface = Lagoon.abyss,
    surfaceVariant = Lagoon.shoal,
    onSurfaceVariant = Lagoon.abyssFaded,
    error = Lagoon.crimson,
    onError = Lagoon.foam,
    outline = Lagoon.ruleLine,
)

internal val DarkColors = darkColorScheme(
    primary = Lagoon.tealBright,
    onPrimary = Lagoon.nightDeep,
    primaryContainer = Lagoon.tealDeep,
    onPrimaryContainer = Lagoon.tealPale,
    secondary = Lagoon.emberBright,
    onSecondary = Lagoon.nightDeep,
    tertiary = Lagoon.seaGreenBright,
    onTertiary = Lagoon.nightDeep,
    background = Lagoon.nightDeep,
    onBackground = Lagoon.nightInk,
    surface = Lagoon.nightCard,
    onSurface = Lagoon.nightInk,
    surfaceVariant = Lagoon.nightVellum,
    onSurfaceVariant = Lagoon.nightInkFaded,
    error = Lagoon.crimsonSoft,
    onError = Lagoon.nightDeep,
    outline = Lagoon.nightRuleLine,
)

/**
 * Serif for names (the product's whole premise is that names matter), monospace for
 * moves (SAN is code), default sans for chrome.
 */
private val CheacherTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
    ),
)

private val CheacherShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** One place for the app's motion voice: springy, physical, never linear. */
object Motion {
    /** Pieces gliding between squares. */
    val pieceTravel = spring<Offset>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)

    /** Cards, chips, reveals. */
    fun <T> settle() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)

    /** Small punchy feedback — the wrong-move shake, a chip popping in. */
    fun <T> snap() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh)

    /** Slow fades for tree branches dimming out. */
    fun <T> fade() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
}

private val LocalCheacherColors = staticCompositionLocalOf { DayCheacherColors }

/** Companion accessor, [MaterialTheme]-style: `CheacherTheme.colors.boardDark`. */
object CheacherTheme {
    val colors: CheacherColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCheacherColors.current
}

@Composable
fun CheacherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCheacherColors provides if (darkTheme) NightCheacherColors else DayCheacherColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CheacherTypography,
            shapes = CheacherShapes,
            content = content,
        )
    }
}
