export const UPDATE_CHANNEL_TAG: string
export const UPDATE_MANIFEST_URL: string
export function prepareReleaseAssets(options: {
  artifactsDirectory: string
  outputDirectory: string
  version: string
  tag: string
  notesPath: string
  publishedAt?: string
}): Promise<{
  updaterManifest: { version: string; platforms: Record<string, { signature: string; url: string }> }
  registryRelease: { assets: Array<{ url: string }> }
}>
export function updateProductManifest(options: { manifestPath: string; releasePath: string }): Promise<unknown>
export function validatePromotion(options: { latestPath: string; releasePath: string; tag: string }): Promise<unknown>
