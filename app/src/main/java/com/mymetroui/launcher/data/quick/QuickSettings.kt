package com.mymetroui.launcher.data.quick

import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Android does not let apps toggle these directly; the buttons open the matching settings screen. */
enum class QuickKind { WIFI, BLUETOOTH, AIRPLANE, LOCATION }

fun quickIntent(kind: QuickKind, sdk: Int = Build.VERSION.SDK_INT): Intent = Intent(
    when (kind) {
        QuickKind.WIFI ->
            if (sdk >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS
        QuickKind.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        QuickKind.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
        QuickKind.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
    },
)
