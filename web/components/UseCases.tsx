/**
 * The moments that make you pick the phone up.
 *
 * Each one is a real shape of message rather than a feature claim — the code is pulled out in
 * monospace because that fragment is the entire reason you unlocked the phone, and seeing it
 * highlighted is the fastest way to recognise the problem.
 *
 * The glyphs live here rather than in Glyphs.tsx: nothing else uses them, and the shared file
 * is long enough without six icons that only ever appear in this grid.
 */

const stroke = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.4,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

function Card() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <rect x="1.4" y="3.4" width="13.2" height="9.2" rx="1.8" {...stroke} />
      <path d="M1.4 6.7h13.2" {...stroke} />
      <path d="M4 10h2.6" {...stroke} />
    </svg>
  )
}

function Key() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <circle cx="6" cy="6" r="3.4" {...stroke} />
      <path d="m8.5 8.5 5 5" {...stroke} />
      <path d="m11.2 11.2 1.5-1.5" {...stroke} />
    </svg>
  )
}

function Shield() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M8 1.6 2.9 3.8v4.3c0 3.1 2.1 5.3 5.1 6.3 3-1 5.1-3.2 5.1-6.3V3.8L8 1.6Z" {...stroke} />
      <path d="m5.9 8 1.5 1.5L10.3 6.6" {...stroke} />
    </svg>
  )
}

function Box() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M2.2 5.1 8 2.2l5.8 2.9v5.8L8 13.8l-5.8-2.9V5.1Z" {...stroke} />
      <path d="m2.2 5.1 5.8 2.9 5.8-2.9M8 8v5.8" {...stroke} />
    </svg>
  )
}

function Transfer() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M4.6 2.8v10.4M4.6 13.2 2.2 10.8M4.6 13.2 7 10.8" {...stroke} />
      <path d="M11.4 13.2V2.8M11.4 2.8 9 5.2M11.4 2.8l2.4 2.4" {...stroke} />
    </svg>
  )
}

function Car() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M2.4 10.6V8.4l1.3-3.1a1.5 1.5 0 0 1 1.4-.9h5.8a1.5 1.5 0 0 1 1.4.9l1.3 3.1v2.2H2.4Z" {...stroke} />
      <path d="M3 8.3h10" {...stroke} />
      <circle cx="5.1" cy="11.8" r="1.1" {...stroke} />
      <circle cx="10.9" cy="11.8" r="1.1" {...stroke} />
    </svg>
  )
}

type UseCase = {
  tag: string
  icon: React.ReactNode
  from: string
  before: string
  code?: string
  after?: string
}

const CASES: UseCase[] = [
  {
    tag: 'Payment',
    icon: <Card />,
    from: 'HDFC Bank',
    before: 'OTP ',
    code: '492013',
    after: ' for your transaction of ₹4,200. Valid for 10 minutes.',
  },
  {
    tag: 'Sign-in',
    icon: <Key />,
    from: 'Google',
    before: '',
    code: 'G-558129',
    after: ' is your Google verification code.',
  },
  {
    tag: 'Two-factor',
    icon: <Shield />,
    from: 'GitHub',
    before: '',
    code: '193472',
    after: ' is your GitHub authentication code. Do not share it.',
  },
  {
    tag: 'Delivery',
    icon: <Box />,
    from: 'Swiggy',
    before: 'Your delivery partner is arriving. Share OTP ',
    code: '7741',
    after: ' to collect your order.',
  },
  {
    tag: 'Transaction',
    icon: <Transfer />,
    from: 'ICICI Bank',
    before: 'Rs.1,250 debited from a/c XX4417 on 01-Nov. Not you? Call 18001080.',
  },
  {
    tag: 'Ride',
    icon: <Car />,
    from: 'Uber',
    before: 'Your driver has arrived. Share PIN ',
    code: '4812',
    after: ' to start the trip.',
  },
]

export function UseCases() {
  return (
    <section className="mt-28">
      <p className="text-xs uppercase tracking-[0.18em] text-muted">What it saves you</p>
      <h2 className="mt-3 text-3xl font-normal tracking-tight sm:text-4xl">
        The messages you unlock your phone for
        <span className="block text-muted">arrive where you already are</span>
      </h2>

      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {CASES.map((item) => (
          <div key={item.from} className="rounded-2xl bg-surface p-5">
            <p className="flex items-center gap-2 text-[11px] uppercase tracking-[0.12em] text-muted">
              <span className="h-3.5 w-3.5 [&>svg]:h-full [&>svg]:w-full">{item.icon}</span>
              {item.tag}
            </p>
            <p className="mt-3 text-sm font-semibold">{item.from}</p>
            <p className="mt-1.5 text-sm leading-relaxed text-muted">
              {item.before}
              {item.code && (
                <span className="rounded bg-white/10 px-1.5 py-0.5 font-mono text-[13px] text-white">
                  {item.code}
                </span>
              )}
              {item.after}
            </p>
          </div>
        ))}
      </div>
    </section>
  )
}
