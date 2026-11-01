/**
 * Pulls the H2s out of rendered post HTML and gives each one an id to link to.
 *
 * Done by rewriting the HTML rather than overriding `marked`'s renderer: the renderer API
 * has changed shape across major versions, and this needs the same slugs in two places — the
 * anchors and the contents list — which is easier to guarantee from one pass.
 */
export type Heading = { id: string; text: string }

function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^\w\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
}

export function withHeadingIds(html: string): { html: string; headings: Heading[] } {
  const headings: Heading[] = []
  const seen = new Set<string>()

  const out = html.replace(/<h2>([\s\S]*?)<\/h2>/g, (_match, inner: string) => {
    const text = inner.replace(/<[^>]+>/g, '').trim()
    let id = slugify(text)
    // Two sections could reasonably share a title; duplicate ids would send both links to
    // whichever came first.
    let n = 2
    while (seen.has(id)) id = `${slugify(text)}-${n++}`
    seen.add(id)
    headings.push({ id, text })
    return `<h2 id="${id}">${inner}</h2>`
  })

  return { html: out, headings }
}
