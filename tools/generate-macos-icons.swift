import AppKit
import CoreGraphics
import Foundation

let sourcePath = CommandLine.arguments[1]
let outDir = URL(fileURLWithPath: CommandLine.arguments[2])

guard let data = try? Data(contentsOf: URL(fileURLWithPath: sourcePath)),
      let source = NSBitmapImageRep(data: data)?.cgImage else {
    fatalError("cannot read source")
}

// The exported PNG is a white mark on a near-black field. Both icon forms want the mark
// alone, so it is converted to a white, alpha-masked image: the source background is not
// quite #000 and would otherwise show as a lighter square against a pure black tile, and a
// template menu bar icon built from it would render as a solid block.
func whiteOnClear(_ image: CGImage) -> (image: CGImage, bounds: CGRect) {
    let w = image.width, h = image.height
    // Explicitly allocated: a Swift array's inout pointer is not valid once the CGContext
    // initialiser returns, so the pixel edits below would land in freed memory.
    let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: w * h * 4)
    defer { buffer.deallocate() }
    buffer.initialize(repeating: 0, count: w * h * 4)

    let ctx = CGContext(data: buffer, width: w, height: h, bitsPerComponent: 8,
                        bytesPerRow: w * 4, space: CGColorSpaceCreateDeviceRGB(),
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))

    var minX = w, minY = h, maxX = 0, maxY = 0
    for y in 0 ..< h {
        for x in 0 ..< w {
            let i = (y * w + x) * 4
            // Luminance becomes alpha, which keeps the original edge antialiasing.
            let luma = (UInt16(buffer[i]) * 54 + UInt16(buffer[i + 1]) * 183 + UInt16(buffer[i + 2]) * 19) >> 8
            // The source "black" is about #0A0A0A, not #000, so a plain luminance-to-alpha
            // leaves the field at alpha 6 — invisible on its own, a faint square once it is
            // composited onto a pure black tile. Everything below the floor is dropped, and
            // the rest is rescaled so edge antialiasing survives.
            let floor: UInt16 = 26
            let alpha = luma <= floor ? 0 : UInt8(min((luma - floor) * 255 / (255 - floor), 255))
            // Premultiplied white: the stored colour channels equal alpha.
            buffer[i] = alpha; buffer[i + 1] = alpha; buffer[i + 2] = alpha; buffer[i + 3] = alpha
            if alpha > 40 {
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
            }
        }
    }
    let out = ctx.makeImage()!
    let box = CGRect(x: minX, y: minY, width: maxX - minX + 1, height: maxY - minY + 1)
    return (out.cropping(to: box)!, box)
}

let (mark, box) = whiteOnClear(source)
print("mark bounding box: \(box)")

func context(_ size: Int) -> CGContext {
    let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8,
                        bytesPerRow: 0, space: CGColorSpaceCreateDeviceRGB(),
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.interpolationQuality = .high
    return ctx
}

/// Centre the mark in `rect` without distorting it.
func fitted(_ image: CGImage, in rect: CGRect) -> CGRect {
    let scale = min(rect.width / CGFloat(image.width), rect.height / CGFloat(image.height))
    let w = CGFloat(image.width) * scale, h = CGFloat(image.height) * scale
    return CGRect(x: rect.midX - w / 2, y: rect.midY - h / 2, width: w, height: h)
}

func write(_ image: CGImage, to url: URL) {
    let rep = NSBitmapImageRep(cgImage: image)
    rep.size = NSSize(width: image.width, height: image.height)
    try! rep.representation(using: .png, properties: [:])!.write(to: url)
}

/// macOS app icon.
///
/// A plain square, filled to every edge, with no rounding of its own. macOS 26 draws the tile
/// shape itself and masks the artwork into it, so anything we round or inset leaves a gap
/// where the system's own plate shows through — which is the white rim that appeared around
/// this icon. Compare any system icon: they fill their tile completely.
func appIcon(size: Int) -> CGImage {
    let ctx = context(size)
    let canvas = CGFloat(size)
    let tile = CGRect(x: 0, y: 0, width: canvas, height: canvas)
    ctx.setFillColor(CGColor(red: 0, green: 0, blue: 0, alpha: 1))
    ctx.fill(tile)
    ctx.draw(mark, in: fitted(mark, in: tile.insetBy(dx: canvas * 0.26, dy: canvas * 0.26)))
    return ctx.makeImage()!
}

/// Menu bar icon: alpha-only, so macOS can tint it for light and dark menu bars.
func menuBarIcon(size: Int) -> CGImage {
    let ctx = context(size)
    let canvas = CGFloat(size)
    ctx.draw(mark, in: fitted(mark, in: CGRect(x: 0, y: 0, width: canvas, height: canvas)
        .insetBy(dx: canvas * 0.06, dy: canvas * 0.06)))
    return ctx.makeImage()!
}

let appIconDir = outDir.appendingPathComponent("AppIcon.appiconset")
let menuBarDir = outDir.appendingPathComponent("MenuBarIcon.imageset")
for dir in [appIconDir, menuBarDir] {
    try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
}

var appEntries: [[String: String]] = []
for base in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let pixels = base * scale
        let name = "icon_\(base)x\(base)\(scale == 2 ? "@2x" : "").png"
        write(appIcon(size: pixels), to: appIconDir.appendingPathComponent(name))
        appEntries.append([
            "size": "\(base)x\(base)", "idiom": "mac", "filename": name, "scale": "\(scale)x",
        ])
    }
}
try! JSONSerialization.data(
    withJSONObject: ["images": appEntries, "info": ["version": 1, "author": "xcode"]],
    options: [.prettyPrinted, .sortedKeys]
).write(to: appIconDir.appendingPathComponent("Contents.json"))

var menuEntries: [[String: String]] = []
for scale in [1, 2, 3] {
    let name = "menubar\(scale == 1 ? "" : "@\(scale)x").png"
    write(menuBarIcon(size: 18 * scale), to: menuBarDir.appendingPathComponent(name))
    menuEntries.append(["idiom": "universal", "filename": name, "scale": "\(scale)x"])
}
try! JSONSerialization.data(
    withJSONObject: [
        "images": menuEntries,
        "info": ["version": 1, "author": "xcode"],
        "properties": ["template-rendering-intent": "template"],
    ],
    options: [.prettyPrinted, .sortedKeys]
).write(to: menuBarDir.appendingPathComponent("Contents.json"))

// Larger template copy of the mark, for use inside the app's own views.
let brandDir = outDir.appendingPathComponent("BrandMark.imageset")
try? FileManager.default.createDirectory(at: brandDir, withIntermediateDirectories: true)
var brandEntries: [[String: String]] = []
for scale in [1, 2, 3] {
    let name = "brand\(scale == 1 ? "" : "@\(scale)x").png"
    write(menuBarIcon(size: 64 * scale), to: brandDir.appendingPathComponent(name))
    brandEntries.append(["idiom": "universal", "filename": name, "scale": "\(scale)x"])
}
try! JSONSerialization.data(
    withJSONObject: [
        "images": brandEntries,
        "info": ["version": 1, "author": "xcode"],
        "properties": ["template-rendering-intent": "template"],
    ],
    options: [.prettyPrinted, .sortedKeys]
).write(to: brandDir.appendingPathComponent("Contents.json"))

print("wrote \(appEntries.count) app icon sizes, \(menuEntries.count) menu bar and \(brandEntries.count) brand sizes")
