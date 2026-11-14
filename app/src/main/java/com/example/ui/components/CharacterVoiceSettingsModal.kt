package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.VoiceGender
import com.example.model.VoiceTimbrePreset

@Composable
fun CharacterVoiceSettingsModal(
    characters: List<CharacterVoiceEntity>,
    initialCharacter: CharacterVoiceEntity,
    onDismiss: () -> Unit,
    onSaveCharacterSettings: (CharacterVoiceEntity) -> Unit,
    onTestCharacterVoice: (CharacterVoiceEntity, EmotionTone, String?) -> Unit
) {
    var activeCharacter by remember(initialCharacter.id) {
        mutableStateOf(initialCharacter)
    }

    var selectedGender by remember(activeCharacter.id) {
        mutableStateOf(activeCharacter.genderEnum)
    }
    var selectedTimbre by remember(activeCharacter.id) {
        mutableStateOf(activeCharacter.timbrePresetEnum)
    }
    var basePitch by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.basePitch)
    }
    var baseRate by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.baseRate)
    }
    var dominantEmotion by remember(activeCharacter.id) {
        mutableStateOf(activeCharacter.dominantEmotionEnum)
    }
    var emotionIntensity by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.emotionIntensity)
    }
    var pitchModulationRange by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.pitchModulationRange)
    }
    var dramaticPauseScale by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.dramaticPauseScale)
    }
    var whisperVolumeScale by remember(activeCharacter.id) {
        mutableFloatStateOf(activeCharacter.whisperVolumeScale)
    }
    var customTestPhrase by remember(activeCharacter.id) {
        mutableStateOf("")
    }

    val genderColor = Color(selectedGender.badgeColorHex)
    val emotionColor = Color(dominantEmotion.colorHex)

    fun buildUpdatedEntity(): CharacterVoiceEntity {
        val styleLabel = "${selectedGender.labelTr} • ${selectedTimbre.labelTr} (${String.format("%.2f", basePitch)}x)"
        return activeCharacter.copy(
            gender = selectedGender.name,
            timbrePreset = selectedTimbre.name,
            basePitch = basePitch,
            baseRate = baseRate,
            dominantEmotion = dominantEmotion.name,
            emotionIntensity = emotionIntensity,
            pitchModulationRange = pitchModulationRange,
            dramaticPauseScale = dramaticPauseScale,
            whisperVolumeScale = whisperVolumeScale,
            voiceStyleLabel = styleLabel,
            colorHex = selectedGender.badgeColorHex
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .border(
                    width = 1.5.dp,
                    color = genderColor.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("character_voice_settings_modal")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Modal Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ses Tonu & Duygu Modülasyonu Ayarları",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeCharacter.name} • Cinsiyet, Tını ve Duygusal Vurgu Parametreleri",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_voice_settings_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Modalı Kapat"
                        )
                    }
                }

                // Character Switcher Strip (if multiple characters exist)
                if (characters.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        characters.forEach { charItem ->
                            val isSelected = charItem.id == activeCharacter.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    // Save current edits before switching character tab
                                    onSaveCharacterSettings(buildUpdatedEntity())
                                    activeCharacter = charItem
                                },
                                label = {
                                    Text(
                                        text = "${charItem.name} (${charItem.genderEnum.labelTr})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("modal_select_character_${charItem.name}")
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }

                // Scrollable Settings Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: Gender & Voice Category
                    SectionHeaderBadge(
                        number = "1",
                        title = "Cinsiyet & Ses Kategorisi",
                        subtitle = "Karakterin temel ses aralığını ve akustik kimliğini belirler"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        VoiceGender.entries.forEach { g ->
                            val selected = selectedGender == g
                            val chipColor = Color(g.badgeColorHex)
                            Surface(
                                color = if (selected) chipColor.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) chipColor else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedGender = g
                                        basePitch = g.defaultBasePitch
                                        baseRate = g.defaultBaseRate
                                    }
                                    .testTag("modal_gender_option_${g.name}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(chipColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = g.labelTr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: Voice Timbre & Pitch/Rate Sliders
                    SectionHeaderBadge(
                        number = "2",
                        title = "Ses Tonu, Tını (Timbre) & Tempo",
                        subtitle = "Sesin rengini, tiz/bas perdesini ve konuşma hızını ayarlayın"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        VoiceTimbrePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedTimbre == preset,
                                onClick = { selectedTimbre = preset },
                                label = { Text(preset.labelTr, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("modal_timbre_${preset.name}")
                            )
                        }
                    }
                    Text(
                        text = "Seçili Tını: ${selectedTimbre.descriptionTr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Base Pitch Slider
                    ParameterSliderRow(
                        title = "Temel Ses Perdesi (Pitch)",
                        valueText = "${String.format("%.2f", basePitch)}x • ${describePitchRegister(basePitch)}",
                        accentColor = genderColor,
                        value = basePitch,
                        valueRange = 0.55f..1.75f,
                        onValueChange = { basePitch = it },
                        testTag = "modal_pitch_slider"
                    )

                    // Base Speech Rate Slider
                    ParameterSliderRow(
                        title = "Okuma Hızı & Tempo (Rate)",
                        valueText = "${String.format("%.2f", baseRate)}x • ${if (baseRate < 0.90f) "Ağır" else if (baseRate > 1.08f) "Hızlı" else "Doğal"}",
                        accentColor = genderColor,
                        value = baseRate,
                        valueRange = 0.65f..1.45f,
                        onValueChange = { baseRate = it },
                        testTag = "modal_rate_slider"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // SECTION 3: Emotion Modulation Engine Parameters
                    SectionHeaderBadge(
                        number = "3",
                        title = "Duygu Modülasyonu Parametreleri",
                        subtitle = "Öfke, hüzün, neşe, korku ve fısıltı anlarında sesin nasıl değişeceğini belirler"
                    )

                    Text(
                        text = "Baskın Duygu Tonu & Test Duygusu:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        EmotionTone.entries.forEach { emotion ->
                            FilterChip(
                                selected = dominantEmotion == emotion,
                                onClick = { dominantEmotion = emotion },
                                label = { Text(emotion.labelTr, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("modal_emotion_${emotion.name}")
                            )
                        }
                    }

                    // Emotion Modulation Intensity
                    ParameterSliderRow(
                        title = "Duygu Modülasyon Derinliği (Yoğunluk)",
                        valueText = "%${(emotionIntensity * 100).toInt()} • ${if (emotionIntensity > 1.35f) "Yüksek Teatral" else if (emotionIntensity < 0.85f) "Doğal Hafif" else "Dengeli"}",
                        accentColor = emotionColor,
                        value = emotionIntensity,
                        valueRange = 0.30f..2.00f,
                        onValueChange = { emotionIntensity = it },
                        testTag = "modal_emotion_intensity_slider"
                    )

                    // Pitch Modulation Range (Inflection)
                    ParameterSliderRow(
                        title = "Duygusal Perde Salınımı (Vurgu Genişliği)",
                        valueText = "+${String.format("%.2f", pitchModulationRange)}x ünlem/soru tonlaması",
                        accentColor = emotionColor,
                        value = pitchModulationRange,
                        valueRange = 0.00f..0.40f,
                        onValueChange = { pitchModulationRange = it },
                        testTag = "modal_pitch_modulation_slider"
                    )

                    // Dramatic Pause Scale
                    ParameterSliderRow(
                        title = "Replik Sonu Dramatik Es Süresi",
                        valueText = "${String.format("%.2f", dramaticPauseScale)}x (${(dominantEmotion.pauseAfterMs * dramaticPauseScale).toInt()} ms)",
                        accentColor = emotionColor,
                        value = dramaticPauseScale,
                        valueRange = 0.50f..2.00f,
                        onValueChange = { dramaticPauseScale = it },
                        testTag = "modal_pause_scale_slider"
                    )

                    // Whisper & Soft Delivery Volume Scale
                    ParameterSliderRow(
                        title = "Fısıltı & Hüzün Dinamik Ses Seviyesi",
                        valueText = "%${(whisperVolumeScale * 100).toInt()} ses şiddeti",
                        accentColor = emotionColor,
                        value = whisperVolumeScale,
                        valueRange = 0.40f..1.00f,
                        onValueChange = { whisperVolumeScale = it },
                        testTag = "modal_whisper_volume_slider"
                    )

                    // Live Voice Preview Box inside Modal
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Canlı Ses & Duygu Modülasyonu Provası",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = customTestPhrase,
                                onValueChange = { customTestPhrase = it },
                                label = { Text("Özel Test Cümlesi (Boş bırakılırsa duyguya özel replik okunur)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("modal_custom_test_phrase_input")
                            )

                            OutlinedButton(
                                onClick = {
                                    onTestCharacterVoice(
                                        buildUpdatedEntity(),
                                        dominantEmotion,
                                        customTestPhrase.takeIf { it.isNotBlank() }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("modal_test_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bu Ayarlarla Sesi Dinle (${dominantEmotion.labelTr})")
                            }
                        }
                    }
                }

                // Bottom Modal Action Bar
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TextButton(
                        onClick = {
                            basePitch = selectedGender.defaultBasePitch
                            baseRate = selectedGender.defaultBaseRate
                            selectedTimbre = VoiceTimbrePreset.CRISP_THEATRICAL
                            emotionIntensity = 1.15f
                            pitchModulationRange = 0.14f
                            dramaticPauseScale = 1.00f
                            whisperVolumeScale = 0.72f
                        },
                        modifier = Modifier.testTag("modal_reset_defaults_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sıfırla")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Vazgeç")
                        }
                        Button(
                            onClick = {
                                onSaveCharacterSettings(buildUpdatedEntity())
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("modal_save_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ayarları Kaydet")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderBadge(
    number: String,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParameterSliderRow(
    title: String,
    valueText: String,
    accentColor: Color,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun describePitchRegister(pitch: Float): String = when {
    pitch < 0.72f -> "Derin Bas"
    pitch < 0.88f -> "Bariton / Tok"
    pitch < 1.10f -> "Doğal Orta"
    pitch < 1.32f -> "Mezzo / Sıcak İnce"
    else -> "Soprano / Parlak Tiz"
}
