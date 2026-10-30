import Foundation

/// UInt32 big-endian length prefix, then that many bytes of UTF-8 JSON.
struct FrameBuffer {
    private var buffer = Data()

    mutating func append(_ data: Data) {
        buffer.append(data)
    }

    /// Pulls off every whole frame currently buffered. Throws on a protocol violation, which
    /// the caller should treat as fatal for the connection.
    /// Pulls off every whole frame currently buffered. Throws on a protocol violation, which
    /// the caller should treat as fatal for the connection.
    mutating func drain() throws -> [Frame] {
        var frames: [Frame] = []
        while buffer.count >= 4 {
            // Every index here is relative to startIndex: slicing a Data does not rebase its
            // indices, so after the first frame is consumed the buffer no longer starts at 0.
            let base = buffer.startIndex
            let length = buffer[base ..< base + 4].reduce(0) { ($0 << 8) | Int($1) }
            guard length > 0, length <= Protocol.maxFrameBytes else {
                throw FrameCodingError.oversized(length)
            }
            guard buffer.count >= 4 + length else { break }
            let bodyStart = base + 4
            let body = buffer[bodyStart ..< bodyStart + length]
            buffer = buffer[(bodyStart + length)...]
            frames.append(try Frame.decode(from: Data(body)))
        }
        // Rebase so the indices, and the retained prefix, do not grow without bound.
        buffer = Data(buffer)
        return frames
    }

    static func encode(_ frame: Frame) throws -> Data {
        let body = try frame.encode()
        guard body.count <= Protocol.maxFrameBytes else {
            throw FrameCodingError.oversized(body.count)
        }
        var out = Data(capacity: body.count + 4)
        let length = UInt32(body.count)
        out.append(UInt8((length >> 24) & 0xff))
        out.append(UInt8((length >> 16) & 0xff))
        out.append(UInt8((length >> 8) & 0xff))
        out.append(UInt8(length & 0xff))
        out.append(body)
        return out
    }
}
