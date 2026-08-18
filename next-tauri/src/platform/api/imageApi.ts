import { invoke } from '@tauri-apps/api/core'
import type { ImageApi, ImageAsset, ImageAssetSummary } from '../contracts/image'

const previewAssets = new Map<string, ImageAsset>()

export const imageApi: ImageApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      list: () => invoke<ImageAssetSummary[]>('list_image_assets'),
      save: (input) => invoke<ImageAssetSummary>('save_image_asset', { input }),
      importPaths: (paths) => invoke<ImageAssetSummary[]>('import_image_files', { paths }),
      read: (name) => invoke<ImageAsset>('read_image_asset', { name }),
      export: (names) => invoke<string[] | null>('export_image_assets', { names }),
      rename: (name, nextName) => invoke<ImageAssetSummary>('rename_image_asset', { name, nextName }),
      delete: (names) => invoke<number>('delete_image_assets', { names })
    }
  : {
      list: async () => [...previewAssets.values()]
        .map(({ dataUrl: _dataUrl, ...summary }) => summary)
        .sort((left, right) => right.updatedAt - left.updatedAt),
      save: async (input) => {
        const mimeType = input.dataUrl.match(/^data:([^;]+);base64,/)?.[1] ?? 'image/png'
        const summary: ImageAssetSummary = {
          name: input.name,
          mimeType,
          width: input.width,
          height: input.height,
          sizeBytes: Math.floor((input.dataUrl.split(',')[1]?.length ?? 0) * 0.75),
          updatedAt: Date.now()
        }
        previewAssets.set(summary.name, { ...summary, dataUrl: input.dataUrl })
        return summary
      },
      importPaths: async () => { throw new Error('浏览器预览不支持原生文件路径导入') },
      read: async (name) => {
        const asset = previewAssets.get(name)
        if (!asset) throw new Error('图片不存在')
        return asset
      },
      export: async (names) => {
        for (const name of names) {
          const asset = previewAssets.get(name)
          if (!asset) throw new Error('图片不存在')
          const anchor = document.createElement('a')
          anchor.href = asset.dataUrl
          anchor.download = asset.name
          anchor.click()
        }
        return names
      },
      rename: async (name, nextName) => {
        const asset = previewAssets.get(name)
        if (!asset) throw new Error('图片不存在')
        previewAssets.delete(name)
        const renamed = { ...asset, name: nextName, updatedAt: Date.now() }
        previewAssets.set(nextName, renamed)
        const { dataUrl: _dataUrl, ...summary } = renamed
        return summary
      },
      delete: async (names) => names.reduce((count, name) => count + Number(previewAssets.delete(name)), 0)
    }
