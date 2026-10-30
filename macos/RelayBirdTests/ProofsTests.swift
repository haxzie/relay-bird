import CryptoKit
import XCTest
@testable import RelayBird

/// The vectors in docs/PROTOCOL.md, asserted here and in
/// android/app/src/test/kotlin/dev/haxzie/relaybird/ProofsTest.kt. This is the test that
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
            "ae5e1c9c5e34a70cdc313eba20ce9c2b4272ed81a8e381b0c187b85bb25f2255"
        )
    }

    func testConfirmProofMatchesSharedVector() {
        XCTAssertEqual(
            Proofs.confirm(secret: secret, spkiHash: spkiHash, macName: macName).hex,
            "688b0e4cbbd809ad9cc27d1b76c628622e3f825130c4ce3d6b9fce3f383f6de2"
        )
    }

    func testAuthProofMatchesSharedVector() {
        XCTAssertEqual(
            Proofs.auth(deviceKey: deviceKey, challenge: challenge, spkiHash: spkiHash).hex,
            "490195dbe69816d467535d13a730bd765eee83aeea6caaff2a4a7b1c4c708f51"
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
