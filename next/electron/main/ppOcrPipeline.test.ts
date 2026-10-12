import { describe, expect, it } from 'vitest'
import { decodeCtc, detectorSize, orderBox, sortBoxes } from './ppOcrPipeline'

describe('PP-OCR preprocessing and decoding', () => {
  it('preserves screenshot pixels below the detection limit and keeps tiny images valid', () => {
    expect(detectorSize(1000, 240)).toEqual({ width: 992, height: 256 })
    expect(detectorSize(4000, 2000)).toEqual({ width: 1536, height: 768 })
    expect(detectorSize(1, 1)).toEqual({ width: 32, height: 32 })
  })
  it('orders rotated corners and near-aligned rows from left to right', () => {
    const box = orderBox([{ x: 30, y: 30 }, { x: 10, y: 10 }, { x: 35, y: 15 }, { x: 5, y: 25 }])
    expect(box).toEqual([{ x: 10, y: 10 }, { x: 35, y: 15 }, { x: 30, y: 30 }, { x: 5, y: 25 }])
    const at = (x: number, y: number) => box.map(p => ({ x: p.x + x, y: p.y + y }))
    const right = at(100, 0), left = at(0, 2), next = at(0, 50)
    expect(sortBoxes([next, right, left])).toEqual([left, right, next])
  })
  it('collapses repeated CTC tokens but preserves duplicates separated by a blank and literal spaces', () => {
    const characters = ['', '中', '文', ' ']
    const indices = [1, 1, 0, 1, 2, 3, 3]
    const data = new Float32Array(indices.length * 4).fill(0.02)
    indices.forEach((index, step) => { data[step * 4 + index] = 0.94 })
    const result = decodeCtc({ data, dims: [1, indices.length, 4] }, characters)
    expect(result.text).toBe('中中文 '); expect(result.confidence).toBeCloseTo(0.94)
    expect(decodeCtc({ data: new Float32Array([1, 0, 0, 0]), dims: [1, 1, 4] }, characters)).toEqual({ text: '', confidence: 0 })
  })
  it('rejects mismatched vocabularies and invalid tensor probabilities instead of garbling text', () => {
    expect(() => decodeCtc({ data: new Float32Array(3), dims: [1, 1, 3] }, ['', '中'])).toThrow('image.ocr.error.failed')
    expect(() => decodeCtc({ data: new Float32Array([NaN, Infinity]), dims: [1, 1, 2] }, ['', '中'])).toThrow('image.ocr.error.failed')
  })
})
