import XCTest
@testable import RelayBird

final class FramingTests: XCTestCase {
    func testRoundTripsAFrame() throws {
        let data = try FrameBuffer.encode(.sync(limit: 7))
        var buffer = FrameBuffer()
        buffer.append(data)
        let frames = try buffer.drain()
        XCTAssertEqual(frames.count, 1)
        guard case .sync(let limit) = frames[0] else { return XCTFail("wrong frame") }
        XCTAssertEqual(limit, 7)
    }

    func testLengthPrefixIsBigEndian() throws {
        let data = try FrameBuffer.encode(.ping)
        let declared = data.prefix(4).reduce(0) { ($0 << 8) | Int($1) }
        XCTAssertEqual(declared, data.count - 4)
    }

    func testDrainsSeveralFramesFromOneStream() throws {
        var stream = Data()
        stream.append(try FrameBuffer.encode(.ping))
        stream.append(try FrameBuffer.encode(.markRead(threadId: 42)))
        stream.append(try FrameBuffer.encode(.sync(limit: 9)))
        var buffer = FrameBuffer()
        buffer.append(stream)

        // Checking contents, not just the count: Data does not rebase its indices when sliced,
        // so a decoder that reads from absolute offsets happily returns the right *number* of
        // frames built from the wrong bytes.
        let frames = try buffer.drain()
        XCTAssertEqual(frames.count, 3)
        guard case .ping = frames[0] else { return XCTFail("frame 0 should be ping") }
        guard case .markRead(let threadId) = frames[1] else {
            return XCTFail("frame 1 should be markRead")
        }
        XCTAssertEqual(threadId, 42)
        guard case .sync(let limit) = frames[2] else { return XCTFail("frame 2 should be sync") }
        XCTAssertEqual(limit, 9)
    }

    func testDrainsAcrossSuccessiveReads() throws {
        // What the socket actually does: append, drain, append, drain, on one long-lived buffer.
        var buffer = FrameBuffer()
        for threadId in Int64(1) ... Int64(20) {
            buffer.append(try FrameBuffer.encode(.markRead(threadId: threadId)))
            let frames = try buffer.drain()
            XCTAssertEqual(frames.count, 1)
            guard case .markRead(let decoded) = frames[0] else {
                return XCTFail("expected markRead")
            }
            XCTAssertEqual(decoded, threadId, "frame \(threadId) decoded from the wrong offset")
        }
    }

    func testDrainsFramesArrivingByteByByte() throws {
        let stream = try FrameBuffer.encode(.sync(limit: 3)) + FrameBuffer.encode(.ping)
        var buffer = FrameBuffer()
        var frames: [Frame] = []
        for byte in stream {
            buffer.append(Data([byte]))
            frames.append(contentsOf: try buffer.drain())
        }
        XCTAssertEqual(frames.count, 2)
        guard case .sync(let limit) = frames[0] else { return XCTFail("expected sync") }
        XCTAssertEqual(limit, 3)
        guard case .ping = frames[1] else { return XCTFail("expected ping") }
    }

    func testWaitsForTheRestOfASplitFrame() throws {
        let data = try FrameBuffer.encode(.thread(threadId: 1, limit: 10, beforeId: 5))
        var buffer = FrameBuffer()
        buffer.append(data.prefix(data.count - 3))
        XCTAssertEqual(try buffer.drain().count, 0, "a partial frame must not decode")
        buffer.append(data.suffix(3))
        XCTAssertEqual(try buffer.drain().count, 1)
    }

    func testRejectsAnOversizedDeclaredLength() {
        var buffer = FrameBuffer()
        buffer.append(Data([0x7f, 0xff, 0xff, 0xff]))
        XCTAssertThrowsError(try buffer.drain())
    }

    func testRejectsAZeroLength() {
        var buffer = FrameBuffer()
        buffer.append(Data([0, 0, 0, 0]))
        XCTAssertThrowsError(try buffer.drain())
    }

    func testDecodesEveryPhoneToMacFrame() throws {
        let hello = """
        {"t":"hello","v":1,"deviceId":"abc","deviceName":"Pixel","paired":true,"challenge":"AAAA"}
        """
        guard case .hello(let version, let deviceId, let name, let paired, let challenge) =
            try Frame.decode(from: Data(hello.utf8))
        else {
            return XCTFail("hello did not decode")
        }
        XCTAssertEqual(version, 1)
        XCTAssertEqual(deviceId, "abc")
        XCTAssertEqual(name, "Pixel")
        XCTAssertTrue(paired)
        XCTAssertEqual(challenge, Data(base64Encoded: "AAAA"))

        let threads = """
        {"t":"threads","threads":[{"threadId":3,"address":"+1555","displayName":null,"snippet":"hi","date":1700000000000,"unread":2}]}
        """
        guard case .threads(let list) = try Frame.decode(from: Data(threads.utf8)) else {
            return XCTFail("threads did not decode")
        }
        XCTAssertEqual(list.first?.threadId, 3)
        XCTAssertEqual(list.first?.title, "+1555", "a nil display name falls back to the address")
        XCTAssertEqual(list.first?.unread, 2)

        let message = """
        {"t":"message","message":{"id":9,"threadId":3,"address":"+1555","body":"code 123456","date":1700000000000,"outgoing":false,"read":false}}
        """
        guard case .message(let wire) = try Frame.decode(from: Data(message.utf8)) else {
            return XCTFail("message did not decode")
        }
        XCTAssertEqual(wire.id, 9)
        XCTAssertFalse(wire.outgoing)

        guard case .error(let code, _) = try Frame.decode(
            from: Data(#"{"t":"error","code":"unpaired","msg":"nope"}"#.utf8)
        ) else {
            return XCTFail("error did not decode")
        }
        XCTAssertEqual(code, ErrorCode.unpaired)
    }

    func testRejectsAnUnknownFrameType() {
        XCTAssertThrowsError(try Frame.decode(from: Data(#"{"t":"wat"}"#.utf8)))
    }

    func testOmitsBeforeIdWhenNil() throws {
        let data = try Frame.thread(threadId: 1, limit: 2, beforeId: nil).encode()
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        XCTAssertNil(json["beforeId"])
    }
}
