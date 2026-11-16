package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val FrauncesFontFamily = FontFamily(
  Font(R.font.fraunces, FontWeight.Normal),
  Font(R.font.fraunces, FontWeight.SemiBold),
  Font(R.font.fraunces, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val VazirmatnFontFamily = FontFamily(
  Font(R.font.vazirmatn, FontWeight.Normal),
  Font(R.font.vazirmatn, FontWeight.Medium),
  Font(R.font.vazirmatn, FontWeight.Bold)
)

val AppTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = FrauncesFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 32.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 28.sp
  ),
  titleLarge = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelMedium = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
  )
)
