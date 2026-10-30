import XCTest
@testable import SmsBridge

final class PairingCodeTests: XCTestCase {
    func testUrlIsParseableByThePhone() throws {
        let code = PairingCode.generate(macName: "Haxzie's MacBook Pro")
        XCTAssertTrue(code.url.hasPrefix("smsbridge://pair?v=1&name="))

        let components = try XCTUnwrap(URLComponents(string: code.url))
        let items = try XCTUnwrap(components.queryItems)
        XCTAssertEqual(items.first { $0.name == "name" }?.value, "Haxzie's MacBook Pro")

        // base64url, unpadded: the Android side decodes with URL_SAFE | NO_PADDING.
        let secret = try XCTUnwrap(items.first { $0.name == "s" }?.value)
        XCTAssertFalse(secret.contains("+"))
        XCTAssertFalse(secret.contains("/"))
        XCTAssertFalse(secret.contains("="))
        XCTAssertEqual(secret, code.secret.base64URLEncodedString())
    }

    func testSecretIs32RandomBytes() {
        let a = PairingCode.generate()
        let b = PairingCode.generate()
        XCTAssertEqual(a.secret.count, 32)
        XCTAssertNotEqual(a.secret, b.secret)
    }

    func testExpiryIsTwoMinutes() {
        let code = PairingCode.generate()
        XCTAssertFalse(code.expired)
        XCTAssertEqual(Double(code.secondsRemaining), PairingCode.lifetime, accuracy: 2)
    }

    func testBlankMacNameFallsBack() {
        XCTAssertEqual(PairingCode.generate(macName: "   ").macName, "Mac")
    }

    func testRendersAQrImage() throws {
        let image = try XCTUnwrap(PairingCode.generate().qrImage(side: 220))
        XCTAssertEqual(image.size.width, 220, accuracy: 1)
    }
}
