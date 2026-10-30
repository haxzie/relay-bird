import AppKit
import CoreImage
import CoreImage.CIFilterBuiltins
import Foundation

/// A live pairing attempt: the one-time secret, the QR the phone scans, and the deadline.
struct PairingCode {
    let secret: Data
    let macName: String
    let url: String
    let expiresAt: Date

    var expired: Bool { Date() >= expiresAt }
    var secondsRemaining: Int { max(0, Int(expiresAt.timeIntervalSinceNow.rounded(.up))) }

    static let lifetime: TimeInterval = 120

    static func generate(macName: String = AppSettings.macName) -> PairingCode {
        let secret = Proofs.randomKey()
        var name = macName.trimmingCharacters(in: .whitespacesAndNewlines)
        if name.isEmpty { name = "Mac" }

        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        let encodedName = name.addingPercentEncoding(withAllowedCharacters: allowed) ?? "Mac"
        let encodedSecret = secret.base64URLEncodedString()

        return PairingCode(
            secret: secret,
            macName: name,
            url: "relaybird://pair?v=\(Protocol.version)&name=\(encodedName)&s=\(encodedSecret)",
            expiresAt: Date().addingTimeInterval(lifetime)
        )
    }

    /// Rendered at a fixed size with high error correction so it scans from a normal viewing
    /// distance — the QR carries a 32-byte secret, so scan success rate is the whole pairing UX.
    func qrImage(side: CGFloat = 220) -> NSImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(url.utf8)
        filter.correctionLevel = "H"
        guard let output = filter.outputImage else { return nil }
        let scale = side / output.extent.width
        let scaled = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let context = CIContext()
        guard let cgImage = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return NSImage(cgImage: cgImage, size: NSSize(width: side, height: side))
    }
}

extension Data {
    /// base64url without padding, matching what the Android parser expects.
    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
