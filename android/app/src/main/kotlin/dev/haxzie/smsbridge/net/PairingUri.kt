package dev.haxzie.smsbridge.net

import java.net.URLDecoder

/**
 * `smsbridge://pair?v=1&name=<mac name>&s=<base64url secret>` — the payload of the Mac's QR.
 *
 * Parsed by hand rather than with android.net.Uri so it stays a plain unit test; the grammar is
 * fixed and tiny, and the Mac is the only thing that ever produces it.
 */
data class PairingUri(val macName: String, val secretBase64Url: String) {
    companion object {
        private const val PREFIX = "smsbridge://pair?"

        fun parse(raw: String): PairingUri? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith(PREFIX)) return null
            val params = trimmed.removePrefix(PREFIX)
                .split('&')
                .mapNotNull { pair ->
                    val separator = pair.indexOf('=')
                    if (separator <= 0) return@mapNotNull null
                    val key = pair.substring(0, separator)
                    val value = runCatching {
                        URLDecoder.decode(pair.substring(separator + 1), "UTF-8")
                    }.getOrNull() ?: return@mapNotNull null
                    key to value
                }
                .toMap()

            if (params["v"]?.toIntOrNull() != PROTOCOL_VERSION) return null
            val name = params["name"]?.takeIf { it.isNotBlank() } ?: return null
            val secret = params["s"]?.takeIf { it.isNotBlank() } ?: return null
            return PairingUri(name, secret)
        }
    }
}
