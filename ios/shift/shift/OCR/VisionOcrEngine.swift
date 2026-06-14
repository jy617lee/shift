import UIKit
import Vision

final class VisionOcrEngine: OcrEngine {
    private static let rowYTolerance: CGFloat = 0.03

    func recognizeText(image: UIImage) async -> OcrResult {
        guard let cgImage = image.cgImage else { return .failure }
        return await withCheckedContinuation { continuation in
            Task.detached {
                let text = Self.performRecognition(cgImage: cgImage)
                continuation.resume(returning: text)
            }
        }
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
        let lines = observations.compactMap { obs -> (text: String, top: CGFloat, left: CGFloat)? in
            guard let candidate = obs.topCandidates(1).first else { return nil }
            let top = obs.boundingBox.origin.y + obs.boundingBox.height
            return (candidate.string, top, obs.boundingBox.origin.x)
        }
        let sorted = lines.sorted { $0.top > $1.top }
        let rows = groupIntoRows(sorted)
        return rows
            .map { row in row.sorted { $0.left < $1.left }.map(\.text).joined(separator: " ") }
            .joined(separator: "\n")
    }

    private static func groupIntoRows(
        _ lines: [(text: String, top: CGFloat, left: CGFloat)]
    ) -> [[(text: String, top: CGFloat, left: CGFloat)]] {
        var rows: [[(text: String, top: CGFloat, left: CGFloat)]] = []
        var current: [(text: String, top: CGFloat, left: CGFloat)] = []
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
