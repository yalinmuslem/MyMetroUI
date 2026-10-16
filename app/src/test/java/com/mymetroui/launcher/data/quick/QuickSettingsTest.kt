package com.mymetroui.launcher.data.quick

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuickSettingsTest {
    @Test fun intentActionsPerKind() {
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, quickIntent(QuickKind.BLUETOOTH).action)
        assertEquals(Settings.ACTION_AIRPLANE_MODE_SETTINGS, quickIntent(QuickKind.AIRPLANE).action)
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, quickIntent(QuickKind.LOCATION).action)
    }

    @Test fun wifi_usesPanelFromApi29_andSettingsBefore() {
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, quickIntent(QuickKind.WIFI, sdk = 29).action)
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, quickIntent(QuickKind.WIFI, sdk = 35).action)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, quickIntent(QuickKind.WIFI, sdk = 28).action)
    }
}
