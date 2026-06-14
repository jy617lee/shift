import UIKit

struct ProcessScheduleImageUseCase {
    let ocrEngine: any OcrEngine
    let parser: any ScheduleParser
    let reporter: any FailedImageReporter

    func invoke(image: UIImage) async -> ParseResult {
        switch await ocrEngine.recognizeText(image: image) {
        case .success(let text):
            return await parser.parse(text)
        case .failure:
            return .failure(.parseError)
        }
    }

    func reportFailure(image: UIImage, reason: String) {
        reporter.reportFailure(image: image, errorReason: reason)
    }
}
