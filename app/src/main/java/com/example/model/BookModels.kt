package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

enum class NarrationLanguage(
    val code: String,
    val country: String,
    val bcp47Tag: String,
    val labelTr: String,
    val nativeName: String,
    val badgeShort: String,
    val narratorDefaultName: String,
    val emotionProcessingDescTr: String
) {
    TURKISH(
        code = "tr",
        country = "TR",
        bcp47Tag = "tr-TR",
        labelTr = "Türkçe",
        nativeName = "Türkçe",
        badgeShort = "TR",
        narratorDefaultName = "Anlatıcı",
        emotionProcessingDescTr = "Türkçe edebi metin, diyalog ve 8 duygu tonu işleme"
    ),
    ENGLISH(
        code = "en",
        country = "US",
        bcp47Tag = "en-US",
        labelTr = "İngilizce",
        nativeName = "English",
        badgeShort = "EN",
        narratorDefaultName = "Narrator",
        emotionProcessingDescTr = "İngilizce roman/kitap seslendirme ve duygu modülasyonu"
    ),
    FRENCH(
        code = "fr",
        country = "FR",
        bcp47Tag = "fr-FR",
        labelTr = "Fransızca",
        nativeName = "Français",
        badgeShort = "FR",
        narratorDefaultName = "Narrateur",
        emotionProcessingDescTr = "Fransızca edebi diyalog ayrıştırma ve duygusal seslendirme"
    ),
    ITALIAN(
        code = "it",
        country = "IT",
        bcp47Tag = "it-IT",
        labelTr = "İtalyanca",
        nativeName = "Italiano",
        badgeShort = "IT",
        narratorDefaultName = "Narratore",
        emotionProcessingDescTr = "İtalyanca karakter tespiti, cinsiyet ve duygu tonlaması"
    ),
    SPANISH(
        code = "es",
        country = "ES",
        bcp47Tag = "es-ES",
        labelTr = "İspanyolca",
        nativeName = "Español",
        badgeShort = "ES",
        narratorDefaultName = "Narrador",
        emotionProcessingDescTr = "İspanyolca çok sesli kitap okuma ve duygu analizi"
    );

    fun toLocale(): Locale = Locale(code, country)

    fun sampleVoiceTestPhrase(characterName: String, emotion: EmotionTone): String {
        return when (this) {
            TURKISH -> when (emotion) {
                EmotionTone.JOYFUL -> "Merhaba, ben $characterName! Bugün kalbim büyük bir neşe ve umutla çarpıyor!"
                EmotionTone.MELANCHOLIC -> "Ben $characterName... Geçmişin bütün hüzünlü hatıraları hâlâ sesimde yankılanıyor."
                EmotionTone.ANGRY -> "Ben $characterName! Bana söylenen yalanlara artık tek bir saniye bile tahammülüm yok!"
                EmotionTone.FEARFUL -> "Duyuyor musunuz? Karanlıkta birisi yaklaşıyor, kalbim yerinden çıkacak gibi!"
                EmotionTone.WHISPER -> "Yaklaş ve dinle... Bu sırrı sadece sana fısıltıyla anlatıyorum."
                EmotionTone.SURPRISED -> "İnanamıyorum! Bunca zamandır aradığımız mektup gerçekten bu muydu?!"
                EmotionTone.DRAMATIC -> "Ben $characterName. Kaderimiz bu satırlarda yeniden yazılıyor!"
                EmotionTone.CALM -> "Merhaba, ben $characterName. Roman boyunca bu ses tonuyla hikayeye hayat vereceğim."
            }
            ENGLISH -> when (emotion) {
                EmotionTone.JOYFUL -> "Hello, I am $characterName! My heart is overflowing with pure joy and hope today!"
                EmotionTone.MELANCHOLIC -> "I am $characterName... All the sorrowful memories of the past still echo in my voice."
                EmotionTone.ANGRY -> "I am $characterName! I will not tolerate these lies for a single second longer!"
                EmotionTone.FEARFUL -> "Did you hear that? Someone is approaching in the dark, my heart is racing with fear!"
                EmotionTone.WHISPER -> "Come closer and listen carefully... I am whispering this secret only to you."
                EmotionTone.SURPRISED -> "I cannot believe it! Was this really the lost letter we searched for all these years?!"
                EmotionTone.DRAMATIC -> "I am $characterName. Our destiny is being rewritten in these very pages!"
                EmotionTone.CALM -> "Hello, I am $characterName. Throughout the novel, I will bring this story to life."
            }
            FRENCH -> when (emotion) {
                EmotionTone.JOYFUL -> "Bonjour, je suis $characterName ! Mon cœur déborde de joie et d'espoir aujourd'hui !"
                EmotionTone.MELANCHOLIC -> "Je suis $characterName... Tous les souvenirs mélancoliques du passé résonnent encore en moi."
                EmotionTone.ANGRY -> "Je suis $characterName ! Je ne supporterai plus ces mensonges une seule seconde !"
                EmotionTone.FEARFUL -> "Entendez-vous ce bruit ? Quelqu'un s'approche dans l'ombre, j'ai tellement peur !"
                EmotionTone.WHISPER -> "Approche-toi et écoute... Je te confie ce secret à voix basse."
                EmotionTone.SURPRISED -> "C'est incroyable ! Était-ce vraiment la lettre perdue que nous cherchions ?!"
                EmotionTone.DRAMATIC -> "Je suis $characterName. Notre destin s'écrit à nouveau dans ces lignes !"
                EmotionTone.CALM -> "Bonjour, je suis $characterName. Je donnerai vie à ce roman avec ma voix."
            }
            ITALIAN -> when (emotion) {
                EmotionTone.JOYFUL -> "Ciao, sono $characterName! Oggi il mio cuore batte di pura gioia e speranza!"
                EmotionTone.MELANCHOLIC -> "Sono $characterName... Tutti i ricordi malinconici del passato risuonano ancora nella mia voce."
                EmotionTone.ANGRY -> "Sono $characterName! Non sopporterò queste bugie nemmeno per un altro secondo!"
                EmotionTone.FEARFUL -> "Avete sentito? Qualcuno si avvicina nel buio, il mio cuore trema di paura!"
                EmotionTone.WHISPER -> "Avvicinati e ascolta... Ti sto sussurrando questo segreto in confidenza."
                EmotionTone.SURPRISED -> "Non ci posso credere! Era davvero questa la lettera perduta che cercavamo?!"
                EmotionTone.DRAMATIC -> "Sono $characterName. Il nostro destino si sta riscrivendo in queste pagine!"
                EmotionTone.CALM -> "Ciao, sono $characterName. Darò voce ed emozione a questo romanzo."
            }
            SPANISH -> when (emotion) {
                EmotionTone.JOYFUL -> "¡Hola, soy $characterName! ¡Hoy mi corazón late con inmensa alegría y esperanza!"
                EmotionTone.MELANCHOLIC -> "Soy $characterName... Todos los recuerdos melancólicos del pasado aún resuenan en mi voz."
                EmotionTone.ANGRY -> "¡Soy $characterName! ¡No toleraré estas mentiras ni un solo segundo más!"
                EmotionTone.FEARFUL -> "¿Escucharon eso? ¡Alguien se acerca en la oscuridad y tiemblo de miedo!"
                EmotionTone.WHISPER -> "Acércate y escucha... Te estoy susurrando este secreto solo a ti."
                EmotionTone.SURPRISED -> "¡No puedo creerlo! ¿Era realmente esta la carta perdida que buscábamos?!"
                EmotionTone.DRAMATIC -> "Soy $characterName. ¡Nuestro destino se escribe de nuevo en estas páginas!"
                EmotionTone.CALM -> "Hola, soy $characterName. Daré vida y emoción a toda esta novela."
            }
        }
    }

    companion object {
        fun fromCode(code: String?): NarrationLanguage {
            if (code.isNullOrBlank()) return TURKISH
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) || it.name.equals(code.trim(), ignoreCase = true) }
                ?: TURKISH
        }
    }
}

enum class VoiceGender(
    val labelTr: String,
    val defaultBasePitch: Float,
    val defaultBaseRate: Float,
    val badgeColorHex: Long
) {
    FEMALE(
        labelTr = "Kadın Ses",
        defaultBasePitch = 1.28f,
        defaultBaseRate = 1.00f,
        badgeColorHex = 0xFFF28482
    ),
    MALE(
        labelTr = "Erkek Ses",
        defaultBasePitch = 0.78f,
        defaultBaseRate = 0.96f,
        badgeColorHex = 0xFF64B5F6
    ),
    CHILD(
        labelTr = "Çocuk / Genç Ses",
        defaultBasePitch = 1.48f,
        defaultBaseRate = 1.08f,
        badgeColorHex = 0xFFB388FF
    ),
    NARRATOR(
        labelTr = "Anlatıcı (Dış Ses)",
        defaultBasePitch = 0.98f,
        defaultBaseRate = 0.94f,
        badgeColorHex = 0xFF2A9D8F
    )
}

enum class VoiceTimbrePreset(
    val labelTr: String,
    val descriptionTr: String,
    val pitchOffset: Float,
    val rateOffset: Float
) {
    WARM_VELVET(
        labelTr = "Sıcak & Kadife",
        descriptionTr = "Yumuşak geçişli, samimi ve edebi tını",
        pitchOffset = -0.04f,
        rateOffset = -0.03f
    ),
    BRIGHT_LYRIC(
        labelTr = "Parlak & Lirik",
        descriptionTr = "Berrak, net ve yüksek enerjili tını",
        pitchOffset = 0.08f,
        rateOffset = 0.03f
    ),
    DEEP_RESONANT(
        labelTr = "Derin & Tok Rezonans",
        descriptionTr = "Göğüs sesli, ağırbaşlı ve güçlü tını",
        pitchOffset = -0.11f,
        rateOffset = -0.05f
    ),
    CRISP_THEATRICAL(
        labelTr = "Teatral & Vurgulu",
        descriptionTr = "Sahne diksiyonuna uygun dramatik tını",
        pitchOffset = 0.03f,
        rateOffset = 0.00f
    ),
    SOFT_INTIMATE(
        labelTr = "Yumuşak & İçten",
        descriptionTr = "Yakın mikrofon hissi veren sakin tını",
        pitchOffset = -0.03f,
        rateOffset = -0.06f
    )
}

enum class EmotionTone(
    val labelTr: String,
    val subtitleTr: String,
    val pitchDelta: Float,
    val rateMultiplier: Float,
    val pauseAfterMs: Long,
    val colorHex: Long
) {
    CALM(
        labelTr = "Sakin",
        subtitleTr = "Dengeli ve akıcı anlatım",
        pitchDelta = 0.0f,
        rateMultiplier = 1.00f,
        pauseAfterMs = 850L,
        colorHex = 0xFF2A9D8F
    ),
    JOYFUL(
        labelTr = "Neşeli & Coşkulu",
        subtitleTr = "Parlak ton, enerjik tempo",
        pitchDelta = 0.14f,
        rateMultiplier = 1.13f,
        pauseAfterMs = 720L,
        colorHex = 0xFFE9C46A
    ),
    MELANCHOLIC(
        labelTr = "Hüzünlü & Kırgın",
        subtitleTr = "Yumuşak, düşük perde ve ağır tempo",
        pitchDelta = -0.12f,
        rateMultiplier = 0.82f,
        pauseAfterMs = 1300L,
        colorHex = 0xFF7EA8BE
    ),
    ANGRY(
        labelTr = "Öfkeli & Gergin",
        subtitleTr = "Tok, keskin vurgu ve hızlı tempo",
        pitchDelta = -0.08f,
        rateMultiplier = 1.16f,
        pauseAfterMs = 680L,
        colorHex = 0xFFE76F51
    ),
    FEARFUL(
        labelTr = "Korku & Endişe",
        subtitleTr = "Titrek yüksek perde, telaşlı ritim",
        pitchDelta = 0.16f,
        rateMultiplier = 1.14f,
        pauseAfterMs = 780L,
        colorHex = 0xFFB388FF
    ),
    WHISPER(
        labelTr = "Fısıltı & Sır",
        subtitleTr = "Derin, ağır ve gizemli tonlama",
        pitchDelta = -0.16f,
        rateMultiplier = 0.78f,
        pauseAfterMs = 1150L,
        colorHex = 0xFF90BE6D
    ),
    SURPRISED(
        labelTr = "Şaşkın & Hayret",
        subtitleTr = "Yüksek vurgu ve ani ton çıkışı",
        pitchDelta = 0.20f,
        rateMultiplier = 1.10f,
        pauseAfterMs = 820L,
        colorHex = 0xFFF4A261
    ),
    DRAMATIC(
        labelTr = "Tutkulu & Dramatik",
        subtitleTr = "Teatral vurgu ve derin esler",
        pitchDelta = 0.06f,
        rateMultiplier = 0.90f,
        pauseAfterMs = 1200L,
        colorHex = 0xFFF28482
    )
}

enum class BookSourceFormat(val displayLabel: String) {
    PDF("PDF Kitap"),
    EPUB("EPUB E-Kitap"),
    TXT("Metin / Bölüm"),
    IMAGE_SCAN("Sayfa Fotoğrafı"),
    SAMPLE("Hazır Klasik Eser")
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val sourceFormat: String, // BookSourceFormat.name
    val languageCode: String = NarrationLanguage.TURKISH.code,
    val summary: String,
    val rawExcerpt: String,
    val characterCount: Int,
    val lineCount: Int,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val sourceText: String = "",
    @ColumnInfo(defaultValue = "0") val playbackLine: Int = 0
) {
    val formatEnum: BookSourceFormat
        get() = runCatching { BookSourceFormat.valueOf(sourceFormat) }.getOrDefault(BookSourceFormat.TXT)

    val languageEnum: NarrationLanguage
        get() = NarrationLanguage.fromCode(languageCode)
}

@Entity(
    tableName = "character_voices",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId"])]
)
data class CharacterVoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val name: String,
    val gender: String, // VoiceGender.name
    val roleDescription: String,
    val dominantEmotion: String, // EmotionTone.name
    val basePitch: Float,
    val baseRate: Float,
    val voiceStyleLabel: String,
    val colorHex: Long,
    // Advanced Voice Tone & Emotion Modulation Parameters
    val timbrePreset: String = VoiceTimbrePreset.CRISP_THEATRICAL.name,
    val emotionIntensity: Float = 1.15f,       // 0.30f .. 2.00f
    val pitchModulationRange: Float = 0.14f,   // 0.00f .. 0.40f
    val dramaticPauseScale: Float = 1.00f,     // 0.50f .. 2.00f
    val whisperVolumeScale: Float = 0.72f      // 0.40f .. 1.00f
) {
    val genderEnum: VoiceGender
        get() = runCatching { VoiceGender.valueOf(gender) }.getOrDefault(VoiceGender.NARRATOR)

    val dominantEmotionEnum: EmotionTone
        get() = runCatching { EmotionTone.valueOf(dominantEmotion) }.getOrDefault(EmotionTone.CALM)

    val timbrePresetEnum: VoiceTimbrePreset
        get() = runCatching { VoiceTimbrePreset.valueOf(timbrePreset) }.getOrDefault(VoiceTimbrePreset.CRISP_THEATRICAL)
}

@Entity(
    tableName = "script_lines",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId", "orderIndex"])]
)
data class ScriptLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val orderIndex: Int,
    val chapterTitle: String,
    val speakerName: String,
    val gender: String, // VoiceGender.name
    val emotion: String, // EmotionTone.name
    val stageDirection: String,
    val text: String
) {
    val genderEnum: VoiceGender
        get() = runCatching { VoiceGender.valueOf(gender) }.getOrDefault(VoiceGender.NARRATOR)

    val emotionEnum: EmotionTone
        get() = runCatching { EmotionTone.valueOf(emotion) }.getOrDefault(EmotionTone.CALM)
}
