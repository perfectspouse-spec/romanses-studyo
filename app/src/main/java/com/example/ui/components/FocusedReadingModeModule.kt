package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookEntity
import com.example.model.EmotionTone
import com.example.model.ScriptLineEntity
import com.example.tts.TtsPlaybackState
import kotlinx.coroutines.launch

enum class SentenceHighlightStyle(
    val labelTr: String,
    val descriptionTr: String
) {
    EMOTIONAL_MARKER(
        labelTr = "Fosforlu Duygu Şeridi",
        descriptionTr = "O an seslendirilen cümleyi baskın duygunun renginde fosforlu şeritle vurgular"
    ),
    KARAOKE_WORD_SWEEP(
        labelTr = "Canlı Kelime Takibi",
        descriptionTr = "Aktif cümleyi ve o an okunan kelimeyi gerçek zamanlı olarak adım adım izler"
    ),
    SPOTLIGHT_GLOW(
        labelTr = "Sinematik Spot Odak",
        descriptionTr = "Aktif cümleyi parlak spot çerçevesine alır ve çevredeki metinleri yumuşakça karartır"
    )
}

enum class AutoScrollAnchor(
    val labelTr: String,
    val viewportFraction: Float
) {
    CENTER_SCREEN("Ekran Ortasında Tut", 0.34f),
    TOP_FOCUS("Üst Odak Hizasında Tut", 0.14f)
}

data class FocusedReadingConfig(
    val isFocusedModeActive: Boolean = false,
    val autoScrollEnabled: Boolean = true,
    val highlightStyle: SentenceHighlightStyle = SentenceHighlightStyle.EMOTIONAL_MARKER,
    val scrollAnchor: AutoScrollAnchor = AutoScrollAnchor.CENTER_SCREEN,
    val dimSurroundingSentences: Boolean = true,
    val fontScaleMultiplier: Float = 1.12f,
    val showInlineSpeakerTags: Boolean = true
)

/**
 * Splits a paragraph or script line into individual sentence ranges (`IntRange`)
 * so the reader can highlight the exact sentence currently being spoken inside a multi-sentence block.
 */
fun extractSentenceRangesForHighlight(text: String): List<IntRange> {
    if (text.isBlank()) return listOf(0..0)
    val ranges = mutableListOf<IntRange>()
    var sentenceStart = 0
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        val isTerminator = ch == '.' || ch == '!' || ch == '?' || ch == '…'
        if (isTerminator) {
            var endIdx = i
            while (
                endIdx + 1 < text.length &&
                (text[endIdx + 1] == '.' ||
                    text[endIdx + 1] == '!' ||
                    text[endIdx + 1] == '?' ||
                    text[endIdx + 1] == '"' ||
                    text[endIdx + 1] == '”' ||
                    text[endIdx + 1] == '\'')
            ) {
                endIdx++
            }
            if (endIdx >= sentenceStart) {
                ranges.add(sentenceStart..endIdx)
            }
            sentenceStart = endIdx + 1
            while (sentenceStart < text.length && text[sentenceStart].isWhitespace()) {
                sentenceStart++
            }
            i = sentenceStart
        } else {
            i++
        }
    }
    if (sentenceStart < text.length) {
        ranges.add(sentenceStart until text.length)
    }
    return if (ranges.isEmpty()) listOf(0 until text.length) else ranges
}

/**
 * Interactive control card on StagePlayerScreen that activates 'Odaklı Okuma Modu' (Focused Reading Mode)
 * and controls real-time sentence highlighting & automatic page scrolling.
 */
@Composable
fun FocusedReadingModeToolbarCard(
    config: FocusedReadingConfig,
    ttsState: TtsPlaybackState,
    totalLines: Int,
    onToggleFocusedMode: (Boolean) -> Unit,
    onToggleAutoScroll: (Boolean) -> Unit,
    onSelectHighlightStyle: (SentenceHighlightStyle) -> Unit,
    onSelectScrollAnchor: (AutoScrollAnchor) -> Unit,
    onToggleDimSurrounding: (Boolean) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onJumpToActiveSentence: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsPanel by remember { mutableStateOf(false) }
    val activeEmotionColor = Color(ttsState.activeEmotion.colorHex)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isFocusedModeActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (config.isFocusedModeActive) 2.dp else 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(activeEmotionColor, MaterialTheme.colorScheme.primary)
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("focused_reading_toolbar_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleFocusedMode(!config.isFocusedModeActive) }
                ) {
                    Surface(
                        color = activeEmotionColor.copy(alpha = 0.22f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = activeEmotionColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Odaklı Okuma Modu",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (config.autoScrollEnabled) {
                                    activeEmotionColor.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (config.autoScrollEnabled) "Oto-Kaydır" else "Manuel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.autoScrollEnabled) {
                                        activeEmotionColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${config.highlightStyle.labelTr} • Cümle ${if (totalLines > 0) ttsState.currentLineIndex + 1 else 0}/$totalLines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onToggleFocusedMode(!config.isFocusedModeActive) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.isFocusedModeActive) {
                                activeEmotionColor
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("toggle_focused_reading_mode_button")
                    ) {
                        Icon(
                            imageVector = if (config.isFocusedModeActive) {
                                Icons.Default.CloseFullscreen
                            } else {
                                Icons.AutoMirrored.Filled.MenuBook
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (config.isFocusedModeActive) "Sahneye Dön" else "Odaklı Oku",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showSettingsPanel = !showSettingsPanel },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("expand_focused_reading_settings_button")
                    ) {
                        Icon(
                            imageVector = if (showSettingsPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Odaklı Okuma Ayarları"
                        )
                    }
                }
            }

            // Live Currently Narrated Sentence Strip
            if (ttsState.activeSentenceText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = activeEmotionColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJumpToActiveSentence() }
                        .testTag("live_narrated_sentence_banner")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Highlight,
                                    contentDescription = null,
                                    tint = activeEmotionColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Şu An Vurgulanan Cümle (${ttsState.activeSpeaker}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = activeEmotionColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Aktif Cümleye Odaklan",
                                    tint = activeEmotionColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cümleye Git",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeEmotionColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "“${ttsState.activeSentenceText}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expandable Settings for Auto-Scroll, Highlight Style, Font Size, and Dimming
            AnimatedVisibility(visible = showSettingsPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // 1. Auto-Scroll Switch + Scroll Anchor
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Sayfayı Otomatik Kaydır (Auto-Scroll)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Seslendirilen cümle ilerledikçe sayfayı yumuşakça kaydırır",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = config.autoScrollEnabled,
                            onCheckedChange = onToggleAutoScroll,
                            modifier = Modifier.testTag("focused_auto_scroll_switch")
                        )
                    }

                    // 2. Highlight Style Chips
                    Text(
                        text = "Cümle Vurgulama Stili:",
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
                        SentenceHighlightStyle.entries.forEach { style ->
                            FilterChip(
                                selected = config.highlightStyle == style,
                                onClick = { onSelectHighlightStyle(style) },
                                label = {
                                    Text(
                                        text = style.labelTr,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("highlight_style_${style.name.lowercase()}")
                            )
                        }
                    }

                    // 3. Auto-Scroll Anchor + Surrounding Dimming
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AutoScrollAnchor.entries.forEach { anchor ->
                                FilterChip(
                                    selected = config.scrollAnchor == anchor,
                                    onClick = { onSelectScrollAnchor(anchor) },
                                    label = {
                                        Text(
                                            text = anchor.labelTr,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 4. Dim Surrounding Sentences & Typography Size Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Okunmayan Cümleleri Hafif Karart (Odak Spotu)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = config.dimSurroundingSentences,
                            onCheckedChange = onToggleDimSurrounding,
                            modifier = Modifier.testTag("dim_surrounding_sentences_switch")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Metin Yazı Boyutu:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                0.95f to "A-",
                                1.10f to "Standart",
                                1.25f to "A+",
                                1.42f to "A++"
                            ).forEach { (scale, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(config.fontScaleMultiplier - scale) < 0.05f,
                                    onClick = { onChangeFontScale(scale) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a script line or book paragraph with sentence-level and word-level real-time highlighting
 * for Focused Reading Mode ('Odaklı Okuma Modu').
 */
@Composable
fun HighlightedScriptSentenceText(
    line: ScriptLineEntity,
    isActiveLine: Boolean,
    isPlaying: Boolean,
    activeSentenceIndexInLine: Int,
    activeWordCharIndex: Int,
    activeLineProgress: Float,
    highlightStyle: SentenceHighlightStyle,
    fontScaleMultiplier: Float = 1.0f,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val emotionColor = Color(line.emotionEnum.colorHex)
    val baseFontSizeSp = (16.5f * fontScaleMultiplier).sp
    val baseLineHeightSp = (25.5f * fontScaleMultiplier).sp

    val sentenceRanges = remember(line.text) {
        extractSentenceRangesForHighlight(line.text)
    }

    val highlightBgColor by animateColorAsState(
        targetValue = if (isActiveLine) {
            emotionColor.copy(alpha = if (isDarkTheme) 0.34f else 0.26f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "sentence_highlight_bg"
    )

    val wordSweepBgColor = emotionColor.copy(alpha = if (isDarkTheme) 0.52f else 0.38f)

    val annotatedText = remember(
        line.text,
        line.genderEnum,
        isActiveLine,
        isPlaying,
        activeSentenceIndexInLine,
        activeWordCharIndex,
        highlightStyle,
        highlightBgColor,
        emotionColor
    ) {
        buildAnnotatedString {
            val isDialogue = line.genderEnum.name != "NARRATOR"
            if (isDialogue) {
                withStyle(
                    SpanStyle(
                        color = if (isActiveLine) emotionColor else Color.Unspecified,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("“")
                }
            }

            if (!isActiveLine) {
                append(line.text)
            } else {
                sentenceRanges.forEachIndexed { sIdx, range ->
                    val safeStart = range.first.coerceIn(0, line.text.length)
                    val safeEnd = (range.last + 1).coerceIn(safeStart, line.text.length)
                    val sentenceChunk = line.text.substring(safeStart, safeEnd)
                    val isCurrentSubSentence = sIdx == activeSentenceIndexInLine || sentenceRanges.size == 1

                    when (highlightStyle) {
                        SentenceHighlightStyle.KARAOKE_WORD_SWEEP -> {
                            if (isCurrentSubSentence) {
                                // Highlight spoken words vs active word vs upcoming words within the sentence
                                val relativeCharIndex = (activeWordCharIndex - safeStart)
                                    .coerceIn(0, sentenceChunk.length)
                                // Find word boundary around relativeCharIndex
                                var wordStart = relativeCharIndex
                                while (wordStart > 0 && !sentenceChunk[wordStart - 1].isWhitespace()) {
                                    wordStart--
                                }
                                var wordEnd = relativeCharIndex
                                while (wordEnd < sentenceChunk.length && !sentenceChunk[wordEnd].isWhitespace()) {
                                    wordEnd++
                                }

                                val beforeWord = sentenceChunk.substring(0, wordStart)
                                val activeWord = sentenceChunk.substring(wordStart, wordEnd)
                                val afterWord = sentenceChunk.substring(wordEnd)

                                // Spoken portion of active sentence
                                if (beforeWord.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            background = highlightBgColor,
                                            color = emotionColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(beforeWord)
                                    }
                                }
                                // Currently spoken word pill
                                if (activeWord.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            background = wordSweepBgColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append(activeWord)
                                    }
                                }
                                // Upcoming portion of active sentence
                                if (afterWord.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            background = highlightBgColor.copy(alpha = 0.16f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append(afterWord)
                                    }
                                }
                            } else if (sIdx < activeSentenceIndexInLine) {
                                // Already spoken sentence in this paragraph
                                withStyle(
                                    SpanStyle(
                                        color = emotionColor.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(sentenceChunk)
                                }
                            } else {
                                append(sentenceChunk)
                            }
                        }

                        SentenceHighlightStyle.EMOTIONAL_MARKER,
                        SentenceHighlightStyle.SPOTLIGHT_GLOW -> {
                            if (isCurrentSubSentence) {
                                withStyle(
                                    SpanStyle(
                                        background = highlightBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (highlightStyle == SentenceHighlightStyle.EMOTIONAL_MARKER) {
                                            TextDecoration.Underline
                                        } else {
                                            TextDecoration.None
                                        }
                                    )
                                ) {
                                    append(sentenceChunk)
                                }
                            } else {
                                append(sentenceChunk)
                            }
                        }
                    }

                    if (sIdx < sentenceRanges.lastIndex) {
                        append(" ")
                    }
                }
            }

            if (isDialogue) {
                withStyle(
                    SpanStyle(
                        color = if (isActiveLine) emotionColor else Color.Unspecified,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("”")
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = baseFontSizeSp,
                lineHeight = baseLineHeightSp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isActiveLine) {
            Spacer(modifier = Modifier.height(8.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = if (isPlaying) activeLineProgress.coerceIn(0.05f, 1f) else 1f,
                animationSpec = tween(durationMillis = 140),
                label = "sentence_progress_bar"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = emotionColor,
                trackColor = emotionColor.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

/**
 * Full-screen / Book Manuscript 'Odaklı Okuma Modu' (Focused Reading Mode) view.
 * Highlights the currently narrated sentence in-place inside a continuous literary page layout
 * and automatically scrolls the page to keep the active sentence in focus.
 */
@Composable
fun FocusedBookManuscriptReader(
    book: BookEntity?,
    scriptLines: List<ScriptLineEntity>,
    ttsState: TtsPlaybackState,
    config: FocusedReadingConfig,
    onPlayLineAtIndex: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleAutoScroll: (Boolean) -> Unit,
    onSelectHighlightStyle: (SentenceHighlightStyle) -> Unit,
    onToggleDimSurrounding: (Boolean) -> Unit,
    onChangeFontScale: (Float) -> Unit,
    onExitFocusedMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val activeEmotionColor = Color(ttsState.activeEmotion.colorHex)

    // Determine whether the active sentence item is currently visible in the viewport
    val isActiveItemVisible by remember {
        derivedStateOf {
            val targetListIndex = ttsState.currentLineIndex + 1 // +1 for manuscript header item
            listState.layoutInfo.visibleItemsInfo.any { it.index == targetListIndex }
        }
    }

    // Automatic Page Scrolling Effect when currentLineIndex or activeSentenceIndexInLine changes
    LaunchedEffect(
        ttsState.currentLineIndex,
        ttsState.activeSentenceIndexInLine,
        ttsState.isPlaying,
        config.autoScrollEnabled,
        config.scrollAnchor
    ) {
        if (config.autoScrollEnabled && scriptLines.isNotEmpty()) {
            val targetItemIndex = (ttsState.currentLineIndex + 1).coerceAtLeast(0)
            val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            val anchorOffset = if (viewportHeight > 0) {
                -(viewportHeight * config.scrollAnchor.viewportFraction).toInt()
            } else {
                -180
            }
            runCatching {
                listState.animateScrollToItem(
                    index = targetItemIndex,
                    scrollOffset = anchorOffset
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("focused_book_manuscript_reader")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Focused Reading Live Teleprompter & Control Header
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = activeEmotionColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = activeEmotionColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = book?.title ?: "Odaklı Okuma Sayfası",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Odaklı Okuma Modu • ${ttsState.activeSpeaker} (${ttsState.activeEmotion.labelTr})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeEmotionColor
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = config.autoScrollEnabled,
                                onClick = { onToggleAutoScroll(!config.autoScrollEnabled) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesomeMotion,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (config.autoScrollEnabled) "Oto-Kaydır" else "Manuel",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("manuscript_auto_scroll_chip")
                            )

                            OutlinedButton(
                                onClick = onExitFocusedMode,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("exit_focused_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloseFullscreen,
                                    contentDescription = "Sahne Görünümüne Dön",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sahne", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Reader Formatting Strip (Highlight Style + Font Size + Dimming)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        SentenceHighlightStyle.entries.forEach { style ->
                            FilterChip(
                                selected = config.highlightStyle == style,
                                onClick = { onSelectHighlightStyle(style) },
                                label = {
                                    Text(style.labelTr, style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = activeEmotionColor.copy(alpha = 0.22f),
                                    selectedLabelColor = activeEmotionColor
                                )
                            )
                        }

                        FilterChip(
                            selected = config.dimSurroundingSentences,
                            onClick = { onToggleDimSurrounding(!config.dimSurroundingSentences) },
                            label = {
                                Text("Çevre Karartma", style = MaterialTheme.typography.labelSmall)
                            }
                        )

                        listOf(0.95f to "A-", 1.12f to "A", 1.28f to "A+", 1.42f to "A++").forEach { (scale, lbl) ->
                            FilterChip(
                                selected = kotlin.math.abs(config.fontScaleMultiplier - scale) < 0.05f,
                                onClick = { onChangeFontScale(scale) },
                                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // 2. Continuous Flowing Book Page with Highlighted Active Sentence & Auto-Scroll
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("focused_manuscript_lazy_column"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Book Manuscript Title Header
                item(key = "manuscript_header") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = book?.title ?: "Roman Metni",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!book?.author.isNullOrBlank()) {
                            Text(
                                text = book?.author.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Herhangi bir cümleye dokunarak seslendirmeyi o noktaya taşıyabilirsiniz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                itemsIndexed(
                    items = scriptLines,
                    key = { _, line -> "focused_line_${line.id}" }
                ) { index, line ->
                    val isCurrentLine = index == ttsState.currentLineIndex
                    val prevChapter = scriptLines.getOrNull(index - 1)?.chapterTitle
                    val showChapterHeading = index == 0 || (line.chapterTitle.isNotBlank() && line.chapterTitle != prevChapter)

                    val distanceFromActive = kotlin.math.abs(index - ttsState.currentLineIndex)
                    val targetAlpha = when {
                        isCurrentLine -> 1.0f
                        !config.dimSurroundingSentences -> 0.92f
                        distanceFromActive == 1 -> 0.62f
                        else -> 0.42f
                    }
                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 300),
                        label = "focused_line_alpha"
                    )

                    val lineEmotionColor = Color(line.emotionEnum.colorHex)
                    val lineGenderColor = Color(line.genderEnum.badgeColorHex)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showChapterHeading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "  ${line.chapterTitle}  ",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        Surface(
                            color = when {
                                isCurrentLine && config.highlightStyle == SentenceHighlightStyle.SPOTLIGHT_GLOW ->
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                                isCurrentLine ->
                                    lineEmotionColor.copy(alpha = 0.12f)
                                else ->
                                    Color.Transparent
                            },
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = if (isCurrentLine) 6.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(animatedAlpha)
                                .then(
                                    if (isCurrentLine) {
                                        Modifier.border(
                                            width = 2.dp,
                                            brush = Brush.horizontalGradient(
                                                listOf(lineEmotionColor, lineGenderColor)
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onPlayLineAtIndex(index) }
                                .testTag("focused_sentence_block_$index")
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = if (isCurrentLine) 16.dp else 12.dp,
                                    vertical = if (isCurrentLine) 14.dp else 8.dp
                                )
                            ) {
                                // Inline Speaker & Emotion Metadata Tag
                                if (config.showInlineSpeakerTags || isCurrentLine) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(lineGenderColor)
                                            )
                                            Text(
                                                text = line.speakerName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentLine) lineGenderColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "• ${line.emotionEnum.labelTr}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                                color = lineEmotionColor
                                            )
                                        }

                                        if (isCurrentLine) {
                                            Surface(
                                                color = lineEmotionColor.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GraphicEq,
                                                        contentDescription = null,
                                                        tint = lineEmotionColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (ttsState.isPlaying) "Seslendiriliyor" else "Odaktaki Cümle",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = lineEmotionColor
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // Highlighted Book Sentence Text
                                HighlightedScriptSentenceText(
                                    line = line,
                                    isActiveLine = isCurrentLine,
                                    isPlaying = ttsState.isPlaying && isCurrentLine,
                                    activeSentenceIndexInLine = if (isCurrentLine) ttsState.activeSentenceIndexInLine else 0,
                                    activeWordCharIndex = if (isCurrentLine) ttsState.activeWordCharIndex else 0,
                                    activeLineProgress = if (isCurrentLine) ttsState.activeLineProgress else 0f,
                                    highlightStyle = config.highlightStyle,
                                    fontScaleMultiplier = config.fontScaleMultiplier
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating "Re-center on Active Sentence" button when scrolled away
        AnimatedVisibility(
            visible = !isActiveItemVisible && scriptLines.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 92.dp)
        ) {
            Button(
                onClick = {
                    onToggleAutoScroll(true)
                    coroutineScope.launch {
                        val targetItemIndex = (ttsState.currentLineIndex + 1).coerceAtLeast(0)
                        listState.animateScrollToItem(targetItemIndex, scrollOffset = -160)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeEmotionColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("recenter_active_sentence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aktif Cümleye Odaklan (#${ttsState.currentLineIndex + 1})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Floating Mini Transport Bar for Focused Reading Mode
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .border(
                    width = 1.5.dp,
                    color = activeEmotionColor.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("focused_reading_mini_player")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val prev = (ttsState.currentLineIndex - 1).coerceAtLeast(0)
                            onPlayLineAtIndex(prev)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Önceki Cümle")
                    }

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = activeEmotionColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("focused_mode_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (ttsState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ttsState.isPlaying) "Duraklat" else "Oynat"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (scriptLines.isNotEmpty()) {
                                val next = (ttsState.currentLineIndex + 1).coerceAtMost(scriptLines.lastIndex)
                                onPlayLineAtIndex(next)
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Sonraki Cümle")
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Cümle ${if (scriptLines.isNotEmpty()) ttsState.currentLineIndex + 1 else 0} / ${scriptLines.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${ttsState.activeSpeaker} • ${ttsState.activeEmotion.labelTr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeEmotionColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.85f to "0.85x", 1.0f to "1.0x", 1.2f to "1.2x").forEach { (spd, lbl) ->
                        FilterChip(
                            selected = kotlin.math.abs(ttsState.globalSpeedMultiplier - spd) < 0.05f,
                            onClick = { onSpeedChange(spd) },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}
