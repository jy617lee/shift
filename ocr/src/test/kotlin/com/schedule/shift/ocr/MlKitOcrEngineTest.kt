package com.schedule.shift.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import com.schedule.shift.domain.ocr.OcrResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class MlKitOcrEngineTest {

    private lateinit var recognizer: TextRecognizer
    private lateinit var engine: MlKitOcrEngine

    @Before
    fun setUp() {
        mockkStatic(InputImage::class)
        recognizer = mockk()
        engine = MlKitOcrEngine(recognizer)
    }

    @After
    fun tearDown() {
        unmockkStatic(InputImage::class)
    }

    @Test
    fun `recognizeText returns Success with extracted text`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val visionText = mockk<Text>()
        every { visionText.text } returns "06/08(월) 09:00~18:00 정상"
        every { InputImage.fromBitmap(any(), any()) } returns mockk()
        every { recognizer.process(any<InputImage>()) } returns Tasks.forResult(visionText)

        val result = engine.recognizeText(bitmap)

        assertTrue(result is OcrResult.Success)
        assertEquals("06/08(월) 09:00~18:00 정상", (result as OcrResult.Success).text)
    }

    @Test
    fun `recognizeText returns Failure when ML Kit throws`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val exception = RuntimeException("ML Kit 처리 실패")
        every { InputImage.fromBitmap(any(), any()) } returns mockk()
        every { recognizer.process(any<InputImage>()) } returns Tasks.forException(exception)

        val result = engine.recognizeText(bitmap)

        assertTrue(result is OcrResult.Failure)
        assertEquals(exception, (result as OcrResult.Failure).cause)
    }

    @Test
    fun `recognizeText converts bitmap to InputImage with zero rotation`() = runTest {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val visionText = mockk<Text>()
        every { visionText.text } returns "텍스트"
        every { InputImage.fromBitmap(any(), any()) } returns mockk()
        every { recognizer.process(any<InputImage>()) } returns Tasks.forResult(visionText)

        engine.recognizeText(bitmap)

        verify { InputImage.fromBitmap(bitmap, 0) }
    }
}
