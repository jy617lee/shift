import UIKit
import Vision

final class VisionOcrEngine: OcrEngine {
    private static let rowYTolerance: CGFloat = 0.03

    func recognizeText(image: UIImage) async -> OcrResult {
        guard let cgImage = image.cgImage else { return .failure }
        return await Task.detached(priority: .userInitiated) {
            Self.performRecognition(cgImage: cgImage)
        }.value
    }

    private static func performRecognition(cgImage: CGImage) -> OcrResult {
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["ko-KR", "en-US"]
        let handler = VNImageRequestHandler(cgImage: cgImage)
        do {
            try handler.perform([request])
        } catch {
            return .failure
        }
        let observations = request.results ?? []
        let text = reconstructRows(from: observations)
        return .success(text)
    }

    private static func reconstructRows(from observations: [VNRecognizedTextObservation]) -> String {
        guard !observations.isEmpty else { return "" }
        let lines = observations.compactMap { obs -> TextLine? in
            guard let candidate = obs.topCandidates(1).first else { return nil }
            let top = obs.boundingBox.origin.y + obs.boundingBox.height
            return TextLine(text: candidate.string, top: top, left: obs.boundingBox.origin.x)
        }
        let sorted = lines.sorted { $0.top > $1.top }
        let rows = groupIntoRows(sorted)
        return rows
            .map { row in row.sorted { $0.left < $1.left }.map(\.text).joined(separator: " ") }
            .joined(separator: "\n")
    }

    private static func groupIntoRows(_ lines: [TextLine]) -> [[TextLine]] {
        var rows: [[TextLine]] = []
        var current: [TextLine] = []
        for line in lines {
            if current.isEmpty {
                current.append(line)
            } else {
                let rowTop = current.map(\.top).max() ?? 0
                if abs(rowTop - line.top) <= rowYTolerance {
                    current.append(line)
                } else {
                    rows.append(current)
                    current = [line]
                }
            }
        }
        if !current.isEmpty { rows.append(current) }
        return rows
    }
}

private struct TextLine {
    let text: String
    let top: CGFloat
    let left: CGFloat
}
