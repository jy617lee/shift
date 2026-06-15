import SwiftUI

enum ShiftBadgeType { case work, off }

struct ShiftTypeBadge: View {
    let label: String
    let type: ShiftBadgeType

    var body: some View {
        let (bg, fg) = colors
        Text(label)
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(fg)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(bg, in: RoundedRectangle(cornerRadius: 6))
    }

    private var colors: (Color, Color) {
        switch type {
        case .work: return (Color.accentColor.opacity(0.15), .accentColor)
        case .off: return (ShiftColors.muted, ShiftColors.mutedForeground)
        }
    }
}
