import XCTest
@testable import RelayBird

final class OneTimeCodeTests: XCTestCase {
    func testFindsACode() {
        XCTAssertEqual("Your code is 482913".oneTimeCode, "482913")
        XCTAssertEqual("123456 is your OTP".oneTimeCode, "123456")
        XCTAssertEqual("Use 4821 to log in".oneTimeCode, "4821")
    }

    func testIgnoresLongerDigitRuns() {
        // Order numbers and phone numbers are not codes.
        XCTAssertNil("Order 1234567890123 shipped".oneTimeCode)
        XCTAssertNil("Call +15551234567".oneTimeCode)
    }

    func testReturnsNilWhenThereIsNoCode() {
        XCTAssertNil("See you at the station".oneTimeCode)
        XCTAssertNil("".oneTimeCode)
    }
}
