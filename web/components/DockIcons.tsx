/**
 * Dock app icons.
 *
 * Recreations of real applications, so the dock reads as somebody's actual Mac rather than a
 * row of coloured squares. Each one draws its own background, because that is what an app
 * icon is — the tile is just the clip.
 *
 * These are other companies' marks and remain their trademarks; they appear here only as the
 * contents of a device mockup, which is the usual way a Mac dock gets illustrated.
 */

type DockApp = { name: string; icon: React.ReactNode }

const FINDER = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <defs>
      <linearGradient id="finder-r" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#2FA8F4" />
        <stop offset="1" stopColor="#0A72D6" />
      </linearGradient>
    </defs>
    <rect width="48" height="48" fill="url(#finder-r)" />
    <path d="M0 0h24v48H0z" fill="#CFE8FA" />
    {/* The two eyes and the smile that make it a face. */}
    <rect x="13.5" y="15" width="2.6" height="7" rx="1.3" fill="#2A2A2E" />
    <rect x="31.9" y="15" width="2.6" height="7" rx="1.3" fill="#F2F7FB" />
    <path d="M12 30c3.6 3.2 8 4.8 12 4.8S32.4 33.2 36 30" stroke="#2A2A2E" strokeWidth="2.4" fill="none" strokeLinecap="round" />
    <path d="M24 30c3.4 3 7.4 4.6 12 4.8" stroke="#F2F7FB" strokeWidth="2.4" fill="none" strokeLinecap="round" />
  </svg>
)

const SAFARI = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <defs>
      <linearGradient id="safari-b" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#3FC4FF" />
        <stop offset="1" stopColor="#0A7CE8" />
      </linearGradient>
    </defs>
    <rect width="48" height="48" fill="url(#safari-b)" />
    <circle cx="24" cy="24" r="17.5" fill="#F4F7FA" />
    <circle cx="24" cy="24" r="17.5" fill="none" stroke="#D5DEE6" strokeWidth="1" />
    {/* The compass needle: red half pointing north-east, pale half opposite. */}
    <path d="M32.5 15.5 26 26 15.5 32.5 22 22Z" fill="#F4393C" />
    <path d="M15.5 32.5 22 22l4 4Z" fill="#C9D2DA" />
  </svg>
)

const SLACK = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <rect width="48" height="48" fill="#fff" />
    <g transform="translate(10 10)">
      <path d="M6.1 17.4a3.05 3.05 0 1 1-3.05-3.05H6.1v3.05ZM7.65 17.4a3.05 3.05 0 0 1 6.1 0v7.65a3.05 3.05 0 1 1-6.1 0V17.4Z" fill="#E01E5A" />
      <path d="M10.7 6.1a3.05 3.05 0 1 1 3.05-3.05V6.1H10.7ZM10.7 7.65a3.05 3.05 0 0 1 0 6.1H3.05a3.05 3.05 0 1 1 0-6.1h7.65Z" fill="#36C5F0" />
      <path d="M21.95 10.7a3.05 3.05 0 1 1 3.05 3.05h-3.05V10.7ZM20.4 10.7a3.05 3.05 0 0 1-6.1 0V3.05a3.05 3.05 0 1 1 6.1 0v7.65Z" fill="#2EB67D" />
      <path d="M17.35 21.95a3.05 3.05 0 1 1-3.05 3.05v-3.05h3.05ZM17.35 20.4a3.05 3.05 0 0 1 0-6.1H25a3.05 3.05 0 1 1 0 6.1h-7.65Z" fill="#ECB22E" />
    </g>
  </svg>
)

const MUSIC = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <defs>
      <linearGradient id="music-g" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#FB5C74" />
        <stop offset="1" stopColor="#FA233B" />
      </linearGradient>
    </defs>
    <rect width="48" height="48" fill="url(#music-g)" />
    {/* Beamed quavers, as on the Music icon. */}
    <path
      d="M33.4 10.2 20.6 13a1.6 1.6 0 0 0-1.3 1.6v14.9a4.6 4.6 0 1 0 3.2 4.4V19.6l10.2-2.2v8.3a4.6 4.6 0 1 0 3.2 4.4V11.8a1.6 1.6 0 0 0-2-1.6Z"
      fill="#fff"
    />
  </svg>
)

const APPLE_TV = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <rect width="48" height="48" fill="#111113" />
    <text
      x="24"
      y="31.5"
      textAnchor="middle"
      fill="#fff"
      fontFamily="system-ui, -apple-system, Helvetica, Arial, sans-serif"
      fontSize="20"
      fontWeight="600"
      letterSpacing="-1"
    >
      tv
    </text>
  </svg>
)

const MAPS = (
  <svg viewBox="0 0 48 48" aria-hidden>
    <rect width="48" height="48" fill="#F3EFE4" />
    {/* Parkland, water and a motorway running across, then the pin. */}
    <path d="M0 0h18l-6 16H0z" fill="#CDE8B5" />
    <path d="M30 0h18v14l-14 5Z" fill="#CDE8B5" />
    <path d="M0 36h48v12H0z" fill="#AFD8F2" />
    <path d="M-2 30c10-2 14-10 24-10s16 8 28 7" stroke="#fff" strokeWidth="5" fill="none" />
    <path d="M-2 30c10-2 14-10 24-10s16 8 28 7" stroke="#F6C64B" strokeWidth="2.6" fill="none" />
    <path d="M24 13.5a6.2 6.2 0 0 1 6.2 6.2c0 4.6-6.2 11.3-6.2 11.3s-6.2-6.7-6.2-11.3A6.2 6.2 0 0 1 24 13.5Z" fill="#E8453C" />
    <circle cx="24" cy="19.7" r="2.3" fill="#fff" />
  </svg>
)

export const DOCK_APPS: DockApp[] = [
  { name: 'Finder', icon: FINDER },
  { name: 'Safari', icon: SAFARI },
  { name: 'Slack', icon: SLACK },
  { name: 'Music', icon: MUSIC },
  { name: 'TV', icon: APPLE_TV },
  { name: 'Maps', icon: MAPS },
]
