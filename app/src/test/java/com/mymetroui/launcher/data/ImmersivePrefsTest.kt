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
class ImmersivePrefsTest {
    private lateinit var prefs: SharedPreferences

    @Before fun setUp() {
        prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("immersive-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test fun defaultsToDisabled() = assertFalse(ImmersivePrefs(prefs).enabled.value)

    @Test fun setEnabled_updatesAndPersists() {
        val p = ImmersivePrefs(prefs)
        p.setEnabled(true)
        assertTrue(p.enabled.value)
        assertTrue(ImmersivePrefs(prefs).enabled.value)
    }
}
