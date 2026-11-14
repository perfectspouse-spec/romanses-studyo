package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BookEntity
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.ScriptLineEntity
import com.example.tts.TtsPlaybackState
import com.example.ui.components.AmbientLightingMode
import com.example.ui.components.AutoScrollAnchor
import com.example.ui.components.CharacterVoiceSettingsModal
import com.example.ui.components.CharacterVoiceTimelineCard
import com.example.ui.components.EmotionalPaletteInspectorCard
import com.example.ui.components.FocusedBookManuscriptReader
import com.example.ui.components.FocusedReadingConfig
import com.example.ui.components.FocusedReadingModeToolbarCard
import com.example.ui.components.HighlightedScriptSentenceText
import com.example.ui.components.SentenceHighlightStyle
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun StagePlayerScreen(
    book: BookEntity?,
    characters: List<CharacterVoiceEntity>,
    scriptLines: List<ScriptLineEntity>,
    ttsState: TtsPlaybackState,
    dynamicPaletteEnabled: Boolean = true,
    ambientLightingMode: AmbientLightingMode = AmbientLightingMode.DYNAMIC_AURA,
    paletteIntensity: Float = 0.88f,
    manualEmotionOverride: EmotionTone? = null,
    onToggleDynamicPalette: (Boolean) -> Unit = {},
    onSelectLightingMode: (AmbientLightingMode) -> Unit = {},
    onChangePaletteIntensity: (Float) -> Unit = {},
    onSelectManualEmotionOverride: (EmotionTone?) -> Unit = {},
    focusedReadingConfig: FocusedReadingConfig = FocusedReadingConfig(),
    onToggleFocusedReadingMode: (Boolean) -> Unit = {},
    onToggleFocusedAutoScroll: (Boolean) -> Unit = {},
    onSelectHighlightStyle: (SentenceHighlightStyle) -> Unit = {},
    onSelectScrollAnchor: (AutoScrollAnchor) -> Unit = {},
    onToggleDimSurrounding: (Boolean) -> Unit = {},
    onChangeFocusedFontScale: (Float) -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onPlayLineAtIndex: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPunctuationPauseChange: (Float) -> Unit = {},
    onToggleTheatricalBoost: (Boolean) -> Unit,
    onUpdateLineEmotion: (ScriptLineEntity, EmotionTone) -> Unit,
    onUpdateLineSpeaker: (ScriptLineEntity, CharacterVoiceEntity) -> Unit,
    onSaveFullCharacterSettings: (CharacterVoiceEntity) -> Unit,
    onTestCharacterVoice: (CharacterVoiceEntity, EmotionTone, String?) -> Unit,
    onOpenCastClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingLine by remember { mutableStateOf<ScriptLineEntity?>(null) }
    var voiceModalCharacter by remember { mutableStateOf<CharacterVoiceEntity?>(null) }
    var selectedSpeakerFilter by remember { mutableStateOf<String?>(null) }
    val stageListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentLineEmotion = scriptLines.getOrNull(ttsState.currentLineIndex)?.emotionEnum
    val dominantEmotion = if (ttsState.isPlaying) {
        ttsState.activeEmotion
    } else {
        currentLineEmotion ?: ttsState.activeEmotion
    }

    val displayedLinesWithOriginalIndex = remember(scriptLines, selectedSpeakerFilter) {
        scriptLines.mapIndexedNotNull { index, line ->
            if (selectedSpeakerFilter == null || line.speakerName == selectedSpeakerFilter) {
                index to line
            } else {
                null
            }
        }
    }

    // BackHandler for exiting full-screen Focused Reading Mode back to Stage overview
    BackHandler(enabled = focusedReadingConfig.isFocusedModeActive) {
        onToggleFocusedReadingMode(false)
    }

    // Automatic page scrolling in StagePlayerScreen LazyColumn when playing and auto-scroll is enabled
    val headerItemOffset = if (scriptLines.isNotEmpty()) 5 else 4
    LaunchedEffect(
        ttsState.currentLineIndex,
        ttsState.isPlaying,
        focusedReadingConfig.autoScrollEnabled,
        focusedReadingConfig.isFocusedModeActive
    ) {
        if (!focusedReadingConfig.isFocusedModeActive && focusedReadingConfig.autoScrollEnabled && ttsState.isPlaying) {
            val filteredPos = displayedLinesWithOriginalIndex.indexOfFirst { it.first == ttsState.currentLineIndex }
            if (filteredPos >= 0) {
                val targetLazyIndex = headerItemOffset + filteredPos
                runCatching {
                    stageListState.animateScrollToItem(
                        index = targetLazyIndex,
                        scrollOffset = -120
                    )
                }
            }
        }
    }

    if (focusedReadingConfig.isFocusedModeActive) {
        FocusedBookManuscriptReader(
            book = book,
            scriptLines = scriptLines,
            ttsState = ttsState,
            config = focusedReadingConfig,
            onPlayLineAtIndex = onPlayLineAtIndex,
            onTogglePlayPause = onTogglePlayPause,
            onStop = onStop,
            onSpeedChange = onSpeedChange,
            onToggleAutoScroll = onToggleFocusedAutoScroll,
            onSelectHighlightStyle = onSelectHighlightStyle,
            onToggleDimSurrounding = onToggleDimSurrounding,
            onChangeFontScale = onChangeFocusedFontScale,
            onExitFocusedMode = { onToggleFocusedReadingMode(false) },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = stageListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("stage_player_screen"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Active Stage Console
        item {
            if (!ttsState.isReady || ttsState.engineStatusLabel.startsWith("Seslendirme kesildi")) {
                Text(
                    text = ttsState.engineStatusLabel,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            ActiveVoiceStageConsole(
                bookTitle = book?.title ?: "Roman Seçilmedi",
                ttsState = ttsState,
                totalLines = scriptLines.size,
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStop,
                onPrevLine = {
                    val prev = (ttsState.currentLineIndex - 1).coerceAtLeast(0)
                    onPlayLineAtIndex(prev)
                },
                onNextLine = {
                    if (scriptLines.isNotEmpty()) {
                        val next = (ttsState.currentLineIndex + 1).coerceAtMost(scriptLines.lastIndex)
                        onPlayLineAtIndex(next)
                    }
                },
                onSpeedChange = onSpeedChange,
                onPunctuationPauseChange = onPunctuationPauseChange,
                onToggleTheatricalBoost = onToggleTheatricalBoost,
                onOpenCastClick = {
                    val activeChar = characters.find { it.name == ttsState.activeSpeaker } ?: characters.firstOrNull()
                    if (activeChar != null) {
                        voiceModalCharacter = activeChar
                    } else {
                        onOpenCastClick()
                    }
                }
            )
        }

        // 2. Focused Reading Mode ('Odaklı Okuma Modu') Control & Live Sentence Highlight Card
        item {
            FocusedReadingModeToolbarCard(
                config = focusedReadingConfig,
                ttsState = ttsState,
                totalLines = scriptLines.size,
                onToggleFocusedMode = onToggleFocusedReadingMode,
                onToggleAutoScroll = onToggleFocusedAutoScroll,
                onSelectHighlightStyle = onSelectHighlightStyle,
                onSelectScrollAnchor = onSelectScrollAnchor,
                onToggleDimSurrounding = onToggleDimSurrounding,
                onChangeFontScale = onChangeFocusedFontScale,
                onJumpToActiveSentence = {
                    val filteredPos = displayedLinesWithOriginalIndex.indexOfFirst { it.first == ttsState.currentLineIndex }
                    if (filteredPos >= 0) {
                        coroutineScope.launch {
                            runCatching {
                                stageListState.animateScrollToItem(
                                    index = headerItemOffset + filteredPos,
                                    scrollOffset = -100
                                )
                            }
                        }
                    }
                }
            )
        }

        // 3. Dynamic Emotional Background Palette Module Inspector & Live Swatch Controller
        item {
            EmotionalPaletteInspectorCard(
                activeEmotion = dominantEmotion,
                manualEmotionOverride = manualEmotionOverride,
                dynamicPaletteEnabled = dynamicPaletteEnabled,
                lightingMode = ambientLightingMode,
                paletteIntensity = paletteIntensity,
                onToggleDynamicPalette = onToggleDynamicPalette,
                onSelectLightingMode = onSelectLightingMode,
                onChangePaletteIntensity = onChangePaletteIntensity,
                onSelectManualEmotionOverride = onSelectManualEmotionOverride
            )
        }

        // 3. Visual Character Narration Timeline (Swimlanes + Proportional Ribbon + Segment Blocks)
        if (scriptLines.isNotEmpty()) {
            item {
                CharacterVoiceTimelineCard(
                    scriptLines = scriptLines,
                    characters = characters,
                    currentLineIndex = ttsState.currentLineIndex,
                    isPlaying = ttsState.isPlaying,
                    globalSpeedMultiplier = ttsState.globalSpeedMultiplier,
                    selectedSpeakerFilter = selectedSpeakerFilter,
                    onSelectSpeakerFilter = { speaker ->
                        selectedSpeakerFilter = speaker
                    },
                    onSeekToLine = { targetIndex ->
                        onPlayLineAtIndex(targetIndex)
                    },
                    onOpenSpeakerVoiceSettings = { characterEntity ->
                        voiceModalCharacter = characterEntity
                    }
                )
            }
        }

        // 4. Script Flow Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedSpeakerFilter != null) {
                            "$selectedSpeakerFilter • Seslendirilen Metin Bölümleri (${displayedLinesWithOriginalIndex.size}/${scriptLines.size})"
                        } else {
                            "Duygusal Senaryo & Diyalog Akışı (${scriptLines.size} Satır)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Satıra dokun: Seslendir • Kaleme dokun: Duygu veya karakter değiştir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectedSpeakerFilter != null) {
                    TextButton(
                        onClick = { selectedSpeakerFilter = null },
                        modifier = Modifier.testTag("clear_speaker_filter_button")
                    ) {
                        Text("Tümünü Göster", maxLines = 1)
                    }
                }
            }
        }

        // 5. Script Lines List with Real-Time Sentence & Word Highlighting
        itemsIndexed(
            items = displayedLinesWithOriginalIndex,
            key = { _, pair -> pair.second.id }
        ) { _, (originalIndex, line) ->
            val isCurrentLine = originalIndex == ttsState.currentLineIndex
            ScriptLineCard(
                index = originalIndex,
                line = line,
                isActive = isCurrentLine,
                isPlaying = ttsState.isPlaying && isCurrentLine,
                activeSentenceIndexInLine = if (isCurrentLine) ttsState.activeSentenceIndexInLine else 0,
                activeWordCharIndex = if (isCurrentLine) ttsState.activeWordCharIndex else 0,
                activeLineProgress = if (isCurrentLine) ttsState.activeLineProgress else 0f,
                highlightStyle = focusedReadingConfig.highlightStyle,
                dimWhenInactive = focusedReadingConfig.dimSurroundingSentences && ttsState.isPlaying && !isCurrentLine,
                fontScaleMultiplier = focusedReadingConfig.fontScaleMultiplier,
                onLineClick = { onPlayLineAtIndex(originalIndex) },
                onEditClick = { editingLine = line }
            )
        }
    }

    editingLine?.let { lineToEdit ->
        EditScriptLineEmotionAndSpeakerDialog(
            line = lineToEdit,
            characters = characters,
            onDismiss = { editingLine = null },
            onSelectEmotion = { newEmotion ->
                onUpdateLineEmotion(lineToEdit, newEmotion)
                editingLine = null
            },
            onSelectSpeaker = { newSpeaker ->
                onUpdateLineSpeaker(lineToEdit, newSpeaker)
                editingLine = null
            }
        )
    }

    voiceModalCharacter?.let { targetChar ->
        CharacterVoiceSettingsModal(
            characters = characters,
            initialCharacter = targetChar,
            onDismiss = { voiceModalCharacter = null },
            onSaveCharacterSettings = onSaveFullCharacterSettings,
            onTestCharacterVoice = onTestCharacterVoice
        )
    }
}

@Composable
private fun ActiveVoiceStageConsole(
    bookTitle: String,
    ttsState: TtsPlaybackState,
    totalLines: Int,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevLine: () -> Unit,
    onNextLine: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPunctuationPauseChange: (Float) -> Unit,
    onToggleTheatricalBoost: (Boolean) -> Unit,
    onOpenCastClick: () -> Unit
) {
    val activeEmotionColor = Color(ttsState.activeEmotion.colorHex)
    val activeGenderColor = Color(ttsState.activeGender.badgeColorHex)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(activeGenderColor, activeEmotionColor)
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$bookTitle • ${ttsState.activeLanguage.badgeShort} (${ttsState.activeLanguage.labelTr})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ttsState.activeSpeaker,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = activeGenderColor.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = ttsState.activeGender.labelTr,
                                style = MaterialTheme.typography.labelSmall,
                                color = activeGenderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = activeEmotionColor.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .border(1.dp, activeEmotionColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable { onOpenCastClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TheaterComedy,
                            contentDescription = null,
                            tint = activeEmotionColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ttsState.activeEmotion.labelTr,
                            style = MaterialTheme.typography.labelMedium,
                            color = activeEmotionColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Waveform Visualizer
            EmotionalVoiceWaveform(
                isPlaying = ttsState.isPlaying,
                primaryColor = activeGenderColor,
                secondaryColor = activeEmotionColor,
                pitchFactor = ttsState.effectivePitch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry & Pitch/Rate/Punctuation Es readout
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ttsState.activePunctuationPauseLabel?.let { "Noktalama Esi: $it" }
                        ?: "Perde: ${String.format("%.2f", ttsState.effectivePitch)}x • Hız: ${String.format("%.2f", ttsState.effectiveRate)}x • Es: ${String.format("%.2f", ttsState.punctuationPauseMultiplier)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ttsState.activePunctuationPauseLabel != null) {
                        activeEmotionColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (ttsState.activePunctuationPauseLabel != null) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Replik ${if (totalLines > 0) ttsState.currentLineIndex + 1 else 0} / $totalLines",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transport Controls + Speed + Theatrical Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onPrevLine,
                        modifier = Modifier.testTag("prev_line_button")
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Önceki Replik")
                    }

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (ttsState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ttsState.isPlaying) "Duraklat" else "Seslendirmeyi Başlat",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextLine,
                        modifier = Modifier.testTag("next_line_button")
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Sonraki Replik")
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.testTag("stop_playback_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Durdur")
                    }
                }

                // Speed chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(0.85f to "0.85x", 1.0f to "1.0x", 1.2f to "1.2x").forEach { (spd, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(ttsState.globalSpeedMultiplier - spd) < 0.05f,
                            onClick = { onSpeedChange(spd) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Punctuation Pause Duration Chips (Noktalama İşaretleri Bekleme Süresi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("punctuation_pause_chip_row")
            ) {
                Text(
                    text = "Noktalama Esi:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                listOf(
                    1.0f to "1.0x Standart",
                    1.35f to "1.35x Edebi Es",
                    1.75f to "1.75x Uzun Es",
                    2.20f to "2.2x Teatral Es"
                ).forEach { (mult, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(ttsState.punctuationPauseMultiplier - mult) < 0.08f,
                        onClick = { onPunctuationPauseChange(mult) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Teatral Duygu Vurgusu (Otomatik Perde & Es Modülasyonu)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = ttsState.theatricalEmotionBoost,
                    onCheckedChange = onToggleTheatricalBoost,
                    modifier = Modifier.testTag("theatrical_boost_switch")
                )
            }
        }
    }
}

@Composable
private fun EmotionalVoiceWaveform(
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    pitchFactor: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 34
        val spacing = 6.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(4f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalized = i.toFloat() / barCount.toFloat()
            val dynamicWave = if (isPlaying) {
                val waveVal = sin(phase + i * 0.45f * pitchFactor)
                (0.28f + 0.72f * kotlin.math.abs(waveVal)).coerceIn(0.2f, 1.0f)
            } else {
                0.22f + 0.12f * kotlin.math.abs(sin(i * 0.5f))
            }

            val barHeight = (size.height * dynamicWave).coerceAtLeast(6f)
            val top = centerY - (barHeight / 2f)
            val left = i * (barWidth + spacing)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun ScriptLineCard(
    index: Int,
    line: ScriptLineEntity,
    isActive: Boolean,
    isPlaying: Boolean,
    activeSentenceIndexInLine: Int,
    activeWordCharIndex: Int,
    activeLineProgress: Float,
    highlightStyle: SentenceHighlightStyle,
    dimWhenInactive: Boolean,
    fontScaleMultiplier: Float,
    onLineClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val genderColor = Color(line.genderEnum.badgeColorHex)
    val emotionColor = Color(line.emotionEnum.colorHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimWhenInactive) 0.62f else 1.0f)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) emotionColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onLineClick() }
            .testTag("script_line_card_$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(genderColor.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = line.speakerName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = genderColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = line.speakerName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${line.genderEnum.labelTr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = genderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = line.stageDirection,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = emotionColor.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable { onEditClick() }
                            .testTag("edit_line_emotion_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = line.emotionEnum.labelTr,
                                style = MaterialTheme.typography.labelSmall,
                                color = emotionColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Duygu ve Karakteri Düzenle",
                                tint = emotionColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HighlightedScriptSentenceText(
                line = line,
                isActiveLine = isActive,
                isPlaying = isPlaying,
                activeSentenceIndexInLine = activeSentenceIndexInLine,
                activeWordCharIndex = activeWordCharIndex,
                activeLineProgress = activeLineProgress,
                highlightStyle = highlightStyle,
                fontScaleMultiplier = fontScaleMultiplier
            )
        }
    }
}

@Composable
private fun EditScriptLineEmotionAndSpeakerDialog(
    line: ScriptLineEntity,
    characters: List<CharacterVoiceEntity>,
    onDismiss: () -> Unit,
    onSelectEmotion: (EmotionTone) -> Unit,
    onSelectSpeaker: (CharacterVoiceEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Replik Duygusu & Konuşmacı")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "“${line.text.take(90)}”",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "1. Duygu Tonlaması Seçin:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmotionTone.entries.chunked(2).forEach { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { emotion ->
                                val isSelected = line.emotionEnum == emotion
                                Surface(
                                    color = if (isSelected) {
                                        Color(emotion.colorHex).copy(alpha = 0.28f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectEmotion(emotion) }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = emotion.labelTr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(emotion.colorHex)
                                        )
                                        Text(
                                            text = emotion.subtitleTr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (characters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Veya Konuşan Karakteri Değiştirin:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        characters.forEach { character ->
                            FilterChip(
                                selected = character.name == line.speakerName,
                                onClick = { onSelectSpeaker(character) },
                                label = {
                                    Text("${character.name} (${character.genderEnum.labelTr})")
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
