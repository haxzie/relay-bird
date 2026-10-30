import Foundation

/// The one phone this Mac is paired with, and the two secrets that make reconnection silent:
/// the pinned certificate hash and the shared device key.
struct PairedPhone: Codable, Equatable {
    let deviceId: String
    var deviceName: String
    /// SHA-256 of the phone certificate's DER SubjectPublicKeyInfo.
    let spkiHash: Data
    let deviceKey: Data
    /// The name this Mac announced at pairing time; the phone shows it in its device list.
    let macName: String
    var pairedAt: Date
}
