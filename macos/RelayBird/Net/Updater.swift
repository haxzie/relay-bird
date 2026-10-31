import Combine
import Foundation
import Sparkle

/// Wraps Sparkle so the rest of the app sees a published flag and two verbs.
///
/// Sparkle's own window is suppressed: this app lives in a popover, and throwing a separate
/// alert in front of someone reading a message is the wrong interruption. The pill in the
/// status bar is the whole announcement, and nothing happens until it is clicked.
@MainActor
final class Updater: NSObject, ObservableObject, SPUUpdaterDelegate, SPUStandardUserDriverDelegate {
    @Published private(set) var availableVersion: String?
    /// Only what is actually observed. Sparkle owns the download window once the pill is
    /// clicked, so inventing progress states here would mean reporting numbers nothing feeds.
    @Published private(set) var lastError: String?

    private var controller: SPUStandardUpdaterController?

    var updateAvailable: Bool { availableVersion != nil }

    func start() {
        // startingUpdater: false — the first check is scheduled by Sparkle itself, and
        // starting it here would run one before the app has finished launching.
        let controller = SPUStandardUpdaterController(
            startingUpdater: false,
            updaterDelegate: self,
            userDriverDelegate: self
        )
        controller.startUpdater()
        self.controller = controller
    }

    /// Checks without putting anything on screen; the pill appears only if one is found.
    func checkInBackground() {
        controller?.updater.checkForUpdatesInBackground()
    }

    /// What the pill does: shows Sparkle's progress and install flow.
    func installAvailableUpdate() {
        controller?.updater.checkForUpdates()
    }

    // MARK: - SPUStandardUserDriverDelegate

    /// Keeps a found update silent until the pill is clicked.
    nonisolated var supportsGentleScheduledUpdateReminders: Bool { true }

    nonisolated func standardUserDriverShouldHandleShowingScheduledUpdate(
        _ update: SUAppcastItem,
        andInImmediateFocus immediateFocus: Bool
    ) -> Bool {
        // Sparkle shows it only when the user asked; otherwise this records the version and
        // the pill carries the news.
        immediateFocus
    }

    nonisolated func standardUserDriverWillHandleShowingUpdate(
        _ handleShowingUpdate: Bool,
        forUpdate update: SUAppcastItem,
        state: SPUUserUpdateState
    ) {
        let version = update.displayVersionString
        Task { @MainActor in self.availableVersion = version }
    }

    nonisolated func standardUserDriverWillFinishUpdateSession() {
        Task { @MainActor in self.availableVersion = nil }
    }

    // MARK: - SPUUpdaterDelegate

    nonisolated func updater(_ updater: SPUUpdater, didFindValidUpdate item: SUAppcastItem) {
        let version = item.displayVersionString
        Task { @MainActor in self.availableVersion = version }
    }

    nonisolated func updaterDidNotFindUpdate(_ updater: SPUUpdater) {
        Task { @MainActor in self.availableVersion = nil }
    }

    nonisolated func updater(_ updater: SPUUpdater, didAbortWithError error: Error) {
        // Cancelling is an abort too, and is not worth reporting as a failure.
        let cancelled = (error as NSError).code == Int(SUError.installationCanceledError.rawValue)
        let message = error.localizedDescription
        Task { @MainActor in self.lastError = cancelled ? nil : message }
    }
}
