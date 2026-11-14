package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.SamplePlaceholderAiRepository
import com.example.model.BookSourceFormat
import com.example.model.EmotionTone
import com.example.model.VoiceGender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RomanSes Stüdyo", appName)
    }

    @Test
    fun `extract characters genders and emotions from novel text`() = runBlocking {
        val repo = SamplePlaceholderAiRepository()
        val sampleNovel = """
            Kış bahçesinde rüzgar uğulduyordu.
            "Bana neden yalan söyledin Selim!" diye bağırdı Leyla, büyük bir öfkeyle.
            "Seni korumak istedim Leyla," diye fısıldadı Selim, hüzünlü bir sesle.
        """.trimIndent()

        val result = repo.analyzeBookTextAndBuildCast(
            bookTitle = "Test Romanı",
            rawText = sampleNovel,
            sourceFormat = BookSourceFormat.TXT
        )

        assertTrue(result.characters.any { it.name == "Leyla" && it.gender == VoiceGender.FEMALE.name })
        assertTrue(result.characters.any { it.name == "Selim" && it.gender == VoiceGender.MALE.name })
        assertTrue(result.scriptLines.any { it.emotion == EmotionTone.ANGRY.name })
        assertTrue(result.scriptLines.any { it.emotion == EmotionTone.WHISPER.name })
    }
}
