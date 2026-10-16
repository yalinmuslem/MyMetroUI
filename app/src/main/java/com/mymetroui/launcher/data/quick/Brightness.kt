package com.mymetroui.launcher.data.quick

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlin.math.roundToInt

interface Brightness {
    fun canWrite(): Boolean
    fun read(): Float
    fun write(fraction: Float)
}

/** 0..1 to the system's 1..255; never 0 so the screen cannot be turned fully dark. */
fun fractionToSystem(f: Float): Int = (f.coerceIn(0f, 1f) * 255).roundToInt().coerceAtLeast(1)

fun systemToFraction(v: Int): Float = v.coerceIn(0, 255) / 255f

fun writeSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))

class SystemBrightness(private val context: Context) : Brightness {
    override fun canWrite() = Settings.System.canWrite(context)

    override fun read(): Float =
        systemToFraction(Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128))

    override fun write(fraction: Float) {
        if (!canWrite()) return
        try {
            Settings.System.putInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, fractionToSystem(fraction))
        } catch (_: SecurityException) {
            // permission revoked between the check and the write
        }
    }
}
