package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.BookEntity
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.VoiceGender
import com.example.ui.components.CharacterVoiceSettingsModal

@Composable
fun VoiceCastScreen(
    book: BookEntity?,
    characters: List<CharacterVoiceEntity>,
    engineStatusLabel: String,
    onUpdateCharacter: (
        character: CharacterVoiceEntity,
        gender: VoiceGender,
        pitch: Float,
        rate: Float,
        emotion: EmotionTone
    ) -> Unit,
    onSaveFullCharacterSettings: (CharacterVoiceEntity) -> Unit,
    onTestCharacterVoice: (CharacterVoiceEntity, EmotionTone, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var modalTargetCharacter by remember { mutableStateOf<CharacterVoiceEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_cast_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Karakter & Cinsiyet Ses Stüdyosu",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${book?.title ?: "Seçili Roman"} • Her karakter için cinsiyet, ses tınısı, perde ve duygu modülasyonunu özelleştirin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = "Aktif Motor: $engineStatusLabel • ${characters.size} Karakter",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (characters.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { modalTargetCharacter = characters.first() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_global_voice_settings_modal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Modülasyon Ayarları",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        items(characters, key = { it.id }) { character ->
            CharacterVoiceEditorCard(
                character = character,
                onSaveProfile = { gender, pitch, rate, emotion ->
                    onUpdateCharacter(character, gender, pitch, rate, emotion)
                },
                onOpenSettingsModal = {
                    modalTargetCharacter = character
                },
                onTestVoice = { previewCharacter, emotion ->
                    onTestCharacterVoice(previewCharacter, emotion, null)
                }
            )
        }
    }

    modalTargetCharacter?.let { targetChar ->
        CharacterVoiceSettingsModal(
            characters = characters,
            initialCharacter = targetChar,
            onDismiss = { modalTargetCharacter = null },
            onSaveCharacterSettings = { updated ->
                onSaveFullCharacterSettings(updated)
            },
            onTestCharacterVoice = { previewChar, emotion, customPhrase ->
                onTestCharacterVoice(previewChar, emotion, customPhrase)
            }
        )
    }
}

@Composable
private fun CharacterVoiceEditorCard(
    character: CharacterVoiceEntity,
    onSaveProfile: (VoiceGender, Float, Float, EmotionTone) -> Unit,
    onOpenSettingsModal: () -> Unit,
    onTestVoice: (CharacterVoiceEntity, EmotionTone) -> Unit
) {
    var selectedGender by remember(character.id, character.gender) {
        mutableStateOf(character.genderEnum)
    }
    var pitch by remember(character.id, character.basePitch) {
        mutableFloatStateOf(character.basePitch)
    }
    var rate by remember(character.id, character.baseRate) {
        mutableFloatStateOf(character.baseRate)
    }
    var selectedEmotion by remember(character.id, character.dominantEmotion) {
        mutableStateOf(character.dominantEmotionEnum)
    }

    val genderColor = Color(selectedGender.badgeColorHex)
    val emotionColor = Color(selectedEmotion.colorHex)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = genderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("character_card_${character.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(genderColor.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = character.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = genderColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${character.voiceStyleLabel} • Modülasyon %${(character.emotionIntensity * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = genderColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = character.roleDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onOpenSettingsModal,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_modal_button_${character.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Ses ve Duygu Modülasyonu Ayarları",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ayarlar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Gender / Voice Type Selector
            Text(
                text = "Cinsiyet & Ses Karakteri:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                VoiceGender.entries.forEach { g ->
                    FilterChip(
                        selected = selectedGender == g,
                        onClick = {
                            selectedGender = g
                            pitch = g.defaultBasePitch
                            rate = g.defaultBaseRate
                            onSaveProfile(g, pitch, rate, selectedEmotion)
                        },
                        label = { Text(g.labelTr, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Pitch & Rate Sliders
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ses Perdesi (Tiz / Bas): ${String.format("%.2f", pitch)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (pitch < 0.85f) "Derin / Tok" else if (pitch > 1.2f) "İnce / Parlak" else "Doğal Orta",
                    style = MaterialTheme.typography.labelSmall,
                    color = genderColor
                )
            }
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                onValueChangeFinished = {
                    onSaveProfile(selectedGender, pitch, rate, selectedEmotion)
                },
                valueRange = 0.60f..1.65f
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Konuşma Temposu: ${String.format("%.2f", rate)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (rate < 0.90f) "Ağır / Dramatik" else if (rate > 1.08f) "Hızlı / Enerjik" else "Dengeli",
                    style = MaterialTheme.typography.labelSmall,
                    color = emotionColor
                )
            }
            Slider(
                value = rate,
                onValueChange = { rate = it },
                onValueChangeFinished = {
                    onSaveProfile(selectedGender, pitch, rate, selectedEmotion)
                },
                valueRange = 0.72f..1.35f
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Emotion Tone Selector for Voice Test & Character Default
            Text(
                text = "Duygu Tonu Seç & Seslendir:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                EmotionTone.entries.forEach { emotion ->
                    FilterChip(
                        selected = selectedEmotion == emotion,
                        onClick = {
                            selectedEmotion = emotion
                            onSaveProfile(selectedGender, pitch, rate, emotion)
                        },
                        label = { Text(emotion.labelTr, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val updatedPreview = character.copy(
                            gender = selectedGender.name,
                            basePitch = pitch,
                            baseRate = rate,
                            dominantEmotion = selectedEmotion.name
                        )
                        onTestVoice(updatedPreview, selectedEmotion)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("test_voice_button_${character.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sesini Test Et (${selectedEmotion.labelTr})", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenSettingsModal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modülasyon", maxLines = 1)
                }
            }
        }
    }
}
