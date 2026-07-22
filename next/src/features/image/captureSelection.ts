import type { CropRect } from './imageTools'

export type CapturePoint = { x: number; y: number }
export type CaptureBounds = { width: number; height: number }
export type CaptureResizeHandle = 'nw' | 'ne' | 'se' | 'sw'

export function clampCaptureRect(rect: CropRect, bounds: CaptureBounds): CropRect {
  const widthLimit = positiveInteger(bounds.width)
  const heightLimit = positiveInteger(bounds.height)
  const x = clamp(integer(rect.x), 0, widthLimit - 1)
  const y = clamp(integer(rect.y), 0, heightLimit - 1)
  return {
    x,
    y,
    width: clamp(positiveInteger(rect.width), 1, widthLimit - x),
    height: clamp(positiveInteger(rect.height), 1, heightLimit - y)
  }
}

export function captureRectFromPoints(start: CapturePoint, end: CapturePoint, bounds: CaptureBounds): CropRect {
  const widthLimit = positiveInteger(bounds.width)
  const heightLimit = positiveInteger(bounds.height)
  const startX = clamp(finite(start.x), 0, widthLimit)
  const startY = clamp(finite(start.y), 0, heightLimit)
  const endX = clamp(finite(end.x), 0, widthLimit)
  const endY = clamp(finite(end.y), 0, heightLimit)
  return clampCaptureRect({
    x: Math.floor(Math.min(startX, endX)),
    y: Math.floor(Math.min(startY, endY)),
    width: Math.ceil(Math.abs(endX - startX)),
    height: Math.ceil(Math.abs(endY - startY))
  }, bounds)
}

export function moveCaptureRect(rect: CropRect, delta: CapturePoint, bounds: CaptureBounds): CropRect {
  const normalized = clampCaptureRect(rect, bounds)
  const widthLimit = positiveInteger(bounds.width)
  const heightLimit = positiveInteger(bounds.height)
  return {
    ...normalized,
    x: clamp(integer(normalized.x + finite(delta.x)), 0, widthLimit - normalized.width),
    y: clamp(integer(normalized.y + finite(delta.y)), 0, heightLimit - normalized.height)
  }
}

export function resizeCaptureRect(rect: CropRect, handle: CaptureResizeHandle, delta: CapturePoint, bounds: CaptureBounds): CropRect {
  const normalized = clampCaptureRect(rect, bounds)
  const widthLimit = positiveInteger(bounds.width)
  const heightLimit = positiveInteger(bounds.height)
  let left = normalized.x
  let top = normalized.y
  let right = normalized.x + normalized.width
  let bottom = normalized.y + normalized.height
  const deltaX = integer(delta.x)
  const deltaY = integer(delta.y)

  if (handle.includes('w')) left = clamp(left + deltaX, 0, right - 1)
  if (handle.includes('e')) right = clamp(right + deltaX, left + 1, widthLimit)
  if (handle.includes('n')) top = clamp(top + deltaY, 0, bottom - 1)
  if (handle.includes('s')) bottom = clamp(bottom + deltaY, top + 1, heightLimit)

  return { x: left, y: top, width: right - left, height: bottom - top }
}

function finite(value: number): number {
  return Number.isFinite(value) ? value : 0
}

function integer(value: number): number {
  return Math.round(finite(value))
}

function positiveInteger(value: number): number {
  return Math.max(1, integer(value))
}

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, value))
}
