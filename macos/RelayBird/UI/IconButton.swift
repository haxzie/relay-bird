import SwiftUI

/// A compact circular control for the header bars.
///
/// A bare `Image` in a borderless button is only as clickable as the glyph is wide — a chevron
/// gives you about eight points to aim at. The circle here is the hit target as well as the
/// decoration, so the button is comfortably clickable and shows where the edge is on hover.
struct IconButton: View {
    let systemName: String
    let help: String
    let action: () -> Void

    /// Filled rather than tinted, for the one action a bar is actually about.
    var prominent: Bool = false

    @State private var hovering = false

    private var diameter: CGFloat { 28 }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(foreground)
                .frame(width: diameter, height: diameter)
                .background(Circle().fill(background))
                // Without this the circle's transparent pixels do not take clicks.
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .onHover { hovering = $0 }
        .help(help)
        .accessibilityLabel(help)
    }

    private var foreground: Color {
        if prominent { return hovering ? .white : .accentColor }
        return hovering ? .primary : .secondary
    }

    private var background: Color {
        if prominent {
            return hovering ? Color.accentColor : Color.accentColor.opacity(0.16)
        }
        return Color.primary.opacity(hovering ? 0.14 : 0.07)
    }
}

/// Shared geometry for the popover's header bars, so they line up screen to screen.
enum Header {
    static let height: CGFloat = 46
    static let horizontalPadding: CGFloat = 10
}
