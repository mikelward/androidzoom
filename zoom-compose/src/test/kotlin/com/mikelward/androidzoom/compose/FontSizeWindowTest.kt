package com.mikelward.androidzoom.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.window.Dialog
import com.mikelward.androidzoom.FontSizeSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [FontSizeWindow] in a real dialog window, and where no window boundary resets the density. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FontSizeWindowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a dialog resets the density, and FontSizeWindow restores the chosen size`() {
        var system = 0f
        var bareDialog = 0f
        var wrappedDialog = 0f
        composeRule.setContent {
            system = LocalDensity.current.fontScale
            val state = rememberFontSizeState(FontSizeSettings(scale = 1.5f), {}, {})
            ProvideFontSize(state) {
                Dialog(onDismissRequest = {}) {
                    bareDialog = LocalDensity.current.fontScale
                    FontSizeWindow { wrappedDialog = LocalDensity.current.fontScale }
                }
            }
        }
        composeRule.waitForIdle()
        // The premise the helper exists for: a Dialog does not inherit the size.
        assertEquals(system, bareDialog, 0.0001f)
        assertEquals(system * 1.5f, wrappedDialog, 0.0001f)
    }

    @Test
    fun `where the density is inherited, the multiplier is not applied twice`() {
        var system = 0f
        var inherited = 0f
        composeRule.setContent {
            system = LocalDensity.current.fontScale
            val state = rememberFontSizeState(FontSizeSettings(scale = 1.2f), {}, {})
            ProvideFontSize(state) {
                FontSizeWindow {
                    FontSizeWindow { inherited = LocalDensity.current.fontScale }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(system * 1.2f, inherited, 0.0001f)
    }
}
