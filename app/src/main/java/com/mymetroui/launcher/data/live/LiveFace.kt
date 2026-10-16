package com.mymetroui.launcher.data.live

sealed interface LiveFace {
    data class Text(val title: String, val lines: List<String> = emptyList()) : LiveFace
    data class Photo(val uri: String) : LiveFace
    object NeedsPermission : LiveFace
}
