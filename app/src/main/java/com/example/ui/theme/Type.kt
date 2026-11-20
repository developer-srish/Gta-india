package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BebasFontFamily = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal))
val JakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

val GrandTheftTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = BebasFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 54.sp,
        lineHeight = 52.sp,
        letterSpacing = 2.sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = BebasFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.5.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = BebasFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.2.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = JakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = JetBrainsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = JetBrainsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
      ),
  )
