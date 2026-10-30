import CryptoKit
import Foundation

/// The pairing and reconnect proofs from docs/PROTOCOL.md. HMAC-SHA256 throughout: there are
/// exactly two peers and no third party to verify a signature against, so asymmetric identity
/// keys would be pure overhead.
///
/// Every context string is versioned so a future protocol change can never produce a colliding
/// MAC for a different purpose.
enum Proofs {
    private static let pairContext = Data("smsbridge-pair-v1".utf8)
    private static let confirmContext = Data("smsbridge-confirm-v1".utf8)
    private static let authContext = Data("smsbridge-auth-v1".utf8)

    static let keyBytes = 32

    /// Proves to the phone that this Mac holds the QR secret *and* saw this exact certificate.
    static func pair(secret: Data, spkiHash: Data) -> Data {
        hmac(key: secret, message: pairContext + spkiHash)
    }

    static func confirm(secret: Data, spkiHash: Data, macName: String) -> Data {
        hmac(key: secret, message: confirmContext + spkiHash + Data(macName.utf8))
    }

    static func auth(deviceKey: Data, challenge: Data, spkiHash: Data) -> Data {
        hmac(key: deviceKey, message: authContext + challenge + spkiHash)
    }

    /// Constant time.
    static func verify(expectedKey: Data, message: Data, against candidate: Data) -> Bool {
        HMAC<SHA256>.isValidAuthenticationCode(
            candidate,
            authenticating: message,
            using: SymmetricKey(data: expectedKey)
        )
    }

    static func verifyConfirm(secret: Data, spkiHash: Data, macName: String, candidate: Data) -> Bool {
        verify(
            expectedKey: secret,
            message: confirmContext + spkiHash + Data(macName.utf8),
            against: candidate
        )
    }

    static func randomKey(bytes: Int = keyBytes) -> Data {
        var data = Data(count: bytes)
        let status = data.withUnsafeMutableBytes { pointer in
            SecRandomCopyBytes(kSecRandomDefault, bytes, pointer.baseAddress!)
        }
        precondition(status == errSecSuccess, "SecRandomCopyBytes failed: \(status)")
        return data
    }

    private static func hmac(key: Data, message: Data) -> Data {
        Data(HMAC<SHA256>.authenticationCode(for: message, using: SymmetricKey(data: key)))
    }
}
