package com.schedule.shift.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.schedule.shift.domain.ocr.OcrEngine
import com.schedule.shift.domain.ocr.OcrResult
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MlKitOcrEngine(private val recognizer: TextRecognizer) : OcrEngine {

    override suspend fun recognizeText(bitmap: Bitmap): OcrResult =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val inlineExecutor = Executor { it.run() }
            recognizer.process(image)
                .addOnSuccessListener(inlineExecutor) { visionText ->
                    if (cont.isActive) cont.resume(OcrResult.Success(visionText.text))
                }
                .addOnFailureListener(inlineExecutor) { e ->
                    if (cont.isActive) cont.resume(OcrResult.Failure(e))
                }
        }
}
