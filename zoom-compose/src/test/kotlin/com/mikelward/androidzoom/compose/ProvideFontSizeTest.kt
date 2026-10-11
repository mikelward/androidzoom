package com.mikelward.androidzoom.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mikelward.androidzoom.FontSizeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What [ProvideFontSize] and its helpers hand to the content they wrap. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class ProvideFontSizeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `content is drawn at the system scale times the chosen one, and only the font scale moves`() {
        var systemDensity = 0f
        var systemFontScale = 0f
        var contentDensity = 0f
        var contentFontScale = 0f
        var baseFontScale = 0f
        composeRule.setContent {
            systemDensity = LocalDensity.current.density
            systemFontScale = LocalDensity.current.fontScale
            val state = rememberFontSizeState(FontSizeSettings(scale = 1.5f), {}, {})
            ProvideFontSize(state) {
                contentDensity = LocalDensity.current.density
                contentFontScale = LocalDensity.current.fontScale
                StableInputDensity { baseFontScale = LocalDensity.current.fontScale }
            }
        }
        composeRule.waitForIdle()
        assertEquals(systemDensity, contentDensity, 0.0001f)
        assertEquals(systemFontScale * 1.5f, contentFontScale, 0.0001f)
        assertEquals(systemFontScale, baseFontScale, 0.0001f)
    }

    @Test
    fun `a popup window re-applies the multiplier exactly once`() {
        var windowFontScale = 0f
        var systemFontScale = 0f
        composeRule.setContent {
            systemFontScale = LocalDensity.current.fontScale
            val state = rememberFontSizeState(FontSizeSettings(scale = 1.2f), {}, {})
            ProvideFontSize(state) {
                // A popup's window starts from the unscaled density; stand in
                // for it by stepping back to the base before re-applying.
                StableInputDensity {
                    FontSizeWindow { windowFontScale = LocalDensity.current.fontScale }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(systemFontScale * 1.2f, windowFontScale, 0.0001f)
    }

    @Test
    fun `outside ProvideFontSize the helpers change nothing`() {
        var fontScale = 0f
        var systemFontScale = 0f
        var state: FontSizeState? = FontSizeState(FontSizeSettings(), {}, {})
        composeRule.setContent {
            systemFontScale = LocalDensity.current.fontScale
            state = LocalFontSizeState.current
            FontSizeWindow { fontScale = LocalDensity.current.fontScale }
        }
        composeRule.waitForIdle()
        assertEquals(null, state)
        assertEquals(systemFontScale, fontScale, 0.0001f)
    }

    @Test
    fun `the latest write callback is the one a settled gesture calls`() {
        val first = mutableListOf<Float>()
        val second = mutableListOf<Float>()
        val callback = mutableStateOf<(Float) -> Unit>({ first += it })
        lateinit var state: FontSizeState
        composeRule.setContent {
            state = rememberFontSizeState(FontSizeSettings(scale = 1f), callback.value, {})
        }
        composeRule.waitForIdle()
        val remembered = state
        callback.value = { second += it }
        composeRule.waitForIdle()
        assertSame(remembered, state)

        state.preview(1.3f)
        state.commit(1.3f)

        assertEquals(emptyList<Float>(), first)
        assertEquals(listOf(1.3f), second)
    }
}
