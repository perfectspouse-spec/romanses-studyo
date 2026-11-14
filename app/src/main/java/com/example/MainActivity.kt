package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel
import com.example.ui.components.DynamicEmotionalBackdropBox
import com.example.ui.components.StudioSettingsModal
import com.example.ui.screens.LibraryAndImportScreen
import com.example.ui.screens.PageVisionAnalysisScreen
import com.example.ui.screens.SceneImageStudioScreen
import com.example.ui.screens.StagePlayerScreen
import com.example.ui.screens.VoiceCastScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this)

        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                RomanSesStudioApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanSesStudioApp(
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    viewModel: StudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val characters by viewModel.activeCharacters.collectAsStateWithLifecycle()
    val scriptLines by viewModel.activeScriptLines.collectAsStateWithLifecycle()

    val activeBook = books.find { it.id == uiState.selectedBookId } ?: books.firstOrNull()
    var showSettingsModal by rememberSaveable { mutableStateOf(false) }

    // Determine the real-time dominant emotion of the currently narrated / selected script line
    val currentLineEmotion = scriptLines.getOrNull(ttsState.currentLineIndex)?.emotionEnum
    val activeDominantEmotion = uiState.manualEmotionPaletteOverride ?: if (ttsState.isPlaying) {
        ttsState.activeEmotion
    } else {
        currentLineEmotion ?: ttsState.activeEmotion
    }

    // Document Picker for PDF, EPUB, TXT -> stages file in the Clean File Picker Inspector
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.stageDocumentFromUri(uri)
        }
    }

    // Photo Picker for gemini-4-argon Vision Analysis
    val visionPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onVisionPhotoSelected(uri)
        }
    }

    // PDF Document Picker for extracting and analyzing visual book pages
    val visionPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onVisionPdfSelected(uri)
        }
    }

    // Photo Picker for gemini-nano-banana-2.1 Image Editing
    val editImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEditSourceImageSelected(uri)
        }
    }

    // BackHandler for secondary tabs
    BackHandler(enabled = uiState.activeTab != StudioTab.LIBRARY) {
        viewModel.selectTab(StudioTab.LIBRARY)
    }

    MyApplicationTheme(
        darkTheme = isDarkTheme,
        activeEmotion = activeDominantEmotion,
        dynamicEmotionPaletteEnabled = uiState.dynamicBackgroundPaletteEnabled,
        paletteIntensity = uiState.paletteIntensity
    ) {
        DynamicEmotionalBackdropBox(
            activeEmotion = activeDominantEmotion,
            isPlaying = ttsState.isPlaying,
            isDarkTheme = isDarkTheme,
            dynamicPaletteEnabled = uiState.dynamicBackgroundPaletteEnabled,
            lightingMode = uiState.ambientLightingMode,
            paletteIntensity = uiState.paletteIntensity
        ) {
            Scaffold(
                containerColor = if (uiState.dynamicBackgroundPaletteEnabled) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                },
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "RomanSes Stüdyo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeBook?.let { "Aktif Eser: ${it.title} • ${activeDominantEmotion.labelTr}" }
                                        ?: "Çok Sesli & Duygusal Roman Seslendirme",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            // Active Language Pill + Settings Trigger
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { showSettingsModal = true }
                                    .testTag("topbar_language_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.selectedLanguage.badgeShort,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showSettingsModal = true },
                                modifier = Modifier.testTag("topbar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Dil ve Duygu Ayarları",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    documentLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/epub+zip",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("topbar_upload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "PDF, EPUB veya Metin Yükle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Temayı Değiştir"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (uiState.dynamicBackgroundPaletteEnabled) {
                                MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = if (uiState.dynamicBackgroundPaletteEnabled) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.LIBRARY,
                            onClick = { viewModel.selectTab(StudioTab.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Kitaplık") },
                            label = { Text("Kitaplık", maxLines = 1) },
                            modifier = Modifier.testTag("nav_tab_library")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.STAGE_PLAYER,
                            onClick = { viewModel.selectTab(StudioTab.STAGE_PLAYER) },
                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Sesli Sahne") },
                            label = { Text("Sesli Sahne", maxLines = 1) },
                            modifier = Modifier.testTag("nav_tab_stage")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.VOICE_CAST,
                            onClick = { viewModel.selectTab(StudioTab.VOICE_CAST) },
                            icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Karakterler") },
                            label = { Text("Karakterler", maxLines = 1) },
                            modifier = Modifier.testTag("nav_tab_cast")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.PAGE_VISION,
                            onClick = { viewModel.selectTab(StudioTab.PAGE_VISION) },
                            icon = { Icon(Icons.Default.DocumentScanner, contentDescription = "Sayfa Analizi") },
                            label = { Text("Sayfa Analizi", maxLines = 1) },
                            modifier = Modifier.testTag("nav_tab_vision")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.IMAGE_STUDIO,
                            onClick = { viewModel.selectTab(StudioTab.IMAGE_STUDIO) },
                            icon = { Icon(Icons.Default.Brush, contentDescription = "Görsel Stüdyo") },
                            label = { Text("Görsel Stüdyo", maxLines = 1) },
                            modifier = Modifier.testTag("nav_tab_image_studio")
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Optional status feedback banner
                    AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                        uiState.statusBannerMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.clearStatusBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Bildirimi Kapat",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (uiState.activeTab) {
                        StudioTab.LIBRARY -> {
                            LibraryAndImportScreen(
                                books = books,
                                selectedBookId = activeBook?.id,
                                selectedLanguage = uiState.selectedLanguage,
                                selectedFormatFilter = uiState.selectedFormatFilter,
                                stagedDocument = uiState.stagedDocument,
                                isReadingFile = uiState.isReadingFile,
                                isProcessing = uiState.isProcessingBook,
                                firebaseNotice = uiState.firebaseNotice,
                                onSelectLanguage = { lang ->
                                    viewModel.setNarrationLanguage(lang)
                                },
                                onOpenSettingsClick = {
                                    showSettingsModal = true
                                },
                                onSelectFormatFilter = { filter ->
                                    viewModel.setFormatFilter(filter)
                                },
                                onLaunchFilePicker = { mimeTypes ->
                                    documentLauncher.launch(mimeTypes)
                                },
                                onStageSampleFile = { format ->
                                    viewModel.stageSampleFileForFormat(format)
                                },
                                onClearStagedDocument = {
                                    viewModel.clearStagedDocument()
                                },
                                onConfirmStagedUpload = { title, author, useFastLocal ->
                                    viewModel.confirmStagedDocumentUpload(
                                        customTitle = title,
                                        customAuthor = author,
                                        useFastLocalAnalyzer = useFastLocal
                                    )
                                },
                                onImportCustomText = { title, author, rawText, useLocal ->
                                    viewModel.importCustomTextBook(title, author, rawText, useLocal)
                                },
                                onSelectBookAndPlay = { id ->
                                    viewModel.selectBook(id, switchTab = true)
                                },
                                onSelectBookAndOpenCast = { id ->
                                    viewModel.selectBook(id, switchTab = false)
                                    viewModel.selectTab(StudioTab.VOICE_CAST)
                                },
                                onDeleteBook = { id ->
                                    viewModel.deleteBook(id)
                                }
                            )
                        }

                        StudioTab.STAGE_PLAYER -> {
                            StagePlayerScreen(
                                book = activeBook,
                                characters = characters,
                                scriptLines = scriptLines,
                                ttsState = ttsState,
                                dynamicPaletteEnabled = uiState.dynamicBackgroundPaletteEnabled,
                                ambientLightingMode = uiState.ambientLightingMode,
                                paletteIntensity = uiState.paletteIntensity,
                                manualEmotionOverride = uiState.manualEmotionPaletteOverride,
                                onToggleDynamicPalette = { enabled ->
                                    viewModel.setDynamicBackgroundPaletteEnabled(enabled)
                                },
                                onSelectLightingMode = { mode ->
                                    viewModel.setAmbientLightingMode(mode)
                                },
                                onChangePaletteIntensity = { intensity ->
                                    viewModel.setPaletteIntensity(intensity)
                                },
                                onSelectManualEmotionOverride = { emotionOverride ->
                                    viewModel.setManualEmotionPaletteOverride(emotionOverride)
                                },
                                focusedReadingConfig = uiState.focusedReadingConfig,
                                onToggleFocusedReadingMode = { active ->
                                    viewModel.setFocusedReadingModeActive(active)
                                },
                                onToggleFocusedAutoScroll = { enabled ->
                                    viewModel.setFocusedAutoScrollEnabled(enabled)
                                },
                                onSelectHighlightStyle = { style ->
                                    viewModel.setSentenceHighlightStyle(style)
                                },
                                onSelectScrollAnchor = { anchor ->
                                    viewModel.setFocusedScrollAnchor(anchor)
                                },
                                onToggleDimSurrounding = { dim ->
                                    viewModel.setDimSurroundingSentences(dim)
                                },
                                onChangeFocusedFontScale = { scale ->
                                    viewModel.setFocusedFontScale(scale)
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onStop = { viewModel.stopPlayback() },
                                onPlayLineAtIndex = { idx -> viewModel.playFromLine(idx) },
                                onSpeedChange = { spd -> viewModel.setGlobalSpeed(spd) },
                                onPunctuationPauseChange = { mult ->
                                    viewModel.setPunctuationPauseMultiplier(mult)
                                },
                                onToggleTheatricalBoost = { enabled ->
                                    viewModel.toggleTheatricalEmotionBoost(enabled)
                                },
                                onUpdateLineEmotion = { line, emotion ->
                                    viewModel.updateLineEmotion(line, emotion)
                                },
                                onUpdateLineSpeaker = { line, character ->
                                    viewModel.updateLineSpeaker(line, character)
                                },
                                onSaveFullCharacterSettings = { updated ->
                                    viewModel.saveFullCharacterSettings(updated)
                                },
                                onTestCharacterVoice = { character, emotion, customPhrase ->
                                    viewModel.testCharacterVoice(character, emotion, customPhrase)
                                },
                                onOpenCastClick = { viewModel.selectTab(StudioTab.VOICE_CAST) }
                            )
                        }

                StudioTab.VOICE_CAST -> {
                    VoiceCastScreen(
                        book = activeBook,
                        characters = characters,
                        engineStatusLabel = ttsState.engineStatusLabel,
                        onUpdateCharacter = { character, gender, pitch, rate, emotion ->
                            viewModel.updateCharacterProfile(character, gender, pitch, rate, emotion)
                        },
                        onSaveFullCharacterSettings = { updated ->
                            viewModel.saveFullCharacterSettings(updated)
                        },
                        onTestCharacterVoice = { character, emotion, customPhrase ->
                            viewModel.testCharacterVoice(character, emotion, customPhrase)
                        }
                    )
                }

                StudioTab.PAGE_VISION -> {
                    PageVisionAnalysisScreen(
                        selectedBitmap = uiState.selectedVisionBitmap,
                        visionFilterMode = uiState.visionFilterMode,
                        visionFocusRegion = uiState.visionFocusRegion,
                        visionTelemetry = uiState.visionTelemetry,
                        visionPdfPageIndex = uiState.visionPdfPageIndex,
                        visionPdfTotalPages = uiState.visionPdfTotalPages,
                        isAnalyzing = uiState.isAnalyzingVision,
                        analysisResult = uiState.visionAnalysisResult,
                        ocrText = uiState.visionOcrText,
                        onRecognizePageText = viewModel::recognizePageText,
                        onImportReviewedText = viewModel::importReviewedPageText,
                        visionError = uiState.visionError,
                        firebaseNotice = uiState.firebaseNotice,
                        onPickPhotoClick = {
                            visionPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickPdfClick = {
                            visionPdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        onSelectPdfPage = { pageIdx ->
                            viewModel.selectVisionPdfPage(pageIdx)
                        },
                        onUseSamplePhotoClick = { useSecondSample ->
                            viewModel.loadSampleVisionImage(useSecondSample = useSecondSample)
                        },
                        onUpdateFilterAndRegion = { filterMode, focusRegion ->
                            viewModel.updateVisionFilterAndRegion(filterMode, focusRegion)
                        },
                        onAnalyzeClick = { prompt ->
                            viewModel.analyzeSelectedPhotoWithArgon(prompt)
                        },
                        onSpeakVisualDescription = { descriptionText, emotion ->
                            viewModel.speakVisualDescriptionAloud(descriptionText, emotion)
                        },
                        onConvertAnalysisToAudiobook = {
                            viewModel.convertVisionAnalysisToAudiobook()
                        }
                    )
                }

                StudioTab.IMAGE_STUDIO -> {
                    SceneImageStudioScreen(
                        sourceEditBitmap = uiState.sourceEditBitmap,
                        isGenerating = uiState.isGeneratingImage,
                        generatedResult = uiState.generatedArtResult,
                        imageGenError = uiState.imageGenError,
                        firebaseNotice = uiState.firebaseNotice,
                        onPickSourceImageForEdit = {
                            editImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearSourceImage = { viewModel.clearEditSourceImage() },
                        onGenerateOrEditImage = { prompt, style ->
                            viewModel.generateOrEditSceneArt(prompt, style)
                        }
                    )
                }
            }
        }
    }

            if (showSettingsModal) {
                StudioSettingsModal(
                    selectedLanguage = uiState.selectedLanguage,
                    emotionProcessingEnabled = ttsState.emotionProcessingEnabled,
                    theatricalEmotionBoost = ttsState.theatricalEmotionBoost,
                    globalSpeedMultiplier = ttsState.globalSpeedMultiplier,
                    punctuationPauseMultiplier = ttsState.punctuationPauseMultiplier,
                    engineStatusLabel = ttsState.engineStatusLabel,
                    onSelectLanguage = { lang ->
                        viewModel.setNarrationLanguage(lang)
                    },
                    onToggleEmotionProcessing = { enabled ->
                        viewModel.setEmotionProcessingEnabled(enabled)
                    },
                    onToggleTheatricalBoost = { enabled ->
                        viewModel.toggleTheatricalEmotionBoost(enabled)
                    },
                    onSpeedChange = { speed ->
                        viewModel.setGlobalSpeed(speed)
                    },
                    onPunctuationPauseChange = { mult ->
                        viewModel.setPunctuationPauseMultiplier(mult)
                    },
                    onTestLanguageVoiceWithEmotion = { lang, emotion ->
                        viewModel.testLanguageVoiceWithEmotion(lang, emotion)
                    },
                    onLoadSampleBookInLanguage = { lang ->
                        viewModel.importSampleBookInLanguage(lang)
                    },
                    onDismiss = { showSettingsModal = false }
                )
            }
        }
    }
}
