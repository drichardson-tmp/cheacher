package com.cheacher.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The palette is verified content, exactly like the repertoires: every text-bearing
 * pairing in both schemes must clear its WCAG threshold, or the build says so.
 *
 * Thresholds: 4.5:1 for body-size text roles, 3:1 for large-text and UI roles.
 * Verdicts additionally carry a deuteranopia guard — correct and miss must differ in
 * *relative luminance* by a clear margin, so the scoreboard still reads when green and
 * red collapse to the same hue.
 *
 * Pieces are the one structured exception: cream on a cream square can never hit 3:1,
 * which is why every physical chess set gives its pieces an edge. Each side's *fill*
 * must clear 3:1 on the opposite square, and on its own same-tone square the *edge*
 * (day shadow, night rim light) must clear it instead.
 */
class PaletteContrastTest {

    // ------------------------------------------------------------------ WCAG math

    /** WCAG 2.x relative luminance over sRGB channels. */
    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    /** WCAG contrast ratio, always >= 1. */
    private fun contrast(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertContrast(threshold: Double, a: Color, b: Color, label: String) {
        val ratio = contrast(a, b)
        assertTrue(
            ratio >= threshold,
            "$label: contrast ${ratio.short()} < $threshold",
        )
    }

    private fun Double.short() = ((this * 100).toInt() / 100.0).toString()

    // ------------------------------------------------------------- scheme sweeps

    private data class Scheme(
        val name: String,
        val m3: ColorScheme,
        val app: CheacherColors,
    )

    private val schemes = listOf(
        Scheme("day", LightColors, DayCheacherColors),
        Scheme("night", DarkColors, NightCheacherColors),
    )

    @Test
    fun bodyTextRolesMeet4to5() {
        for ((name, m3, app) in schemes) {
            assertContrast(4.5, m3.onBackground, m3.background, "$name onBackground/background")
            assertContrast(4.5, m3.onSurface, m3.surface, "$name onSurface/surface")
            assertContrast(4.5, m3.onSurfaceVariant, m3.surfaceVariant, "$name onSurfaceVariant/surfaceVariant")
            assertContrast(4.5, m3.onPrimary, m3.primary, "$name onPrimary/primary")
            assertContrast(4.5, m3.onPrimaryContainer, m3.primaryContainer, "$name onPrimaryContainer/primaryContainer")
            assertContrast(4.5, m3.onSecondary, m3.secondary, "$name onSecondary/secondary")
            assertContrast(4.5, m3.onTertiary, m3.tertiary, "$name onTertiary/tertiary")
            assertContrast(4.5, m3.onError, m3.error, "$name onError/error")
            assertContrast(4.5, m3.error, m3.background, "$name error-as-text/background")
            // Tertiary and the review wash are used as body-size text on both fields.
            assertContrast(4.5, m3.tertiary, m3.background, "$name tertiary-as-text/background")
            assertContrast(4.5, m3.tertiary, m3.surface, "$name tertiary-as-text/surface")
            assertContrast(4.5, app.reviewTint, m3.background, "$name reviewTint/background")
            assertContrast(4.5, app.reviewTint, m3.surface, "$name reviewTint/surface")
        }
    }

    @Test
    fun largeTextAndUiRolesMeet3to1() {
        for ((name, m3, app) in schemes) {
            assertContrast(3.0, m3.secondary, m3.surface, "$name secondary-as-label/surface")
            assertContrast(3.0, m3.secondary, m3.background, "$name secondary-as-label/background")
            assertContrast(3.0, m3.outline, m3.surface, "$name outline/surface")
            assertContrast(3.0, app.streakAccent, m3.surface, "$name streakAccent/surface")
            assertContrast(3.0, app.boardLight, app.boardDark, "$name board square pair")
        }
    }

    @Test
    fun piecesReadOnBothSquares() {
        for ((name, _, app) in schemes) {
            // Fill carries the piece on the opposite-tone square...
            assertContrast(3.0, app.pieceCream, app.boardDark, "$name cream piece fill/dark square")
            assertContrast(3.0, app.pieceInk, app.boardLight, "$name ink piece fill/light square")
            // ...and the edge carries it on its own same-tone square.
            assertContrast(3.0, app.pieceCreamEdge, app.boardLight, "$name cream piece edge/light square")
            val inkOnDark = maxOf(
                contrast(app.pieceInk, app.boardDark),
                contrast(app.pieceInkEdge, app.boardDark),
            )
            assertTrue(
                inkOnDark >= 3.0,
                "$name ink piece (fill or edge)/dark square: ${inkOnDark.short()} < 3.0",
            )
        }
    }

    @Test
    fun treeNodeTextReadsOnEveryFill() {
        for ((name, _, app) in schemes) {
            assertContrast(3.0, app.treeOpenText, app.treeUnvisited, "$name tree text/unvisited")
            assertContrast(3.0, app.treeOpenText, app.treeInProgress, "$name tree text/inProgress")
            assertContrast(3.0, app.onVerdict, app.treeCompleted, "$name tree text/completed")
            assertContrast(3.0, app.onVerdict, app.treeFailed, "$name tree text/failed")
            assertContrast(3.0, app.onVerdict, app.verdictCorrect, "$name onVerdict/verdictCorrect")
            assertContrast(3.0, app.onVerdict, app.verdictMiss, "$name onVerdict/verdictMiss")
        }
    }

    /**
     * The accent guard. Ember and crimson are the app's only two warm families and they
     * mean opposite things — a spark worth chasing versus a move you got wrong. When they
     * sat 8 degrees apart, every ember ring read as a stray error colour. Holding them a
     * full step apart on the wheel is what makes ember legible as an accent at all.
     */
    @Test
    fun emberAndCrimsonStayApart() {
        val ember = listOf("ember" to Lagoon.ember, "emberDeep" to Lagoon.emberDeep, "emberBright" to Lagoon.emberBright)
        val crimson = listOf("crimson" to Lagoon.crimson, "crimsonFill" to Lagoon.crimsonFill, "crimsonSoft" to Lagoon.crimsonSoft)
        for ((warmName, warm) in ember) {
            // Ember must actually be amber, not a red wearing an amber label.
            val h = hue(warm)
            assertTrue(h in 22.0..45.0, "$warmName hue ${h.short()} is outside the amber band 22-45")
            for ((coolName, cool) in crimson) {
                val gap = hueGap(warm, cool)
                assertTrue(gap >= 20.0, "$warmName vs $coolName: hue gap ${gap.short()} < 20")
            }
        }
    }

    /** HSL hue in degrees. */
    private fun hue(color: Color): Double {
        val r = color.red
        val g = color.green
        val b = color.blue
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val d = hi - lo
        if (d == 0f) return 0.0
        val h = when (hi) {
            r -> ((g - b) / d + if (g < b) 6f else 0f)
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } * 60f
        return h.toDouble()
    }

    /** Shortest distance between two hues on the 360-degree wheel. */
    private fun hueGap(a: Color, b: Color): Double {
        val raw = abs(hue(a) - hue(b))
        return min(raw, 360.0 - raw)
    }

    /**
     * The deuteranopia guard: with red and green hues removed, the verdicts must still
     * differ by value. 0.10 relative luminance is roughly the gap between two adjacent
     * steps of a Munsell value scale — comfortably visible as light-vs-dark.
     */
    @Test
    fun verdictsDifferByValueNotOnlyHue() {
        for ((name, _, app) in schemes) {
            val margin = abs(
                relativeLuminance(app.verdictCorrect) - relativeLuminance(app.verdictMiss),
            )
            assertTrue(
                margin >= 0.10,
                "$name verdict luminance margin ${margin.short()} < 0.10",
            )
            // And the polarity is fixed: correct is the lighter of the two, both schemes.
            assertTrue(
                relativeLuminance(app.verdictCorrect) > relativeLuminance(app.verdictMiss),
                "$name verdictCorrect must be lighter than verdictMiss",
            )
        }
    }
}
