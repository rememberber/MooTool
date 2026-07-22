export type SampledScreenColor = {
  hex: string
  r: number
  g: number
  b: number
}

export function sampleScreenColor(
  pixels: Uint8ClampedArray,
  width: number,
  height: number,
  x: number,
  y: number
): SampledScreenColor | null {
  if (width <= 0 || height <= 0 || pixels.length < width * height * 4) return null
  const pixelX = Math.min(width - 1, Math.max(0, Math.floor(x)))
  const pixelY = Math.min(height - 1, Math.max(0, Math.floor(y)))
  const offset = (pixelY * width + pixelX) * 4
  const r = pixels[offset]
  const g = pixels[offset + 1]
  const b = pixels[offset + 2]
  return { hex: rgbToHex(r, g, b), r, g, b }
}

export function rgbToHex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((value) => Math.min(255, Math.max(0, Math.round(value))).toString(16).padStart(2, '0')).join('').toUpperCase()}`
}
