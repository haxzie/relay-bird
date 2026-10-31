import { defineCloudflareConfig } from '@opennextjs/cloudflare'

/**
 * No incremental cache, no queue, no tag cache — every route here is server-rendered, and
 * OpenNext handles that out of the box. The alternative was an R2 bucket and a Durable Object
 * queue to make ISR work; see the note in lib/releases.ts for why that is not worth it for a
 * download page that must never show a stale version.
 */
export default defineCloudflareConfig()
