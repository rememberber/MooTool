// Adapted for JavaScript from RapidOCR v3.10.0 / PaddleOCR's DB and CTC pipeline.
// Copyright (c) 2021 RapidOCR Authors; Copyright (c) 2020 PaddlePaddle Authors.
// Licensed under Apache-2.0. See resources/ocr/NOTICE and licenses/Apache-2.0.txt.
// Changes: bounded single-image CPU inference, OpenCV.js, original-pixel crops,
// process isolation, RGBA compositing, and the MooTool line-result contract.
import type { OcrLine } from '../../src/shared/contracts/ocr'

type Point = { x: number; y: number }
type Tensor = { data: Float32Array; dims: readonly number[] }
type Session = { inputNames: readonly string[]; outputNames: readonly string[]; run(feeds: Record<string, unknown>): Promise<Record<string, Tensor>>; release(): Promise<void> }
export type OcrRuntime = {
  Tensor: new (type: string, data: Float32Array, dims: number[]) => unknown
  InferenceSession: { create(path: string, options: Record<string, unknown>): Promise<Session> }
  env: { logLevel: string }
}
// OpenCV.js exposes dynamically initialized embind objects rather than a stable
// TypeScript API. Keep that boundary inside this module.
type Cv = any
const failure = (code: string): never => { throw new Error(`image.ocr.error.${code}`) }
const distance = (a: Point, b: Point) => Math.hypot(a.x - b.x, a.y - b.y)

export function detectorSize(width: number, height: number) {
  const ratio = Math.min(1, 1536 / Math.max(width, height))
  return { width: Math.max(32, Math.round(Math.floor(width * ratio) / 32) * 32), height: Math.max(32, Math.round(Math.floor(height * ratio) / 32) * 32) }
}
export function orderBox(points: Point[]): Point[] {
  const sorted = [...points].sort((a, b) => a.x - b.x)
  const left = sorted.slice(0, 2).sort((a, b) => a.y - b.y)
  const right = sorted.slice(2).sort((a, b) => a.y - b.y)
  return [left[0], right[0], right[1], left[1]]
}
export function sortBoxes(boxes: Point[][]) {
  boxes.sort((a, b) => a[0].y - b[0].y || a[0].x - b[0].x)
  for (let i = 1; i < boxes.length; i++) {
    for (let j = i; j > 0 && Math.abs(boxes[j][0].y - boxes[j - 1][0].y) < 10 && boxes[j][0].x < boxes[j - 1][0].x; j--) {
      [boxes[j], boxes[j - 1]] = [boxes[j - 1], boxes[j]]
    }
  }
  return boxes
}
export function decodeCtc(tensor: Tensor, characters: string[]) {
  const [batch, steps, classes] = tensor.dims
  if (batch !== 1 || !Number.isInteger(steps) || steps < 1 || steps > 2048 || classes !== characters.length || tensor.data.length !== steps * classes) failure('failed')
  let text = '', sum = 0, count = 0, previous = -1
  for (let step = 0; step < steps; step++) {
    let index = 0, score = -Infinity
    for (let c = 0; c < classes; c++) { const value = tensor.data[step * classes + c]; if (value > score) { score = value; index = c } }
    if (!Number.isFinite(score) || score < 0 || score > 1.001) failure('failed')
    if (index !== 0 && index !== previous) { text += characters[index]; sum += Math.min(1, score); count++ }
    previous = index
  }
  return { text, confidence: count ? sum / count : 0 }
}
function miniBox(cv: Cv, contour: any) {
  const rect = cv.minAreaRect(contour)
  return { points: orderBox(cv.boxPoints(rect)), side: Math.min(rect.size.width, rect.size.height) }
}
function scoreBox(cv: Cv, prediction: any, points: Point[]): number {
  const x = Math.max(0, Math.floor(Math.min(...points.map(p => p.x))))
  const y = Math.max(0, Math.floor(Math.min(...points.map(p => p.y))))
  const right = Math.min(prediction.cols - 1, Math.ceil(Math.max(...points.map(p => p.x))))
  const bottom = Math.min(prediction.rows - 1, Math.ceil(Math.max(...points.map(p => p.y))))
  if (right < x || bottom < y) return 0
  const mask = cv.Mat.zeros(bottom - y + 1, right - x + 1, cv.CV_8UC1)
  const polygon = cv.matFromArray(4, 1, cv.CV_32SC2, points.flatMap(p => [Math.trunc(p.x - x), Math.trunc(p.y - y)]))
  const polygons = new cv.MatVector(), roi = prediction.roi(new cv.Rect(x, y, mask.cols, mask.rows))
  try { polygons.push_back(polygon); cv.fillPoly(mask, polygons, new cv.Scalar(1)); return cv.mean(roi, mask)[0] }
  finally { roi.delete(); polygons.delete(); polygon.delete(); mask.delete() }
}
function detectBoxes(cv: Cv, clipper: any, tensor: Tensor, originalWidth: number, originalHeight: number): Point[][] {
  const [batch, channels, height, width] = tensor.dims
  if (batch !== 1 || channels !== 1 || !Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 || width > 1536 || height > 1536 || tensor.data.length !== width * height) failure('failed')
  const prediction = cv.matFromArray(height, width, cv.CV_32FC1, tensor.data)
  const mask = new cv.Mat(height, width, cv.CV_8UC1), dilated = new cv.Mat()
  const kernel = cv.Mat.ones(2, 2, cv.CV_8UC1), contours = new cv.MatVector(), hierarchy = new cv.Mat()
  try {
    for (let i = 0; i < tensor.data.length; i++) mask.data[i] = tensor.data[i] > 0.3 ? 255 : 0
    cv.dilate(mask, dilated, kernel)
    cv.findContours(dilated, contours, hierarchy, cv.RETR_LIST, cv.CHAIN_APPROX_SIMPLE)
    if (contours.size() > 2000) failure('tooComplex')
    const boxes: Point[][] = []
    for (let i = 0; i < contours.size(); i++) {
      const contour = contours.get(i)
      let box: ReturnType<typeof miniBox>
      try { box = miniBox(cv, contour) } finally { contour.delete() }
      if (box.side < 3 || scoreBox(cv, prediction, box.points) < 0.5) continue
      const paths = box.points.map(p => ({ X: Math.trunc(p.x), Y: Math.trunc(p.y) }))
      const area = Math.abs(clipper.Clipper.Area(paths))
      const perimeter = box.points.reduce((sum, p, j) => sum + distance(p, box.points[(j + 1) % 4]), 0)
      if (!perimeter || !area) continue
      const offset = new clipper.ClipperOffset(), expanded: { X: number; Y: number }[][] = []
      offset.AddPath(paths, clipper.JoinType.jtRound, clipper.EndType.etClosedPolygon)
      offset.Execute(expanded, area * 1.6 / perimeter)
      if (expanded.length !== 1 || expanded[0].length < 4) continue
      const points = cv.matFromArray(expanded[0].length, 1, cv.CV_32FC2, expanded[0].flatMap(p => [p.X, p.Y]))
      try { box = miniBox(cv, points) } finally { points.delete() }
      if (box.side < 5) continue
      const mapped = orderBox(box.points.map(p => ({ x: Math.min(originalWidth - 1, Math.max(0, Math.round(p.x / width * originalWidth))), y: Math.min(originalHeight - 1, Math.max(0, Math.round(p.y / height * originalHeight))) })))
      if (distance(mapped[0], mapped[1]) <= 3 || distance(mapped[0], mapped[3]) <= 3) continue
      boxes.push(mapped)
      if (boxes.length > 200) failure('tooComplex')
    }
    return sortBoxes(boxes)
  } finally { hierarchy.delete(); contours.delete(); kernel.delete(); dilated.delete(); mask.delete(); prediction.delete() }
}
function normalized(cv: Cv, source: any, width: number, height: number, paddedWidth = width): Float32Array {
  const resized = new cv.Mat(), output = new Float32Array(3 * height * paddedWidth)
  try {
    cv.resize(source, resized, new cv.Size(width, height), 0, 0, cv.INTER_LINEAR)
    const plane = height * paddedWidth
    for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) for (let c = 0; c < 3; c++) output[c * plane + y * paddedWidth + x] = (resized.data[(y * width + x) * 3 + c] / 255 - 0.5) / 0.5
    return output
  } finally { resized.delete() }
}
function cropBox(cv: Cv, image: any, points: Point[]) {
  const width = Math.floor(Math.max(distance(points[0], points[1]), distance(points[2], points[3])))
  const height = Math.floor(Math.max(distance(points[0], points[3]), distance(points[1], points[2])))
  if (!width || !height || width * height > 16000000) failure('tooComplex')
  const source = cv.matFromArray(4, 1, cv.CV_32FC2, points.flatMap(p => [p.x, p.y]))
  const target = cv.matFromArray(4, 1, cv.CV_32FC2, [0, 0, width, 0, width, height, 0, height])
  const transform = cv.getPerspectiveTransform(source, target), crop = new cv.Mat()
  try {
    cv.warpPerspective(image, crop, transform, new cv.Size(width, height), cv.INTER_CUBIC, cv.BORDER_REPLICATE)
    if (height / width >= 1.5) { const rotated = new cv.Mat(); cv.rotate(crop, rotated, cv.ROTATE_90_COUNTERCLOCKWISE); crop.delete(); return rotated }
    return crop
  } catch (error) { crop.delete(); throw error }
  finally { transform.delete(); target.delete(); source.delete() }
}

export async function recognizePpOcr(dependencies: { cv: Cv; clipper: any; ort: OcrRuntime }, models: { detector: string; recognizer: string; characters: string[] }, image: { width: number; height: number; data: Uint8Array }): Promise<{ width: number; height: number; lines: OcrLine[] }> {
  const { cv, clipper, ort } = dependencies
  const { width, height, data } = image
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 || width > 16384 || height > 16384 || width * height > 32000000 || data.length !== width * height * 4) failure('input')
  ort.env.logLevel = 'error'
  const options = { executionProviders: ['cpu'], intraOpNumThreads: 4, interOpNumThreads: 1, graphOptimizationLevel: 'all', enableCpuMemArena: false, enableMemPattern: false, logSeverityLevel: 3 }
  const detector = await ort.InferenceSession.create(models.detector, options)
  let recognizer: Session | undefined
  const original = new cv.Mat(height, width, cv.CV_8UC3)
  try {
    for (let p = 0; p < width * height; p++) {
      const alpha = data[p * 4 + 3] / 255
      for (let c = 0; c < 3; c++) original.data[p * 3 + c] = Math.round(data[p * 4 + 2 - c] * alpha + 255 * (1 - alpha))
    }
    const size = detectorSize(width, height)
    const tensor = new ort.Tensor('float32', normalized(cv, original, size.width, size.height), [1, 3, size.height, size.width])
    const prediction = (await detector.run({ [detector.inputNames[0]]: tensor }))[detector.outputNames[0]]
    const boxes = detectBoxes(cv, clipper, prediction, width, height)
    const lines: OcrLine[] = []
    if (!boxes.length) return { width, height, lines }
    recognizer = await ort.InferenceSession.create(models.recognizer, options)
    let textLength = 0
    for (const points of boxes) {
      const crop = cropBox(cv, original, points)
      let input: unknown
      try {
        const resizedWidth = Math.ceil(48 * crop.cols / crop.rows)
        if (resizedWidth > 4096) failure('tooComplex')
        const paddedWidth = Math.max(320, Math.floor(48 * Math.max(320 / 48, crop.cols / crop.rows)))
        input = new ort.Tensor('float32', normalized(cv, crop, Math.min(resizedWidth, paddedWidth), 48, paddedWidth), [1, 3, 48, paddedWidth])
      } finally { crop.delete() }
      const output = (await recognizer.run({ [recognizer.inputNames[0]]: input }))[recognizer.outputNames[0]]
      const decoded = decodeCtc(output, models.characters)
      if (!decoded.text.trim() || decoded.confidence < 0.5) continue
      textLength += decoded.text.length + 1
      if (textLength > 100000) failure('tooLarge')
      const x = Math.min(...points.map(p => p.x)), y = Math.min(...points.map(p => p.y))
      lines.push({ ...decoded, box: { x, y, width: Math.max(...points.map(p => p.x)) - x, height: Math.max(...points.map(p => p.y)) - y } })
    }
    return { width, height, lines }
  } finally { original.delete(); if (recognizer) await recognizer.release(); await detector.release() }
}
