/**
 * The desktop wallpaper behind the Mac — macOS Tahoe (dark), supplied by the user.
 *
 * Re-encoded from the 434 KB source to a 19 KB WebP: cropped to the screen's 16:10.3 from the
 * square original, and sized at 1600px, which still covers the largest the Mac is ever drawn
 * on a 2x display. Smooth gradients compress almost for free, so there was no reason to
 * accept the full-size file for something that renders a few hundred pixels wide.
 *
 * A plain <img> rather than next/image: optimising at request time would mean running an
 * image pipeline inside the Worker to re-derive a constant, and this file is already smaller
 * than anything that pipeline would emit.
 */
export function Wallpaper() {
  return (
    <img
      className="wallpaper"
      src="/wallpaper.webp"
      alt=""
      width={1600}
      height={1030}
      loading="lazy"
      decoding="async"
      aria-hidden
    />
  )
}
