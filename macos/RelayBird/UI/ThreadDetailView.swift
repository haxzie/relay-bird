import AppKit
import SwiftUI

struct ThreadDetailView: View {
    @ObservedObject var client: BridgeClient
    let threadId: Int64
    let onBack: () -> Void

    private var thread: WireThread? {
        client.threads.first { $0.threadId == threadId }
    }

    private var messages: [WireMessage] {
        client.messagesByThread[threadId] ?? []
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 6) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
                .buttonStyle(.borderless)

                VStack(alignment: .leading, spacing: 0) {
                    Text(thread?.title ?? "Conversation")
                        .font(.headline)
                        .lineLimit(1)
                    if let thread, thread.displayName != nil {
                        Text(thread.address)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)

            Divider()

            if messages.isEmpty {
                EmptyStateView(
                    symbol: "ellipsis.bubble",
                    title: "Loading…",
                    detail: "Fetching this conversation from the phone."
                )
            } else {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 6) {
                            ForEach(messages) { message in
                                MessageBubble(message: message).id(message.id)
                            }
                        }
                        .padding(12)
                    }
                    // A conversation opens at its newest message, however much history sits
                    // above it and whether or not any of it is unread. This does the work on
                    // the first layout pass; a scrollTo in onAppear races it and often lands
                    // before the lazy rows exist.
                    .defaultScrollAnchor(.bottom)
                    .onChange(of: messages.last?.id) { _, id in
                        withAnimation { proxy.scrollTo(id, anchor: .bottom) }
                    }
                }
            }
        }
    }
}

private struct MessageBubble: View {
    let message: WireMessage

    var body: some View {
        HStack {
            if message.outgoing { Spacer(minLength: 40) }
            VStack(alignment: message.outgoing ? .trailing : .leading, spacing: 2) {
                // Selectable so a code can be copied out by hand, on top of the one-click
                // button below.
                Text(message.body)
                    .textSelection(.enabled)
                    .font(.system(size: 13))
                    .foregroundStyle(message.outgoing ? Color.white : Color.primary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(
                        message.outgoing ? Color.accentColor : Color.primary.opacity(0.08),
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )

                HStack(spacing: 6) {
                    Text(message.timestamp.formatted(date: .omitted, time: .shortened))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    if let code = message.body.oneTimeCode {
                        Button("Copy \(code)") {
                            NSPasteboard.general.clearContents()
                            NSPasteboard.general.setString(code, forType: .string)
                        }
                        .buttonStyle(.link)
                        .font(.caption2)
                    }
                }
            }
            if !message.outgoing { Spacer(minLength: 40) }
        }
    }
}

extension String {
    /// Pulls out a 4-8 digit code, which is the single most common reason to read an SMS on a
    /// computer.
    var oneTimeCode: String? {
        guard let regex = try? NSRegularExpression(pattern: "(?<![0-9])([0-9]{4,8})(?![0-9])") else {
            return nil
        }
        let range = NSRange(startIndex ..< endIndex, in: self)
        guard let match = regex.firstMatch(in: self, range: range),
              let matchRange = Range(match.range(at: 1), in: self)
        else {
            return nil
        }
        return String(self[matchRange])
    }
}
