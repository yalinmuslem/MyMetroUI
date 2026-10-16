package com.mymetroui.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WelcomePrefsTest {
    private lateinit var prefs: SharedPreferences

    @Before fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("welcome-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test fun defaultsToEnabled() = assertTrue(WelcomePrefs(prefs).enabled.value)

    @Test fun setEnabled_updatesAndPersists() {
        val p = WelcomePrefs(prefs)
        p.setEnabled(false)
        assertFalse(p.enabled.value)
        assertFalse(WelcomePrefs(prefs).enabled.value)
    }
}
