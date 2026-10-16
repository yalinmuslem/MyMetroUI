package com.mymetroui.launcher.data

import com.mymetroui.launcher.ui.theme.DefaultAccent

enum class ThemeMode { DARK, LIGHT }

data class ThemeSettings(val accentId: String = DefaultAccent.id, val mode: ThemeMode = ThemeMode.DARK)
