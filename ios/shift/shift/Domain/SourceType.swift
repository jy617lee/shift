import Foundation

enum SourceType: String, Codable, Hashable, Sendable {
    case parsed = "PARSED"
    case edited = "EDITED"
    case manual = "MANUAL"
}
