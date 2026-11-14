package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.ScriptLineEntity
import com.example.model.VoiceGender
import kotlin.math.max
import kotlin.math.roundToInt

enum class TimelineViewMode(val labelTr: String) {
    SWIMLANES("Çok Kanallı Şerit"),
    RIBBON_BLOCKS("Zaman Blokları")
}

data class TimelineSegmentUiModel(
    val lineIndex: Int,
    val lineId: Long,
    val speakerName: String,
    val gender: VoiceGender,
    val emotion: EmotionTone,
    val speakerColorHex: Long,
    val textSnippet: String,
    val charLength: Int,
    val startSeconds: Int,
    val endSeconds: Int,
    val durationSeconds: Int,
    val startFraction: Float,
    val widthFraction: Float
)

data class CharacterTimelineShare(
    val speakerName: String,
    val gender: VoiceGender,
    val colorHex: Long,
    val lineCount: Int,
    val totalDurationSeconds: Int,
    val percentage: Int
)

@Composable
fun CharacterVoiceTimelineCard(
    scriptLines: List<ScriptLineEntity>,
    characters: List<CharacterVoiceEntity>,
    currentLineIndex: Int,
    isPlaying: Boolean,
    globalSpeedMultiplier: Float,
    selectedSpeakerFilter: String?,
    onSelectSpeakerFilter: (String?) -> Unit,
    onSeekToLine: (Int) -> Unit,
    onOpenSpeakerVoiceSettings: (CharacterVoiceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(TimelineViewMode.SWIMLANES) }

    // Map each character to a distinct, high-contrast color
    val speakerColorMap = remember(characters, scriptLines) {
        buildMap<String, Long> {
            characters.forEach { char ->
                put(char.name, char.colorHex)
            }
            scriptLines.forEach { line ->
                if (!containsKey(line.speakerName)) {
                    put(line.speakerName, line.genderEnum.badgeColorHex)
                }
            }
        }
    }

    // Build proportional timeline segments with start/end timestamps
    val timelineSegments = remember(scriptLines, globalSpeedMultiplier, speakerColorMap) {
        if (scriptLines.isEmpty()) return@remember emptyList<TimelineSegmentUiModel>()

        val effectiveSpeed = globalSpeedMultiplier.coerceIn(0.6f, 1.8f)
        // Estimate reading duration in seconds (~14 chars per second in Turkish TTS, min 3s per segment)
        val rawDurations = scriptLines.map { line ->
            val baseSec = (line.text.length / 13.5f) / effectiveSpeed
            max(3, baseSec.roundToInt())
        }
        val totalSec = rawDurations.sum().coerceAtLeast(1)

        var accumulatedSec = 0
        scriptLines.mapIndexed { index, line ->
            val segDuration = rawDurations[index]
            val startSec = accumulatedSec
            val endSec = accumulatedSec + segDuration
            accumulatedSec = endSec

            val startFrac = startSec.toFloat() / totalSec.toFloat()
            val widthFrac = (segDuration.toFloat() / totalSec.toFloat()).coerceAtLeast(0.015f)
            val colorHex = speakerColorMap[line.speakerName] ?: line.genderEnum.badgeColorHex

            TimelineSegmentUiModel(
                lineIndex = index,
                lineId = line.id,
                speakerName = line.speakerName,
                gender = line.genderEnum,
                emotion = line.emotionEnum,
                speakerColorHex = colorHex,
                textSnippet = line.text,
                charLength = line.text.length,
                startSeconds = startSec,
                endSeconds = endSec,
                durationSeconds = segDuration,
                startFraction = startFrac,
                widthFraction = widthFrac
            )
        }
    }

    val totalDurationSec = remember(timelineSegments) {
        timelineSegments.lastOrNull()?.endSeconds ?: 0
    }

    val currentSegment = timelineSegments.getOrNull(currentLineIndex.coerceIn(0, max(0, timelineSegments.lastIndex)))

    // Compute per-character share of narration
    val characterShares = remember(timelineSegments, totalDurationSec) {
        if (timelineSegments.isEmpty() || totalDurationSec <= 0) return@remember emptyList<CharacterTimelineShare>()
        timelineSegments
            .groupBy { it.speakerName }
            .map { (speaker, segs) ->
                val sumSec = segs.sumOf { it.durationSeconds }
                val pct = ((sumSec.toFloat() / totalDurationSec.toFloat()) * 100f).roundToInt().coerceIn(1, 100)
                val first = segs.first()
                CharacterTimelineShare(
                    speakerName = speaker,
                    gender = first.gender,
                    colorHex = first.speakerColorHex,
                    lineCount = segs.size,
                    totalDurationSeconds = sumSec,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalDurationSeconds }
    }

    val blocksListState = rememberLazyListState()
    LaunchedEffect(currentLineIndex, viewMode) {
        if (timelineSegments.isNotEmpty() && currentLineIndex in timelineSegments.indices) {
            blocksListState.animateScrollToItem(currentLineIndex)
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("character_voice_timeline_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title + Elapsed/Total Time Badge + View Mode Toggle + Expand/Collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ViewTimeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seslendirme Zaman Çizelgesi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatSecondsToMmSs(currentSegment?.startSeconds ?: 0)} / ${formatSecondsToMmSs(totalDurationSec)} • ${characterShares.size} Karakter • ${timelineSegments.size} Bölüm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimelineViewMode.entries.forEach { mode ->
                        val selected = viewMode == mode
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable {
                                    viewMode = mode
                                    isExpanded = true
                                }
                                .testTag("timeline_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.labelTr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("toggle_timeline_expand_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Zaman Çizelgesini Daralt" else "Zaman Çizelgesini Genişlet"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Always-visible Proportional Overview Scrubber Bar (shows whole book text distribution)
            ProportionalOverviewRibbon(
                segments = timelineSegments,
                currentLineIndex = currentLineIndex,
                selectedSpeakerFilter = selectedSpeakerFilter,
                onSeekToLine = onSeekToLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("proportional_timeline_ribbon")
            )

            // Time markers under the ribbon
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp)
            ) {
                Text(
                    text = "00:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                currentSegment?.let { seg ->
                    Text(
                        text = "▶ ${seg.speakerName} (${formatSecondsToMmSs(seg.startSeconds)} - ${formatSecondsToMmSs(seg.endSeconds)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(seg.speakerColorHex),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 6.dp)
                    )
                }
                Text(
                    text = formatSecondsToMmSs(totalDurationSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Character Distribution & Filter Chips (Interactive Legend)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("timeline_character_filter_row")
                    ) {
                        FilterChip(
                            selected = selectedSpeakerFilter == null,
                            onClick = { onSelectSpeakerFilter(null) },
                            label = {
                                Text(
                                    text = "Tümü (%100)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("timeline_filter_all")
                        )

                        characterShares.forEach { share ->
                            val isSelected = selectedSpeakerFilter == share.speakerName
                            val charColor = Color(share.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onSelectSpeakerFilter(
                                        if (isSelected) null else share.speakerName
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(charColor)
                                    )
                                },
                                trailingIcon = {
                                    val matchingEntity = characters.find { it.name == share.speakerName }
                                    if (matchingEntity != null) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "${share.speakerName} Ses Ayarı",
                                            tint = charColor,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    onOpenSpeakerVoiceSettings(matchingEntity)
                                                }
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "${share.speakerName} • %${share.percentage} (${share.lineCount} kısım)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = charColor.copy(alpha = 0.22f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("timeline_filter_${share.speakerName}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Detailed View Mode: Multi-Track Swimlanes OR Interactive Segment Blocks
                    when (viewMode) {
                        TimelineViewMode.SWIMLANES -> {
                            MultiTrackCharacterSwimlanes(
                                characterShares = characterShares,
                                segments = timelineSegments,
                                currentLineIndex = currentLineIndex,
                                selectedSpeakerFilter = selectedSpeakerFilter,
                                onSeekToLine = onSeekToLine
                            )
                        }

                        TimelineViewMode.RIBBON_BLOCKS -> {
                            val filteredSegments = remember(timelineSegments, selectedSpeakerFilter) {
                                if (selectedSpeakerFilter == null) {
                                    timelineSegments
                                } else {
                                    timelineSegments.filter { it.speakerName == selectedSpeakerFilter }
                                }
                            }

                            LazyRow(
                                state = blocksListState,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("timeline_blocks_lazy_row")
                            ) {
                                items(filteredSegments, key = { it.lineId }) { segment ->
                                    TimelineSegmentBlockCard(
                                        segment = segment,
                                        isActive = segment.lineIndex == currentLineIndex,
                                        isPlaying = isPlaying && segment.lineIndex == currentLineIndex,
                                        onClick = { onSeekToLine(segment.lineIndex) }
                                    )
                                }
                            }
                        }
                    }

                    // Active Segment Preview Bar at bottom of Timeline Card
                    currentSegment?.let { activeSeg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        ActiveTimelineSegmentPreviewStrip(
                            segment = activeSeg,
                            totalSegments = timelineSegments.size,
                            onJumpClick = { onSeekToLine(activeSeg.lineIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProportionalOverviewRibbon(
    segments: List<TimelineSegmentUiModel>,
    currentLineIndex: Int,
    selectedSpeakerFilter: String?,
    onSeekToLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(segments) {
                detectTapGestures { tapOffset ->
                    if (segments.isEmpty() || size.width <= 0) return@detectTapGestures
                    val tapFraction = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val target = segments.firstOrNull { seg ->
                        tapFraction >= seg.startFraction && tapFraction <= (seg.startFraction + seg.widthFraction)
                    } ?: segments.minByOrNull { seg ->
                        kotlin.math.abs((seg.startFraction + seg.widthFraction / 2f) - tapFraction)
                    }
                    if (target != null) {
                        onSeekToLine(target.lineIndex)
                    }
                }
            }
    ) {
        if (segments.isEmpty()) return@Canvas

        val totalW = size.width
        val totalH = size.height
        val gapPx = 2.dp.toPx()

        segments.forEach { seg ->
            val startX = seg.startFraction * totalW
            val segW = max(4f, (seg.widthFraction * totalW) - gapPx)
            val isCurrent = seg.lineIndex == currentLineIndex
            val isFilteredIn = selectedSpeakerFilter == null || seg.speakerName == selectedSpeakerFilter

            val baseColor = Color(seg.speakerColorHex)
            val fillAlpha = when {
                isCurrent -> 1.0f
                isFilteredIn -> 0.78f
                else -> 0.18f
            }

            val topY = if (isCurrent) 0f else totalH * 0.14f
            val rectH = if (isCurrent) totalH else totalH * 0.72f

            drawRoundRect(
                color = baseColor.copy(alpha = fillAlpha),
                topLeft = Offset(startX, topY),
                size = Size(segW, rectH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw a small emotion accent bar along the bottom of each segment
            val emotionColor = Color(seg.emotion.colorHex)
            drawRoundRect(
                color = emotionColor.copy(alpha = if (isFilteredIn) 0.95f else 0.25f),
                topLeft = Offset(startX, topY + rectH - 4.dp.toPx()),
                size = Size(segW, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Draw Playhead indicator on the active segment
            if (isCurrent) {
                val centerX = (startX + segW / 2f).coerceIn(6f, totalW - 6f)
                drawLine(
                    color = outlineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, totalH),
                    strokeWidth = 2.5.dp.toPx()
                )
                val trianglePath = Path().apply {
                    moveTo(centerX - 5.dp.toPx(), 0f)
                    lineTo(centerX + 5.dp.toPx(), 0f)
                    lineTo(centerX, 6.dp.toPx())
                    close()
                }
                drawPath(path = trianglePath, color = outlineColor)
            }
        }
    }
}

@Composable
private fun MultiTrackCharacterSwimlanes(
    characterShares: List<CharacterTimelineShare>,
    segments: List<TimelineSegmentUiModel>,
    currentLineIndex: Int,
    selectedSpeakerFilter: String?,
    onSeekToLine: (Int) -> Unit
) {
    val activeIndicatorColor = MaterialTheme.colorScheme.onSurface

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_swimlanes_container")
    ) {
        Text(
                            text = "Çok Kanallı Karakter Ses Şeritleri (İlgili bölüme atlamak için renkli bloğa dokunun):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        characterShares.forEach { share ->
            val isLaneHighlighted = selectedSpeakerFilter == null || selectedSpeakerFilter == share.speakerName
            val charColor = Color(share.colorHex)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag("swimlane_row_${share.speakerName}")
            ) {
                // Left character label column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(112.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(charColor.copy(alpha = if (isLaneHighlighted) 1f else 0.35f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = share.speakerName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLaneHighlighted) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "%${share.percentage} • ${formatSecondsToMmSs(share.totalDurationSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = charColor.copy(alpha = if (isLaneHighlighted) 0.9f else 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right horizontal timeline track for this character
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .pointerInput(segments, share.speakerName) {
                            detectTapGestures { tapOffset ->
                                if (segments.isEmpty() || size.width <= 0) return@detectTapGestures
                                val tapFrac = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                // Prefer segments belonging to this character lane first
                                val laneSegments = segments.filter { it.speakerName == share.speakerName }
                                val exactMatch = laneSegments.firstOrNull { seg ->
                                    tapFrac >= seg.startFraction && tapFrac <= (seg.startFraction + seg.widthFraction)
                                } ?: laneSegments.minByOrNull { seg ->
                                    kotlin.math.abs((seg.startFraction + seg.widthFraction / 2f) - tapFrac)
                                }
                                if (exactMatch != null) {
                                    onSeekToLine(exactMatch.lineIndex)
                                }
                            }
                        }
                ) {
                    val trackW = size.width
                    val trackH = size.height

                    // Draw segments voiced by this character
                    segments.forEach { seg ->
                        val startX = seg.startFraction * trackW
                        val segW = max(5f, seg.widthFraction * trackW - 1.5.dp.toPx())
                        val isCurrent = seg.lineIndex == currentLineIndex

                        if (seg.speakerName == share.speakerName) {
                            val emotionColor = Color(seg.emotion.colorHex)
                            drawRoundRect(
                                color = charColor.copy(alpha = if (isCurrent) 1.0f else if (isLaneHighlighted) 0.85f else 0.3f),
                                topLeft = Offset(startX, 3.dp.toPx()),
                                size = Size(segW, trackH - 6.dp.toPx()),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            // Top emotion indicator stripe
                            drawRoundRect(
                                color = emotionColor,
                                topLeft = Offset(startX, 3.dp.toPx()),
                                size = Size(segW, 3.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }

                        // Draw vertical playhead across all swimlanes so user sees sync position
                        if (isCurrent) {
                            val playheadX = (startX + segW / 2f).coerceIn(2f, trackW - 2f)
                            drawLine(
                                color = activeIndicatorColor,
                                start = Offset(playheadX, 0f),
                                end = Offset(playheadX, trackH),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSegmentBlockCard(
    segment: TimelineSegmentUiModel,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val speakerColor = Color(segment.speakerColorHex)
    val emotionColor = Color(segment.emotion.colorHex)

    Surface(
        color = if (isActive) {
            speakerColor.copy(alpha = 0.20f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(195.dp)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) speakerColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("timeline_block_${segment.lineIndex}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(speakerColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = segment.speakerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${formatSecondsToMmSs(segment.startSeconds)}-${formatSecondsToMmSs(segment.endSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = speakerColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = emotionColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = segment.emotion.labelTr,
                        style = MaterialTheme.typography.labelSmall,
                        color = emotionColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Bölüm #${segment.lineIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = segment.textSnippet,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = if (segment.gender == VoiceGender.NARRATOR) FontStyle.Normal else FontStyle.Italic,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveTimelineSegmentPreviewStrip(
    segment: TimelineSegmentUiModel,
    totalSegments: Int,
    onJumpClick: () -> Unit
) {
    val speakerColor = Color(segment.speakerColorHex)
    val emotionColor = Color(segment.emotion.colorHex)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJumpClick() }
            .testTag("active_timeline_segment_strip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = speakerColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bölüm ${segment.lineIndex + 1}/$totalSegments • ${segment.speakerName} (${segment.gender.labelTr})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = speakerColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${segment.emotion.labelTr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = emotionColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = segment.textSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Bu Bölümü Seslendir",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatSecondsToMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}
