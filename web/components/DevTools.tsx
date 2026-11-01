'use client'

import { useEffect } from 'react'

/**
 * React Grab, in development only.
 *
 * Hover any element and press ⌘C to copy it along with the component stack and source
 * location, which is what makes pointing an agent at "this bit here" accurate.
 *
 * Loaded through a dynamic import behind a `NODE_ENV` check rather than a plain top-level
 * import: the check folds to `false` at build time, so the branch and the chunk both drop out
 * of the production bundle. That matters because it is a devDependency — a static import
 * would make the Worker's bundle depend on a package that is not meant to ship.
 *
 * The upstream docs suggest a <Script> tag from unpkg instead; a real dependency keeps the
 * version in the lockfile and means the dev server does not reach out to a CDN.
 */
export function DevTools() {
  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      void import('react-grab')
    }
  }, [])

  return null
}
