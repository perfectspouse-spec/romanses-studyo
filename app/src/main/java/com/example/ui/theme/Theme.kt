package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.model.EmotionTone
import com.example.ui.components.EmotionalPaletteCatalog

private val BaseDarkColorScheme = darkColorScheme(
    primary = AmberSpotlight,
    onPrimary = Color(0xFF231304),
    primaryContainer = Color(0xFF4F3116),
    onPrimaryContainer = Color(0xFFFFDDBE),
    secondary = CoralDrama,
    onSecondary = Color(0xFF2B0E06),
    secondaryContainer = Color(0xFF542317),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = MintNarrator,
    onTertiary = Color(0xFF042622),
    tertiaryContainer = Color(0xFF124640),
    onTertiaryContainer = Color(0xFFB4F4EA),
    background = MidnightPlum,
    onBackground = CreamScriptText,
    surface = DeepStageSurface,
    onSurface = CreamScriptText,
    surfaceVariant = VelvetCardSurface,
    onSurfaceVariant = SoftParchmentSubtext,
    outline = MutedStageBorder,
    error = Color(0xFFFFB4AB)
)

private val BaseLightColorScheme = lightColorScheme(
    primary = SepiaAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF361700),
    secondary = BurgundySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DC),
    onSecondaryContainer = Color(0xFF390711),
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB0F1E6),
    onTertiaryContainer = Color(0xFF00201C),
    background = ParchmentBackground,
    onBackground = InkPlumText,
    surface = WarmPaperSurface,
    onSurface = InkPlumText,
    surfaceVariant = Color(0xFFE9DFCF),
    onSurfaceVariant = Color(0xFF4D4357),
    outline = Color(0xFF81758C)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    activeEmotion: EmotionTone? = null,
    dynamicEmotionPaletteEnabled: Boolean = true,
    paletteIntensity: Float = 0.85f,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) BaseDarkColorScheme else BaseLightColorScheme
    val clampedIntensity = paletteIntensity.coerceIn(0.25f, 1.0f)

    val emotionSpec = remember(activeEmotion) {
        activeEmotion?.let { EmotionalPaletteCatalog.specForEmotion(it) }
    }

    val targetBackground = if (dynamicEmotionPaletteEnabled && emotionSpec != null) {
        val emoBg = if (darkTheme) emotionSpec.darkMidBackground else emotionSpec.lightMidBackground
        lerp(baseScheme.background, emoBg, clampedIntensity)
    } else {
        baseScheme.background
    }

    val targetSurface = if (dynamicEmotionPaletteEnabled && emotionSpec != null) {
        val emoSurf = if (darkTheme) emotionSpec.darkSurface else emotionSpec.lightSurface
        lerp(baseScheme.surface, emoSurf, clampedIntensity * 0.90f)
    } else {
        baseScheme.surface
    }

    val targetSurfaceVariant = if (dynamicEmotionPaletteEnabled && emotionSpec != null) {
        val emoSurfVar = if (darkTheme) emotionSpec.darkSurfaceVariant else emotionSpec.lightSurfaceVariant
        lerp(baseScheme.surfaceVariant, emoSurfVar, clampedIntensity * 0.92f)
    } else {
        baseScheme.surfaceVariant
    }

    val targetPrimary = if (dynamicEmotionPaletteEnabled && emotionSpec != null) {
        lerp(baseScheme.primary, emotionSpec.vibrantAccent, (clampedIntensity * 0.72f).coerceIn(0f, 1f))
    } else {
        baseScheme.primary
    }

    val animSpec = tween<Color>(durationMillis = 850, easing = FastOutSlowInEasing)
    val animatedBackground by animateColorAsState(targetBackground, animSpec, label = "theme_bg")
    val animatedSurface by animateColorAsState(targetSurface, animSpec, label = "theme_surface")
    val animatedSurfaceVariant by animateColorAsState(targetSurfaceVariant, animSpec, label = "theme_surf_var")
    val animatedPrimary by animateColorAsState(targetPrimary, animSpec, label = "theme_primary")

    val dynamicColorScheme = baseScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}
