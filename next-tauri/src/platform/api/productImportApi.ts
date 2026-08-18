import { invoke } from '@tauri-apps/api/core'
import { open } from '@tauri-apps/plugin-dialog'
import type {
  ProductImportApi,
  ProductImportPreview,
  ProductImportResult,
  ProductImportSource
} from '../contracts/productImport'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>
type ChooseDirectory = () => Promise<string | string[] | null>

export function createProductImportApi(
  invokeCommand: Invoke = invoke,
  chooseDirectory: ChooseDirectory = () => open({ directory: true, multiple: false })
): ProductImportApi {
  return {
    chooseSourceDirectory: async () => {
      const selected = await chooseDirectory()
      return typeof selected === 'string' ? selected : null
    },
    preview: (sourceProduct: ProductImportSource, sourceDirectory: string) => (
      invokeCommand<ProductImportPreview>('preview_product_import', { sourceProduct, sourceDirectory })
    ),
    run: (sourceProduct: ProductImportSource, sourceDirectory: string, expectedFingerprint: string) => (
      invokeCommand<ProductImportResult>('run_product_import', {
        sourceProduct,
        sourceDirectory,
        expectedFingerprint
      })
    )
  }
}

const browserProductImportApi: ProductImportApi = {
  chooseSourceDirectory: async () => null,
  preview: async () => { throw new Error('浏览器预览不能扫描其他桌面产品的数据目录') },
  run: async () => { throw new Error('浏览器预览不能执行桌面产品数据导入') }
}

export const productImportApi: ProductImportApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createProductImportApi()
  : browserProductImportApi
