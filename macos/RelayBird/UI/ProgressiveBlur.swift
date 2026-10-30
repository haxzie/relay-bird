import SwiftUI

/// A blur that is strongest at one edge and fades to nothing at the other, so a floating bar
/// dissolves into the content scrolling past it instead of being cut off by a rule.
///
/// SwiftUI has no variable-radius blur, so this stacks several material layers, each masked to
/// reach a little further down than the last. Where they overlap — the top — the blur is
/// heaviest, and the overlap thins out towards the bottom edge.
struct ProgressiveBlur: View {
    /// Which edge the blur is heaviest at — the edge the bar is pinned to.
    enum Anchor {
        case top
        case bottom
    }

    var anchor: Anchor = .top
    var layers: Int = 4

    private var start: UnitPoint { anchor == .top ? .top : .bottom }
    private var end: UnitPoint { anchor == .top ? .bottom : .top }

    var body: some View {
        ZStack {
            ForEach(0 ..< layers, id: \.self) { layer in
                let reach = CGFloat(layer + 1) / CGFloat(layers)
                Rectangle()
                    .fill(.ultraThinMaterial)
                    .mask(
                        LinearGradient(
                            stops: [
                                .init(color: .black, location: 0),
                                .init(color: .black, location: reach * 0.55),
                                .init(color: .clear, location: reach),
                            ],
                            startPoint: start,
                            endPoint: end
                        )
                    )
            }
        }
        // Clipped to whatever it backs; it must not creep past the bar it belongs to.
        .allowsHitTesting(false)
    }
}
