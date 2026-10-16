package com.mymetroui.launcher.data.quick

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrightnessTest {
    @Test fun fractionToSystem_clampsAndNeverZero() {
        assertEquals(1, fractionToSystem(-1f))
        assertEquals(1, fractionToSystem(0f))
        assertEquals(128, fractionToSystem(0.5f))
        assertEquals(255, fractionToSystem(1f))
        assertEquals(255, fractionToSystem(2f))
    }

    @Test fun systemToFraction_clamps() {
        assertEquals(0f, systemToFraction(0), 0f)
        assertEquals(1f, systemToFraction(255), 0f)
        assertEquals(1f, systemToFraction(999), 0f)
        assertEquals(0f, systemToFraction(-5), 0f)
    }

    @Test fun writeSettingsIntent_targetsPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = writeSettingsIntent(context)
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }
}
