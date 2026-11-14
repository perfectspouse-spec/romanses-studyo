package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.BookEntity
import com.example.model.BookSourceFormat
import com.example.model.CharacterVoiceEntity
import com.example.model.EmotionTone
import com.example.model.ScriptLineEntity
import com.example.model.VoiceGender
import kotlinx.coroutines.flow.Flow

@Dao
interface BookStudioDao {
    @Query("""
        SELECT id, title, author, sourceFormat, languageCode, summary, rawExcerpt, characterCount,
            lineCount, isSample, createdAt, '' AS sourceText, playbackLine
        FROM books ORDER BY createdAt DESC, id DESC
    """)
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Insert
    suspend fun insertBook(book: BookEntity): Long

    @Query("UPDATE books SET playbackLine = :line WHERE id = :bookId")
    suspend fun updatePlaybackLine(bookId: Long, line: Int)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    @Query("SELECT * FROM character_voices WHERE bookId = :bookId ORDER BY id ASC")
    fun observeCharactersForBook(bookId: Long): Flow<List<CharacterVoiceEntity>>

    @Query("SELECT * FROM character_voices WHERE bookId = :bookId ORDER BY id ASC")
    suspend fun getCharactersForBookNow(bookId: Long): List<CharacterVoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterVoiceEntity>)

    @Update
    suspend fun updateCharacterVoice(character: CharacterVoiceEntity)


    @Query("SELECT * FROM script_lines WHERE bookId = :bookId ORDER BY orderIndex ASC")
    fun observeScriptLinesForBook(bookId: Long): Flow<List<ScriptLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScriptLines(lines: List<ScriptLineEntity>)

    @Update
    suspend fun updateScriptLine(line: ScriptLineEntity)

}

@Database(
    entities = [
        BookEntity::class,
        CharacterVoiceEntity::class,
        ScriptLineEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookStudioDao(): BookStudioDao

    companion object {
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN sourceText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN playbackLine INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE character_voices_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bookId INTEGER NOT NULL,
                        name TEXT NOT NULL, gender TEXT NOT NULL, roleDescription TEXT NOT NULL,
                        dominantEmotion TEXT NOT NULL, basePitch REAL NOT NULL, baseRate REAL NOT NULL,
                        voiceStyleLabel TEXT NOT NULL, colorHex INTEGER NOT NULL, timbrePreset TEXT NOT NULL,
                        emotionIntensity REAL NOT NULL, pitchModulationRange REAL NOT NULL,
                        dramaticPauseScale REAL NOT NULL, whisperVolumeScale REAL NOT NULL,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE script_lines_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bookId INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL, chapterTitle TEXT NOT NULL, speakerName TEXT NOT NULL,
                        gender TEXT NOT NULL, emotion TEXT NOT NULL, stageDirection TEXT NOT NULL,
                        text TEXT NOT NULL, FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                for (table in listOf("character_voices", "script_lines")) {
                    db.execSQL("INSERT INTO ${table}_new SELECT * FROM $table WHERE bookId IN (SELECT id FROM books)")
                    db.execSQL("DROP TABLE $table")
                    db.execSQL("ALTER TABLE ${table}_new RENAME TO $table")
                }
                db.execSQL("CREATE INDEX index_character_voices_bookId ON character_voices(bookId)")
                db.execSQL("CREATE INDEX index_script_lines_bookId_orderIndex ON script_lines(bookId, orderIndex)")
                db.execSQL("""
                    UPDATE books SET
                        characterCount = (SELECT COUNT(*) FROM character_voices WHERE bookId = books.id),
                        lineCount = (SELECT COUNT(*) FROM script_lines WHERE bookId = books.id)
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "romanses_studio.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
        }
    }
}

class BookStudioRepository(private val database: AppDatabase) {
    private val dao = database.bookStudioDao()

    val allBooks: Flow<List<BookEntity>> = dao.observeAllBooks()

    fun observeCharacters(bookId: Long): Flow<List<CharacterVoiceEntity>> =
        dao.observeCharactersForBook(bookId)

    fun observeScriptLines(bookId: Long): Flow<List<ScriptLineEntity>> =
        dao.observeScriptLinesForBook(bookId)

    suspend fun saveBookWithCastAndScript(
        book: BookEntity,
        characters: List<CharacterVoiceEntity>,
        scriptLines: List<ScriptLineEntity>
    ): Long = database.withTransaction {
        val bookId = dao.insertBook(
            book.copy(
                characterCount = characters.size,
                lineCount = scriptLines.size
            )
        )
        dao.insertCharacters(characters.map { it.copy(bookId = bookId) })
        dao.insertScriptLines(scriptLines.mapIndexed { idx, line ->
            line.copy(bookId = bookId, orderIndex = idx)
        })
        bookId
    }

    suspend fun updateCharacterVoice(character: CharacterVoiceEntity) {
        dao.updateCharacterVoice(character)
    }

    suspend fun updateScriptLineEmotion(line: ScriptLineEntity, newEmotion: EmotionTone) {
        dao.updateScriptLine(line.copy(emotion = newEmotion.name))
    }

    suspend fun updateScriptLineSpeaker(
        line: ScriptLineEntity,
        character: CharacterVoiceEntity
    ) {
        dao.updateScriptLine(
            line.copy(
                speakerName = character.name,
                gender = character.gender
            )
        )
    }

    suspend fun deleteBook(bookId: Long) {
        dao.deleteBookById(bookId)
    }

    suspend fun savePlaybackLine(bookId: Long, line: Int) = dao.updatePlaybackLine(bookId, line)

    suspend fun ensureSampleBooksSeeded() {
        if (dao.getBookCount() > 0) return

        // Sample Book 1: Aşk-ı Memnu (Halit Ziya Uşaklıgil)
        val book1 = BookEntity(
            title = "Aşk-ı Memnu • Kış Bahçesi",
            author = "Halit Ziya Uşaklıgil",
            sourceFormat = BookSourceFormat.SAMPLE.name,
            summary = "Boğaz kıyısındaki yalının kış bahçesinde Bihter, Behlül, Firdevs Hanım ve Adnan Bey arasındaki gerilim, pişmanlık ve tutku dolu diyalogların çok sesli sahne uyarlaması.",
            rawExcerpt = "Kış bahçesinde camlara vuran kar taneleri altında Bihter ve Behlül karşı karşıya duruyordu...",
            characterCount = 5,
            lineCount = 9,
            isSample = true,
            createdAt = System.currentTimeMillis()
        )
        val cast1 = listOf(
            CharacterVoiceEntity(
                bookId = 0,
                name = "Anlatıcı",
                gender = VoiceGender.NARRATOR.name,
                roleDescription = "Yalının boğucu ve görkemli atmosferini betimleyen edebi dış ses",
                dominantEmotion = EmotionTone.CALM.name,
                basePitch = 0.96f,
                baseRate = 0.94f,
                voiceStyleLabel = "Kadife Bariton • Klasik Anlatıcı",
                colorHex = VoiceGender.NARRATOR.badgeColorHex
            ),
            CharacterVoiceEntity(
                bookId = 0,
                name = "Bihter",
                gender = VoiceGender.FEMALE.name,
                roleDescription = "Gururlu, tutkulu ve derin bir iç çatışma yaşayan genç kadın",
                dominantEmotion = EmotionTone.DRAMATIC.name,
                basePitch = 1.28f,
                baseRate = 0.98f,
                voiceStyleLabel = "Dramatik Soprano • Tutkulu Kadın Sesi",
                colorHex = VoiceGender.FEMALE.badgeColorHex
            ),
            CharacterVoiceEntity(
                bookId = 0,
                name = "Behlül",
                gender = VoiceGender.MALE.name,
                roleDescription = "Çekingen, vicdan azabı ile korku arasında sıkışmış genç adam",
                dominantEmotion = EmotionTone.FEARFUL.name,
                basePitch = 0.82f,
                baseRate = 1.04f,
                voiceStyleLabel = "Genç Tenor • Gerilimli Erkek Sesi",
                colorHex = VoiceGender.MALE.badgeColorHex
            ),
            CharacterVoiceEntity(
                bookId = 0,
                name = "Firdevs Hanım",
                gender = VoiceGender.FEMALE.name,
                roleDescription = "Keskin zekâlı, soğukkanlı ve otoriter anne",
                dominantEmotion = EmotionTone.SURPRISED.name,
                basePitch = 1.14f,
                baseRate = 0.90f,
                voiceStyleLabel = "Olgun Alto • Otoriter Kadın Sesi",
                colorHex = 0xFFE9C46A
            ),
            CharacterVoiceEntity(
                bookId = 0,
                name = "Adnan Bey",
                gender = VoiceGender.MALE.name,
                roleDescription = "Yalının ağırbaşlı, şefkatli fakat kırgın sahibi",
                dominantEmotion = EmotionTone.MELANCHOLIC.name,
                basePitch = 0.68f,
                baseRate = 0.88f,
                voiceStyleLabel = "Derin Bas • Ağırbaşlı Erkek Sesi",
                colorHex = 0xFF7EA8BE
            )
        )
        val lines1 = listOf(
            ScriptLineEntity(
                bookId = 0, orderIndex = 0, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Anlatıcı", gender = VoiceGender.NARRATOR.name, emotion = EmotionTone.CALM.name,
                stageDirection = "Sakin ve atmosferik giriş",
                text = "Boğaz'ın karanlık sularına kar yağarken, kış bahçesinin buğulu camları ardında derin ve tehlikeli bir sessizlik hüküm sürüyordu."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 1, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Bihter", gender = VoiceGender.FEMALE.name, emotion = EmotionTone.DRAMATIC.name,
                stageDirection = "Tutkulu ve titreyen bir sesle",
                text = "Gözlerimin içine bakmaya neden cesaret edemiyorsun Behlül? Bütün bu fırtınayı başlatıp şimdi sessizce uzaklaşabileceğini mi sandın!"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 2, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Behlül", gender = VoiceGender.MALE.name, emotion = EmotionTone.FEARFUL.name,
                stageDirection = "Endişeli ve telaşlı",
                text = "Bihter, yalvarırım sesini alçalt! Merdivenlerde ayak sesleri var, birisi duyarsa hepimiz mahvoluruz."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 3, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Bihter", gender = VoiceGender.FEMALE.name, emotion = EmotionTone.ANGRY.name,
                stageDirection = "Öfkeli ve meydan okuyan",
                text = "Korkaklığını sağduyu gibi göstermekten vazgeç! Kalbimin paramparça oluşunu izlerken tek düşündüğün kendi rahatın!"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 4, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Anlatıcı", gender = VoiceGender.NARRATOR.name, emotion = EmotionTone.WHISPER.name,
                stageDirection = "Gerilimli sahne geçişi",
                text = "Tam o anda salonun ağır kadife perdesi aralandı ve Firdevs Hanım keskin bakışlarıyla eşikte belirdi."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 5, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Firdevs Hanım", gender = VoiceGender.FEMALE.name, emotion = EmotionTone.SURPRISED.name,
                stageDirection = "Soğukkanlı ve iğneleyici",
                text = "Bu saatte kış bahçesinde ne hararetli bir sohbet böyle... Bihter, yüzün bembeyaz olmuş kızım, yoksa bana sakladığın bir sır mı var?"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 6, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Adnan Bey", gender = VoiceGender.MALE.name, emotion = EmotionTone.MELANCHOLIC.name,
                stageDirection = "Derin, ağır ve hüzünlü",
                text = "Bu evin çatısı altında son günlerde anlam veremediğim bir gölge dolaşıyor. Sadece huzur istemiştim, çok mu şey bekledim?"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 7, chapterTitle = "Kış Bahçesinde Yüzleşme",
                speakerName = "Bihter", gender = VoiceGender.FEMALE.name, emotion = EmotionTone.WHISPER.name,
                stageDirection = "Fısıltıyla, kendi kendine",
                text = "Artık hiçbir şey eskisi gibi olmayacak... Bu karanlıktan çıkış yok."
            )
        )
        saveBookWithCastAndScript(book1, cast1, lines1)

        // Sample Book 2: Küçük Prens (Antoine de Saint-Exupéry)
        val book2 = BookEntity(
            title = "Küçük Prens • Tilki ile Dostluk",
            author = "Antoine de Saint-Exupéry",
            sourceFormat = BookSourceFormat.SAMPLE.name,
            summary = "Çocuk sesi, bilge Tilki ve Pilot Anlatıcı arasında geçen; merak, hüzün ve saf neşe dolu çok sesli felsefi masal.",
            rawExcerpt = "Evcilleştirmek ne demek? diye sordu Küçük Prens merakla...",
            characterCount = 3,
            lineCount = 6,
            isSample = true,
            createdAt = System.currentTimeMillis() - 60_000L
        )
        val cast2 = listOf(
            CharacterVoiceEntity(
                bookId = 0, name = "Anlatıcı (Pilot)", gender = VoiceGender.NARRATOR.name,
                roleDescription = "Çölde Küçük Prens'in hikâyesini hatırlayan sıcak anlatıcı",
                dominantEmotion = EmotionTone.CALM.name, basePitch = 0.95f, baseRate = 0.93f,
                voiceStyleLabel = "Sıcak Bariton • Masal Anlatıcısı", colorHex = VoiceGender.NARRATOR.badgeColorHex
            ),
            CharacterVoiceEntity(
                bookId = 0, name = "Küçük Prens", gender = VoiceGender.CHILD.name,
                roleDescription = "Altın saçlı, meraklı ve saf kalpli küçük gezgin",
                dominantEmotion = EmotionTone.JOYFUL.name, basePitch = 1.48f, baseRate = 1.06f,
                voiceStyleLabel = "Berrak Çocuk Sesi • Neşeli & Meraklı", colorHex = VoiceGender.CHILD.badgeColorHex
            ),
            CharacterVoiceEntity(
                bookId = 0, name = "Tilki", gender = VoiceGender.MALE.name,
                roleDescription = "Sevginin ve bağ kurmanın sırrını öğreten bilge dost",
                dominantEmotion = EmotionTone.WHISPER.name, basePitch = 0.84f, baseRate = 0.92f,
                voiceStyleLabel = "Bilge Tenor • Yumuşak & Gizemli", colorHex = VoiceGender.MALE.badgeColorHex
            )
        )
        val lines2 = listOf(
            ScriptLineEntity(
                bookId = 0, orderIndex = 0, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Anlatıcı (Pilot)", gender = VoiceGender.NARRATOR.name, emotion = EmotionTone.CALM.name,
                stageDirection = "Yumuşak masalsı anlatım",
                text = "Tilki tam o sırada elma ağacının altında belirdi ve altın sarısı başakların arasından Küçük Prens'e baktı."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 1, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Küçük Prens", gender = VoiceGender.CHILD.name, emotion = EmotionTone.JOYFUL.name,
                stageDirection = "Saf bir neşe ve heyecanla",
                text = "Günaydın! Sen ne kadar güzel bir canlısın, gelip benimle oyun oynar mısın? Bugün çok yalnızım!"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 2, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Tilki", gender = VoiceGender.MALE.name, emotion = EmotionTone.MELANCHOLIC.name,
                stageDirection = "Hüzünlü ve sakin bir bilgelikle",
                text = "Seninle oynayamam Küçük Prens... Çünkü henüz beni evcilleştirmedin. Şu an senin için yüz binlerce tilkiden farksızım."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 3, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Küçük Prens", gender = VoiceGender.CHILD.name, emotion = EmotionTone.SURPRISED.name,
                stageDirection = "Merak ve hayretle",
                text = "Evcilleştirmek mi? Bu sözcük ne anlama geliyor, bana hemen anlatır mısın?"
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 4, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Tilki", gender = VoiceGender.MALE.name, emotion = EmotionTone.WHISPER.name,
                stageDirection = "Sır verir gibi fısıltıyla",
                text = "İşte benim sırrım, çok basit: İnsan ancak yüreğiyle baktığında gerçeği görebilir. Gerçeğin mayası gözle görülmez."
            ),
            ScriptLineEntity(
                bookId = 0, orderIndex = 5, chapterTitle = "21. Bölüm • Elma Ağacının Altında",
                speakerName = "Küçük Prens", gender = VoiceGender.CHILD.name, emotion = EmotionTone.DRAMATIC.name,
                stageDirection = "Unutmak istemeyen içten bir vurguyla",
                text = "Gerçeğin mayası gözle görülmez... Gülüm için harcadığım zaman onu bu kadar değerli kılıyor, bunu asla unutmayacağım!"
            )
        )
        saveBookWithCastAndScript(book2, cast2, lines2)
    }
}
