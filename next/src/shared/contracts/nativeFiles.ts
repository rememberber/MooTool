export const digestAlgorithmIds = ['MD5', 'SHA-1', 'SHA-256', 'SHA-384', 'SHA-512', 'SM3'] as const
export type DigestAlgorithmId = (typeof digestAlgorithmIds)[number]

export type DigestFileResult = {
  path: string
  name: string
  size: number
  digest: string
}

export type BinaryFileKind = 'image' | 'pdf' | 'binary'

export type SaveBinaryFileInput = {
  kind: BinaryFileKind
  defaultName: string
  dataUrl: string
}

export type ImageFilePayload = {
  path: string
  name: string
  size: number
  width: number
  height: number
  dataUrl: string
}
