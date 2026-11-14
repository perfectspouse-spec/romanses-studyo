package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ai.ACTIVE_FIREBASE_AI_NOTICE
import com.example.data.ai.AiRepository
import com.example.data.ai.FirebaseAiRepository
import com.example.data.ai.GeneratedSceneArtResult
import com.example.data.ai.PhotoAnalysisResult
import com.example.data.ai.SamplePlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.BookStudioRepository
import com.example.data.parser.BookPageImageProcessor
import com.example.data.parser.DocumentParser
import com.example.data.parser.PageFilterMode
import com.example.data.parser.PageFocusRegion
import com.example.data.parser.PageVisualTelemetry
import com.example.data.parser.ParsedBookDocument
import com.example.model.BookEntity
import com.example.model.BookSourceFormat
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.NarrationLanguage
import com.example.model.ScriptLineEntity
import com.example.model.VoiceGender
import com.example.tts.EmotionalMultiVoiceTtsEngine
import com.example.tts.TtsPlaybackState
import com.example.ui.components.AmbientLightingMode
import com.example.ui.components.AutoScrollAnchor
import com.example.ui.components.FocusedReadingConfig
import com.example.ui.components.SentenceHighlightStyle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioTab(val titleTr: String) {
    LIBRARY("Kitaplık"),
    STAGE_PLAYER("Sesli Sahne"),
    VOICE_CAST("Karakterler"),
    PAGE_VISION("Sayfa Analizi"),
    IMAGE_STUDIO("Görsel Stüdyo")
}

enum class PickerFormatFilter(
    val labelTr: String,
    val subtitleTr: String,
    val mimeTypes: Array<String>
) {
    ALL(
        labelTr = "Tümü (PDF & EPUB)",
        subtitleTr = ".pdf, .epub, .txt",
        mimeTypes = arrayOf("application/pdf", "application/epub+zip", "text/plain", "*/*")
    ),
    PDF_ONLY(
        labelTr = "PDF Kitap",
        subtitleTr = "Sadece .pdf dosyaları",
        mimeTypes = arrayOf("application/pdf")
    ),
    EPUB_ONLY(
        labelTr = "EPUB E-Kitap",
        subtitleTr = "Sadece .epub dosyaları",
        mimeTypes = arrayOf("application/epub+zip", "application/octet-stream")
    ),
    TXT_ONLY(
        labelTr = "Metin (TXT)",
        subtitleTr = "Düz metin dosyaları",
        mimeTypes = arrayOf("text/plain")
    )
}

data class StudioUiState(
    val activeTab: StudioTab = StudioTab.LIBRARY,
    val selectedBookId: Long? = null,
    val selectedLanguage: NarrationLanguage = NarrationLanguage.TURKISH,
    val selectedFormatFilter: PickerFormatFilter = PickerFormatFilter.ALL,
    val stagedDocument: ParsedBookDocument? = null,
    val isReadingFile: Boolean = false,
    val isProcessingBook: Boolean = false,
    val statusBannerMessage: String? = null,
    val firebaseNotice: String = ACTIVE_FIREBASE_AI_NOTICE,
    // Dynamic Emotional Background Palette Module state
    val dynamicBackgroundPaletteEnabled: Boolean = true,
    val ambientLightingMode: AmbientLightingMode = AmbientLightingMode.DYNAMIC_AURA,
    val paletteIntensity: Float = 0.88f,
    val manualEmotionPaletteOverride: EmotionTone? = null,
    // Focused Reading Mode ('Odaklı Okuma Modu') state
    val focusedReadingConfig: FocusedReadingConfig = FocusedReadingConfig(),
    // Page Vision & Book Page Image Processing Module (gemini-4-argon) state
    val rawVisionBitmap: Bitmap? = null,
    val selectedVisionBitmap: Bitmap? = null,
    val visionFilterMode: PageFilterMode = PageFilterMode.ORIGINAL,
    val visionFocusRegion: PageFocusRegion = PageFocusRegion.FULL_PAGE,
    val visionTelemetry: PageVisualTelemetry? = null,
    val visionPdfUri: Uri? = null,
    val visionPdfPageIndex: Int = 0,
    val visionPdfTotalPages: Int = 1,
    val isAnalyzingVision: Boolean = false,
    val visionAnalysisResult: PhotoAnalysisResult? = null,
    val visionOcrText: String? = null,
    val visionError: String? = null,
    // Image Studio (gemini-nano-banana-2.1) state
    val sourceEditBitmap: Bitmap? = null,
    val isGeneratingImage: Boolean = false,
    val generatedArtResult: GeneratedSceneArtResult? = null,
    val imageGenError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = BookStudioRepository(database)
    private val aiRepository: AiRepository = FirebaseAiRepository()
    private val fastLocalAnalyzer = SamplePlaceholderAiRepository()
    private val ttsEngine = EmotionalMultiVoiceTtsEngine(application)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<TtsPlaybackState> = ttsEngine.state

    val books: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var visionSourceJob: Job? = null
    private var visionAnalysisJob: Job? = null
    private var editSourceJob: Job? = null
    private var imageGenerationJob: Job? = null
    private var playbackBookId: Long? = null

    private val selectedBookIdFlow = MutableStateFlow<Long?>(null)

    val activeCharacters: StateFlow<List<CharacterVoiceEntity>> = selectedBookIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeCharacters(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeScriptLines: StateFlow<List<ScriptLineEntity>> = selectedBookIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeScriptLines(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            var lastSaved: Pair<Long, Int>? = null
            ttsState.collect { state ->
                val bookId = playbackBookId ?: return@collect
                val position = bookId to state.currentLineIndex
                if (position != lastSaved) {
                    try {
                        repository.savePlaybackLine(bookId, position.second)
                        lastSaved = position
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.update { it.copy(statusBannerMessage = "Dinleme konumu kaydedilemedi.") }
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.ensureSampleBooksSeeded()
            loadSampleVisionImage()
            books.collect { list ->
                if (_uiState.value.selectedBookId == null && list.isNotEmpty()) {
                    selectBook(list.first().id, switchTab = false)
                }
            }
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setNarrationLanguage(language: NarrationLanguage) {
        ttsEngine.pausePlayback()
        ttsEngine.setNarrationLanguage(language)
        _uiState.update { current ->
            val updatedStaged = current.stagedDocument?.copy(language = language)
            current.copy(
                selectedLanguage = language,
                stagedDocument = updatedStaged,
                statusBannerMessage = "Dil seçeneği ${language.labelTr} (${language.nativeName} • ${language.bcp47Tag}) olarak ayarlandı. Yerel çözümleme metni çevirmeden bu dilin sesini kullanır."
            )
        }
    }

    fun setEmotionProcessingEnabled(enabled: Boolean) {
        ttsEngine.setEmotionProcessingEnabled(enabled)
        _uiState.update {
            it.copy(
                statusBannerMessage = if (enabled) {
                    "Çok dilli duygu işleme (Emotion Processing) etkinleştirildi."
                } else {
                    "Duygu işleme devre dışı bırakıldı (düz okuma modu)."
                }
            )
        }
    }

    fun setDynamicBackgroundPaletteEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                dynamicBackgroundPaletteEnabled = enabled,
                statusBannerMessage = if (enabled) {
                    "Dinamik duygu arka plan renk paleti etkinleştirildi."
                } else {
                    "Dinamik arka plan renk paleti kapatıldı (klasik tema)."
                }
            )
        }
    }

    fun setAmbientLightingMode(mode: AmbientLightingMode) {
        _uiState.update {
            it.copy(
                ambientLightingMode = mode,
                dynamicBackgroundPaletteEnabled = true,
                statusBannerMessage = "Arka plan atmosfer modu \"${mode.labelTr}\" olarak güncellendi."
            )
        }
    }

    fun setPaletteIntensity(intensity: Float) {
        _uiState.update {
            it.copy(paletteIntensity = intensity.coerceIn(0.30f, 1.00f))
        }
    }

    fun setManualEmotionPaletteOverride(emotion: EmotionTone?) {
        _uiState.update {
            it.copy(
                manualEmotionPaletteOverride = emotion,
                dynamicBackgroundPaletteEnabled = true,
                statusBannerMessage = if (emotion != null) {
                    "Arka plan renk paleti \"${emotion.labelTr}\" duygusuna göre önizleniyor."
                } else {
                    "Arka plan renk paleti seslendirilen metnin duygusuyla otomatik senkronize edildi."
                }
            )
        }
    }

    // Focused Reading Mode ('Odaklı Okuma Modu') Controls
    fun setFocusedReadingModeActive(active: Boolean) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(isFocusedModeActive = active),
                statusBannerMessage = if (active) {
                    "Odaklı Okuma Modu açıldı: Seslendirilen cümle vurgulanır ve sayfa otomatik kaydırılır."
                } else {
                    "Standart Sesli Sahne görünümüne dönüldü."
                }
            )
        }
    }

    fun setFocusedAutoScrollEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(autoScrollEnabled = enabled),
                statusBannerMessage = if (enabled) {
                    "Otomatik sayfa kaydırma (Auto-Scroll) etkinleştirildi."
                } else {
                    "Otomatik sayfa kaydırma duraklatıldı (manuel kaydırma)."
                }
            )
        }
    }

    fun setSentenceHighlightStyle(style: SentenceHighlightStyle) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(highlightStyle = style),
                statusBannerMessage = "Cümle vurgulama stili \"${style.labelTr}\" olarak seçildi."
            )
        }
    }

    fun setFocusedScrollAnchor(anchor: AutoScrollAnchor) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(scrollAnchor = anchor)
            )
        }
    }

    fun setDimSurroundingSentences(dim: Boolean) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(dimSurroundingSentences = dim)
            )
        }
    }

    fun setFocusedFontScale(scale: Float) {
        _uiState.update {
            it.copy(
                focusedReadingConfig = it.focusedReadingConfig.copy(
                    fontScaleMultiplier = scale.coerceIn(0.85f, 1.60f)
                )
            )
        }
    }

    fun testLanguageVoiceWithEmotion(language: NarrationLanguage, emotion: EmotionTone) {
        setNarrationLanguage(language)
        val demoCharacter = CharacterVoiceEntity(
            bookId = 0,
            name = language.narratorDefaultName,
            gender = VoiceGender.FEMALE.name,
            roleDescription = "${language.labelTr} Duygusal Ses Testi",
            dominantEmotion = emotion.name,
            basePitch = VoiceGender.FEMALE.defaultBasePitch,
            baseRate = VoiceGender.FEMALE.defaultBaseRate,
            voiceStyleLabel = "${language.badgeShort} • ${emotion.labelTr}",
            colorHex = VoiceGender.FEMALE.badgeColorHex
        )
        ttsEngine.testCharacterVoice(
            character = demoCharacter,
            emotion = emotion,
            customSampleText = null,
            language = language
        )
    }

    fun importSampleBookInLanguage(language: NarrationLanguage) {
        setNarrationLanguage(language)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingBook = true,
                    statusBannerMessage = "${language.labelTr} (${language.nativeName}) örnek romanı karakter ve duygu analiziyle hazırlanıyor..."
                )
            }
            try {
                val context = getApplication<Application>()
                val parsedDoc = DocumentParser.createAndParseSampleFile(
                    context = context,
                    format = BookSourceFormat.PDF,
                    language = language
                )
                val dramatized = fastLocalAnalyzer.analyzeBookTextAndBuildCast(
                    bookTitle = parsedDoc.title,
                    rawText = parsedDoc.extractedText,
                    sourceFormat = parsedDoc.format,
                    language = language
                )
                val newBook = BookEntity(
                    title = parsedDoc.title,
                    author = parsedDoc.author,
                    sourceFormat = parsedDoc.format.name,
                    languageCode = language.code,
                    summary = dramatized.summary,
                    rawExcerpt = parsedDoc.extractedText.take(600),
                    characterCount = dramatized.characters.size,
                    lineCount = dramatized.scriptLines.size,
                    isSample = false
                )
                val newId = repository.saveBookWithCastAndScript(
                    book = newBook,
                    characters = dramatized.characters,
                    scriptLines = dramatized.scriptLines
                )
                selectedBookIdFlow.value = newId
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        selectedBookId = newId,
                        selectedLanguage = language,
                        activeTab = StudioTab.STAGE_PLAYER,
                        statusBannerMessage = "\"${parsedDoc.title}\" (${language.labelTr}) yüklendi: ${dramatized.characters.size} karakter ve ${dramatized.scriptLines.size} duygusal replik hazır. " + dramatized.firebaseNotice
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        statusBannerMessage = "Örnek kitap yüklenemedi: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setFormatFilter(filter: PickerFormatFilter) {
        _uiState.update { it.copy(selectedFormatFilter = filter) }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun selectBook(bookId: Long, switchTab: Boolean = true) {
        playbackBookId = null
        val matchedBook = books.value.find { it.id == bookId }
        ttsEngine.stopPlayback(matchedBook?.playbackLine ?: 0)
        selectedBookIdFlow.value = bookId
        val bookLang = matchedBook?.languageEnum ?: _uiState.value.selectedLanguage
        ttsEngine.setNarrationLanguage(bookLang)
        _uiState.update {
            it.copy(
                selectedBookId = bookId,
                selectedLanguage = bookLang,
                activeTab = if (switchTab) StudioTab.STAGE_PLAYER else it.activeTab
            )
        }
    }

    fun stageDocumentFromUri(uri: Uri) {
        val activeLang = _uiState.value.selectedLanguage
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReadingFile = true,
                    activeTab = StudioTab.LIBRARY,
                    statusBannerMessage = "Seçilen dosya (${activeLang.labelTr}) okunuyor ve önizleme hazırlanıyor..."
                )
            }
            runCatching {
                val context = getApplication<Application>()
                val parsedDoc = DocumentParser.parseDocumentUri(context, uri, activeLang)
                _uiState.update {
                    it.copy(
                        isReadingFile = false,
                        stagedDocument = parsedDoc,
                        statusBannerMessage = "\"${parsedDoc.fileName}\" (${parsedDoc.format.displayLabel}, ${activeLang.labelTr}) seçildi. Önizleme kartından yüklemeyi onaylayın."
                    )
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _uiState.update {
                    it.copy(
                        isReadingFile = false,
                        statusBannerMessage = "Dosya okunamadı: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun stageSampleFileForFormat(format: BookSourceFormat) {
        val activeLang = _uiState.value.selectedLanguage
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReadingFile = true,
                    statusBannerMessage = "${activeLang.labelTr} örnek ${format.displayLabel} dosyası hazırlanıyor..."
                )
            }
            runCatching {
                val context = getApplication<Application>()
                val parsedDoc = DocumentParser.createAndParseSampleFile(context, format, activeLang)
                _uiState.update {
                    it.copy(
                        isReadingFile = false,
                        stagedDocument = parsedDoc,
                        statusBannerMessage = "\"${parsedDoc.fileName}\" (${activeLang.labelTr}, ${parsedDoc.formattedFileSize}) dosya seçiciye yüklendi."
                    )
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _uiState.update {
                    it.copy(
                        isReadingFile = false,
                        statusBannerMessage = "Örnek dosya oluşturulamadı: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearStagedDocument() {
        _uiState.update { it.copy(stagedDocument = null) }
    }

    fun confirmStagedDocumentUpload(
        customTitle: String,
        customAuthor: String,
        useFastLocalAnalyzer: Boolean = false
    ) {
        val staged = _uiState.value.stagedDocument ?: return
        val targetLang = _uiState.value.selectedLanguage
        val finalTitle = customTitle.trim().ifBlank { staged.title }
        val finalAuthor = customAuthor.trim().ifBlank { staged.author }

        if (_uiState.value.isProcessingBook) return
        _uiState.update { it.copy(isProcessingBook = true) }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingBook = true,
                    statusBannerMessage = if (useFastLocalAnalyzer) {
                        "\"$finalTitle\" (${targetLang.labelTr}) çok dilli duygu çözümleyici ile sesli sahneye aktarılıyor..."
                    } else {
                        "\"$finalTitle\" (${targetLang.labelTr}) Gemini AI (gemini-3.8-flash) ile karakter ve duygu analizinden geçiriliyor..."
                    }
                )
            }
            try {
                val analyzer: AiRepository = if (useFastLocalAnalyzer) fastLocalAnalyzer else aiRepository
                val dramatized = analyzer.analyzeBookTextAndBuildCast(
                    bookTitle = finalTitle,
                    rawText = staged.extractedText,
                    sourceFormat = staged.format,
                    language = targetLang
                )
                val newBook = BookEntity(
                    title = finalTitle,
                    author = finalAuthor,
                    sourceFormat = staged.format.name,
                    languageCode = targetLang.code,
                    summary = dramatized.summary,
                    rawExcerpt = staged.extractedText.take(600),
                    sourceText = staged.extractedText,
                    characterCount = dramatized.characters.size,
                    lineCount = dramatized.scriptLines.size,
                    isSample = false
                )
                val newId = repository.saveBookWithCastAndScript(
                    book = newBook,
                    characters = dramatized.characters,
                    scriptLines = dramatized.scriptLines
                )
                playbackBookId = null
                ttsEngine.stopPlayback()
                selectedBookIdFlow.value = newId
                ttsEngine.setNarrationLanguage(targetLang)
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        stagedDocument = null,
                        selectedBookId = newId,
                        activeTab = StudioTab.STAGE_PLAYER,
                        statusBannerMessage = "\"$finalTitle\" (${targetLang.labelTr}) yüklendi: ${dramatized.characters.size} karakter ve ${dramatized.scriptLines.size} duygusal replik hazır. " + dramatized.firebaseNotice
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        statusBannerMessage = "Kitap analizi hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingBook = false) }
            }
        }
    }

    fun importCustomTextBook(title: String, author: String, rawText: String, useFastLocal: Boolean = true) {
        if (rawText.isBlank()) return
        if (rawText.length > DocumentParser.MAX_TEXT_CHARS) {
            _uiState.update { it.copy(statusBannerMessage = "Metin 2 milyon karakteri aşıyor.") }
            return
        }
        val targetLang = _uiState.value.selectedLanguage
        if (_uiState.value.isProcessingBook) return
        _uiState.update { it.copy(isProcessingBook = true) }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingBook = true,
                    statusBannerMessage = if (useFastLocal) "Metin cihazda çözümleniyor…" else "Metin Gemini ile çözümleniyor…"
                )
            }
            try {
                val cleanTitle = title.trim().ifBlank { "Özel Roman Sahnesi (${targetLang.badgeShort})" }
                val cleanAuthor = author.trim().ifBlank { "Okur Uyarlaması" }
                val analyzer = if (useFastLocal) fastLocalAnalyzer else aiRepository
                val dramatized = analyzer.analyzeBookTextAndBuildCast(
                    bookTitle = cleanTitle,
                    rawText = rawText,
                    sourceFormat = BookSourceFormat.TXT,
                    language = targetLang
                )
                val newBook = BookEntity(
                    title = cleanTitle,
                    author = cleanAuthor,
                    sourceFormat = BookSourceFormat.TXT.name,
                    languageCode = targetLang.code,
                    summary = dramatized.summary,
                    rawExcerpt = rawText.take(600),
                    sourceText = rawText,
                    characterCount = dramatized.characters.size,
                    lineCount = dramatized.scriptLines.size,
                    isSample = false
                )
                val newId = repository.saveBookWithCastAndScript(
                    book = newBook,
                    characters = dramatized.characters,
                    scriptLines = dramatized.scriptLines
                )
                playbackBookId = null
                ttsEngine.stopPlayback()
                selectedBookIdFlow.value = newId
                ttsEngine.setNarrationLanguage(targetLang)
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        selectedBookId = newId,
                        activeTab = StudioTab.STAGE_PLAYER,
                        statusBannerMessage = "\"$cleanTitle\" (${targetLang.labelTr}) için ${dramatized.characters.size} farklı ses profili oluşturuldu. " + dramatized.firebaseNotice
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingBook = false,
                        statusBannerMessage = "Metin analizi başarısız: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isProcessingBook = false) }
            }
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBook(bookId)
                if (_uiState.value.selectedBookId != bookId) return@launch
                val next = books.value.firstOrNull { it.id != bookId }
                if (next != null) {
                    selectBook(next.id, switchTab = false)
                } else {
                    playbackBookId = null
                    ttsEngine.stopPlayback()
                    selectedBookIdFlow.value = null
                    _uiState.update { it.copy(selectedBookId = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBannerMessage = "Kitap silinemedi.") }
            }
        }
    }

    fun togglePlayPause() {
        if (ttsState.value.isPlaying) ttsEngine.pausePlayback()
        else playFromLine(ttsState.value.currentLineIndex)
    }

    fun playFromLine(index: Int) {
        val bookId = _uiState.value.selectedBookId ?: return
        val lines = activeScriptLines.value
        if (lines.isEmpty() || lines.first().bookId != bookId) return
        playbackBookId = bookId
        ttsEngine.startOrResumePlayback(
            lines = lines,
            characters = activeCharacters.value.filter { it.bookId == bookId },
            startIndex = index,
            language = _uiState.value.selectedLanguage
        )
    }

    fun stopPlayback() {
        ttsEngine.stopPlayback()
    }

    fun setGlobalSpeed(speed: Float) {
        ttsEngine.setGlobalSpeed(speed)
    }

    fun setPunctuationPauseMultiplier(multiplier: Float) {
        ttsEngine.setPunctuationPauseMultiplier(multiplier)
    }

    fun toggleTheatricalEmotionBoost(enabled: Boolean) {
        ttsEngine.toggleTheatricalBoost(enabled)
    }

    // Character Voice Profile Customization
    fun updateCharacterProfile(
        character: CharacterVoiceEntity,
        newGender: VoiceGender,
        newPitch: Float,
        newRate: Float,
        newDominantEmotion: EmotionTone
    ) {
        viewModelScope.launch {
            val styleLabel = "${newGender.labelTr} • ${character.timbrePresetEnum.labelTr} (${String.format("%.2f", newPitch)}x)"
            val updated = character.copy(
                gender = newGender.name,
                basePitch = newPitch,
                baseRate = newRate,
                dominantEmotion = newDominantEmotion.name,
                voiceStyleLabel = styleLabel,
                colorHex = newGender.badgeColorHex
            )
            repository.updateCharacterVoice(updated)
            _uiState.update {
                it.copy(statusBannerMessage = "${character.name} ses profili güncellendi (${newGender.labelTr}, ${newDominantEmotion.labelTr}).")
            }
        }
    }

    fun saveFullCharacterSettings(updatedCharacter: CharacterVoiceEntity) {
        viewModelScope.launch {
            repository.updateCharacterVoice(updatedCharacter)
            _uiState.update {
                it.copy(
                    statusBannerMessage = "${updatedCharacter.name} için cinsiyet, tını (${updatedCharacter.timbrePresetEnum.labelTr}) ve duygu modülasyonu (%${(updatedCharacter.emotionIntensity * 100).toInt()}) kaydedildi."
                )
            }
        }
    }

    fun testCharacterVoice(
        character: CharacterVoiceEntity,
        emotion: EmotionTone,
        customSampleText: String? = null
    ) {
        ttsEngine.testCharacterVoice(
            character = character,
            emotion = emotion,
            customSampleText = customSampleText,
            language = _uiState.value.selectedLanguage
        )
    }

    fun updateLineEmotion(line: ScriptLineEntity, emotion: EmotionTone) {
        viewModelScope.launch {
            repository.updateScriptLineEmotion(line, emotion)
        }
    }

    fun updateLineSpeaker(line: ScriptLineEntity, character: CharacterVoiceEntity) {
        viewModelScope.launch {
            repository.updateScriptLineSpeaker(line, character)
        }
    }

    // =========================================================================
    // Book Page Image Processing & Description Module (gemini-4-argon)
    // =========================================================================

    fun onVisionPhotoSelected(uri: Uri) = updateVisionSource {
        processVisionSource(decodeUriToBitmap(uri), null, 0, 1)
    }

    fun onVisionPdfSelected(uri: Uri) = renderVisionPage(uri, 0)

    fun selectVisionPdfPage(newPageIndex: Int) {
        val state = _uiState.value
        val uri = state.visionPdfUri ?: return
        renderVisionPage(uri, newPageIndex.coerceIn(0, state.visionPdfTotalPages - 1))
    }

    private fun renderVisionPage(uri: Uri, index: Int) = updateVisionSource {
        val (bitmap, count) = BookPageImageProcessor.renderPdfPageFromUri(getApplication(), uri, index)
            ?: error("PDF sayfası açılamadı.")
        processVisionSource(bitmap, uri, index, count)
    }

    fun loadSampleVisionImage(useSecondSample: Boolean = false) = updateVisionSource {
        val context = getApplication<Application>()
        val drawable = if (useSecondSample) R.drawable.img_hero_audiobook_studio_1794504929592
            else R.drawable.img_sample_novel_scene_1794504955396
        val bitmap = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(context.resources, drawable, BitmapFactory.Options().apply {
                inScaled = false
            })
        } ?: error("Örnek görsel açılamadı.")
        processVisionSource(bitmap, null, 0, 1)
    }

    fun updateVisionFilterAndRegion(
        filterMode: PageFilterMode = _uiState.value.visionFilterMode,
        focusRegion: PageFocusRegion = _uiState.value.visionFocusRegion
    ) {
        val state = _uiState.value
        val raw = state.rawVisionBitmap ?: return
        updateVisionSource {
            processVisionSource(raw, state.visionPdfUri, state.visionPdfPageIndex,
                state.visionPdfTotalPages, filterMode, focusRegion)
        }
    }

    private fun updateVisionSource(action: suspend () -> Unit) {
        visionSourceJob?.cancel()
        visionAnalysisJob?.cancel()
        _uiState.update { it.copy(visionAnalysisResult = null, visionOcrText = null, visionError = null, isAnalyzingVision = false) }
        visionSourceJob = viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(visionError = e.localizedMessage ?: "Görsel açılamadı.") }
            }
        }
    }

    private suspend fun processVisionSource(
        bitmap: Bitmap,
        uri: Uri?,
        index: Int,
        count: Int,
        filter: PageFilterMode = _uiState.value.visionFilterMode,
        region: PageFocusRegion = _uiState.value.visionFocusRegion
    ) {
        val (processed, telemetry) = BookPageImageProcessor.processPageBitmap(bitmap, filter, region)
        _uiState.update {
            it.copy(
                rawVisionBitmap = bitmap,
                selectedVisionBitmap = processed,
                visionPdfUri = uri,
                visionPdfPageIndex = index,
                visionPdfTotalPages = count,
                visionFilterMode = filter,
                visionFocusRegion = region,
                visionTelemetry = telemetry
            )
        }
    }

    fun analyzeSelectedPhotoWithArgon(userPrompt: String) = runVisionAnalysis { bitmap ->
        val result = aiRepository.analyzeImageWithGeminiArgon(bitmap, userPrompt)
        _uiState.update { it.copy(visionAnalysisResult = result) }
    }

    fun recognizePageText() = runVisionAnalysis { bitmap ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = suspendCancellableCoroutine { continuation ->
            try {
                recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnCompleteListener { task ->
                    recognizer.close()
                    if (task.isCanceled) continuation.cancel()
                    else if (task.isSuccessful) continuation.resume(task.result.text)
                    else continuation.resumeWithException(task.exception ?: IllegalStateException("Metin tanınamadı."))
                }
            } catch (e: Exception) {
                recognizer.close()
                continuation.resumeWithException(e)
            }
        }
        require(text.isNotBlank()) { "Sayfada okunabilir metin bulunamadı." }
        _uiState.update { it.copy(visionOcrText = text) }
    }

    fun importReviewedPageText(text: String) {
        if (_uiState.value.visionOcrText == null || text.isBlank()) return
        val page = _uiState.value.visionPdfPageIndex + 1
        _uiState.update { it.copy(visionOcrText = null) }
        importCustomTextBook("Taranan sayfa $page", "", text)
    }

    private fun runVisionAnalysis(action: suspend (Bitmap) -> Unit) {
        if (visionSourceJob?.isActive == true || visionAnalysisJob?.isActive == true) return
        val bitmap = _uiState.value.selectedVisionBitmap ?: return
        visionAnalysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isAnalyzingVision = true, visionError = null, visionOcrText = null, visionAnalysisResult = null)
            }
            try {
                action(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(visionError = e.localizedMessage ?: "Sayfa analizi başarısız oldu.") }
            } finally {
                _uiState.update { it.copy(isAnalyzingVision = false) }
            }
        }
    }

    fun speakVisualDescriptionAloud(descriptionText: String, emotion: EmotionTone) {
        val activeLang = _uiState.value.selectedLanguage
        val narratorCharacter = activeCharacters.value.find { it.genderEnum == VoiceGender.NARRATOR }
            ?: CharacterVoiceEntity(
                bookId = 0,
                name = activeLang.narratorDefaultName,
                gender = VoiceGender.NARRATOR.name,
                roleDescription = "Kitap sayfasındaki görselleri betimleyen dış ses",
                dominantEmotion = emotion.name,
                basePitch = 0.96f,
                baseRate = 0.94f,
                voiceStyleLabel = "Edebi Betimleme Sesi (${activeLang.badgeShort})",
                colorHex = VoiceGender.NARRATOR.badgeColorHex
            )
        ttsEngine.testCharacterVoice(narratorCharacter, emotion, descriptionText, activeLang)
    }

    fun convertVisionAnalysisToAudiobook() {
        val analysis = _uiState.value.visionAnalysisResult ?: return
        val combinedScript = buildString {
            if (analysis.visualAudioDescription.isNotBlank()) {
                append(analysis.visualAudioDescription.trim()).append("\n")
            }
            if (analysis.extractedDialoguePreview.isNotBlank()) {
                append(analysis.extractedDialoguePreview.trim())
            }
        }
        importCustomTextBook(
            title = analysis.detectedTitle,
            author = "Görsel İşleme & Betimleme (gemini-4-argon)",
            rawText = combinedScript
        )
    }

    // Image Studio (gemini-nano-banana-2.1 Create & Edit Images flow via AiRepository)
    fun onEditSourceImageSelected(uri: Uri) {
        clearEditSourceImage()
        editSourceJob = viewModelScope.launch {
            try {
                val bmp = decodeUriToBitmap(uri)
                _uiState.update { it.copy(sourceEditBitmap = bmp) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(imageGenError = "Düzenlenecek görsel açılamadı.") }
            }
        }
    }

    fun clearEditSourceImage() {
        editSourceJob?.cancel()
        imageGenerationJob?.cancel()
        _uiState.update {
            it.copy(sourceEditBitmap = null, generatedArtResult = null, imageGenError = null, isGeneratingImage = false)
        }
    }

    fun generateOrEditSceneArt(prompt: String, stylePreset: String) {
        if (editSourceJob?.isActive == true || imageGenerationJob?.isActive == true) return
        imageGenerationJob = viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingImage = true, imageGenError = null) }
            try {
                val result = aiRepository.createOrEditImageWithNanoBanana(
                    context = getApplication(),
                    prompt = prompt,
                    sourceBitmap = _uiState.value.sourceEditBitmap,
                    stylePreset = stylePreset
                )
                _uiState.update {
                    it.copy(
                        isGeneratingImage = false,
                        generatedArtResult = result,
                        imageGenError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingImage = false,
                        imageGenError = e.localizedMessage ?: "gemini-nano-banana-2.1 görsel üretimi başarısız oldu."
                    )
                }
            }
        }
    }

    private suspend fun decodeUriToBitmap(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val maxEdge = 1600
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = (maxEdge.toFloat() / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            require(options.outWidth > 0 && options.outHeight > 0) { "Görsel boyutu okunamadı." }
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > maxEdge) sample *= 2
            options.inJustDecodeBounds = false
            options.inSampleSize = sample
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Görsel açılamadı.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }
}
