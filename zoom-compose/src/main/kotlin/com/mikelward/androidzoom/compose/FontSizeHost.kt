package com.mikelward.androidzoom.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The text size in force, for a popup that opens its own window.
 *
 * A dialog, a bottom sheet or a dropdown menu is a separate window, and neither
 * the scaled density [ProvideFontSize] sets nor the pinch it hosts crosses a
 * window boundary on its own: text in one came out at the system size however
 * the user had set it, and a pinch made there was never seen. So the state is
 * provided here, and [FontSizeWindow] / [pinchFontSizeHost] re-establish both.
 *
 * Null outside [ProvideFontSize], so a composable used on its own still renders.
 */
val LocalFontSizeState = staticCompositionLocalOf<FontSizeState?> { null }

/**
 * The density before the text-size multiplier, for a control whose own gesture
 * would otherwise be hosted under a density its drag keeps changing (Codex,
 * mikelward/snoozemo#217).
 *
 * Compose resets a pointer-input handler when the density under it changes, so
 * a size slider -- which changes exactly that on every frame of its drag -- died
 * on its first resizing movement. Null outside [ProvideFontSize].
 */
val LocalBaseDensity = staticCompositionLocalOf<Density?> { null }

/**
 * Sizes [content]'s text by [state] and lets a two-finger pinch anywhere in it
 * resize that text.
 *
 * Only `fontScale` is overridden, so text grows while paddings, icons and touch
 * targets keep their layout -- and it multiplies the system's own scale rather
 * than replacing it, so an accessibility setting made in Android is respected.
 *
 * The gesture is hosted **outside** the scaled density, not inside it (Codex,
 * mikelward/snoozemo#217). Compose restarts a pointer-input handler when the
 * density under it changes, and every preview frame of a pinch changes exactly
 * that -- so hosting the gesture under the scaled density killed it on its own
 * first resize. Only `fontScale` differs between the two, so the slop the
 * gesture reads is unchanged either way: it is a distance in fingers, not in
 * text.
 */
@Composable
fun ProvideFontSize(
    state: FontSizeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaled = remember(density, state.scale) {
        Density(density.density, density.fontScale * state.scale)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pinchFontSize(
                enabled = { state.pinchEnabled },
                scale = { state.scale },
                onStart = state::startGesture,
                onPreview = state::preview,
                onSettled = state::commit,
            ),
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaled,
            LocalFontSizeState provides state,
            LocalBaseDensity provides density,
            content = content,
        )
    }
}

/**
 * Hosts [content] at the unscaled density, for a control that resizes text
 * while being dragged -- a size slider. See [LocalBaseDensity].
 *
 * Only `fontScale` differs between the two densities, so a control with no
 * text of its own -- a slider's track and thumb are dp -- looks identical
 * either way.
 */
@Composable
fun StableInputDensity(content: @Composable () -> Unit) {
    val base = LocalBaseDensity.current
    if (base == null) {
        content()
        return
    }
    CompositionLocalProvider(LocalDensity provides base, content = content)
}

/**
 * Re-provides the chosen text size inside a popup's own window.
 *
 * For a slot that only shows text -- a dialog's title, body or buttons.
 * Idempotent: the font scale is derived from [LocalBaseDensity], the density
 * before the multiplier, never from whatever density is current. A Compose
 * `Dialog` resets the density to the system's (verified in
 * `FontSizeWindowTest`), but a popup that inherits the caller's density would
 * otherwise be scaled twice -- 1.2 becoming 1.44 (Codex,
 * mikelward/androidzoom#1). The window's own `density` is kept, so a window on
 * another display still lays out in its own pixels.
 */
@Composable
fun FontSizeWindow(content: @Composable () -> Unit) {
    val state = LocalFontSizeState.current
    val base = LocalBaseDensity.current
    val current = LocalDensity.current
    if (state == null || base == null) {
        content()
        return
    }
    val scaled = remember(current.density, base.fontScale, state.scale) {
        Density(current.density, base.fontScale * state.scale)
    }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

/**
 * The pinch, for a surface in a popup's own window.
 *
 * Apply it to the surface *containing* the text rather than inside it, so one
 * pinch spans a dialog's title, body and buttons instead of dying at the edge
 * of whichever slot it started in. It handles the Initial pass, so it reaches
 * the surface ahead of anything inside it and consumes nothing until the
 * fingers have actually spread -- see [pinchFontSize].
 *
 * Never nest two of these, nor put one inside [ProvideFontSize] in the same
 * window: both would see the same events and apply the same zoom twice. No-op
 * outside [ProvideFontSize].
 */
@Composable
fun Modifier.pinchFontSizeHost(): Modifier {
    val state = LocalFontSizeState.current ?: return this
    return pinchFontSize(
        enabled = { state.pinchEnabled },
        scale = { state.scale },
        onStart = state::startGesture,
        onPreview = state::preview,
        onSettled = state::commit,
    )
}

/**
 * [FontSizeWindow] plus [pinchFontSizeHost], for a whole surface in its own
 * window -- a bottom sheet that is a screen in its own right.
 *
 * The gesture is hosted outside the scaled density for the reason
 * [ProvideFontSize] gives.
 */
@Composable
fun FontSizePinchWindow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.pinchFontSizeHost()) {
        FontSizeWindow(content)
    }
}
