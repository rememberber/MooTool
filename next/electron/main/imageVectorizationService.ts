import { convertBuffer, type Options as VTracerOptions } from '@visioncortex/vtracer'
import type { ImageVectorizeOptions } from '../../src/shared/contracts/images'

const presets = new Set(['poster', 'photo', 'bw'])
const detailLevels = new Set(['low', 'medium', 'high'])

export function normalizeImageVectorizeOptions(value: unknown): ImageVectorizeOptions {
  if (!isRecord(value) || !presets.has(String(value.preset)) || !detailLevels.has(String(value.detail))) {
    throw new Error('Invalid image vectorization options')
  }
  const colorCount = normalizedInteger(value.colorCount, 2, 64, 'color count')
  const filterSpeckle = normalizedInteger(value.filterSpeckle, 0, 128, 'speckle filter')
  return {
    preset: value.preset as ImageVectorizeOptions['preset'],
    detail: value.detail as ImageVectorizeOptions['detail'],
    colorCount,
    filterSpeckle
  }
}

export function toVTracerOptions(options: ImageVectorizeOptions): VTracerOptions {
  const simplify = options.detail === 'low' ? 2.5 : options.detail === 'high' ? 0.5 : 1.25
  const pathPrecision = options.detail === 'high' ? 3 : 2
  return {
    preset: options.preset,
    clustering: options.preset === 'bw' ? 'bw' : 'color-cluster',
    hierarchical: options.preset === 'photo' ? 'stacked' : 'cutout',
    mode: 'spline',
    filterSpeckle: options.filterSpeckle,
    colorPrecision: 8,
    simplify,
    pathPrecision,
    maxColors: options.preset === 'bw' ? undefined : options.colorCount,
    optimize: 2
  }
}

export function vectorizePng(png: Uint8Array, options: ImageVectorizeOptions): string {
  const svg = convertBuffer(png, toVTracerOptions(options))
  if (!/<svg\b[^>]*xmlns=["']http:\/\/www\.w3\.org\/2000\/svg["']/.test(svg) || !/<path\b/.test(svg)) {
    throw new Error('Vectorizer returned invalid SVG output')
  }
  if (/<image\b/i.test(svg) || /data:image\//i.test(svg)) {
    throw new Error('Vectorizer returned an embedded bitmap instead of vector paths')
  }
  return svg
}

function normalizedInteger(value: unknown, minimum: number, maximum: number, name: string): number {
  if (typeof value !== 'number' || !Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`Invalid ${name}`)
  }
  return value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
