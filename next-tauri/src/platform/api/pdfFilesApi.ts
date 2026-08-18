import { invoke } from '@tauri-apps/api/core'
import type { PdfFilesApi } from '../contracts/pdfFiles'

interface NativePdfExportPlan { sessionId: string; targetPaths: string[] }

const CHUNK_BYTES = 512 * 1024

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createPdfFilesApi(call: Invoke = invoke): PdfFilesApi {
  return {
    begin: async (names) => {
      const plan = await call<NativePdfExportPlan | null>('begin_pdf_export', { names })
      if (!plan) return null
      let closed = false
      return {
        targetPaths: plan.targetPaths,
        write: async (fileIndex, bytes, onProgress) => {
          if (closed) throw new Error('PDF 导出会话已经结束')
          let offset = 0
          while (offset < bytes.byteLength) {
            const chunk = bytes.subarray(offset, Math.min(bytes.byteLength, offset + CHUNK_BYTES))
            const nextOffset = await call<number>('write_pdf_export_chunk', {
              sessionId: plan.sessionId,
              fileIndex,
              offset,
              chunk: Array.from(chunk)
            })
            if (nextOffset <= offset || nextOffset > bytes.byteLength) throw new Error('PDF 导出分块进度无效')
            offset = nextOffset
            onProgress?.(offset)
          }
        },
        finish: async () => {
          if (closed) throw new Error('PDF 导出会话已经结束')
          const paths = await call<string[]>('finish_pdf_export', { sessionId: plan.sessionId })
          closed = true
          return paths
        },
        cancel: async () => {
          if (closed) return
          closed = true
          await call<boolean>('cancel_pdf_export', { sessionId: plan.sessionId })
        }
      }
    }
  }
}

const browserPdfFilesApi: PdfFilesApi = {
  begin: async (names) => {
    let closed = false
    const written = new Set<number>()
    return {
      targetPaths: names,
      write: async (fileIndex, bytes, onProgress) => {
        if (closed) throw new Error('PDF 导出会话已经结束')
        const name = names[fileIndex]
        if (!name) throw new Error('PDF 导出文件序号无效')
        const buffer = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer
        const url = URL.createObjectURL(new Blob([buffer], { type: 'application/pdf' }))
        const anchor = document.createElement('a')
        anchor.href = url
        anchor.download = name
        anchor.click()
        window.setTimeout(() => URL.revokeObjectURL(url), 15_000)
        written.add(fileIndex)
        onProgress?.(bytes.byteLength)
      },
      finish: async () => {
        if (closed) throw new Error('PDF 导出会话已经结束')
        if (written.size !== names.length) throw new Error('存在尚未写入的 PDF 输出')
        closed = true
        return names
      },
      cancel: async () => { closed = true }
    }
  }
}

export const pdfFilesApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createPdfFilesApi()
  : browserPdfFilesApi
