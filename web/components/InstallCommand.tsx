'use client'

import { useState } from 'react'

import { track } from '@/lib/track'

const COMMAND = 'curl -fsSL https://relaybird.haxzie.com/install.sh | sh'

/**
 * The one-liner, with a copy button.
 *
 * This is the recommended route because it is the only one that gets the Android half onto
 * a phone without a fight: it installs over ADB, and Play Protect blocks SMS apps installed
 * any other way.
 */
export function InstallCommand() {
  const [copied, setCopied] = useState(false)

  const copy = () => {
    navigator.clipboard.writeText(COMMAND).then(() => {
      setCopied(true)
      track('install_command_copied')
      setTimeout(() => setCopied(false), 2000)
    })
  }

  return (
    <div>
      <div className="flex items-center gap-3 rounded-xl bg-raised px-4 py-3 text-left">
        <code className="min-w-0 flex-1 overflow-x-auto whitespace-nowrap font-mono text-[13px] text-white">
          <span className="select-none text-muted">$ </span>
          {COMMAND}
        </code>
        <button
          onClick={copy}
          className="shrink-0 rounded-md border border-edge px-2.5 py-1 text-xs text-muted transition hover:border-white/40 hover:text-white"
        >
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
    </div>
  )
}
