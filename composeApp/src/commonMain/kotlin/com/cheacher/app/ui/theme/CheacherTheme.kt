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
 * Cheacher's visual identity: **warm wood and cool ink**.
 *
 * The app should feel like a well-loved chess study — walnut boards, cream paper,
 * ink annotations, one flash of brass. The palette is built on classic colour theory:
 *
 * - **60/30: a warm analogous field.** Paper, parchment, and wood all sit on one short
 *   arc of ochre-to-umber hues; they are the dominant field and never compete.
 * - **The complement: iron-gall ink.** Real iron-gall ink is not brown — it dries to a
 *   cool blue-black with a hint of indigo. Every text shade sits on that cool axis, so
 *   *warm field against cool ink* is the scheme's one fundamental contrast, and the
 *   warm hues get the complement they were missing.
 * - **10: brass.** One metal, used only for the moments that deserve it — streaks,
 *   unlocks, the cursor ring.
 * - **Verdicts as natural pigments.** Leaf green and madder red, tuned so a banked
 *   branch is clearly *lighter* than a lost one — the verdict reads by value as well
 *   as hue, which is what keeps it legible to deuteranopic eyes.
 *
 * Every text-bearing pairing here is enforced by `PaletteContrastTest`: the palette is
 * verified content, exactly like the repertoires.
 */
object Ink {
    // The warm field: one analogous arc from paper to walnut.
    val paper = Color(0xFFFDF8EE)
    val parchment = Color(0xFFF6EFE3)
    val vellum = Color(0xFFEFE4D0)
    val boardLight = Color(0xFFEED9B7)
    val boardDark = Color(0xFF9A6743)
    val walnut = Color(0xFF6B4A31)
    val walnutDeep = Color(0xFF4A3122)
    val ruleLine = Color(0xFF9A8A6A)

    // The cool axis: iron-gall ink, blue-black leaning indigo.
    val ink = Color(0xFF262B33)
    val inkFaded = Color(0xFF5C6068)
    val inkWash = Color(0xFF4E5A75)

    // Brass, the single accent. `brassDeep` is the text-grade cut; the bright cuts
    // are for glows and rings, never for words.
    val brass = Color(0xFFB8863B)
    val brassDeep = Color(0xFF96682A)
    val brassBright = Color(0xFFD9A84E)

    // Natural pigments for verdicts. The *Fill cuts carry white text on chips; the
    // plain cuts are text-grade on paper.
    val leaf = Color(0xFF477443)
    val leafFill = Color(0xFF5F8F52)
    val leafBright = Color(0xFF6FA36A)
    val madder = Color(0xFFA83E32)
    val madderFill = Color(0xFF8E2F24)
    val madderSoft = Color(0xFFD8887F)

    // Pieces: cream and ink, each with the edge that carries it on its same-tone square.
    val pieceCream = Color(0xFFFBF3E4)
    val pieceCreamEdge = Color(0xFF3A322A)
    val pieceInk = Color(0xFF20242C)
    val pieceInkEdgeDay = Color(0xFF0F1115)
    val pieceInkEdgeNight = Color(0xFFC9BFA9)

    // Night study: same wood, lamp off — darker values, slightly desaturated, and the
    // iron-gall ink flips to the pale role while staying cool.
    val nightPaper = Color(0xFF201A14)
    val nightCard = Color(0xFF2B231B)
    val nightVellum = Color(0xFF3A2F24)
    val nightInk = Color(0xFFD9DDE4)
    val nightInkFaded = Color(0xFFAEB0B6)
    val nightRuleLine = Color(0xFF8C7B60)
    val nightBoardLight = Color(0xFFC4A87E)
    val nightBoardDark = Color(0xFF5E4630)
    val nightLeafFill = Color(0xFF55814B)
    val nightMadderFill = Color(0xFF7E2B20)
    val nightInkWash = Color(0xFFA9B4CD)
    val lockedGhostDay = Color(0xFFE9DFCB)
    val lockedGhostNight = Color(0xFF352C22)
}

/**
 * The app-specific colour roles Material3's scheme has no words for: board wood,
 * move annotations, verdict pigments, the brass moments, and the tree's four states.
 *
 * One immutable value per scheme; both are derived from the same hue relationships in
 * [Ink], so day and night are the same study with the lamp on or off. Reach it as
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
    val streakBrass: Color,
    val treeUnvisited: Color,
    val treeInProgress: Color,
    val treeCompleted: Color,
    val treeFailed: Color,
    val treeOpenText: Color,
)

internal val DayCheacherColors = CheacherColors(
    boardLight = Ink.boardLight,
    boardDark = Ink.boardDark,
    lastMoveGlow = Ink.brassBright.copy(alpha = 0.40f),
    selectedGlow = Ink.brass.copy(alpha = 0.55f),
    targetDot = Ink.ink.copy(alpha = 0.35f),
    checkGlow = Ink.madder.copy(alpha = 0.50f),
    pieceCream = Ink.pieceCream,
    pieceCreamEdge = Ink.pieceCreamEdge,
    pieceInk = Ink.pieceInk,
    pieceInkEdge = Ink.pieceInkEdgeDay,
    verdictCorrect = Ink.leafFill,
    verdictMiss = Ink.madderFill,
    onVerdict = Ink.paper,
    reviewTint = Ink.inkWash,
    lockedGhost = Ink.lockedGhostDay,
    streakBrass = Ink.brassDeep,
    treeUnvisited = Ink.vellum,
    treeInProgress = Ink.boardLight,
    treeCompleted = Ink.leafFill,
    treeFailed = Ink.madderFill,
    treeOpenText = Ink.ink,
)

internal val NightCheacherColors = CheacherColors(
    boardLight = Ink.nightBoardLight,
    boardDark = Ink.nightBoardDark,
    lastMoveGlow = Ink.brassBright.copy(alpha = 0.35f),
    selectedGlow = Ink.brassBright.copy(alpha = 0.50f),
    // A dark dot vanishes on dark wood, so at night the annotation ink goes pale.
    targetDot = Ink.pieceCream.copy(alpha = 0.40f),
    checkGlow = Ink.madderSoft.copy(alpha = 0.50f),
    pieceCream = Ink.pieceCream,
    pieceCreamEdge = Ink.pieceCreamEdge,
    pieceInk = Ink.pieceInk,
    // Lamp off: the dark pieces are carried by rim light instead of shadow.
    pieceInkEdge = Ink.pieceInkEdgeNight,
    verdictCorrect = Ink.nightLeafFill,
    verdictMiss = Ink.nightMadderFill,
    onVerdict = Ink.paper,
    reviewTint = Ink.nightInkWash,
    lockedGhost = Ink.lockedGhostNight,
    streakBrass = Ink.brassBright,
    treeUnvisited = Ink.nightVellum,
    treeInProgress = Ink.walnutDeep,
    treeCompleted = Ink.nightLeafFill,
    treeFailed = Ink.nightMadderFill,
    treeOpenText = Ink.nightInk,
)

internal val LightColors = lightColorScheme(
    primary = Ink.walnut,
    onPrimary = Ink.paper,
    primaryContainer = Ink.boardLight,
    onPrimaryContainer = Ink.walnutDeep,
    secondary = Ink.brassDeep,
    onSecondary = Ink.paper,
    tertiary = Ink.leaf,
    onTertiary = Ink.paper,
    background = Ink.parchment,
    onBackground = Ink.ink,
    surface = Ink.paper,
    onSurface = Ink.ink,
    surfaceVariant = Ink.vellum,
    onSurfaceVariant = Ink.inkFaded,
    error = Ink.madder,
    onError = Ink.paper,
    outline = Ink.ruleLine,
)

internal val DarkColors = darkColorScheme(
    primary = Ink.brassBright,
    onPrimary = Ink.nightPaper,
    primaryContainer = Ink.walnutDeep,
    onPrimaryContainer = Ink.boardLight,
    secondary = Ink.brass,
    onSecondary = Ink.nightPaper,
    tertiary = Ink.leafBright,
    onTertiary = Ink.nightPaper,
    background = Ink.nightPaper,
    onBackground = Ink.nightInk,
    surface = Ink.nightCard,
    onSurface = Ink.nightInk,
    surfaceVariant = Ink.nightVellum,
    onSurfaceVariant = Ink.nightInkFaded,
    error = Ink.madderSoft,
    onError = Ink.nightPaper,
    outline = Ink.nightRuleLine,
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

    /** The board turning to the other chair — slow enough to read as one rotation. */
    val tableTurn = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)

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
