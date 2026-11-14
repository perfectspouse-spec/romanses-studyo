package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.EmotionTone
import kotlin.math.cos
import kotlin.math.sin

enum class AmbientLightingMode(val labelTr: String, val descriptionTr: String) {
    DYNAMIC_AURA(
        labelTr = "Dinamik Sahne Aurası",
        descriptionTr = "Baskın duyguya göre çok katmanlı arka plan geçişi ve spot ışığı"
    ),
    DEEP_IMMERSION(
        labelTr = "Tam Ekran Yoğun Atmosfer",
        descriptionTr = "Tüm ekran yüzeylerini duygunun renk paletiyle derinlemesine boyar"
    ),
    SUBTLE_HALO(
        labelTr = "Yumuşak Kenar Işıltısı",
        descriptionTr = "Klasik okuma zeminini koruyarak duygusal hale efekti uygular"
    )
}

data class EmotionPaletteSpec(
    val emotion: EmotionTone,
    val paletteTitleTr: String,
    val moodDescriptionTr: String,
    // Dark theme palette
    val darkTopGlow: Color,
    val darkMidBackground: Color,
    val darkBottomDeep: Color,
    val darkSurface: Color,
    val darkSurfaceVariant: Color,
    // Light theme palette
    val lightTopGlow: Color,
    val lightMidBackground: Color,
    val lightBottomDeep: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color,
    // Shared vibrant accent & swatch hex codes for UI display
    val vibrantAccent: Color,
    val secondaryGlow: Color,
    val swatchHexCodes: List<Pair<String, Long>>
)

object EmotionalPaletteCatalog {

    fun specForEmotion(emotion: EmotionTone): EmotionPaletteSpec {
        return when (emotion) {
            EmotionTone.CALM -> EmotionPaletteSpec(
                emotion = EmotionTone.CALM,
                paletteTitleTr = "Zümrüt Sükûnet & Deniz Esintisi",
                moodDescriptionTr = "Dengeli anlatım için dingin turkuaz, derin çam yeşili ve yumuşak su tonları",
                darkTopGlow = Color(0xFF133836),
                darkMidBackground = Color(0xFF0E2226),
                darkBottomDeep = Color(0xFF091519),
                darkSurface = Color(0xFF142C30),
                darkSurfaceVariant = Color(0xFF1C3B40),
                lightTopGlow = Color(0xFFD8F3EE),
                lightMidBackground = Color(0xFFEEF8F6),
                lightBottomDeep = Color(0xFFE0F0EC),
                lightSurface = Color(0xFFF5FBF9),
                lightSurfaceVariant = Color(0xFFDCEEE9),
                vibrantAccent = Color(0xFF2A9D8F),
                secondaryGlow = Color(0xFF52B788),
                swatchHexCodes = listOf(
                    "#133836" to 0xFF133836,
                    "#1C3B40" to 0xFF1C3B40,
                    "#2A9D8F" to 0xFF2A9D8F,
                    "#52B788" to 0xFF52B788
                )
            )

            EmotionTone.JOYFUL -> EmotionPaletteSpec(
                emotion = EmotionTone.JOYFUL,
                paletteTitleTr = "Altın Günışığı & Sıcak Kehribar",
                moodDescriptionTr = "Neşeli ve coşkulu diyaloglar için parlak altın, bal kehribarı ve sıcak şeftali tonları",
                darkTopGlow = Color(0xFF463214),
                darkMidBackground = Color(0xFF2B1F11),
                darkBottomDeep = Color(0xFF1B130B),
                darkSurface = Color(0xFF352615),
                darkSurfaceVariant = Color(0xFF48341C),
                lightTopGlow = Color(0xFFFFF1C9),
                lightMidBackground = Color(0xFFFFF8E6),
                lightBottomDeep = Color(0xFFFAEBD2),
                lightSurface = Color(0xFFFFFBF0),
                lightSurfaceVariant = Color(0xFFF6E5C4),
                vibrantAccent = Color(0xFFE9C46A),
                secondaryGlow = Color(0xFFF4A261),
                swatchHexCodes = listOf(
                    "#463214" to 0xFF463214,
                    "#48341C" to 0xFF48341C,
                    "#E9C46A" to 0xFFE9C46A,
                    "#F4A261" to 0xFFF4A261
                )
            )

            EmotionTone.MELANCHOLIC -> EmotionPaletteSpec(
                emotion = EmotionTone.MELANCHOLIC,
                paletteTitleTr = "Gece Mavisi & Puslu Yağmur",
                moodDescriptionTr = "Hüzünlü ve kırgın sahneler için derin çivit mavisi, arduvaz grisi ve gümüşi sis tonları",
                darkTopGlow = Color(0xFF1A2C42),
                darkMidBackground = Color(0xFF111B2B),
                darkBottomDeep = Color(0xFF0A101A),
                darkSurface = Color(0xFF172538),
                darkSurfaceVariant = Color(0xFF21344D),
                lightTopGlow = Color(0xFFD9E6F5),
                lightMidBackground = Color(0xFFEDF3FA),
                lightBottomDeep = Color(0xFFDFEAF5),
                lightSurface = Color(0xFFF4F8FC),
                lightSurfaceVariant = Color(0xFFD8E4F2),
                vibrantAccent = Color(0xFF7EA8BE),
                secondaryGlow = Color(0xFF5C859E),
                swatchHexCodes = listOf(
                    "#1A2C42" to 0xFF1A2C42,
                    "#21344D" to 0xFF21344D,
                    "#7EA8BE" to 0xFF7EA8BE,
                    "#5C859E" to 0xFF5C859E
                )
            )

            EmotionTone.ANGRY -> EmotionPaletteSpec(
                emotion = EmotionTone.ANGRY,
                paletteTitleTr = "Kızıl Kor & Alev Gerilimi",
                moodDescriptionTr = "Öfkeli ve çatışmalı replikler için koyu yakut, kor kırmızısı ve yanık bakır tonları",
                darkTopGlow = Color(0xFF4A1815),
                darkMidBackground = Color(0xFF2B0E0D),
                darkBottomDeep = Color(0xFF190808),
                darkSurface = Color(0xFF381312),
                darkSurfaceVariant = Color(0xFF4D1B19),
                lightTopGlow = Color(0xFFFFD8D4),
                lightMidBackground = Color(0xFFFFEFEA),
                lightBottomDeep = Color(0xFFFCE2DC),
                lightSurface = Color(0xFFFFF6F4),
                lightSurfaceVariant = Color(0xFFF7D7D0),
                vibrantAccent = Color(0xFFE76F51),
                secondaryGlow = Color(0xFFD62828),
                swatchHexCodes = listOf(
                    "#4A1815" to 0xFF4A1815,
                    "#4D1B19" to 0xFF4D1B19,
                    "#E76F51" to 0xFFE76F51,
                    "#D62828" to 0xFFD62828
                )
            )

            EmotionTone.FEARFUL -> EmotionPaletteSpec(
                emotion = EmotionTone.FEARFUL,
                paletteTitleTr = "Mor Gölge & Mistik Gerilim",
                moodDescriptionTr = "Korku ve endişe anları için gece menekşesi, derin ametist ve soğuk leylak tonları",
                darkTopGlow = Color(0xFF2E1A47),
                darkMidBackground = Color(0xFF1A102B),
                darkBottomDeep = Color(0xFF0F091A),
                darkSurface = Color(0xFF241638),
                darkSurfaceVariant = Color(0xFF33204F),
                lightTopGlow = Color(0xFFE9DCFF),
                lightMidBackground = Color(0xFFF4EEFF),
                lightBottomDeep = Color(0xFFEAE0FC),
                lightSurface = Color(0xFFF9F5FF),
                lightSurfaceVariant = Color(0xFFE3D4FA),
                vibrantAccent = Color(0xFFB388FF),
                secondaryGlow = Color(0xFF7B2CBF),
                swatchHexCodes = listOf(
                    "#2E1A47" to 0xFF2E1A47,
                    "#33204F" to 0xFF33204F,
                    "#B388FF" to 0xFFB388FF,
                    "#7B2CBF" to 0xFF7B2CBF
                )
            )

            EmotionTone.WHISPER -> EmotionPaletteSpec(
                emotion = EmotionTone.WHISPER,
                paletteTitleTr = "Gece Ormanı & Gizemli Fısıltı",
                moodDescriptionTr = "Sırlar ve fısıltılı konuşmalar için adaçayı yeşili, loş yosun ve sisli zümrüt tonları",
                darkTopGlow = Color(0xFF1E3524),
                darkMidBackground = Color(0xFF122117),
                darkBottomDeep = Color(0xFF0B140E),
                darkSurface = Color(0xFF182B1E),
                darkSurfaceVariant = Color(0xFF233D2B),
                lightTopGlow = Color(0xFFDDF2DF),
                lightMidBackground = Color(0xFFEEF8EE),
                lightBottomDeep = Color(0xFFE2F0E3),
                lightSurface = Color(0xFFF6FBF6),
                lightSurfaceVariant = Color(0xFFD9ECD9),
                vibrantAccent = Color(0xFF90BE6D),
                secondaryGlow = Color(0xFF43AA8B),
                swatchHexCodes = listOf(
                    "#1E3524" to 0xFF1E3524,
                    "#233D2B" to 0xFF233D2B,
                    "#90BE6D" to 0xFF90BE6D,
                    "#43AA8B" to 0xFF43AA8B
                )
            )

            EmotionTone.SURPRISED -> EmotionPaletteSpec(
                emotion = EmotionTone.SURPRISED,
                paletteTitleTr = "Turuncu Kıvılcım & Hayret Işığı",
                moodDescriptionTr = "Şaşkınlık ve beklenmedik sırlar için parlak mandalina, gün batımı bakırı ve altın tonları",
                darkTopGlow = Color(0xFF4A2812),
                darkMidBackground = Color(0xFF2B180B),
                darkBottomDeep = Color(0xFF1A0E06),
                darkSurface = Color(0xFF382010),
                darkSurfaceVariant = Color(0xFF4D2C16),
                lightTopGlow = Color(0xFFFFE3C8),
                lightMidBackground = Color(0xFFFFF2E6),
                lightBottomDeep = Color(0xFFFAE6D4),
                lightSurface = Color(0xFFFFF8F2),
                lightSurfaceVariant = Color(0xFFF7DEC8),
                vibrantAccent = Color(0xFFF4A261),
                secondaryGlow = Color(0xFFFB8500),
                swatchHexCodes = listOf(
                    "#4A2812" to 0xFF4A2812,
                    "#4D2C16" to 0xFF4D2C16,
                    "#F4A261" to 0xFFF4A261,
                    "#FB8500" to 0xFFFB8500
                )
            )

            EmotionTone.DRAMATIC -> EmotionPaletteSpec(
                emotion = EmotionTone.DRAMATIC,
                paletteTitleTr = "Bordo Kadife & Teatral Spot",
                moodDescriptionTr = "Tutkulu ve dramatik zirveler için opera bordosu, gül kurusu ve erguvan tonları",
                darkTopGlow = Color(0xFF45192B),
                darkMidBackground = Color(0xFF26101B),
                darkBottomDeep = Color(0xFF160910),
                darkSurface = Color(0xFF341523),
                darkSurfaceVariant = Color(0xFF491E31),
                lightTopGlow = Color(0xFFFFD9E4),
                lightMidBackground = Color(0xFFFFEEF3),
                lightBottomDeep = Color(0xFFFAE0E8),
                lightSurface = Color(0xFFFFF6F9),
                lightSurfaceVariant = Color(0xFFF6D6E1),
                vibrantAccent = Color(0xFFF28482),
                secondaryGlow = Color(0xFFB5179E),
                swatchHexCodes = listOf(
                    "#45192B" to 0xFF45192B,
                    "#491E31" to 0xFF491E31,
                    "#F28482" to 0xFFF28482,
                    "#B5179E" to 0xFFB5179E
                )
            )
        }
    }
}

/**
 * Full-screen animated backdrop wrapper that dynamically shifts the screen's background
 * color palette, radial stage spotlights, and surface ambiance according to the current dominant emotion.
 */
@Composable
fun DynamicEmotionalBackdropBox(
    activeEmotion: EmotionTone,
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    dynamicPaletteEnabled: Boolean,
    lightingMode: AmbientLightingMode,
    paletteIntensity: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spec = remember(activeEmotion) { EmotionalPaletteCatalog.specForEmotion(activeEmotion) }
    val clampedIntensity = paletteIntensity.coerceIn(0.25f, 1.0f)

    // Default neutral base colors
    val neutralTop = if (isDarkTheme) Color(0xFF130F1C) else Color(0xFFFBF7F0)
    val neutralMid = if (isDarkTheme) Color(0xFF130F1C) else Color(0xFFFBF7F0)
    val neutralBottom = if (isDarkTheme) Color(0xFF0E0B15) else Color(0xFFF4ECE0)

    val rawTargetTop = if (isDarkTheme) spec.darkTopGlow else spec.lightTopGlow
    val rawTargetMid = if (isDarkTheme) spec.darkMidBackground else spec.lightMidBackground
    val rawTargetBottom = if (isDarkTheme) spec.darkBottomDeep else spec.lightBottomDeep

    val modeMultiplier = when (lightingMode) {
        AmbientLightingMode.DEEP_IMMERSION -> 1.0f
        AmbientLightingMode.DYNAMIC_AURA -> 0.85f
        AmbientLightingMode.SUBTLE_HALO -> 0.48f
    } * clampedIntensity

    val targetTop = if (dynamicPaletteEnabled) lerp(neutralTop, rawTargetTop, modeMultiplier) else neutralTop
    val targetMid = if (dynamicPaletteEnabled) lerp(neutralMid, rawTargetMid, modeMultiplier) else neutralMid
    val targetBottom = if (dynamicPaletteEnabled) lerp(neutralBottom, rawTargetBottom, modeMultiplier) else neutralBottom

    val animSpec = tween<Color>(durationMillis = 850, easing = FastOutSlowInEasing)
    val animatedTop by animateColorAsState(targetValue = targetTop, animationSpec = animSpec, label = "bg_top")
    val animatedMid by animateColorAsState(targetValue = targetMid, animationSpec = animSpec, label = "bg_mid")
    val animatedBottom by animateColorAsState(targetValue = targetBottom, animationSpec = animSpec, label = "bg_bottom")
    val animatedAccent by animateColorAsState(targetValue = spec.vibrantAccent, animationSpec = animSpec, label = "bg_accent")
    val animatedSecondary by animateColorAsState(targetValue = spec.secondaryGlow, animationSpec = animSpec, label = "bg_sec")

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_emotion_pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("dynamic_emotional_backdrop")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Base 3-Stop Emotional Gradient Canvas
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(animatedTop, animatedMid, animatedBottom)
                )
            )

            if (dynamicPaletteEnabled) {
                val breathingScale = if (isPlaying) {
                    0.82f + 0.18f * sin(pulsePhase)
                } else {
                    0.76f + 0.06f * sin(pulsePhase * 0.5f)
                }

                // 2. Primary Emotion Radial Spotlight (upper-center stage aura)
                val primaryCenter = Offset(
                    x = size.width * (0.5f + 0.08f * cos(pulsePhase * 0.7f)),
                    y = size.height * 0.22f
                )
                val primaryRadius = (size.width.coerceAtLeast(size.height) * 0.62f * breathingScale).coerceAtLeast(100f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = (if (isDarkTheme) 0.26f else 0.22f) * modeMultiplier),
                            animatedSecondary.copy(alpha = (if (isDarkTheme) 0.10f else 0.08f) * modeMultiplier),
                            Color.Transparent
                        ),
                        center = primaryCenter,
                        radius = primaryRadius
                    ),
                    center = primaryCenter,
                    radius = primaryRadius
                )

                // 3. Secondary Ambient Rim Glow (lower-right atmospheric halo)
                val secondaryCenter = Offset(
                    x = size.width * (0.82f - 0.06f * sin(pulsePhase)),
                    y = size.height * 0.76f
                )
                val secondaryRadius = (size.width * 0.55f).coerceAtLeast(80f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha = (if (isDarkTheme) 0.20f else 0.16f) * modeMultiplier),
                            Color.Transparent
                        ),
                        center = secondaryCenter,
                        radius = secondaryRadius
                    ),
                    center = secondaryCenter,
                    radius = secondaryRadius
                )
            }
        }

        content()
    }
}

/**
 * Interactive control & live swatch inspector card for the Dynamic Emotional Background Palette Module.
 */
@Composable
fun EmotionalPaletteInspectorCard(
    activeEmotion: EmotionTone,
    manualEmotionOverride: EmotionTone?,
    dynamicPaletteEnabled: Boolean,
    lightingMode: AmbientLightingMode,
    paletteIntensity: Float,
    onToggleDynamicPalette: (Boolean) -> Unit,
    onSelectLightingMode: (AmbientLightingMode) -> Unit,
    onChangePaletteIntensity: (Float) -> Unit,
    onSelectManualEmotionOverride: (EmotionTone?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val effectiveEmotion = manualEmotionOverride ?: activeEmotion
    val spec = remember(effectiveEmotion) { EmotionalPaletteCatalog.specForEmotion(effectiveEmotion) }
    val emotionColor = Color(effectiveEmotion.colorHex)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(emotionColor, spec.secondaryGlow)
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("emotional_palette_inspector_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Active Emotion Palette Title + Live Color Swatches + Switch + Expand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Surface(
                        color = emotionColor.copy(alpha = 0.22f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = emotionColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dinamik Duygu Arka Plan Paleti",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = emotionColor.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = effectiveEmotion.labelTr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = emotionColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${spec.paletteTitleTr} • ${if (manualEmotionOverride != null) "Manuel Önizleme" else "Metinle Senkronize"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 4 Live Color Swatch Circles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    spec.swatchHexCodes.forEach { (_, hex) ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("toggle_palette_inspector_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Palet Ayarlarını Daralt" else "Palet Ayarlarını Genişlet"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // Enable/Disable Switch + Mood Description
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Baskın Duyguya Göre Arka Planı Değiştir",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = spec.moodDescriptionTr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = dynamicPaletteEnabled,
                            onCheckedChange = onToggleDynamicPalette,
                            modifier = Modifier.testTag("dynamic_palette_enable_switch")
                        )
                    }

                    // Active 4-Swatch Hex Bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_emotion_swatch_strip")
                    ) {
                        spec.swatchHexCodes.forEach { (hexLabel, hexVal) ->
                            Surface(
                                color = Color(hexVal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = hexLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Lighting Mode Chips
                    Text(
                        text = "Arka Plan Atmosfer Modu:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        AmbientLightingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = lightingMode == mode,
                                onClick = { onSelectLightingMode(mode) },
                                label = {
                                    Text(
                                        text = mode.labelTr,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("ambient_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Intensity Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Renk Paleti Yoğunluğu:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%${(paletteIntensity * 100).toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = emotionColor
                            )
                        }
                        Slider(
                            value = paletteIntensity,
                            onValueChange = onChangePaletteIntensity,
                            valueRange = 0.30f..1.00f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("palette_intensity_slider")
                        )
                    }

                    // 8 Emotion Palette Live Preview / Override Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Duygu Paletlerini Canlı Test Et:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (manualEmotionOverride != null) {
                            TextButton(
                                onClick = { onSelectManualEmotionOverride(null) },
                                modifier = Modifier.testTag("reset_emotion_palette_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Metinle Senkronize Et", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("emotion_palette_preview_row")
                    ) {
                        FilterChip(
                            selected = manualEmotionOverride == null,
                            onClick = { onSelectManualEmotionOverride(null) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Otomatik (Metin Duygusu)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("emotion_palette_auto_chip")
                        )

                        EmotionTone.entries.forEach { emotion ->
                            val isSelected = manualEmotionOverride == emotion
                            val chipColor = Color(emotion.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSelectManualEmotionOverride(
                                        if (isSelected) null else emotion
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                    )
                                },
                                label = {
                                    Text(
                                        text = emotion.labelTr,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.25f),
                                    selectedLabelColor = chipColor
                                ),
                                modifier = Modifier.testTag("emotion_palette_chip_${emotion.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }
}
