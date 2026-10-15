package com.mymetroui.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mymetroui.launcher.R

val OpenSans = FontFamily(
    Font(R.font.open_sans_light, FontWeight.Light),
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semibold, FontWeight.SemiBold),
)

val MetroTypography = Typography(
    headlineLarge = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Light, fontSize = 40.sp),
    titleMedium = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = OpenSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)
