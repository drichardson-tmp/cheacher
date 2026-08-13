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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cheacher's visual identity: **warm wood and ink**.
 *
 * The app should feel like a well-loved chess study — walnut boards, cream paper,
 * ink annotations, one flash of brass. Opening names are typography events (big serif),
 * moves are ink on paper (monospace), and everything else stays out of the way.
 */
object Ink {
    // Paper & wood.
    val parchment = Color(0xFFF6EFE3)
    val paper = Color(0xFFFDF8EE)
    val walnut = Color(0xFF6B4A31)
    val walnutDeep = Color(0xFF4A3122)
    val boardLight = Color(0xFFEED9B7)
    val boardDark = Color(0xFFA57552)

    // Ink & accents.
    val ink = Color(0xFF2B2119)
    val inkFaded = Color(0xFF7A6A5A)
    val brass = Color(0xFFB8863B)
    val brassBright = Color(0xFFD9A84E)

    // Verdicts.
    val leaf = Color(0xFF4E7C4A)
    val leafBright = Color(0xFF6FA36A)
    val madder = Color(0xFFA83E32)
    val madderSoft = Color(0xFFD8887F)

    // Board annotations.
    val lastMoveGlow = Color(0x66D9A84E)
    val selectedGlow = Color(0x8CB8863B)
    val targetDot = Color(0x59332211)
    val checkGlow = Color(0x80A83E32)

    // Night study: same wood, lamp off.
    val nightPaper = Color(0xFF201A14)
    val nightCard = Color(0xFF2B231B)
    val nightInk = Color(0xFFEADDC8)
}

private val LightColors = lightColorScheme(
    primary = Ink.walnut,
    onPrimary = Ink.paper,
    primaryContainer = Ink.boardLight,
    onPrimaryContainer = Ink.walnutDeep,
    secondary = Ink.brass,
    onSecondary = Ink.ink,
    tertiary = Ink.leaf,
    onTertiary = Ink.paper,
    background = Ink.parchment,
    onBackground = Ink.ink,
    surface = Ink.paper,
    onSurface = Ink.ink,
    surfaceVariant = Color(0xFFEFE4D0),
    onSurfaceVariant = Ink.inkFaded,
    error = Ink.madder,
    onError = Ink.paper,
    outline = Color(0xFFC9B99F),
)

private val DarkColors = darkColorScheme(
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
    surfaceVariant = Color(0xFF3A2F24),
    onSurfaceVariant = Color(0xFFB4A48E),
    error = Ink.madderSoft,
    onError = Ink.nightPaper,
    outline = Color(0xFF5A4C3B),
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

@Composable
fun CheacherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CheacherTypography,
        shapes = CheacherShapes,
        content = content,
    )
}
