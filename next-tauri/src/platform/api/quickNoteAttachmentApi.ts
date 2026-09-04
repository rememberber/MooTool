import { invoke } from '@tauri-apps/api/core'
import { open, save } from '@tauri-apps/plugin-dialog'
import type { QuickNoteAttachment } from '../contracts/localData'

export interface QuickNoteAttachmentApi {
  list(noteId: string): Promise<QuickNoteAttachment[]>
  chooseAndImport(noteId: string): Promise<QuickNoteAttachment | null>
  importPath(noteId: string, sourcePath: string): Promise<QuickNoteAttachment>
  importImageFile(noteId: string, file: File): Promise<QuickNoteAttachment>
  exportFile(attachment: QuickNoteAttachment, defaultDirectory?: string): Promise<string | null>
  delete(id: string): Promise<boolean>
}

const desktopApi: QuickNoteAttachmentApi = {
  list: (noteId) => invoke<QuickNoteAttachment[]>('list_quick_note_attachments', { noteId }),
  chooseAndImport: async (noteId) => {
    const selected = await open({ multiple: false, directory: false, title: 'Attach a file to Quick Note' })
    const sourcePath = Array.isArray(selected) ? selected[0] : selected
    if (!sourcePath) return null
    return desktopApi.importPath(noteId, sourcePath)
  },
  importPath: (noteId, sourcePath) => invoke<QuickNoteAttachment>('import_quick_note_attachment', {
    request: { id: crypto.randomUUID(), noteId, sourcePath, createdAt: Date.now() }
  }),
  importImageFile: async (noteId, file) => {
    if (!/^image\/(png|jpeg|gif|webp)$/.test(file.type) || file.size === 0 || file.size > 10 * 1024 * 1024) {
      throw new Error('Only PNG, JPEG, GIF, or WebP images up to 10 MiB can be attached directly')
    }
    return invoke<QuickNoteAttachment>('import_quick_note_attachment_data', {
      request: {
        id: crypto.randomUUID(),
        noteId,
        name: normalizedImageName(file.name, file.type),
        mimeType: file.type,
        dataBase64: await fileBase64(file),
        createdAt: Date.now()
      }
    })
  },
  exportFile: async (attachment, defaultDirectory) => {
    const destinationPath = await save({
      defaultPath: joinDialogPath(defaultDirectory, attachment.name),
      title: 'Export Quick Note attachment'
    })
    if (!destinationPath) return null
    await invoke('export_quick_note_attachment', { id: attachment.id, destinationPath })
    return destinationPath
  },
  delete: (id) => invoke<boolean>('delete_quick_note_attachment', { id })
}

const browserApi: QuickNoteAttachmentApi = {
  list: async () => [],
  chooseAndImport: async () => { throw new Error('Quick Note attachments require the Tauri desktop app') },
  importPath: async () => { throw new Error('Native file drops require the Tauri desktop app') },
  importImageFile: async (noteId, file) => ({ id: crypto.randomUUID(), noteId, name: file.name || 'Pasted-image.png', mimeType: file.type, sizeBytes: file.size, createdAt: Date.now() }),
  exportFile: async () => { throw new Error('Quick Note attachments require the Tauri desktop app') },
  delete: async () => false
}

export const quickNoteAttachmentApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? desktopApi : browserApi

function joinDialogPath(directory: string | undefined, name: string): string {
  if (!directory) return name
  if (/[\\/]$/.test(directory)) return `${directory}${name}`
  return `${directory}${directory.includes('\\') ? '\\' : '/'}${name}`
}

function fileBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = () => reject(reader.error ?? new Error('Unable to read image attachment'))
    reader.readAsDataURL(file)
  })
}

function normalizedImageName(name: string, mimeType: string): string {
  const extension = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp' }[mimeType] ?? 'png'
  const safe = name.trim().replace(/[\\/:*?"<>|\u0000-\u001f]/g, '-').slice(0, 240)
  return safe && /\.(?:png|jpe?g|gif|webp)$/i.test(safe) ? safe : `${safe || 'Pasted-image'}.${extension}`
}
