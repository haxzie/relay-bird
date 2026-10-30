import SwiftUI

/// A blur that is strongest at the top and fades to nothing at the bottom, so a floating bar
/// dissolves into the content scrolling beneath it instead of being cut off by a rule.
///
/// SwiftUI has no variable-radius blur, so this stacks several material layers, each masked to
/// reach a little further down than the last. Where they overlap — the top — the blur is
/// heaviest, and the overlap thins out towards the bottom edge.
struct ProgressiveBlur: View {
    var layers: Int = 4

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
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
            }
        }
        // Clipped to whatever it backs; it must not creep past the bar it belongs to.
        .allowsHitTesting(false)
    }
}
