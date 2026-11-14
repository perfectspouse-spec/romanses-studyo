package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.parser.ParsedBookDocument
import com.example.model.BookEntity
import com.example.model.BookSourceFormat
import com.example.model.NarrationLanguage
import com.example.ui.PickerFormatFilter

@Composable
fun LibraryAndImportScreen(
    books: List<BookEntity>,
    selectedBookId: Long?,
    selectedLanguage: NarrationLanguage = NarrationLanguage.TURKISH,
    selectedFormatFilter: PickerFormatFilter,
    stagedDocument: ParsedBookDocument?,
    isReadingFile: Boolean,
    isProcessing: Boolean,
    firebaseNotice: String,
    onSelectLanguage: (NarrationLanguage) -> Unit = {},
    onOpenSettingsClick: () -> Unit = {},
    onSelectFormatFilter: (PickerFormatFilter) -> Unit,
    onLaunchFilePicker: (Array<String>) -> Unit,
    onStageSampleFile: (BookSourceFormat) -> Unit,
    onClearStagedDocument: () -> Unit,
    onConfirmStagedUpload: (title: String, author: String, useFastLocal: Boolean) -> Unit,
    onImportCustomText: (title: String, author: String, rawText: String, useLocal: Boolean) -> Unit,
    onSelectBookAndPlay: (Long) -> Unit,
    onSelectBookAndOpenCast: (Long) -> Unit,
    onDeleteBook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPasteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean PDF & EPUB File Picker Center Card
        item {
            CleanBookFilePickerCard(
                selectedLanguage = selectedLanguage,
                selectedFormatFilter = selectedFormatFilter,
                isReadingFile = isReadingFile,
                isProcessing = isProcessing,
                onSelectLanguage = onSelectLanguage,
                onOpenSettingsClick = onOpenSettingsClick,
                onSelectFormatFilter = onSelectFormatFilter,
                onBrowseFilesClick = {
                    onLaunchFilePicker(selectedFormatFilter.mimeTypes)
                },
                onPickPdfDirectly = {
                    onSelectFormatFilter(PickerFormatFilter.PDF_ONLY)
                    onLaunchFilePicker(PickerFormatFilter.PDF_ONLY.mimeTypes)
                },
                onPickEpubDirectly = {
                    onSelectFormatFilter(PickerFormatFilter.EPUB_ONLY)
                    onLaunchFilePicker(PickerFormatFilter.EPUB_ONLY.mimeTypes)
                },
                onStageSampleFile = onStageSampleFile,
                onOpenPasteDialog = { showPasteDialog = true }
            )
        }

        // 2. Staged File Preview & Upload Confirmation Card (shown when a PDF/EPUB/TXT file is selected)
        item {
            AnimatedVisibility(visible = stagedDocument != null) {
                stagedDocument?.let { doc ->
                    StagedDocumentInspectorCard(
                        document = doc,
                        isProcessing = isProcessing,
                        onCancel = onClearStagedDocument,
                        onBrowseAnother = {
                            onLaunchFilePicker(selectedFormatFilter.mimeTypes)
                        },
                        onConfirmGeminiUpload = { title, author ->
                            onConfirmStagedUpload(title, author, false)
                        },
                        onConfirmFastUpload = { title, author ->
                            onConfirmStagedUpload(title, author, true)
                        }
                    )
                }
            }
        }

        if (isProcessing || isReadingFile) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isReadingFile) {
                                "Dosya okunuyor, sayfa ve metin önizlemesi hazırlanıyor..."
                            } else {
                                "Kitap ayrıştırılıyor, karakter cinsiyetleri ve duygu tonları hazırlanıyor..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Firebase AI Status Notice
        item {
            FirebasePlaceholderNoticeCard(noticeText = firebaseNotice)
        }

        // 4. Library Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sesli Kitaplığın (${books.size} Eser)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Yüklenen PDF/EPUB kitaplara dokunarak çok sesli sahneyi dinleyin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 5. Book Cards
        items(books, key = { it.id }) { book ->
            val isSelected = book.id == selectedBookId
            BookLibraryCard(
                book = book,
                isSelected = isSelected,
                onPlayClick = { onSelectBookAndPlay(book.id) },
                onCastClick = { onSelectBookAndOpenCast(book.id) },
                onDeleteClick = { onDeleteBook(book.id) }
            )
        }
    }

    if (showPasteDialog) {
        PasteNovelChapterDialog(
            onDismiss = { showPasteDialog = false },
            onConfirm = { title, author, text, useLocal ->
                showPasteDialog = false
                onImportCustomText(title, author, text, useLocal)
            }
        )
    }
}

@Composable
private fun CleanBookFilePickerCard(
    selectedLanguage: NarrationLanguage,
    selectedFormatFilter: PickerFormatFilter,
    isReadingFile: Boolean,
    isProcessing: Boolean,
    onSelectLanguage: (NarrationLanguage) -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSelectFormatFilter: (PickerFormatFilter) -> Unit,
    onBrowseFilesClick: () -> Unit,
    onPickPdfDirectly: () -> Unit,
    onPickEpubDirectly: () -> Unit,
    onStageSampleFile: (BookSourceFormat) -> Unit,
    onOpenPasteDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_picker_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle header banner
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_audiobook_studio_1794504929592),
                    contentDescription = "RomanSes Stüdyo Kapak Görseli",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x66130F1C),
                                    Color(0xE61E182B)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PDF & EPUB Kitap Dosyası Seçici",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kitap Dosyasını Seç ve Çok Sesli Sahneye Yükle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cihazınızdaki PDF, EPUB veya TXT formatındaki romanları seçin ve seslendirin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE5DCF2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 0: Language & Emotion Processing Selector (TR, EN, FR, IT, ES)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Seslendirme & Duygu Dili (${selectedLanguage.labelTr}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onOpenSettingsClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("open_language_settings_inline_button")
                    ) {
                        Text(
                            text = "Dil & Duygu Ayarları",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("picker_language_selector_row")
                ) {
                    NarrationLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = {
                                Text(
                                    text = "${lang.badgeShort} • ${lang.labelTr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("picker_lang_${lang.code}")
                        )
                    }
                }

                // Step 1: Format Filter Chips
                Text(
                    text = "1. Dosya Formatı Filtresi:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    PickerFormatFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFormatFilter == filter,
                            onClick = { onSelectFormatFilter(filter) },
                            label = {
                                Text(
                                    text = filter.labelTr,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                val icon = when (filter) {
                                    PickerFormatFilter.PDF_ONLY -> Icons.Default.PictureAsPdf
                                    PickerFormatFilter.EPUB_ONLY -> Icons.Default.MenuBook
                                    PickerFormatFilter.TXT_ONLY -> Icons.Default.Description
                                    PickerFormatFilter.ALL -> Icons.Default.FolderOpen
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("format_filter_${filter.name}")
                        )
                    }
                }

                // Step 2: Interactive Dropzone Surface
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(enabled = !isProcessing && !isReadingFile) {
                            onBrowseFilesClick()
                        }
                        .testTag("file_picker_dropzone")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Dosya Seç",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Telefondan ${selectedFormatFilter.labelTr} Dosyası Seçmek İçin Dokunun",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Desteklenen uzantılar: ${selectedFormatFilter.subtitleTr} • Yüklenmeden önce önizlenir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dedicated PDF & EPUB Picker Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onPickPdfDirectly,
                                enabled = !isProcessing && !isReadingFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Seç",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PDF Dosyası Seç",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onPickEpubDirectly,
                                enabled = !isProcessing && !isReadingFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_epub_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "EPUB Seç",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EPUB Dosyası Seç",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Step 3: Quick Sample PDF/EPUB Test Files & Paste Text Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    AssistChip(
                        onClick = { onStageSampleFile(BookSourceFormat.PDF) },
                        enabled = !isProcessing && !isReadingFile,
                        label = {
                            Text(
                                text = "Örnek PDF Seç (Suç ve Ceza.pdf)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("stage_sample_pdf_chip")
                    )

                    AssistChip(
                        onClick = { onStageSampleFile(BookSourceFormat.EPUB) },
                        enabled = !isProcessing && !isReadingFile,
                        label = {
                            Text(
                                text = "Örnek EPUB Seç (Çalıkuşu.epub)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("stage_sample_epub_chip")
                    )

                    AssistChip(
                        onClick = onOpenPasteDialog,
                        enabled = !isProcessing && !isReadingFile,
                        label = {
                            Text(
                                text = "Metin Yapıştır",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("paste_chapter_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun StagedDocumentInspectorCard(
    document: ParsedBookDocument,
    isProcessing: Boolean,
    onCancel: () -> Unit,
    onBrowseAnother: () -> Unit,
    onConfirmGeminiUpload: (title: String, author: String) -> Unit,
    onConfirmFastUpload: (title: String, author: String) -> Unit
) {
    var editableTitle by remember(document.fileName, document.title) {
        mutableStateOf(document.title)
    }
    var editableAuthor by remember(document.fileName, document.author) {
        mutableStateOf(document.author)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("staged_document_inspector_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: File Ready Badge + Close
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seçilen Dosya Önizlemesi & Yükleme Onayı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("clear_staged_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Seçimi İptal Et"
                    )
                }
            }

            // File Metadata Summary Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = when (document.format) {
                                BookSourceFormat.PDF -> Icons.Default.PictureAsPdf
                                BookSourceFormat.EPUB -> Icons.Default.MenuBook
                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = document.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${document.format.displayLabel} • Boyut: ${document.formattedFileSize} • ${document.pageCount} Sayfa/Bölüm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onBrowseAnother) {
                        Text("Değiştir", maxLines = 1)
                    }
                }
            }

            // PDF First Page Preview + Extracted Text Snippet
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                document.previewBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF İlk Sayfa Önizlemesi",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(92.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Çıkarılan Metin Önizlemesi:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = document.extractedText.replace("\n", " ").take(240),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Editable Title & Author before final import
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    label = { Text("Kitap Başlığı", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staged_book_title_input")
                )
                OutlinedTextField(
                    value = editableAuthor,
                    onValueChange = { editableAuthor = it },
                    label = { Text("Yazar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("staged_book_author_input")
                )
            }

            // Upload Confirmation Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onConfirmGeminiUpload(editableTitle, editableAuthor) },
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("confirm_gemini_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gemini’ye gönder ve yükle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { onConfirmFastUpload(editableTitle, editableAuthor) },
                    enabled = !isProcessing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_fast_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hızlı Yükle",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FirebasePlaceholderNoticeCard(
    noticeText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("firebase_placeholder_notice")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Bilgi",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = noticeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BookLibraryCard(
    book: BookEntity,
    isSelected: Boolean,
    onPlayClick: () -> Unit,
    onCastClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onPlayClick() }
            .testTag("book_card_${book.id}")
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.author} • ${book.formatEnum.displayLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!book.isSample) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_book_${book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Kitabı Sil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = book.summary.replace("\n", " "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${book.languageEnum.badgeShort} • ${book.languageEnum.labelTr}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${book.characterCount} Karakter Sesi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${book.lineCount} Duygusal Replik",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlayClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sesli Sahneyi Aç",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onCastClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cast_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Karakter Sesleri",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PasteNovelChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String, rawText: String, useLocal: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Gece Yarısı Mektubu") }
    var author by remember { mutableStateOf("Özel Roman Bölümü") }
    var text by remember {
        mutableStateOf(
            """
            Konak salonunda şöminenin ateşi yavaşça sönüyordu.
            "Bu mektubu bana neden daha önce göstermedin Kerem?" diye sordu Zeynep, hüzünlü ve titrek bir sesle.
            Kerem elindeki zarfı masaya bıraktı.
            "Seni o fırtınalı gecenin gerçeğinden korumak istedim Zeynep!" dedi Kerem, öfkeli ve gergin bir tonla.
            Küçük Ege merdivenlerin başından merakla seslendi.
            "Abla! Bahçedeki fener yanıyor, birisi kapıya çiçek bırakmış!" diye bağırdı Ege, büyük bir neşe ve heyecanla.
            "İnanamıyorum... Yıllar sonra geri dönmüş olmalı," diye fısıldadı Zeynep, şaşkınlıkla.
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Roman Metni veya Diyalog Ekle")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tırnak işareti (\"...\") veya konuşma çizgisi (—) içeren roman metnini yapıştırın. Yerel çözümleme çeviri yapmaz. Gemini seçildiğinde metin Google Firebase AI Logic hizmetine gönderilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Roman / Bölüm Başlığı") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_book_title_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Yazar Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Roman Metni / Diyaloglar") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_book_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, author, text, true) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("confirm_paste_book_button")
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cihazda çözümle")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(title, author, text, false) },
                enabled = text.isNotBlank()
            ) {
                Text("Gemini’ye gönder")
            }
        }
    )
}
