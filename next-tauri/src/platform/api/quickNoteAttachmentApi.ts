import { invoke } from '@tauri-apps/api/core'
import { open, save } from '@tauri-apps/plugin-dialog'
import type { QuickNoteAttachment } from '../contracts/localData'

export interface QuickNoteAttachmentApi {
  list(noteId: string): Promise<QuickNoteAttachment[]>
  chooseAndImport(noteId: string): Promise<QuickNoteAttachment | null>
  exportFile(attachment: QuickNoteAttachment): Promise<string | null>
  delete(id: string): Promise<boolean>
}

const desktopApi: QuickNoteAttachmentApi = {
  list: (noteId) => invoke<QuickNoteAttachment[]>('list_quick_note_attachments', { noteId }),
  chooseAndImport: async (noteId) => {
    const selected = await open({ multiple: false, directory: false, title: 'Attach a file to Quick Note' })
    const sourcePath = Array.isArray(selected) ? selected[0] : selected
    if (!sourcePath) return null
    return invoke<QuickNoteAttachment>('import_quick_note_attachment', {
      request: { id: crypto.randomUUID(), noteId, sourcePath, createdAt: Date.now() }
    })
  },
  exportFile: async (attachment) => {
    const destinationPath = await save({ defaultPath: attachment.name, title: 'Export Quick Note attachment' })
    if (!destinationPath) return null
    await invoke('export_quick_note_attachment', { id: attachment.id, destinationPath })
    return destinationPath
  },
  delete: (id) => invoke<boolean>('delete_quick_note_attachment', { id })
}

const browserApi: QuickNoteAttachmentApi = {
  list: async () => [],
  chooseAndImport: async () => { throw new Error('Quick Note attachments require the Tauri desktop app') },
  exportFile: async () => { throw new Error('Quick Note attachments require the Tauri desktop app') },
  delete: async () => false
}

export const quickNoteAttachmentApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? desktopApi : browserApi
