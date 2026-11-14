package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.NarrationLanguage
import com.example.model.ScriptLineEntity
import com.example.model.VoiceGender
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class PunctuationPauseType(
    val symbolLabelTr: String,
    val basePauseMs: Long,
    private val endsSentence: Boolean = false
) {
    COMMA("Virgül (,)", 550L),
    SEMICOLON_COLON("Noktalı Virgül / İki Nokta (; :)", 760L),
    DASH("Ara Çizgi (—)", 680L),
    PERIOD("Nokta (.)", 1100L, endsSentence = true),
    QUESTION_EXCLAMATION("Soru / Ünlem (? !)", 1250L, endsSentence = true),
    ELLIPSIS("Üç Nokta (...)", 1550L, endsSentence = true),
    NONE("", 0L);

    fun adjustPause(durationMs: Long): Long = if (endsSentence) durationMs / 2 else durationMs
}

private data class PunctuationClause(
    val clauseText: String,
    val charRange: IntRange,
    val sentenceIndex: Int,
    val sentenceText: String,
    val pauseType: PunctuationPauseType
)

data class TtsPlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val currentLineIndex: Int = 0,
    val activeLanguage: NarrationLanguage = NarrationLanguage.TURKISH,
    val emotionProcessingEnabled: Boolean = true,
    val activeSpeaker: String = "Anlatıcı",
    val activeGender: VoiceGender = VoiceGender.NARRATOR,
    val activeEmotion: EmotionTone = EmotionTone.CALM,
    val activeTimbreLabel: String = "Teatral & Vurgulu",
    val effectivePitch: Float = 1.0f,
    val effectiveRate: Float = 1.0f,
    val emotionModulationDepth: Float = 1.15f,
    val globalSpeedMultiplier: Float = 1.0f,
    val punctuationPauseMultiplier: Float = 1.35f,
    val activePunctuationPauseLabel: String? = null,
    val theatricalEmotionBoost: Boolean = true,
    val engineStatusLabel: String = "Ses Motoru Hazırlanıyor...",
    val availableVoiceCount: Int = 1,
    // Real-time sentence & word tracking for Focused Reading Mode ('Odaklı Okuma Modu')
    val activeLineProgress: Float = 0f,
    val activeSentenceIndexInLine: Int = 0,
    val activeSentenceCountInLine: Int = 1,
    val activeSentenceText: String = "",
    val activeWordCharIndex: Int = 0
)

class EmotionalMultiVoiceTtsEngine(context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            pausePlayback()
        }
    }
    private var focusRequest: AudioFocusRequest? = null
    private var holdsFocus = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var playbackJob: Job? = null
    private var engineInitialized = false

    private val _state = MutableStateFlow(TtsPlaybackState())
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    private var cachedVoices: List<Voice> = emptyList()
    private var currentCharacterMap: Map<String, CharacterVoiceEntity> = emptyMap()

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext, this)
        }.onFailure {
            _state.update {
                it.copy(
                    isReady = false,
                    engineStatusLabel = "Cihazın ses motoru başlatılamadı."
                )
            }
        }
    }

    override fun onInit(status: Int) {
        val engine = tts
        if (status == TextToSpeech.SUCCESS && engine != null) {
            engineInitialized = true
            engine.setAudioAttributes(audioAttributes)
            cachedVoices = runCatching {
                engine.voices?.filter { !it.isNetworkConnectionRequired }?.toList().orEmpty()
            }.getOrDefault(emptyList())

            applyLanguageToEngine(engine, _state.value.activeLanguage)
        } else {
            _state.update {
                it.copy(
                    isReady = false,
                    engineStatusLabel = "Cihazın ses motoru başlatılamadı."
                )
            }
        }
    }

    fun setNarrationLanguage(language: NarrationLanguage) {
        _state.update { it.copy(activeLanguage = language) }
        val engine = tts
        if (engine != null && engineInitialized) {
            applyLanguageToEngine(engine, language)
        } else {
            _state.update {
                it.copy(
                    engineStatusLabel = "${language.labelTr} (${language.bcp47Tag}) Çoklu Ses & Duygu Sentezi"
                )
            }
        }
    }

    fun setEmotionProcessingEnabled(enabled: Boolean) {
        _state.update { it.copy(emotionProcessingEnabled = enabled) }
    }

    private fun applyLanguageToEngine(engine: TextToSpeech, language: NarrationLanguage) {
        val targetLocale = language.toLocale()
        val result = runCatching { engine.setLanguage(targetLocale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val finalResult = if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { engine.setLanguage(Locale(language.code)) }.getOrDefault(result)
        } else {
            result
        }

        val matchingLangVoices = cachedVoices.count {
            it.locale.language.equals(language.code, ignoreCase = true)
        }

        val supported = finalResult != TextToSpeech.LANG_MISSING_DATA &&
            finalResult != TextToSpeech.LANG_NOT_SUPPORTED
        _state.update {
            it.copy(
                isReady = supported,
                activeLanguage = language,
                engineStatusLabel = if (supported) "Ses motoru hazır."
                    else "Seçilen dil için cihazın ses paketi eksik veya desteklenmiyor.",
                availableVoiceCount = matchingLangVoices
            )
        }
    }

    fun setGlobalSpeed(multiplier: Float) {
        _state.update { it.copy(globalSpeedMultiplier = multiplier.coerceIn(0.70f, 1.60f)) }
    }

    fun setPunctuationPauseMultiplier(multiplier: Float) {
        _state.update { it.copy(punctuationPauseMultiplier = multiplier.coerceIn(0.75f, 2.50f)) }
    }

    fun toggleTheatricalBoost(enabled: Boolean) {
        _state.update { it.copy(theatricalEmotionBoost = enabled) }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(true)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        holdsFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!holdsFocus) {
            _state.update { it.copy(engineStatusLabel = "Seslendirme kesildi. Ses odağı alınamadı.") }
        }
        return holdsFocus
    }

    private fun abandonAudioFocus() {
        if (!holdsFocus) return
        holdsFocus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    fun startOrResumePlayback(
        lines: List<ScriptLineEntity>,
        characters: List<CharacterVoiceEntity>,
        startIndex: Int = _state.value.currentLineIndex,
        language: NarrationLanguage = _state.value.activeLanguage
    ) {
        if (lines.isEmpty()) return
        currentCharacterMap = characters.associateBy { it.name }

        if (_state.value.activeLanguage != language) {
            setNarrationLanguage(language)
        }

        if (!_state.value.isReady) return
        val safeIndex = startIndex.coerceIn(0, lines.lastIndex)
        pausePlayback()
        if (!requestAudioFocus()) return
        playbackJob = scope.launch {
            _state.update {
                it.copy(
                    isPlaying = true,
                    currentLineIndex = safeIndex,
                    activeLanguage = language,
                    engineStatusLabel = "Ses motoru hazır.",
                    activePunctuationPauseLabel = null
                )
            }
            for (index in safeIndex..lines.lastIndex) {
                if (!_state.value.isPlaying) break
                val line = lines[index]
                val profile = currentCharacterMap[line.speakerName]
                if (!speakSingleLineAndAwait(line, index, language)) break
                if (!_state.value.isPlaying) break
                val charPauseScale = if (_state.value.emotionProcessingEnabled) {
                    (profile?.dramaticPauseScale ?: 1.0f).coerceIn(0.6f, 2.0f)
                } else {
                    0.85f
                }
                val puncScale = _state.value.punctuationPauseMultiplier.coerceIn(0.75f, 2.50f)
                val interLinePauseMs =
                    (line.emotionEnum.pauseAfterMs * charPauseScale * (0.65f + 0.35f * puncScale)).toLong() / 2
                _state.update {
                    it.copy(activePunctuationPauseLabel = "Paragraf / Replik Geçiş Esi (${interLinePauseMs} ms)")
                }
                awaitInterruptiblePause(interLinePauseMs)
                _state.update { it.copy(activePunctuationPauseLabel = null) }
            }
            abandonAudioFocus()
            _state.update { it.copy(isPlaying = false, activePunctuationPauseLabel = null) }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        abandonAudioFocus()
        runCatching { tts?.stop() }
        _state.update { it.copy(isPlaying = false, activePunctuationPauseLabel = null) }
    }

    fun stopPlayback(lineIndex: Int = 0) {
        playbackJob?.cancel()
        abandonAudioFocus()
        runCatching { tts?.stop() }
        _state.update {
            it.copy(
                isPlaying = false,
                currentLineIndex = lineIndex.coerceAtLeast(0),
                activePunctuationPauseLabel = null,
                activeLineProgress = 0f,
                activeSentenceIndexInLine = 0,
                activeWordCharIndex = 0
            )
        }
    }

    fun testCharacterVoice(
        character: CharacterVoiceEntity,
        emotion: EmotionTone,
        customSampleText: String? = null,
        language: NarrationLanguage = _state.value.activeLanguage
    ) {
        pausePlayback()
        if (_state.value.activeLanguage != language) {
            setNarrationLanguage(language)
        }
        if (!_state.value.isReady || !requestAudioFocus()) return
        playbackJob = scope.launch {
            val sampleText = customSampleText?.takeIf { it.isNotBlank() }
                ?: language.sampleVoiceTestPhrase(character.name, emotion)

            val previewLine = ScriptLineEntity(
                id = -1,
                bookId = character.bookId,
                orderIndex = 0,
                chapterTitle = "Ses Testi (${language.labelTr})",
                speakerName = character.name,
                gender = character.gender,
                emotion = emotion.name,
                stageDirection = "${language.labelTr} • ${character.timbrePresetEnum.labelTr}",
                text = sampleText
            )
            currentCharacterMap = mapOf(character.name to character)
            _state.update { it.copy(isPlaying = true, activeLanguage = language, activePunctuationPauseLabel = null) }
            speakSingleLineAndAwait(previewLine, _state.value.currentLineIndex, language)
            abandonAudioFocus()
            _state.update { it.copy(isPlaying = false, activePunctuationPauseLabel = null) }
        }
    }

    private suspend fun speakSingleLineAndAwait(
        line: ScriptLineEntity,
        index: Int,
        language: NarrationLanguage
    ): Boolean {
        val profile = currentCharacterMap[line.speakerName]
        val gender = profile?.genderEnum ?: line.genderEnum
        val emotion = if (_state.value.emotionProcessingEnabled) line.emotionEnum else EmotionTone.CALM
        val timbre = profile?.timbrePresetEnum
        val basePitch = (profile?.basePitch ?: gender.defaultBasePitch) + (timbre?.pitchOffset ?: 0f)
        val baseRate = (profile?.baseRate ?: gender.defaultBaseRate) + (timbre?.rateOffset ?: 0f)

        val charEmotionIntensity = if (_state.value.emotionProcessingEnabled) {
            (profile?.emotionIntensity ?: 1.15f).coerceIn(0.30f, 2.00f)
        } else {
            0.0f
        }
        val charPitchModRange = (profile?.pitchModulationRange ?: 0.14f).coerceIn(0.00f, 0.40f)
        val globalBoost = if (_state.value.theatricalEmotionBoost) 1.20f else 0.90f

        val inflectionBonus = if (!_state.value.emotionProcessingEnabled) {
            0f
        } else {
            when {
                line.text.contains("?!") || line.text.endsWith("!") || line.text.startsWith("¡") -> charPitchModRange * 0.45f
                line.text.endsWith("?") || line.text.startsWith("¿") -> charPitchModRange * 0.35f
                else -> 0f
            }
        }

        val effectivePitch = (
            basePitch + (emotion.pitchDelta * charEmotionIntensity * globalBoost) + inflectionBonus
        ).coerceIn(0.50f, 1.95f)

        val emotionRateDelta = (emotion.rateMultiplier - 1.0f) * charEmotionIntensity
        val effectiveRate = (
            baseRate * (1.0f + emotionRateDelta) * _state.value.globalSpeedMultiplier
        ).coerceIn(0.55f, 1.85f)

        val volumeLevel = if (!_state.value.emotionProcessingEnabled) {
            1.0f
        } else {
            when (emotion) {
                EmotionTone.WHISPER -> (profile?.whisperVolumeScale ?: 0.70f).coerceIn(0.35f, 1.0f)
                EmotionTone.MELANCHOLIC -> ((profile?.whisperVolumeScale ?: 0.75f) + 0.12f).coerceIn(0.50f, 1.0f)
                else -> 1.0f
            }
        }

        val sentenceRanges = splitTextIntoSentenceRanges(line.text)
        val clauses = splitTextIntoPunctuationClauses(line.text, sentenceRanges)
        val initialSentenceText = sentenceRanges.firstOrNull()?.let { range ->
            line.text.substring(range.first, (range.last + 1).coerceAtMost(line.text.length)).trim()
        } ?: line.text

        _state.update {
            it.copy(
                currentLineIndex = index,
                activeLanguage = language,
                activeSpeaker = line.speakerName,
                activeGender = gender,
                activeEmotion = emotion,
                activeTimbreLabel = timbre?.labelTr ?: "Teatral & Vurgulu",
                effectivePitch = effectivePitch,
                effectiveRate = effectiveRate,
                emotionModulationDepth = charEmotionIntensity,
                activePunctuationPauseLabel = null,
                activeLineProgress = 0f,
                activeSentenceIndexInLine = 0,
                activeSentenceCountInLine = sentenceRanges.size.coerceAtLeast(1),
                activeSentenceText = initialSentenceText,
                activeWordCharIndex = 0
            )
        }

        val totalChars = line.text.length.coerceAtLeast(1)
        val engine = tts ?: return false
        if (!_state.value.isReady) return false
        selectMatchingSystemVoiceIfAvailable(engine, gender, line.speakerName, language)

        val maxInput = TextToSpeech.getMaxSpeechInputLength()
        val speechClauses = clauses.flatMap { clause ->
            buildList {
                var offset = 0
                while (offset < clause.clauseText.length) {
                    var end = (offset + maxInput).coerceAtMost(clause.clauseText.length)
                    if (end < clause.clauseText.length && Character.isHighSurrogate(clause.clauseText[end - 1])) end--
                    val start = clause.charRange.first + offset
                    add(clause.copy(
                        clauseText = clause.clauseText.substring(offset, end),
                        charRange = start until clause.charRange.first + end,
                        pauseType = if (end == clause.clauseText.length) clause.pauseType else PunctuationPauseType.NONE
                    ))
                    offset = end
                }
            }
        }
        for ((clauseIdx, clause) in speechClauses.withIndex()) {
            if (!_state.value.isPlaying) break

            val clauseInflection = if (!_state.value.emotionProcessingEnabled) {
                0f
            } else {
                when (clause.pauseType) {
                    PunctuationPauseType.QUESTION_EXCLAMATION -> charPitchModRange * 0.30f
                    PunctuationPauseType.ELLIPSIS -> -charPitchModRange * 0.18f
                    else -> 0f
                }
            }
            val clausePitch = (effectivePitch + clauseInflection).coerceIn(0.50f, 1.95f)

            val completion = CompletableDeferred<Boolean>()
            val engineReportedClauseCharOffset = AtomicInteger(-1)
            val utteranceId = "utt_${index}_${clauseIdx}_${System.currentTimeMillis()}"

            _state.update {
                it.copy(
                    activePunctuationPauseLabel = null,
                    activeSentenceIndexInLine = clause.sentenceIndex,
                    activeSentenceCountInLine = sentenceRanges.size.coerceAtLeast(1),
                    activeSentenceText = clause.sentenceText,
                    activeWordCharIndex = clause.charRange.first.coerceIn(0, line.text.lastIndex.coerceAtLeast(0))
                )
            }

            try {
                engine.setPitch(clausePitch)
                engine.setSpeechRate(effectiveRate)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                        if (id == utteranceId) engineReportedClauseCharOffset.set(start)
                    }
                    override fun onDone(id: String?) {
                        if (id == utteranceId) completion.complete(true)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId) completion.complete(false)
                    }
                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId) completion.complete(false)
                    }
                    override fun onStop(id: String?, interrupted: Boolean) {
                        if (id == utteranceId) completion.complete(false)
                    }
                })
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel) }
                if (engine.speak(clause.clauseText, TextToSpeech.QUEUE_ADD, params, utteranceId) == TextToSpeech.ERROR) {
                    completion.complete(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                completion.complete(false)
            }

            val clauseWordCount = clause.clauseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
            val estimatedClauseDurationMs = ((clauseWordCount * 365L) / effectiveRate).toLong().coerceIn(650L, 8500L)
            val clauseStartTime = android.os.SystemClock.elapsedRealtime()
            val clauseLength = (clause.charRange.last - clause.charRange.first + 1).coerceAtLeast(1)

            val timeout = (clauseWordCount * 1400L).coerceIn(30_000L, 600_000L)
            val spoken = withTimeoutOrNull(timeout) {
                while (!completion.isCompleted) {
                    val elapsed = android.os.SystemClock.elapsedRealtime() - clauseStartTime
                    val clauseTimeProgress = (elapsed.toFloat() / estimatedClauseDurationMs.toFloat()).coerceIn(0f, 1f)
                    val reportedOffset = engineReportedClauseCharOffset.get()
                    val localCharOffset = if (reportedOffset >= 0) {
                        reportedOffset.coerceIn(0, clauseLength - 1)
                    } else {
                        (clauseTimeProgress * clauseLength).toInt().coerceIn(0, clauseLength - 1)
                    }
                    val globalCharPos = (clause.charRange.first + localCharOffset)
                        .coerceIn(0, line.text.lastIndex.coerceAtLeast(0))
                    val globalProgress = ((globalCharPos + 1).toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)

                    _state.update {
                        it.copy(
                            activeLineProgress = globalProgress,
                            activeSentenceIndexInLine = clause.sentenceIndex,
                            activeSentenceCountInLine = sentenceRanges.size.coerceAtLeast(1),
                            activeSentenceText = clause.sentenceText,
                            activeWordCharIndex = globalCharPos
                        )
                    }

                    delay(75L)
                }
                completion.await()
            }
            if (spoken != true) {
                engine.stop()
                _state.update { it.copy(engineStatusLabel = "Seslendirme kesildi. Cihazın ses motorunu kontrol edin.") }
                return false
            }

            // Explicit natural punctuation pause after this clause's punctuation mark
            if (_state.value.isPlaying && clause.pauseType != PunctuationPauseType.NONE) {
                val pauseMs = computePunctuationPauseDurationMs(
                    pauseType = clause.pauseType,
                    emotion = emotion,
                    profile = profile
                )
                if (pauseMs > 0L) {
                    val endCharPos = clause.charRange.last.coerceIn(0, line.text.lastIndex.coerceAtLeast(0))
                    val endProgress = ((endCharPos + 1).toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
                    _state.update {
                        it.copy(
                            activeLineProgress = endProgress,
                            activeWordCharIndex = endCharPos,
                            activePunctuationPauseLabel = "${clause.pauseType.symbolLabelTr} • ${pauseMs} ms es"
                        )
                    }
                    awaitInterruptiblePause(pauseMs)
                    _state.update { it.copy(activePunctuationPauseLabel = null) }
                }
            }
        }

        _state.update {
            it.copy(
                activeLineProgress = 1f,
                activePunctuationPauseLabel = null
            )
        }
        return true
    }

    private fun computePunctuationPauseDurationMs(
        pauseType: PunctuationPauseType,
        emotion: EmotionTone,
        profile: CharacterVoiceEntity?
    ): Long {
        if (pauseType == PunctuationPauseType.NONE) return 0L
        val userPunctuationScale = _state.value.punctuationPauseMultiplier.coerceIn(0.75f, 2.50f)
        val charPauseScale = if (_state.value.emotionProcessingEnabled) {
            (profile?.dramaticPauseScale ?: 1.0f).coerceIn(0.65f, 2.0f)
        } else {
            0.90f
        }
        val emotionPauseFactor = if (!_state.value.emotionProcessingEnabled) {
            1.0f
        } else {
            when (emotion) {
                EmotionTone.MELANCHOLIC -> 1.28f
                EmotionTone.WHISPER -> 1.24f
                EmotionTone.DRAMATIC -> 1.25f
                EmotionTone.CALM -> 1.12f
                EmotionTone.FEARFUL -> 1.15f
                EmotionTone.SURPRISED -> 1.10f
                EmotionTone.JOYFUL -> 0.96f
                EmotionTone.ANGRY -> 0.92f
            }
        }
        val theatricalFactor = if (_state.value.theatricalEmotionBoost) 1.12f else 1.0f
        val speedCompensation = (1.0f / _state.value.globalSpeedMultiplier.coerceIn(0.75f, 1.35f)).coerceIn(0.80f, 1.30f)

        val pauseMs = (
            pauseType.basePauseMs *
                userPunctuationScale *
                charPauseScale *
                emotionPauseFactor *
                theatricalFactor *
                speedCompensation
            ).toLong().coerceIn(280L, 4200L)
        return pauseType.adjustPause(pauseMs)
    }

    private suspend fun awaitInterruptiblePause(durationMs: Long) {
        var remaining = durationMs
        val step = 75L
        while (remaining > 0L && _state.value.isPlaying) {
            val sleepTime = remaining.coerceAtMost(step)
            delay(sleepTime)
            remaining -= sleepTime
        }
    }

    private fun splitTextIntoPunctuationClauses(
        text: String,
        sentenceRanges: List<IntRange>
    ): List<PunctuationClause> {
        if (text.isBlank()) {
            return listOf(
                PunctuationClause(
                    clauseText = text,
                    charRange = 0..0,
                    sentenceIndex = 0,
                    sentenceText = text,
                    pauseType = PunctuationPauseType.NONE
                )
            )
        }

        val clauses = mutableListOf<PunctuationClause>()
        var clauseStart = 0
        var i = 0

        fun sentenceIndexForChar(charPos: Int): Int {
            val found = sentenceRanges.indexOfFirst { charPos in it }
            return if (found >= 0) found else (sentenceRanges.size - 1).coerceAtLeast(0)
        }

        fun sentenceTextForIndex(sIdx: Int): String {
            val range = sentenceRanges.getOrNull(sIdx) ?: return text
            return text.substring(range.first, (range.last + 1).coerceAtMost(text.length)).trim()
        }

        while (i < text.length) {
            val ch = text[i]
            val pauseType: PunctuationPauseType? = when {
                ch == '…' -> PunctuationPauseType.ELLIPSIS
                ch == '.' && i + 2 < text.length && text[i + 1] == '.' && text[i + 2] == '.' -> PunctuationPauseType.ELLIPSIS
                ch == '.' && (i + 1 >= text.length || !text[i + 1].isDigit()) -> PunctuationPauseType.PERIOD
                ch == '!' || ch == '?' -> PunctuationPauseType.QUESTION_EXCLAMATION
                ch == ';' || ch == ':' -> PunctuationPauseType.SEMICOLON_COLON
                ch == ',' -> PunctuationPauseType.COMMA
                ch == '—' || ch == '–' -> PunctuationPauseType.DASH
                else -> null
            }

            if (pauseType != null) {
                var endIdx = i
                // Consume consecutive punctuation or closing quotes belonging to this mark
                while (endIdx + 1 < text.length) {
                    val nextCh = text[endIdx + 1]
                    if (nextCh == '.' || nextCh == '!' || nextCh == '?' || nextCh == '…' ||
                        nextCh == '"' || nextCh == '”' || nextCh == '\'' || nextCh == '»' || nextCh == ')'
                    ) {
                        endIdx++
                    } else {
                        break
                    }
                }
                val finalPauseType = if (
                    (endIdx - i >= 2 && text.substring(i..endIdx).contains("...")) ||
                    text.substring(i..endIdx).contains("…")
                ) {
                    PunctuationPauseType.ELLIPSIS
                } else if (text.substring(i..endIdx).any { it == '?' || it == '!' }) {
                    PunctuationPauseType.QUESTION_EXCLAMATION
                } else {
                    pauseType
                }

                val rawSlice = text.substring(clauseStart, endIdx + 1).trim()
                if (rawSlice.isNotEmpty()) {
                    val sIdx = sentenceIndexForChar(clauseStart)
                    clauses.add(
                        PunctuationClause(
                            clauseText = rawSlice,
                            charRange = clauseStart..endIdx,
                            sentenceIndex = sIdx,
                            sentenceText = sentenceTextForIndex(sIdx),
                            pauseType = finalPauseType
                        )
                    )
                }

                clauseStart = endIdx + 1
                while (clauseStart < text.length && text[clauseStart].isWhitespace()) {
                    clauseStart++
                }
                i = clauseStart
            } else {
                i++
            }
        }

        if (clauseStart < text.length) {
            val remainingSlice = text.substring(clauseStart).trim()
            if (remainingSlice.isNotEmpty()) {
                val sIdx = sentenceIndexForChar(clauseStart)
                clauses.add(
                    PunctuationClause(
                        clauseText = remainingSlice,
                        charRange = clauseStart until text.length,
                        sentenceIndex = sIdx,
                        sentenceText = sentenceTextForIndex(sIdx),
                        pauseType = PunctuationPauseType.PERIOD
                    )
                )
            }
        }

        return if (clauses.isEmpty()) {
            listOf(
                PunctuationClause(
                    clauseText = text,
                    charRange = 0 until text.length,
                    sentenceIndex = 0,
                    sentenceText = text,
                    pauseType = PunctuationPauseType.PERIOD
                )
            )
        } else {
            clauses
        }
    }

    private fun splitTextIntoSentenceRanges(text: String): List<IntRange> {
        if (text.isBlank()) return listOf(0..0)
        val ranges = mutableListOf<IntRange>()
        var sentenceStart = 0
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val isTerminator = ch == '.' || ch == '!' || ch == '?' || ch == '…'
            if (isTerminator) {
                var endIdx = i
                while (endIdx + 1 < text.length && (text[endIdx + 1] == '.' || text[endIdx + 1] == '!' || text[endIdx + 1] == '?' || text[endIdx + 1] == '"' || text[endIdx + 1] == '”' || text[endIdx + 1] == '\'')) {
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

    private fun selectMatchingSystemVoiceIfAvailable(
        engine: TextToSpeech,
        gender: VoiceGender,
        speakerName: String,
        language: NarrationLanguage
    ) {
        val langVoices = cachedVoices.filter {
            it.locale.language.equals(language.code, ignoreCase = true)
        }
        val pool = langVoices
        if (pool.isEmpty()) return

        val genderMatched = when (gender) {
            VoiceGender.FEMALE, VoiceGender.CHILD -> pool.filter {
                val n = it.name.lowercase()
                n.contains("female") || n.contains("f-") || n.contains("wavenet-a") || n.contains("wavenet-c") || n.contains("sfg")
            }
            VoiceGender.MALE, VoiceGender.NARRATOR -> pool.filter {
                val n = it.name.lowercase()
                (n.contains("male") && !n.contains("female")) || n.contains("m-") || n.contains("wavenet-b") || n.contains("wavenet-d") || n.contains("smg")
            }
        }
        val targetPool = genderMatched.ifEmpty { pool }
        val chosen = targetPool[Math.floorMod(speakerName.hashCode(), targetPool.size)]
        runCatching { engine.voice = chosen }
    }

    fun shutdown() {
        scope.cancel()
        abandonAudioFocus()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
