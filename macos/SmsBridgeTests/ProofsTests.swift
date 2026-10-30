import CryptoKit
import XCTest
@testable import SmsBridge

/// The vectors in docs/PROTOCOL.md, asserted here and in
/// android/app/src/test/kotlin/dev/haxzie/smsbridge/ProofsTest.kt. This is the test that
/// catches silent interop drift between the two implementations.
final class ProofsTests: XCTestCase {
    private let secret = Data((0x00 ..< 0x20).map(UInt8.init))
    private let spkiHash = Data((0x20 ..< 0x40).map(UInt8.init))
    private let deviceKey = Data((0x40 ..< 0x60).map(UInt8.init))
    private let challenge = Data((0x60 ..< 0x80).map(UInt8.init))
    private let macName = "Test Mac"

    func testPairProofMatchesSharedVector() {
        XCTAssertEqual(
            Proofs.pair(secret: secret, spkiHash: spkiHash).hex,
            "5beecd9d109a6b7fed62a2704689789b3b6f52f4b78fe3bf362450c6c9bdcfc3"
        )
    }

    func testConfirmProofMatchesSharedVector() {
        XCTAssertEqual(
            Proofs.confirm(secret: secret, spkiHash: spkiHash, macName: macName).hex,
            "c8d7debfe9850c2568e41cf93580cfb2f134905ef30165686b7525f1c8feffa0"
        )
    }

    func testAuthProofMatchesSharedVector() {
        XCTAssertEqual(
            Proofs.auth(deviceKey: deviceKey, challenge: challenge, spkiHash: spkiHash).hex,
            "fcbe3f0a851c78503d84f4019bfc83a6cc6481bd09a8bbfdbfb5901831884157"
        )
    }

    func testConfirmVerificationAcceptsTheRealProof() {
        let confirm = Proofs.confirm(secret: secret, spkiHash: spkiHash, macName: macName)
        XCTAssertTrue(
            Proofs.verifyConfirm(
                secret: secret, spkiHash: spkiHash, macName: macName, candidate: confirm
            )
        )
    }

    func testConfirmVerificationIsBoundToTheCertificate() {
        // A man in the middle relays a confirmation computed over the real phone's certificate;
        // the Mac hashed the attacker's, so it must reject.
        let confirm = Proofs.confirm(secret: secret, spkiHash: spkiHash, macName: macName)
        var otherSpki = spkiHash
        otherSpki[0] = 0xff
        XCTAssertFalse(
            Proofs.verifyConfirm(
                secret: secret, spkiHash: otherSpki, macName: macName, candidate: confirm
            )
        )
    }

    func testConfirmVerificationIsBoundToTheMacName() {
        let confirm = Proofs.confirm(secret: secret, spkiHash: spkiHash, macName: macName)
        XCTAssertFalse(
            Proofs.verifyConfirm(
                secret: secret, spkiHash: spkiHash, macName: "Other Mac", candidate: confirm
            )
        )
    }

    func testContextsAreDomainSeparated() {
        XCTAssertNotEqual(
            Proofs.pair(secret: secret, spkiHash: spkiHash),
            Proofs.auth(deviceKey: secret, challenge: Data(), spkiHash: spkiHash)
        )
    }

    func testRandomKeysAre32BytesAndDistinct() {
        let a = Proofs.randomKey()
        let b = Proofs.randomKey()
        XCTAssertEqual(a.count, 32)
        XCTAssertNotEqual(a, b)
    }
}

extension Data {
    var hex: String { map { String(format: "%02x", $0) }.joined() }
}
