package dev.haxzie.relaybird.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Monochrome and always dark: black canvas, near-black cards, white primary actions.
 *
 * Deliberately not dynamic colour — the point of this palette is that it is the same on every
 * phone, and a wallpaper-derived accent would undo it.
 */
object RelayBirdColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF141414)
    val SurfaceRaised = Color(0xFF1F1F1F)
    val Border = Color(0xFF2E2E2E)
    val White = Color(0xFFFFFFFF)
    val Muted = Color(0xFFA6A6A6)
    val Red = Color(0xFFFF4D4F)
    val Green = Color(0xFF3FCF7F)
}

private val colors = darkColorScheme(
    primary = RelayBirdColors.White,
    onPrimary = RelayBirdColors.Black,
    secondary = RelayBirdColors.White,
    onSecondary = RelayBirdColors.Black,
    background = RelayBirdColors.Black,
    onBackground = RelayBirdColors.White,
    surface = RelayBirdColors.Surface,
    onSurface = RelayBirdColors.White,
    surfaceVariant = RelayBirdColors.SurfaceRaised,
    onSurfaceVariant = RelayBirdColors.Muted,
    outline = RelayBirdColors.Border,
    outlineVariant = RelayBirdColors.Border,
    error = RelayBirdColors.Red,
    onError = RelayBirdColors.Black,
)

/** Tight, heavy headings over plain body text — the contrast is the whole look. */
private val typography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    )
}

/** Section headers: small, spaced, uppercase. */
val SectionLabelStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
)

object Dimens {
    val screenPadding = 20.dp
    val cardRadius = 14.dp
    val buttonRadius = 10.dp
    val buttonHeight = 56.dp
}

@Composable
fun RelayBirdTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
