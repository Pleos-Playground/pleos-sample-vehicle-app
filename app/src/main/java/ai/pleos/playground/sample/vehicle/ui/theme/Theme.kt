package ai.pleos.playground.sample.vehicle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = Blue,
    primaryContainer = BlueContainer,
    onPrimaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    tertiary = Green,
    error = Red
)

@Composable
fun VehicleSampleAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = AppTypography,
        content = content
    )
}
