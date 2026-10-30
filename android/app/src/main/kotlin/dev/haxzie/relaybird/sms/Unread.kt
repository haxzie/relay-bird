package dev.haxzie.relaybird.sms

/**
 * Unread counting, kept free of Android types so it can be tested directly.
 *
 * The phone's own read flags are only half the story: the Mac cannot write them back (a
 * non-default SMS app has no write access to the SMS provider), so it reports how far it has
 * read in each thread and those messages are discounted here.
 */
object Unread {
    fun countsByThread(
        unreadIdsByThread: Map<Long, List<Long>>,
        watermarks: Map<Long, Long>,
    ): Map<Long, Int> = unreadIdsByThread
        .mapValues { (threadId, ids) ->
            val readUpTo = watermarks[threadId] ?: 0L
            ids.count { it > readUpTo }
        }
        .filterValues { it > 0 }
}
