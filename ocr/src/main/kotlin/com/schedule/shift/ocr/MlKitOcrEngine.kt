package com.schedule.shift.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as VisionText
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
                    val reconstructed = reconstructRows(visionText)
                    Log.d(TAG, "OCR raw:\n${visionText.text}")
                    Log.d(TAG, "OCR reconstructed:\n$reconstructed")
                    if (cont.isActive) cont.resume(OcrResult.Success(reconstructed))
                }
                .addOnFailureListener(inlineExecutor) { e ->
                    Log.e(TAG, "OCR failed", e)
                    if (cont.isActive) cont.resume(OcrResult.Failure(e))
                }
        }

    // ML Kit reads table columns as separate blocks. Regroup by Y-coordinate so each
    // visual row (date + start time + end time + code) becomes one line for the parser.
    private fun reconstructRows(visionText: VisionText): String {
        data class TextLine(val text: String, val top: Int, val left: Int)

        val allLines = visionText.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                TextLine(line.text, box.top, box.left)
            }

        if (allLines.isEmpty()) return visionText.text

        val sorted = allLines.sortedBy { it.top }
        val rows = mutableListOf<MutableList<TextLine>>()
        var currentRow = mutableListOf(sorted.first())

        for (line in sorted.drop(1)) {
            val rowTop = currentRow.minOf { it.top }
            if (line.top - rowTop <= ROW_Y_TOLERANCE) {
                currentRow.add(line)
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(line)
            }
        }
        rows.add(currentRow)

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text }
        }
    }

    companion object {
        private const val TAG = "ShiftOCR"
        private const val ROW_Y_TOLERANCE = 25
    }
}
