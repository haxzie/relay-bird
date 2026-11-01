import type { MetadataRoute } from 'next'

import { allPosts } from '@/content/posts'
import { SITE_URL } from '@/lib/site'

/** Built from the same post list the blog renders, so a new post is listed by existing. */
export default function sitemap(): MetadataRoute.Sitemap {
  const posts = allPosts()
  const newest = posts[0]?.date

  return [
    { url: SITE_URL, lastModified: newest, changeFrequency: 'weekly', priority: 1 },
    { url: `${SITE_URL}/download`, lastModified: newest, changeFrequency: 'weekly', priority: 0.9 },
    { url: `${SITE_URL}/blog`, lastModified: newest, changeFrequency: 'weekly', priority: 0.8 },
    { url: `${SITE_URL}/about`, changeFrequency: 'monthly', priority: 0.5 },
    ...posts.map((post) => ({
      url: `${SITE_URL}/blog/${post.slug}`,
      lastModified: post.date,
      changeFrequency: 'monthly' as const,
      priority: 0.7,
    })),
  ]
}
