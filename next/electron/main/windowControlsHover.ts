type Point = { x: number; y: number }
type WindowBounds = { x: number; y: number; width: number; height: number }

const brandRegionInset = { top: 18, left: 20, height: 32, maximumWidth: 420, reservedRight: 360 }
const trafficLightRegionInset = { top: 0, left: 0, width: 104, height: 58 }

export function isWindowControlsHoverTarget(cursor: Point, bounds: WindowBounds): boolean {
  const local = { x: cursor.x - bounds.x, y: cursor.y - bounds.y }
  const brandWidth = Math.min(brandRegionInset.maximumWidth, Math.max(0, bounds.width - brandRegionInset.reservedRight))
  const overBrandRegion = local.x >= brandRegionInset.left
    && local.x <= brandRegionInset.left + brandWidth
    && local.y >= brandRegionInset.top
    && local.y <= brandRegionInset.top + brandRegionInset.height
  const overTrafficLights = local.x >= trafficLightRegionInset.left
    && local.x <= trafficLightRegionInset.left + trafficLightRegionInset.width
    && local.y >= trafficLightRegionInset.top
    && local.y <= trafficLightRegionInset.top + trafficLightRegionInset.height
  return overBrandRegion || overTrafficLights
}
