export interface ImageAssetSummary {
  name: string
  mimeType: string
  width: number
  height: number
  sizeBytes: number
  updatedAt: number
}

export interface ImageAsset extends ImageAssetSummary {
  dataUrl: string
}

export interface ImageAssetInput {
  name: string
  dataUrl: string
  width: number
  height: number
}

export type ImageVectorizePreset = 'poster' | 'photo' | 'bw'
export type ImageVectorizeDetail = 'low' | 'medium' | 'high'

export interface ImageVectorizeOptions {
  preset: ImageVectorizePreset
  colorCount: number
  detail: ImageVectorizeDetail
  filterSpeckle: number
}

export interface ImageApi {
  list(): Promise<ImageAssetSummary[]>
  save(input: ImageAssetInput): Promise<ImageAssetSummary>
  importPaths(paths: string[]): Promise<ImageAssetSummary[]>
  read(name: string): Promise<ImageAsset>
  export(names: string[]): Promise<string[] | null>
  vectorize(names: string[], options: ImageVectorizeOptions): Promise<string[] | null>
  rename(name: string, nextName: string): Promise<ImageAssetSummary>
  delete(names: string[]): Promise<number>
}
