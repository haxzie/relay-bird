import type { Metadata } from 'next'
import Link from 'next/link'

import { allPosts, formatDate } from '@/content/posts'

export const metadata: Metadata = {
  title: 'Blog — RelayBird',
  description: 'Notes on building a local-first bridge between an Android phone and a Mac.',
}

export default function Blog() {
  const posts = allPosts()

  return (
    <main className="pt-16 sm:pt-24">
      <h1 className="text-4xl font-normal tracking-tight sm:text-5xl">
        Blog
        <span className="block text-muted">notes from building it</span>
      </h1>

      <div className="mt-12 max-w-3xl">
        {posts.map((post) => (
          <Link
            key={post.slug}
            href={`/blog/${post.slug}`}
            className="group block border-t border-edge py-8 last:border-b"
          >
            <p className="text-xs uppercase tracking-[0.12em] text-muted">
              {formatDate(post.date)}
            </p>
            <h2 className="mt-3 text-xl font-semibold transition group-hover:text-white/80">
              {post.title}
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-muted">{post.summary}</p>
          </Link>
        ))}
      </div>
    </main>
  )
}
