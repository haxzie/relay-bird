import type { MetadataRoute } from 'next'

import { SITE_URL } from '@/lib/site'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      // `/shots` only exists to generate the blog images; it is not content and would be a
      // confusing thing to land on from a search result.
      disallow: ['/shots', '/api/'],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  }
}
