package com.mymetroui.launcher.ui.theme

import androidx.compose.ui.graphics.Color

data class AccentColor(val id: String, val name: String, val color: Color)

private fun accent(id: String, hex: Long) = AccentColor(id, id.replaceFirstChar { it.uppercase() }, Color(0xFF000000 or hex))

val Accents: List<AccentColor> = listOf(
    accent("lime", 0xA4C400), accent("green", 0x60A917), accent("emerald", 0x008A00), accent("teal", 0x00ABA9),
    accent("cyan", 0x1BA1E2), accent("cobalt", 0x0050EF), accent("indigo", 0x6A00FF), accent("violet", 0xAA00FF),
    accent("pink", 0xF472D0), accent("magenta", 0xD80073), accent("crimson", 0xA20025), accent("red", 0xE51400),
    accent("orange", 0xFA6800), accent("amber", 0xF0A30A), accent("yellow", 0xE3C800), accent("brown", 0x825A2C),
    accent("olive", 0x6D8764), accent("steel", 0x647687), accent("mauve", 0x76608A), accent("taupe", 0x87794E),
)

val DefaultAccent: AccentColor = Accents.first { it.id == "cyan" }

fun accentFor(id: String): AccentColor = Accents.firstOrNull { it.id == id } ?: DefaultAccent
