import type { Metadata } from 'next'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import { marked } from 'marked'

import { POSTS, findPost, formatDate } from '@/content/posts'
import { DownloadCard } from '@/components/DownloadCard'
import { withHeadingIds } from '@/lib/headings'
import { AUTHOR_URL, SITE_URL } from '@/lib/site'

/** The set is known at build time, so each post is prerendered rather than rendered per hit. */
export function generateStaticParams() {
  return POSTS.map((post) => ({ slug: post.slug }))
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>
}): Promise<Metadata> {
  const { slug } = await params
  const post = findPost(slug)
  if (!post) return { title: 'Not found — RelayBird' }

  const url = `${SITE_URL}/blog/${post.slug}`
  return {
    title: `${post.title} — RelayBird`,
    description: post.summary,
    // Canonical matters here: a post is reachable with and without a trailing slash, and
    // search engines treat those as two pages competing with each other.
    alternates: { canonical: url },
    openGraph: {
      type: 'article',
      title: post.title,
      description: post.summary,
      url,
      publishedTime: post.date,
      siteName: 'RelayBird',
      // Declaring `openGraph` replaces the root's wholesale, so the shared card image has to
      // be named again here or posts go out with no image at all.
      images: ['/opengraph-image.png'],
    },
    twitter: {
      card: 'summary_large_image',
      title: post.title,
      description: post.summary,
      images: ['/opengraph-image.png'],
    },
  }
}

export default async function Post({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  const post = findPost(slug)
  if (!post) notFound()

  // `marked` is synchronous for plain markdown, but its type is a union with Promise.
  const rendered = await marked.parse(post.body)
  const { html, headings } = withHeadingIds(rendered)

  return (
    <main className="pt-16 sm:pt-24">
      <Link href="/blog" className="text-sm text-muted transition hover:text-white">
        ← Blog
      </Link>

      <div className="mt-8 flex gap-10">
        {/* Contents on the left, download on the right, article between them. Each rail
            drops out at the width where keeping it would start squeezing the column: the
            contents first, then the card, which is why the card also appears inline below. */}
        {headings.length > 2 && (
          <aside className="hidden w-44 shrink-0 xl:block">
            <nav className="sticky top-10">
              <p className="text-[11px] uppercase tracking-[0.14em] text-muted">On this page</p>
              <ul className="mt-4 space-y-2.5 border-l border-edge">
                {headings.map((h) => (
                  <li key={h.id}>
                    <a
                      href={`#${h.id}`}
                      className="-ml-px block border-l border-transparent pl-4 text-sm leading-snug text-muted transition hover:border-white/50 hover:text-white"
                    >
                      {h.text}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          </aside>
        )}

        <article className="min-w-0 max-w-2xl flex-1">
          <p className="text-xs uppercase tracking-[0.12em] text-muted">{formatDate(post.date)}</p>
          <h1 className="mt-3 text-3xl font-normal leading-tight tracking-tight sm:text-4xl">
            {post.title}
          </h1>
          <div className="prose mt-10" dangerouslySetInnerHTML={{ __html: html }} />

          {/* Narrow screens lose the right rail, so the call to action follows the piece. */}
          <div className="mt-14 max-w-sm lg:hidden">
            <DownloadCard />
          </div>
        </article>

        <aside className="hidden w-72 shrink-0 lg:block">
          <div className="sticky top-10">
            <DownloadCard />
          </div>
        </aside>
      </div>

      {/* Article structured data, so the post is eligible for rich results rather than being
          inferred from the markup. Built from the same fields the page renders, so the two
          cannot disagree. */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'Article',
            headline: post.title,
            description: post.summary,
            datePublished: post.date,
            dateModified: post.date,
            author: { '@type': 'Person', name: 'Haxzie', url: AUTHOR_URL },
            publisher: { '@type': 'Organization', name: 'RelayBird', url: SITE_URL },
            mainEntityOfPage: `${SITE_URL}/blog/${post.slug}`,
          }),
        }}
      />
    </main>
  )
}
