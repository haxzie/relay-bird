import type { NextConfig } from 'next'

const config: NextConfig = {
  // The site is static apart from the two API routes, which fetch from R2 at request time.
  reactStrictMode: true,
}

export default config
