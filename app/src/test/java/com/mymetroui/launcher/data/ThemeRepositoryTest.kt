package com.mymetroui.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.mymetroui.launcher.ui.theme.Accents
import com.mymetroui.launcher.ui.theme.DefaultAccent
import com.mymetroui.launcher.ui.theme.accentFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeRepositoryTest {
    private lateinit var prefs: SharedPreferences

    @Before fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("theme-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test fun defaults_whenEmpty() {
        val s = ThemeRepository(prefs).settings.value
        assertEquals("cyan", s.accentId)
        assertEquals(ThemeMode.DARK, s.mode)
    }

    @Test fun setAccentAndMode_persistAcrossInstances() {
        ThemeRepository(prefs).apply { setAccent("red"); setMode(ThemeMode.LIGHT) }
        val s = ThemeRepository(prefs).settings.value
        assertEquals("red", s.accentId)
        assertEquals(ThemeMode.LIGHT, s.mode)
    }

    @Test fun unknownAccentId_fallsBackToDefaultAccent() {
        assertSame(DefaultAccent, accentFor("nope"))
        prefs.edit().putString("accent", "nope").commit()
        assertEquals("cyan", ThemeRepository(prefs).settings.value.accentId)
        assertEquals("nope", prefs.getString("accent", null)) // stored id is left alone
    }

    @Test fun invalidMode_fallsBackToDark() {
        prefs.edit().putString("mode", "SEPIA").commit()
        assertEquals(ThemeMode.DARK, ThemeRepository(prefs).settings.value.mode)
    }

    @Test fun wrongTypeInPrefs_fallsBackToDefaults() {
        prefs.edit().putInt("accent", 7).putInt("mode", 3).commit()
        val s = ThemeRepository(prefs).settings.value
        assertEquals("cyan", s.accentId)
        assertEquals(ThemeMode.DARK, s.mode)
    }

    @Test fun setAccent_unknownId_isIgnored() {
        val repo = ThemeRepository(prefs)
        repo.setAccent("nope")
        assertEquals("cyan", repo.settings.value.accentId)
        assertEquals(null, prefs.getString("accent", null))
    }

    @Test fun paletteIdsAreUnique() {
        assertEquals(20, Accents.size)
        assertEquals(Accents.size, Accents.map { it.id }.toSet().size)
    }
}
