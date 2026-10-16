package ai.pleos.playground.sample.vehicle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

val AppTypography = DefaultTypography.copy(
    headlineLarge = DefaultTypography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 46.sp),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontSize = 34.sp, lineHeight = 42.sp),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 38.sp),
    titleLarge = DefaultTypography.titleLarge.copy(fontSize = 26.sp, lineHeight = 34.sp),
    titleMedium = DefaultTypography.titleMedium.copy(fontSize = 22.sp, lineHeight = 30.sp),
    titleSmall = DefaultTypography.titleSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
    labelMedium = DefaultTypography.labelMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontSize = 18.sp, lineHeight = 24.sp)
)
