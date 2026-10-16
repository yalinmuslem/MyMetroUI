package com.mymetroui.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.mymetroui.launcher.data.ThemeMode
import com.mymetroui.launcher.data.ThemeSettings
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeMappingTest {
    @get:Rule val rule = createComposeRule()

    private data class Seen(val background: Color, val content: Color, val primary: Color)

    private fun seen(settings: ThemeSettings): Seen {
        var out: Seen? = null
        rule.setContent {
            MyMetroUITheme(settings) {
                out = Seen(MaterialTheme.colorScheme.background, LocalContentColor.current, MaterialTheme.colorScheme.primary)
            }
        }
        rule.waitForIdle()
        return out!!
    }

    @Test fun dark_background_isBlack_andContentColorIsWhite() {
        val s = seen(ThemeSettings(mode = ThemeMode.DARK))
        assertEquals(Color.Black, s.background)
        assertEquals(Color.White, s.content)
    }

    @Test fun light_background_isWhite_andContentColorIsBlack() {
        val s = seen(ThemeSettings(mode = ThemeMode.LIGHT))
        assertEquals(Color.White, s.background)
        assertEquals(Color.Black, s.content)
    }

    @Test fun primary_isSelectedAccent() {
        assertEquals(Color(0xFFE51400), seen(ThemeSettings(accentId = "red")).primary)
    }

    @Test fun unknownAccent_usesDefaultPrimary() {
        assertEquals(Color(0xFF1BA1E2), seen(ThemeSettings(accentId = "nope")).primary)
    }
}
