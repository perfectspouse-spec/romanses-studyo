package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BookStudioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryMigrationTest {
    @Test
    fun versionThreeBooksAndLinesSurviveUpgradeAndDeleteCascades() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            createVersionThreeSchema(old)
            old.execSQL("INSERT INTO books VALUES (1, 'Roman', 'Yazar', 'TXT', 'tr', '', 'Alıntı', 0, 1, 0, 0)")
            old.execSQL("INSERT INTO script_lines VALUES (1, 1, 0, '', 'Anlatıcı', 'NARRATOR', 'CALM', '', 'Replik')")
            old.execSQL("INSERT INTO script_lines VALUES (2, 99, 0, '', 'Anlatıcı', 'NARRATOR', 'CALM', '', 'Artık')")
            old.version = 3
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_3_4).build()
        try {
            val repo = BookStudioRepository(db)
            assertEquals("Roman", repo.allBooks.first().single().title)
            assertEquals("Replik", repo.observeScriptLines(1).first().single().text)
            assertEquals(emptyList<Any>(), repo.observeScriptLines(99).first())
            repo.savePlaybackLine(1, 7)
            assertEquals(7, repo.allBooks.first().single().playbackLine)
            repo.deleteBook(1)
            assertEquals(emptyList<Any>(), repo.observeScriptLines(1).first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
        Unit
    }

    private fun createVersionThreeSchema(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE books (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL,
                sourceFormat TEXT NOT NULL, languageCode TEXT NOT NULL, summary TEXT NOT NULL,
                rawExcerpt TEXT NOT NULL, characterCount INTEGER NOT NULL, lineCount INTEGER NOT NULL,
                isSample INTEGER NOT NULL, createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE character_voices (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bookId INTEGER NOT NULL, name TEXT NOT NULL,
                gender TEXT NOT NULL, roleDescription TEXT NOT NULL, dominantEmotion TEXT NOT NULL,
                basePitch REAL NOT NULL, baseRate REAL NOT NULL, voiceStyleLabel TEXT NOT NULL,
                colorHex INTEGER NOT NULL, timbrePreset TEXT NOT NULL, emotionIntensity REAL NOT NULL,
                pitchModulationRange REAL NOT NULL, dramaticPauseScale REAL NOT NULL, whisperVolumeScale REAL NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE script_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bookId INTEGER NOT NULL, orderIndex INTEGER NOT NULL,
                chapterTitle TEXT NOT NULL, speakerName TEXT NOT NULL, gender TEXT NOT NULL, emotion TEXT NOT NULL,
                stageDirection TEXT NOT NULL, text TEXT NOT NULL
            )
        """.trimIndent())
    }
}
