import UIKit

final class NoOpFailedImageReporter: FailedImageReporter {
    func reportFailure(image: UIImage, errorReason: String) {}
}
