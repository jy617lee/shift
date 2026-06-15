import Foundation

enum DayType: String, Codable, Hashable, Sendable {
    case work = "WORK"
    case off = "OFF"
    case other = "OTHER"
    case unregistered = "UNREGISTERED"
}
