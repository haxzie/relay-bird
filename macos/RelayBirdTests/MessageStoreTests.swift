import XCTest
@testable import RelayBird

final class MessageStoreTests: XCTestCase {
    private var url: URL!
    private var store: MessageStore!

    override func setUp() {
        super.setUp()
        url = FileManager.default.temporaryDirectory
            .appendingPathComponent("relaybird-test-\(UUID().uuidString).sqlite")
        store = MessageStore(url: url)
    }

    override func tearDown() {
        store = nil
        try? FileManager.default.removeItem(at: url)
        super.tearDown()
    }

    private func thread(_ id: Int64, unread: Int, latest: Int64, date: Int64 = 1_700_000_000_000) -> WireThread {
        WireThread(
            threadId: id, address: "+1555000\(id)", displayName: nil,
            snippet: "hello", date: date, unread: unread, latestMessageId: latest
        )
    }

    private func message(_ id: Int64, thread: Int64, outgoing: Bool = false, read: Bool = false) -> WireMessage {
        WireMessage(
            id: id, threadId: thread, address: "+1555000\(thread)", body: "body \(id)",
            date: 1_700_000_000_000 + id, outgoing: outgoing, read: read
        )
    }

    // MARK: - The reported bug

    func testReadThreadStaysReadWhenThePhoneKeepsReportingItUnread() {
        store.upsertThreads([thread(1, unread: 3, latest: 100)])
        store.upsertMessages([message(98, thread: 1), message(99, thread: 1), message(100, thread: 1)])
        XCTAssertEqual(store.threads().first?.unread, 3)

        store.markRead(threadId: 1, upTo: store.latestMessageId(threadId: 1))
        XCTAssertEqual(store.threads().first?.unread, 0)

        // The phone cannot record the read state, so every later sync says "still 3 unread".
        // That must not resurrect the badge.
        store.upsertThreads([thread(1, unread: 3, latest: 100)])
        XCTAssertEqual(store.threads().first?.unread, 0, "a resync must not undo a read mark")
        XCTAssertEqual(store.totalUnread(), 0)
    }

    func testANewerMessageMakesTheThreadUnreadAgain() {
        store.upsertThreads([thread(1, unread: 1, latest: 100)])
        store.upsertMessages([message(100, thread: 1)])
        store.markRead(threadId: 1, upTo: 100)
        XCTAssertEqual(store.threads().first?.unread, 0)

        store.upsertThreads([thread(1, unread: 2, latest: 101)])
        XCTAssertEqual(store.threads().first?.unread, 2, "a message past the read mark counts again")
    }

    func testReadMarkNeverMovesBackwards() {
        store.upsertThreads([thread(1, unread: 0, latest: 100)])
        store.markRead(threadId: 1, upTo: 100)
        store.markRead(threadId: 1, upTo: 50)
        XCTAssertEqual(store.watermarks(), [MessageStore.Watermark(threadId: 1, lastReadId: 100)])
    }

    func testUnreadSurvivesAnUnknownLatestMessageId() {
        // A peer that never sends latestMessageId leaves it at zero. The read mark must not
        // then compare 0 >= 0 and swallow every unread count.
        store.upsertThreads([thread(1, unread: 4, latest: 0)])
        XCTAssertEqual(store.threads().first?.unread, 4)
        XCTAssertEqual(store.totalUnread(), 4)
    }

    // MARK: - Mark all as read

    func testMarkAllReadClearsEveryThread() {
        store.upsertThreads([
            thread(1, unread: 3, latest: 30),
            thread(2, unread: 7, latest: 70),
            thread(3, unread: 0, latest: 90),
        ])
        XCTAssertEqual(store.totalUnread(), 10)

        store.markAllRead()
        XCTAssertEqual(store.totalUnread(), 0)
        XCTAssertTrue(store.threads().allSatisfy { $0.unread == 0 })
    }

    func testMarkAllReadSticksAcrossAResync() {
        store.upsertThreads([thread(1, unread: 3, latest: 30)])
        store.markAllRead()
        store.upsertThreads([thread(1, unread: 3, latest: 30)])
        XCTAssertEqual(store.totalUnread(), 0)
    }

    func testMarkAllReadProducesWatermarksForThePhone() {
        store.upsertThreads([thread(1, unread: 3, latest: 30), thread(2, unread: 1, latest: 70)])
        store.markAllRead()
        XCTAssertEqual(
            store.watermarks().sorted { $0.threadId < $1.threadId },
            [
                MessageStore.Watermark(threadId: 1, lastReadId: 30),
                MessageStore.Watermark(threadId: 2, lastReadId: 70),
            ]
        )
    }

    func testMarkAllReadLeavesThreadsWithAnUnknownLatestAlone() {
        // Marking read against a zero would wrongly silence the thread forever.
        store.upsertThreads([thread(1, unread: 5, latest: 0)])
        store.markAllRead()
        XCTAssertEqual(store.totalUnread(), 5)
        XCTAssertTrue(store.watermarks().isEmpty)
    }

    func testMarkAllReadDoesNotStopLaterMessages() {
        store.upsertThreads([thread(1, unread: 2, latest: 20)])
        store.markAllRead()
        store.applyIncoming(message(21, thread: 1), displayName: nil)
        XCTAssertEqual(store.threads().first?.unread, 1, "a new message still arrives unread")
    }

    // MARK: - Watermarks

    func testOnlyReadThreadsProduceWatermarks() {
        store.upsertThreads([thread(1, unread: 1, latest: 10), thread(2, unread: 1, latest: 20)])
        store.markRead(threadId: 2, upTo: 20)
        XCTAssertEqual(store.watermarks(), [MessageStore.Watermark(threadId: 2, lastReadId: 20)])
    }

    func testLatestMessageIdPrefersWhicheverSourceIsAhead() {
        store.upsertThreads([thread(1, unread: 0, latest: 100)])
        XCTAssertEqual(store.latestMessageId(threadId: 1), 100, "falls back to the thread summary")
        store.upsertMessages([message(140, thread: 1)])
        XCTAssertEqual(store.latestMessageId(threadId: 1), 140, "a known message can be newer")
    }

    // MARK: - Storage

    func testSurvivesReopening() {
        store.upsertThreads([thread(7, unread: 2, latest: 70)])
        store.upsertMessages([message(70, thread: 7)])
        store.markRead(threadId: 7, upTo: 70)
        store.phoneName = "Pixel"

        let reopened = MessageStore(url: url)
        XCTAssertEqual(reopened.threads().first?.threadId, 7)
        XCTAssertEqual(reopened.threads().first?.unread, 0, "the read mark is durable")
        XCTAssertEqual(reopened.messages(threadId: 7).count, 1)
        XCTAssertEqual(reopened.phoneName, "Pixel")
    }

    func testMessagesComeBackOldestFirst() {
        store.upsertMessages([message(3, thread: 1), message(1, thread: 1), message(2, thread: 1)])
        XCTAssertEqual(store.messages(threadId: 1).map(\.id), [1, 2, 3])
    }

    func testUpsertingAMessageTwiceDoesNotDuplicateIt() {
        store.upsertMessages([message(1, thread: 1)])
        store.upsertMessages([message(1, thread: 1, read: true)])
        let stored = store.messages(threadId: 1)
        XCTAssertEqual(stored.count, 1)
        XCTAssertTrue(stored[0].read, "the newer copy wins")
    }

    func testThreadsAreNewestFirst() {
        store.upsertThreads([
            thread(1, unread: 0, latest: 1, date: 1_000),
            thread(2, unread: 0, latest: 2, date: 3_000),
            thread(3, unread: 0, latest: 3, date: 2_000),
        ])
        XCTAssertEqual(store.threads().map(\.threadId), [2, 3, 1])
    }

    // MARK: - Live messages

    func testAnIncomingMessageCreatesAndCountsAgainstItsThread() {
        store.applyIncoming(message(5, thread: 9), displayName: "Alice")
        let thread = try? XCTUnwrap(store.threads().first)
        XCTAssertEqual(thread?.threadId, 9)
        XCTAssertEqual(thread?.unread, 1)
        XCTAssertEqual(thread?.displayName, "Alice")
        XCTAssertEqual(thread?.snippet, "body 5")
    }

    func testAnOutgoingMessageDoesNotCountAsUnread() {
        store.applyIncoming(message(5, thread: 9, outgoing: true), displayName: nil)
        XCTAssertEqual(store.threads().first?.unread, 0)
        XCTAssertEqual(store.totalUnread(), 0)
    }

    func testAnIncomingMessageReopensAReadThread() {
        store.upsertThreads([thread(9, unread: 0, latest: 4)])
        store.markRead(threadId: 9, upTo: 4)
        XCTAssertEqual(store.threads().first?.unread, 0)

        store.applyIncoming(message(5, thread: 9), displayName: nil)
        XCTAssertEqual(store.threads().first?.unread, 1)
    }

    func testClearRemovesEverything() {
        store.upsertThreads([thread(1, unread: 1, latest: 1)])
        store.upsertMessages([message(1, thread: 1)])
        store.phoneName = "Pixel"
        store.clear()
        XCTAssertTrue(store.threads().isEmpty)
        XCTAssertTrue(store.messages(threadId: 1).isEmpty)
        XCTAssertNil(store.phoneName)
    }

    func testHandlesAThousandMessagesQuickly() {
        let many = (1 ... 1000).map { message(Int64($0), thread: 1) }
        let started = Date()
        store.upsertMessages(many)
        XCTAssertLessThan(Date().timeIntervalSince(started), 2.0)
        XCTAssertEqual(store.messages(threadId: 1, limit: 2000).count, 1000)
    }
}
