import Foundation

struct Stage1Validator {
    private static let datePattern: NSRegularExpression = makePattern(
        #"\d{1,2}/\d{1,2}\([월화수목금토일]\)"#
    )
    private static let headerKeywords = [
        "근무", "출근", "시간표", "스케줄", "스케쥴", "근태", "일정표",
    ]

    func isScheduleText(_ text: String) -> Bool {
        containsDatePattern(text) && containsHeaderKeyword(text)
    }

    private func containsDatePattern(_ text: String) -> Bool {
        let range = NSRange(text.startIndex..., in: text)
        return Stage1Validator.datePattern.firstMatch(in: text, range: range) != nil
    }

    private func containsHeaderKeyword(_ text: String) -> Bool {
        Stage1Validator.headerKeywords.contains { text.contains($0) }
    }

    private static func makePattern(_ pattern: String) -> NSRegularExpression {
        do {
            return try NSRegularExpression(pattern: pattern)
        } catch {
            fatalError("Stage1Validator: invalid regex pattern — \(error)")
        }
    }
}
