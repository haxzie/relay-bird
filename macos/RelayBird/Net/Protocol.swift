import Foundation

/// Wire protocol v1. Mirrored by hand in
/// android/app/src/main/kotlin/dev/haxzie/relaybird/net/Protocol.kt — see docs/PROTOCOL.md,
/// which is the source of truth for both.
enum Protocol {
    static let version = 1
    static let serviceType = "_relaybird._tcp"
    static let maxFrameBytes = 1 << 20
}

struct WireMessage: Codable, Identifiable, Equatable, Hashable {
    let id: Int64
    let threadId: Int64
    let address: String
    let body: String
    /// Epoch milliseconds.
    let date: Int64
    let outgoing: Bool
    let read: Bool

    var timestamp: Date { Date(timeIntervalSince1970: Double(date) / 1000) }
}

struct WireThread: Codable, Identifiable, Equatable, Hashable {
    let threadId: Int64
    let address: String
    let displayName: String?
    let snippet: String
    let date: Int64
    let unread: Int

    var id: Int64 { threadId }
    var title: String { displayName ?? address }
    var timestamp: Date { Date(timeIntervalSince1970: Double(date) / 1000) }
}

/// Frames as they appear on the wire, discriminated by `t`.
enum Frame {
    // phone -> mac
    case hello(version: Int, deviceId: String, deviceName: String, paired: Bool, challenge: Data)
    case paired(confirm: Data, deviceKey: Data)
    case ready
    case threads([WireThread])
    case messages(threadId: Int64, messages: [WireMessage])
    case message(WireMessage)
    case error(code: String, message: String)

    // mac -> phone
    case pair(name: String, proof: Data)
    case auth(proof: Data)
    case sync(limit: Int)
    case thread(threadId: Int64, limit: Int, beforeId: Int64?)
    case markRead(threadId: Int64)

    // both
    case ping
    case pong
}

enum ErrorCode {
    static let unpaired = "unpaired"
    static let badProof = "bad_proof"
    static let protocolViolation = "protocol"
    static let noPermission = "no_permission"
    static let revoked = "revoked"
}

enum FrameCodingError: Error, LocalizedError {
    case unknownType(String)
    case missingField(String)
    case badBase64(String)
    case oversized(Int)

    var errorDescription: String? {
        switch self {
        case .unknownType(let t): return "unknown frame type \"\(t)\""
        case .missingField(let f): return "frame is missing \"\(f)\""
        case .badBase64(let f): return "\"\(f)\" is not valid base64"
        case .oversized(let n): return "frame of \(n) bytes exceeds the limit"
        }
    }
}

extension Frame {
    /// Hand-rolled rather than Codable-with-enum: the Kotlin side uses kotlinx.serialization's
    /// `classDiscriminator`, and spelling the mapping out keeps the two readable side by side.
    static func decode(from data: Data) throws -> Frame {
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = object["t"] as? String
        else {
            throw FrameCodingError.missingField("t")
        }

        func int(_ key: String) throws -> Int {
            guard let value = object[key] as? Int else { throw FrameCodingError.missingField(key) }
            return value
        }
        func int64(_ key: String) throws -> Int64 {
            guard let value = object[key] as? NSNumber else { throw FrameCodingError.missingField(key) }
            return value.int64Value
        }
        func string(_ key: String) throws -> String {
            guard let value = object[key] as? String else { throw FrameCodingError.missingField(key) }
            return value
        }
        func base64(_ key: String) throws -> Data {
            guard let decoded = Data(base64Encoded: try string(key)) else {
                throw FrameCodingError.badBase64(key)
            }
            return decoded
        }
        func decodeNested<T: Decodable>(_ key: String, as type: T.Type) throws -> T {
            guard let raw = object[key] else { throw FrameCodingError.missingField(key) }
            let json = try JSONSerialization.data(withJSONObject: raw)
            return try JSONDecoder().decode(T.self, from: json)
        }

        switch type {
        case "hello":
            return .hello(
                version: try int("v"),
                deviceId: try string("deviceId"),
                deviceName: try string("deviceName"),
                paired: object["paired"] as? Bool ?? false,
                challenge: try base64("challenge")
            )
        case "paired":
            return .paired(confirm: try base64("confirm"), deviceKey: try base64("deviceKey"))
        case "ready":
            return .ready
        case "threads":
            return .threads(try decodeNested("threads", as: [WireThread].self))
        case "messages":
            return .messages(
                threadId: try int64("threadId"),
                messages: try decodeNested("messages", as: [WireMessage].self)
            )
        case "message":
            return .message(try decodeNested("message", as: WireMessage.self))
        case "error":
            return .error(code: try string("code"), message: try string("msg"))
        case "ping":
            return .ping
        case "pong":
            return .pong

        // The Mac never receives these, but decoding them keeps the codec symmetric, which
        // makes the frame tests meaningful and the wire log readable in both directions.
        case "pair":
            return .pair(name: try string("name"), proof: try base64("proof"))
        case "auth":
            return .auth(proof: try base64("proof"))
        case "sync":
            return .sync(limit: try int("limit"))
        case "thread":
            return .thread(
                threadId: try int64("threadId"),
                limit: try int("limit"),
                beforeId: (object["beforeId"] as? NSNumber)?.int64Value
            )
        case "markRead":
            return .markRead(threadId: try int64("threadId"))

        default:
            throw FrameCodingError.unknownType(type)
        }
    }

    func encode() throws -> Data {
        var object: [String: Any]
        switch self {
        case .pair(let name, let proof):
            object = ["t": "pair", "name": name, "proof": proof.base64EncodedString()]
        case .auth(let proof):
            object = ["t": "auth", "proof": proof.base64EncodedString()]
        case .sync(let limit):
            object = ["t": "sync", "limit": limit]
        case .thread(let threadId, let limit, let beforeId):
            object = ["t": "thread", "threadId": threadId, "limit": limit]
            if let beforeId { object["beforeId"] = beforeId }
        case .markRead(let threadId):
            object = ["t": "markRead", "threadId": threadId]
        case .ping:
            object = ["t": "ping"]
        case .pong:
            object = ["t": "pong"]
        default:
            // Everything else is phone-to-mac; the Mac never sends it.
            throw FrameCodingError.unknownType("outbound \(self)")
        }
        return try JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
    }
}
