import Foundation
import SQLite3

/// SQLite is passed a pointer to memory it must copy, not borrow.
private let sqliteTransient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

/// The Mac's own copy of the phone's messages, and the only place read state lives.
///
/// Read marks cannot be delegated to the phone: a non-default SMS app has no write access to
/// the SMS provider, so `markThreadRead` there is dropped silently and every resync would
/// report the same conversations as unread again. Here a thread records how far the Mac has
/// read, that survives restarts, and it is sent back with each sync so the phone discounts it.
final class MessageStore {
    struct Watermark: Equatable {
        let threadId: Int64
        let lastReadId: Int64
    }

    private var db: OpaquePointer?
    /// Every statement runs here, so the store is safe to touch from any thread.
    private let queue = DispatchQueue(label: "dev.haxzie.relaybird.store")

    static let `default` = MessageStore(url: MessageStore.defaultURL)

    static var defaultURL: URL? {
        guard let base = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        ).first else { return nil }
        let directory = base.appendingPathComponent("RelayBird", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        // The old JSON cache is superseded; leaving it would just be litter.
        try? FileManager.default.removeItem(at: directory.appendingPathComponent("cache.json"))
        return directory.appendingPathComponent("relaybird.sqlite")
    }

    init(url: URL?) {
        queue.sync {
            let path = url?.path ?? ":memory:"
            guard sqlite3_open_v2(
                path, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX, nil
            ) == SQLITE_OK else {
                NSLog("RelayBird: cannot open %@: %@", path, lastErrorMessage)
                db = nil
                return
            }
            // WAL survives an abrupt quit without losing the last writes, and NORMAL is the
            // matching durability setting for it.
            exec("PRAGMA journal_mode = WAL")
            exec("PRAGMA synchronous = NORMAL")
            exec("PRAGMA busy_timeout = 3000")
            migrate()
        }
    }

    deinit {
        if let db { sqlite3_close_v2(db) }
    }

    // MARK: - Schema

    private func migrate() {
        var version: Int32 = 0
        if let statement = prepare("PRAGMA user_version") {
            if sqlite3_step(statement) == SQLITE_ROW { version = sqlite3_column_int(statement, 0) }
            sqlite3_finalize(statement)
        }
        guard version < 1 else { return }

        exec("""
            CREATE TABLE IF NOT EXISTS threads (
                thread_id            INTEGER PRIMARY KEY,
                address              TEXT    NOT NULL DEFAULT '',
                display_name         TEXT,
                snippet              TEXT    NOT NULL DEFAULT '',
                date                 INTEGER NOT NULL DEFAULT 0,
                latest_message_id    INTEGER NOT NULL DEFAULT 0,
                phone_unread         INTEGER NOT NULL DEFAULT 0,
                last_read_message_id INTEGER NOT NULL DEFAULT 0
            )
        """)
        exec("""
            CREATE TABLE IF NOT EXISTS messages (
                id         INTEGER PRIMARY KEY,
                thread_id  INTEGER NOT NULL,
                address    TEXT    NOT NULL DEFAULT '',
                body       TEXT    NOT NULL DEFAULT '',
                date       INTEGER NOT NULL DEFAULT 0,
                outgoing   INTEGER NOT NULL DEFAULT 0,
                phone_read INTEGER NOT NULL DEFAULT 0
            )
        """)
        exec("CREATE INDEX IF NOT EXISTS messages_thread_date ON messages (thread_id, date)")
        exec("CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        exec("PRAGMA user_version = 1")
    }

    // MARK: - Writes

    /// Upserts what a sync reported. A thread's read mark is deliberately left alone — that is
    /// the Mac's state, and the phone has no say in it.
    func upsertThreads(_ threads: [WireThread]) {
        queue.sync {
            transaction {
                let sql = """
                    INSERT INTO threads
                        (thread_id, address, display_name, snippet, date, latest_message_id, phone_unread)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(thread_id) DO UPDATE SET
                        address           = excluded.address,
                        display_name      = excluded.display_name,
                        snippet           = excluded.snippet,
                        date              = excluded.date,
                        latest_message_id = MAX(threads.latest_message_id, excluded.latest_message_id),
                        phone_unread      = excluded.phone_unread
                """
                guard let statement = prepare(sql) else { return }
                defer { sqlite3_finalize(statement) }
                for thread in threads {
                    sqlite3_reset(statement)
                    bind(statement, 1, thread.threadId)
                    bind(statement, 2, thread.address)
                    bind(statement, 3, thread.displayName)
                    bind(statement, 4, thread.snippet)
                    bind(statement, 5, thread.date)
                    bind(statement, 6, thread.latestMessageId)
                    bind(statement, 7, Int64(thread.unread))
                    step(statement)
                }
            }
        }
    }

    func upsertMessages(_ messages: [WireMessage]) {
        queue.sync {
            transaction {
                let sql = """
                    INSERT INTO messages (id, thread_id, address, body, date, outgoing, phone_read)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        body       = excluded.body,
                        date       = excluded.date,
                        phone_read = excluded.phone_read
                """
                guard let statement = prepare(sql) else { return }
                defer { sqlite3_finalize(statement) }
                for message in messages {
                    sqlite3_reset(statement)
                    bind(statement, 1, message.id)
                    bind(statement, 2, message.threadId)
                    bind(statement, 3, message.address)
                    bind(statement, 4, message.body)
                    bind(statement, 5, message.date)
                    bind(statement, 6, message.outgoing ? 1 : 0)
                    bind(statement, 7, message.read ? 1 : 0)
                    step(statement)
                }
            }
        }
    }

    /// A message pushed between syncs: keep the thread summary current, and count it as unread
    /// until the next sync replaces the estimate with the phone's own number.
    func applyIncoming(_ message: WireMessage, displayName: String?) {
        upsertMessages([message])
        queue.sync {
            let sql = """
                INSERT INTO threads
                    (thread_id, address, display_name, snippet, date, latest_message_id, phone_unread)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(thread_id) DO UPDATE SET
                    address           = excluded.address,
                    snippet           = excluded.snippet,
                    date              = excluded.date,
                    latest_message_id = MAX(threads.latest_message_id, excluded.latest_message_id),
                    phone_unread      = threads.phone_unread + excluded.phone_unread
            """
            guard let statement = prepare(sql) else { return }
            defer { sqlite3_finalize(statement) }
            let counts = (!message.outgoing && !message.read) ? 1 : 0
            bind(statement, 1, message.threadId)
            bind(statement, 2, message.address)
            bind(statement, 3, displayName)
            bind(statement, 4, String(message.body.prefix(140)))
            bind(statement, 5, message.date)
            bind(statement, 6, message.id)
            bind(statement, 7, Int64(counts))
            step(statement)
        }
    }

    /// Records that everything up to `messageId` has been seen. Never moves backwards.
    func markRead(threadId: Int64, upTo messageId: Int64) {
        queue.sync {
            guard let statement = prepare("""
                UPDATE threads
                   SET last_read_message_id = MAX(last_read_message_id, ?)
                 WHERE thread_id = ?
            """) else { return }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, messageId)
            bind(statement, 2, threadId)
            step(statement)
        }
    }

    func clear() {
        queue.sync {
            exec("DELETE FROM messages")
            exec("DELETE FROM threads")
            exec("DELETE FROM meta")
        }
    }

    // MARK: - Reads

    /// `unread` is the display value: zero once the read mark has caught up with the newest
    /// message, otherwise whatever the phone last reported.
    ///
    /// The `latest_message_id > 0` guard matters — without it a thread whose newest id is
    /// unknown (zero) would compare `0 >= 0` and silently report nothing as unread.
    func threads(limit: Int = 200) -> [WireThread] {
        queue.sync {
            let sql = """
                SELECT thread_id, address, display_name, snippet, date, latest_message_id,
                       CASE WHEN latest_message_id > 0 AND last_read_message_id >= latest_message_id
                            THEN 0 ELSE phone_unread END
                  FROM threads
                 ORDER BY date DESC
                 LIMIT ?
            """
            guard let statement = prepare(sql) else { return [] }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, Int64(limit))
            var out: [WireThread] = []
            while sqlite3_step(statement) == SQLITE_ROW {
                out.append(
                    WireThread(
                        threadId: sqlite3_column_int64(statement, 0),
                        address: text(statement, 1) ?? "",
                        displayName: text(statement, 2),
                        snippet: text(statement, 3) ?? "",
                        date: sqlite3_column_int64(statement, 4),
                        unread: Int(sqlite3_column_int64(statement, 6)),
                        latestMessageId: sqlite3_column_int64(statement, 5)
                    )
                )
            }
            return out
        }
    }

    func messages(threadId: Int64, limit: Int = 200) -> [WireMessage] {
        queue.sync {
            // Newest `limit` rows, handed back oldest-first so the thread view reads downwards.
            let sql = """
                SELECT id, thread_id, address, body, date, outgoing, phone_read FROM (
                    SELECT * FROM messages WHERE thread_id = ? ORDER BY date DESC, id DESC LIMIT ?
                ) ORDER BY date ASC, id ASC
            """
            guard let statement = prepare(sql) else { return [] }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, threadId)
            bind(statement, 2, Int64(limit))
            var out: [WireMessage] = []
            while sqlite3_step(statement) == SQLITE_ROW {
                out.append(
                    WireMessage(
                        id: sqlite3_column_int64(statement, 0),
                        threadId: sqlite3_column_int64(statement, 1),
                        address: text(statement, 2) ?? "",
                        body: text(statement, 3) ?? "",
                        date: sqlite3_column_int64(statement, 4),
                        outgoing: sqlite3_column_int64(statement, 5) == 1,
                        read: sqlite3_column_int64(statement, 6) == 1
                    )
                )
            }
            return out
        }
    }

    func totalUnread() -> Int {
        queue.sync {
            let sql = """
                SELECT COALESCE(SUM(CASE WHEN latest_message_id > 0
                                          AND last_read_message_id >= latest_message_id
                                         THEN 0 ELSE phone_unread END), 0)
                  FROM threads
            """
            guard let statement = prepare(sql) else { return 0 }
            defer { sqlite3_finalize(statement) }
            return sqlite3_step(statement) == SQLITE_ROW ? Int(sqlite3_column_int64(statement, 0)) : 0
        }
    }

    /// Sent with every sync so the phone can discount what this Mac has already read.
    func watermarks() -> [Watermark] {
        queue.sync {
            guard let statement = prepare("""
                SELECT thread_id, last_read_message_id FROM threads WHERE last_read_message_id > 0
            """) else { return [] }
            defer { sqlite3_finalize(statement) }
            var out: [Watermark] = []
            while sqlite3_step(statement) == SQLITE_ROW {
                out.append(
                    Watermark(
                        threadId: sqlite3_column_int64(statement, 0),
                        lastReadId: sqlite3_column_int64(statement, 1)
                    )
                )
            }
            return out
        }
    }

    /// Newest message id held for a thread, which is what a read mark is set to.
    func latestMessageId(threadId: Int64) -> Int64 {
        queue.sync {
            guard let statement = prepare("""
                SELECT MAX(COALESCE((SELECT MAX(id) FROM messages WHERE thread_id = ?1), 0),
                           COALESCE((SELECT latest_message_id FROM threads WHERE thread_id = ?1), 0))
            """) else { return 0 }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, threadId)
            return sqlite3_step(statement) == SQLITE_ROW ? sqlite3_column_int64(statement, 0) : 0
        }
    }

    // MARK: - Meta

    func string(forKey key: String) -> String? {
        queue.sync {
            guard let statement = prepare("SELECT value FROM meta WHERE key = ?") else { return nil }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, key)
            return sqlite3_step(statement) == SQLITE_ROW ? text(statement, 0) : nil
        }
    }

    func set(_ value: String?, forKey key: String) {
        queue.sync {
            guard let value else {
                if let statement = prepare("DELETE FROM meta WHERE key = ?") {
                    bind(statement, 1, key); step(statement); sqlite3_finalize(statement)
                }
                return
            }
            guard let statement = prepare("""
                INSERT INTO meta (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """) else { return }
            defer { sqlite3_finalize(statement) }
            bind(statement, 1, key)
            bind(statement, 2, value)
            step(statement)
        }
    }

    var syncedAt: Date? {
        get { string(forKey: "syncedAt").flatMap { Double($0) }.map(Date.init(timeIntervalSince1970:)) }
        set { set(newValue.map { String($0.timeIntervalSince1970) }, forKey: "syncedAt") }
    }

    var phoneName: String? {
        get { string(forKey: "phoneName") }
        set { set(newValue, forKey: "phoneName") }
    }

    // MARK: - SQLite plumbing

    private var lastErrorMessage: String {
        db.flatMap { String(cString: sqlite3_errmsg($0)) } ?? "no database"
    }

    private func exec(_ sql: String) {
        guard let db else { return }
        if sqlite3_exec(db, sql, nil, nil, nil) != SQLITE_OK {
            NSLog("RelayBird: %@ failed: %@", sql, lastErrorMessage)
        }
    }

    private func prepare(_ sql: String) -> OpaquePointer? {
        guard let db else { return nil }
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else {
            NSLog("RelayBird: prepare failed: %@", lastErrorMessage)
            return nil
        }
        return statement
    }

    private func step(_ statement: OpaquePointer) {
        if sqlite3_step(statement) != SQLITE_DONE {
            NSLog("RelayBird: step failed: %@", lastErrorMessage)
        }
    }

    private func transaction(_ body: () -> Void) {
        exec("BEGIN IMMEDIATE")
        body()
        exec("COMMIT")
    }

    private func bind(_ statement: OpaquePointer, _ index: Int32, _ value: Int64) {
        sqlite3_bind_int64(statement, index, value)
    }

    private func bind(_ statement: OpaquePointer, _ index: Int32, _ value: String?) {
        if let value {
            sqlite3_bind_text(statement, index, value, -1, sqliteTransient)
        } else {
            sqlite3_bind_null(statement, index)
        }
    }

    private func text(_ statement: OpaquePointer, _ index: Int32) -> String? {
        guard let raw = sqlite3_column_text(statement, index) else { return nil }
        return String(cString: raw)
    }
}
