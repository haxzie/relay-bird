import type { NextConfig } from 'next'

const config: NextConfig = {
  // The site is static apart from the two API routes, which fetch from R2 at request time.
  reactStrictMode: true,

  /**
   * The dev server writes to its own directory.
   *
   * `next dev` and `next build` both default to `.next`, so building the Worker while the
   * dev server is up replaces the chunks it is serving and every page starts 500ing with
   * "Cannot find module './959.js'" until it is restarted. Giving dev its own directory
   * means the two can run at the same time, which they routinely need to.
   */
  distDir: process.env.NEXT_DIST_DIR ?? '.next',
}

export default config
