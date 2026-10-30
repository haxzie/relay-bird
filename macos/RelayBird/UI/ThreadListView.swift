import SwiftUI

struct ThreadListView: View {
    @ObservedObject var client: BridgeClient
    let onOpen: (Int64) -> Void

    @State private var confirmingMarkAll = false

    var body: some View {
        VStack(spacing: 0) {
            if confirmingMarkAll {
                MarkAllConfirmation(
                    count: client.unreadCount,
                    onCancel: { confirmingMarkAll = false },
                    onConfirm: {
                        client.markAllRead()
                        confirmingMarkAll = false
                    }
                )
            } else {
                HStack(spacing: 8) {
                    Text("Messages").font(.headline)
                    Spacer()
                    if client.unreadCount > 0 {
                        Text("\(client.unreadCount) unread")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button {
                            confirmingMarkAll = true
                        } label: {
                            Image(systemName: "checkmark.message")
                        }
                        .buttonStyle(.borderless)
                        .help("Mark all as read")
                        .accessibilityLabel("Mark all as read")
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
            }

            Divider()

            if client.threads.isEmpty {
                EmptyStateView(
                    symbol: "message",
                    title: "No messages yet",
                    detail: client.state == .connected
                        ? "Nothing in the phone's inbox."
                        : "Waiting for the phone to come online."
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(client.threads) { thread in
                            ThreadRow(thread: thread) { onOpen(thread.threadId) }
                            Divider().padding(.leading, 12)
                        }
                    }
                }
            }
        }
    }
}

/// Asked inline rather than with a confirmationDialog: this lives in a transient popover,
/// which dismisses as soon as a modal takes focus away from it.
private struct MarkAllConfirmation: View {
    let count: Int
    let onCancel: () -> Void
    let onConfirm: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Mark \(count) message\(count == 1 ? "" : "s") as read?")
                .font(.subheadline.weight(.medium))
            // Said plainly, because it is the part people would otherwise be surprised by.
            Text("This clears them on this Mac. Your phone will still show them as unread — a non-default SMS app cannot change that.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            HStack {
                Spacer()
                Button("Cancel", action: onCancel)
                Button("Mark all read", action: onConfirm)
                    .keyboardShortcut(.defaultAction)
            }
        }
        .controlSize(.small)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
    }
}

private struct ThreadRow: View {
    let thread: WireThread
    let onTap: () -> Void
    @State private var hovering = false

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 8) {
                Circle()
                    .fill(thread.unread > 0 ? Color.accentColor : .clear)
                    .frame(width: 7, height: 7)
                    .padding(.top, 5)

                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(thread.title)
                            .font(.system(size: 13, weight: thread.unread > 0 ? .semibold : .regular))
                            .lineLimit(1)
                        Spacer()
                        Text(thread.timestamp.shortRelative)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Text(thread.snippet)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(hovering ? Color.primary.opacity(0.06) : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovering = $0 }
    }
}

struct EmptyStateView: View {
    let symbol: String
    let title: String
    let detail: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.system(size: 28))
                .foregroundStyle(.tertiary)
            Text(title).font(.headline)
            Text(detail)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

extension Date {
    /// "9:41", "Yesterday", "Mon" or "12/03" depending on how far back it is — what a message
    /// list wants, rather than a full timestamp on every row.
    var shortRelative: String {
        let calendar = Calendar.current
        if calendar.isDateInToday(self) {
            return formatted(date: .omitted, time: .shortened)
        }
        if calendar.isDateInYesterday(self) {
            return "Yesterday"
        }
        if let days = calendar.dateComponents([.day], from: self, to: Date()).day, days < 7 {
            return formatted(.dateTime.weekday(.abbreviated))
        }
        return formatted(.dateTime.month(.defaultDigits).day())
    }
}
