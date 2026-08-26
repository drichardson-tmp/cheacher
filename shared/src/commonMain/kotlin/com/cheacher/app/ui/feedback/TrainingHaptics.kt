package com.cheacher.app.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The three tactile beats shared by every training mode. */
enum class TrainingHaptic {
    Correct,
    Wrong,
    LineComplete,
}

/**
 * Returns a stable feedback callback. A new result cancels any tail left by the previous
 * one, so quick play feels crisp rather than stacking vibrations into a buzz.
 */
@Composable
fun rememberTrainingHaptics(enabled: Boolean): (TrainingHaptic) -> Unit {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val performer = remember(haptics, scope) { TrainingHapticPerformer(haptics, scope) }

    LaunchedEffect(enabled) {
        if (!enabled) performer.cancel()
    }

    return remember(enabled, performer) {
        { feedback -> if (enabled) performer.play(feedback) }
    }
}

private class TrainingHapticPerformer(
    private val haptics: HapticFeedback,
    private val scope: CoroutineScope,
) {
    private var active: Job? = null

    fun play(feedback: TrainingHaptic) {
        active?.cancel()
        active = scope.launch {
            when (feedback) {
                // A selection tick maps to the smallest native feedback on both platforms.
                TrainingHaptic.Correct -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)

                // Two light impacts make a miss unmistakable without feeling punitive.
                TrainingHaptic.Wrong -> {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    delay(60)
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                }

                // Native success feedback has its own rounded, resolved cadence.
                TrainingHaptic.LineComplete ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }

    fun cancel() {
        active?.cancel()
        active = null
    }
}
