package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.GeneratedSceneArtResult
import com.example.data.ai.PhotoAnalysisResult
import com.example.data.parser.PageFilterMode
import com.example.data.parser.PageFocusRegion
import com.example.data.parser.PageVisualTelemetry
import com.example.model.EmotionTone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageVisionAnalysisScreen(
    selectedBitmap: Bitmap?,
    visionFilterMode: PageFilterMode,
    visionFocusRegion: PageFocusRegion,
    visionTelemetry: PageVisualTelemetry?,
    visionPdfPageIndex: Int,
    visionPdfTotalPages: Int,
    isAnalyzing: Boolean,
    analysisResult: PhotoAnalysisResult?,
    ocrText: String?,
    onRecognizePageText: () -> Unit,
    onImportReviewedText: (String) -> Unit,
    visionError: String?,
    firebaseNotice: String,
    onPickPhotoClick: () -> Unit,
    onPickPdfClick: () -> Unit,
    onSelectPdfPage: (Int) -> Unit,
    onUseSamplePhotoClick: (Boolean) -> Unit,
    onUpdateFilterAndRegion: (PageFilterMode, PageFocusRegion) -> Unit,
    onAnalyzeClick: (String) -> Unit,
    onSpeakVisualDescription: (String, EmotionTone) -> Unit,
    onConvertAnalysisToAudiobook: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userQuestion by remember {
        mutableStateOf(
            "Bu kitap sayfasındaki görselleri analiz et: içerikleri tanımla, nesneleri ve mekanı betimle, karakterleri ve duyguları çözümle."
        )
    }

    var reviewedText by remember(ocrText) { mutableStateOf(ocrText.orEmpty()) }

    val quickVisionPrompts = listOf(
        "Sayfadaki illüstrasyonu sesli kitap dinleyicisi için edebi dille betimle",
        "Görseldeki karakterleri, kıyafetlerini, bakışlarını ve duygularını tanımla",
        "Mekan mimarisini, ışık-gölge atmosferini ve sembolik nesneleri listele"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("page_vision_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FirebasePlaceholderNoticeCard(noticeText = firebaseNotice)
        }

        if (selectedBitmap != null) {
            item {
                OutlinedButton(onClick = onRecognizePageText, enabled = !isAnalyzing) {
                    Text("Sayfa metnini cihazda çıkar")
                }
            }
        }
        if (ocrText != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reviewedText,
                        onValueChange = { reviewedText = it },
                        label = { Text("Tanınan metni kontrol edin") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                    Button(
                        onClick = { onImportReviewedText(reviewedText) },
                        enabled = reviewedText.isNotBlank()
                    ) {
                        Text("Kontrol edilen metni kitaplığa ekle")
                    }
                }
            }
        }

        if (visionError != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = visionError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Source Selection & Page Preview Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vision_processor_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kitap Sayfası Görsel İşleme & Betimleme Modülü",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Model: gemini-4-argon • Sayfa illüstrasyonlarını, kapakları ve PDF sayfalarını işleyerek içerikleri tanımlayın ve sesli betimleyin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Image Preview Canvas with Active Filter & Region Badge Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap.asImageBitmap(),
                                contentDescription = "İşlenen Kitap Sayfası Görseli",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_sample_novel_scene_1794504955396),
                                contentDescription = "Örnek Kitap Sayfası İllüstrasyonu",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Top-left active processing filter badge
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${visionFilterMode.labelTr} • ${visionFocusRegion.labelTr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // PDF page badge if loaded from a multi-page PDF
                        if (visionPdfTotalPages > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "PDF Sayfa ${visionPdfPageIndex + 1} / $visionPdfTotalPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // PDF Page Stepper Bar (shown when a PDF is loaded into the image processor)
                    if (visionPdfTotalPages > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vision_pdf_page_stepper")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                IconButton(
                                    onClick = { onSelectPdfPage(visionPdfPageIndex - 1) },
                                    enabled = visionPdfPageIndex > 0,
                                    modifier = Modifier.testTag("vision_prev_pdf_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                        contentDescription = "Önceki PDF Sayfası"
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "PDF Sayfa Gezgini (${visionPdfPageIndex + 1} / $visionPdfTotalPages)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Analiz edilecek görsel sayfasını değiştirin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onSelectPdfPage(visionPdfPageIndex + 1) },
                                    enabled = visionPdfPageIndex < visionPdfTotalPages - 1,
                                    modifier = Modifier.testTag("vision_next_pdf_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = "Sonraki PDF Sayfası"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Source Selection Buttons Row 1: Photo Picker + PDF Page Extractor
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPickPhotoClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_vision_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sayfa Fotoğrafı Seç", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onPickPdfClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_vision_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Sayfası Seç", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sample Book Illustrations Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onUseSamplePhotoClick(false) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_sample_vision_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Örnek Sayfa 1 (Sahne)", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onUseSamplePhotoClick(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_sample_vision_second_button")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Örnek Sayfa 2 (Portre)", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Image Processing Pipeline Controls (Filter & Region)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Contrast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Görsel Ön-İşleme & Netleştirme Filtresi",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = visionFilterMode.subtitleTr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("vision_filter_row")
                    ) {
                        PageFilterMode.entries.forEach { mode ->
                            val selected = mode == visionFilterMode
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateFilterAndRegion(mode, visionFocusRegion) },
                                label = { Text(mode.labelTr, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("vision_filter_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sayfa Odak Bölgesi (Kırpma & İllüstrasyon Odağı)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("vision_focus_region_row")
                    ) {
                        PageFocusRegion.entries.forEach { region ->
                            val selected = region == visionFocusRegion
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateFilterAndRegion(visionFilterMode, region) },
                                label = { Text(region.labelTr, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("vision_region_${region.name.lowercase()}")
                            )
                        }
                    }

                    // 3. Visual Telemetry & Dominant Color Swatches
                    visionTelemetry?.let { telemetry ->
                        Spacer(modifier = Modifier.height(12.dp))
                        PageVisualTelemetryStrip(telemetry = telemetry)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Prompt Chips
                    Text(
                        text = "Hızlı Görsel Betimleme Şablonları:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        quickVisionPrompts.forEachIndexed { idx, preset ->
                            FilterChip(
                                selected = userQuestion == preset,
                                onClick = { userQuestion = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("quick_vision_prompt_$idx")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        label = { Text("Görsel Analiz & Betimleme Talimatı (gemini-4-argon)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vision_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onAnalyzeClick(userQuestion) },
                        enabled = !isAnalyzing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_photo_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("gemini-4-argon Görseli Çözümlüyor ve Betimliyor...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sayfa Görselini Analiz Et & Betimle (gemini-4-argon)")
                        }
                    }
                }
            }
        }

        // 4. Visual Description & Analysis Result Card
        analysisResult?.let { result ->
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .testTag("vision_analysis_result_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = result.modelLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = Color(result.suggestedEmotion.colorHex).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Baskın Duygu: ${result.suggestedEmotion.labelTr}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(result.suggestedEmotion.colorHex),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = result.detectedTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Highlighted Literary Audio Description Box (Sesli Kitap Betimlemesi)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("visual_audio_description_box")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Edebi Görsel Betimleme (Sesli Kitap Anlatımı):",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.visualAudioDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        onSpeakVisualDescription(
                                            result.visualAudioDescription,
                                            result.suggestedEmotion
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("speak_visual_description_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Görsel Betimlemeyi Sesli Dinle (${result.suggestedEmotion.labelTr})")
                                }
                            }
                        }

                        // Detected Visual Objects & Elements Chips
                        if (result.detectedVisualElements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tanımlanan Görsel Öğeler, Nesneler & Semboller:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detected_visual_elements_flow")
                            ) {
                                result.detectedVisualElements.forEach { element ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = element,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Composition & Lighting Analysis
                        Text(
                            text = "Kompozisyon, Işık & Perspektif Analizi:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.compositionAndLightingNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Çözümlenen Karakterler & Ses Türleri:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.detectedCharactersSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Atmosfer & Duygu Analizi (${result.suggestedEmotion.labelTr}):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(result.suggestedEmotion.colorHex)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.atmosphericAnalysis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Sayfadan Çıkarılan / Üretilen Diyalog Senaryosu:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.extractedDialoguePreview,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onConvertAnalysisToAudiobook,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("convert_vision_to_audiobook_button")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bu Sayfayı Çok Sesli Romana Dönüştür & Sahneye Aktar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageVisualTelemetryStrip(
    telemetry: PageVisualTelemetry,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("page_visual_telemetry_strip")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Görsel Telemetri & Renk Paleti",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Dominant color swatches
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    telemetry.dominantColorsHex.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Boyut: ${telemetry.widthPx}×${telemetry.heightPx}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Parlaklık: %${telemetry.brightnessPercent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Kontrast: %${telemetry.contrastPercent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Renk Atmosferi: ${telemetry.warmthLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SceneImageStudioScreen(
    sourceEditBitmap: Bitmap?,
    isGenerating: Boolean,
    generatedResult: GeneratedSceneArtResult?,
    imageGenError: String?,
    firebaseNotice: String,
    onPickSourceImageForEdit: () -> Unit,
    onClearSourceImage: () -> Unit,
    onGenerateOrEditImage: (prompt: String, stylePreset: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var prompt by remember {
        mutableStateOf("Kış bahçesinde mum ışığında yüzleşen kadın ve erkek roman karakterleri, dramatik atmosfer")
    }
    val stylePresets = listOf(
        "Sıcak Kehribar Sahne",
        "Karanlık Viktorya",
        "Sinematik Yağlıboya",
        "Mistik Gece"
    )
    var selectedStyle by remember { mutableStateOf(stylePresets.first()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("scene_image_studio_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FirebasePlaceholderNoticeCard(noticeText = firebaseNotice)
        }

        if (imageGenError != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hata (gemini-nano-banana-2.1): $imageGenError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sahne & Karakter Görseli Üret / Düzenle",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Model: gemini-nano-banana-2.1 • Metin istemiyle (prompt) yeni bir roman sahnesi/karakter portresi oluşturun veya mevcut bir görseli seçip düzenleyin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = {
                            Text(
                                if (sourceEditBitmap != null) {
                                    "Görsel Düzenleme Talimatı (gemini-nano-banana-2.1)"
                                } else {
                                    "Görsel Oluşturma İstemi (gemini-nano-banana-2.1)"
                                }
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_gen_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sanat Stili:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        stylePresets.forEach { preset ->
                            FilterChip(
                                selected = selectedStyle == preset,
                                onClick = { selectedStyle = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onPickSourceImageForEdit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_image_to_edit_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (sourceEditBitmap != null) "Görseli Değiştir" else "Düzenlenecek Görsel Seç")
                        }

                        if (sourceEditBitmap != null) {
                            OutlinedButton(
                                onClick = onClearSourceImage,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Görseli Kaldır", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sıfırla")
                            }
                        }
                    }

                    if (sourceEditBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Düzenlenecek Kaynak Görsel Seçildi:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            bitmap = sourceEditBitmap.asImageBitmap(),
                            contentDescription = "Düzenlenecek Kaynak Görsel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onGenerateOrEditImage(prompt, selectedStyle) },
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_or_edit_image_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("gemini-nano-banana-2.1 Çalışıyor...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (sourceEditBitmap != null) {
                                    "Görseli Düzenle (gemini-nano-banana-2.1)"
                                } else {
                                    "Sahne Görseli Oluştur (gemini-nano-banana-2.1)"
                                }
                            )
                        }
                    }
                }
            }
        }

        generatedResult?.let { art ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("generated_image_result_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = art.modelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Image(
                            bitmap = art.bitmap.asImageBitmap(),
                            contentDescription = "Üretilen Roman Sahnesi Görseli",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Uygulanan İstem: “${art.appliedPrompt}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Stil: ${art.styleName} • Mod: ${if (art.editMode) "Görsel Düzenleme" else "Metinden Görsel Üretimi"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
