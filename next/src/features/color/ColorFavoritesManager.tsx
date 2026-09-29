import { MoreHorizontal, Check } from 'lucide-react'
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useToolActivity } from '@/shared/components/ToolActivity'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteFolderRecord, FavoriteRecord } from '@/shared/contracts/favorites'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { formatColor, parseColor } from './colorTools'

export function ColorFavoritesManager({ open, currentValue, onClose, onApply }: {
  open: boolean; currentValue: string; onClose: () => void; onApply: (value: string) => void
}) {
  const { t, language } = useI18n()
  const toolActive = useToolActivity()
  const dialog = useDesktopDialog()
  const words = language === 'zh-CN' ? ['编辑颜色', '上移', '下移', '选择', '完成', '全选', '点击色块应用颜色 · 右键管理', '备注'] : language === 'ja-JP' ? ['色を編集', '上へ', '下へ', '選択', '完了', 'すべて選択', 'クリックで適用・右クリックで管理', 'メモ'] : ['Edit color', 'Move up', 'Move down', 'Select', 'Done', 'Select all', 'Click to apply · Right-click to manage', 'Notes']
  const [folders, setFolders] = useState<FavoriteFolderRecord[]>([])
  const [folderId, setFolderId] = useState<number>()
  const [records, setRecords] = useState<FavoriteRecord[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [selecting, setSelecting] = useState(false)
  const [editing, setEditing] = useState<{ id?: number; name: string; value: string; description: string } | null>(null)
  const [error, setError] = useState('')
  const [menu, setMenu] = useState<{ ids: number[]; left: number; top: number } | null>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const anchor = useRef<number | undefined>(undefined)
  async function run(action: () => Promise<unknown>) {
    try { setError(''); await action() } catch (caught) { setError(caught instanceof Error ? caught.message : String(caught)) }
  }
  async function load(preferred = folderId) {
    const next = await window.mootool.listFavoriteFolders('color')
    const id = next.some(folder => folder.id === preferred) ? preferred : next[0]?.id
    setFolders(next); setFolderId(id)
    const items = await window.mootool.listFavorites('color', id)
    setRecords(items)
    setSelected(current => current.filter(value => items.some(item => item.id === value)))
  }
  useEffect(() => {
    if (open) { setSelecting(false); setSelected([]); setMenu(null); setEditing(null); void run(() => load()) }
  }, [open])
  useLayoutEffect(() => {
    const element = menuRef.current
    if (!menu || !open || !toolActive || !element) return
    const bounds = element.getBoundingClientRect()
    element.style.left = `${Math.max(8, Math.min(menu.left, window.innerWidth - bounds.width - 8))}px`
    element.style.top = `${Math.max(8, Math.min(menu.top, window.innerHeight - bounds.height - 8))}px`
    element.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus({ preventScroll: true })
  }, [menu, open, toolActive])
  useEffect(() => {
    if (!menu || !open || !toolActive) return
    const close = (event: PointerEvent) => { if (!menuRef.current?.contains(event.target as Node)) setMenu(null) }
    const dismiss = () => setMenu(null)
    document.addEventListener('pointerdown', close)
    window.addEventListener('resize', dismiss)
    window.addEventListener('blur', dismiss)
    return () => {
      document.removeEventListener('pointerdown', close)
      window.removeEventListener('resize', dismiss)
      window.removeEventListener('blur', dismiss)
    }
  }, [menu, open, toolActive])
  async function folderAction(action: 'create' | 'rename' | 'delete') {
    const folder = folders.find(item => item.id === folderId)
    if (action === 'delete') {
      if (!folder || !await dialog.confirm(t('favorite.deleteFolderConfirm', { name: folder.title }), { danger: true })) return
      await window.mootool.deleteFavoriteFolder(folder.id)
      await load(); return
    }
    const title = await dialog.prompt(t('favorite.folderName'), { defaultValue: action === 'rename' ? folder?.title : '' })
    if (!title?.trim()) return
    const saved = action === 'create' ? await window.mootool.createFavoriteFolder({ kind: 'color', title }) : await window.mootool.renameFavoriteFolder({ id: folderId!, title })
    setSelected([]); await load(saved.id)
  }
  async function remove(ids: number[]) {
    setMenu(null)
    if (!await dialog.confirm(`${t('common.action.delete')} (${ids.length})`, { danger: true })) return
    for (const id of ids) await window.mootool.deleteFavorite(id)
    await load()
  }
  async function move(ids: number[], direction: -1 | 1) {
    setMenu(null)
    const ordered = records.filter(item => ids.includes(item.id))
    if (direction === 1) ordered.reverse()
    for (const item of ordered) await window.mootool.moveFavorite(item.id, direction)
    await load()
  }
  function openMenu(record: FavoriteRecord, left: number, top: number) {
    setMenu({ ids: selecting && selected.includes(record.id) ? selected : [record.id], left: Math.max(8, Math.min(left, window.innerWidth - 180)), top: Math.max(8, Math.min(top, window.innerHeight - 170)) })
  }
  function choose(record: FavoriteRecord, event: React.MouseEvent) {
    if (!selecting && !event.metaKey && !event.ctrlKey && !event.shiftKey) { onApply(record.value); onClose(); return }
    setSelecting(true)
    if (event.shiftKey && records.some(item => item.id === anchor.current)) {
      const a = records.findIndex(item => item.id === anchor.current)
      const b = records.indexOf(record)
      setSelected(records.slice(Math.min(a, b), Math.max(a, b) + 1).map(item => item.id))
    } else {
      anchor.current = record.id
      setSelected(current => current.includes(record.id) ? current.filter(id => id !== record.id) : [...current, record.id])
    }
  }
  const editRecord = menu?.ids.length === 1 ? records.find(item => item.id === menu.ids[0]) : undefined
  return <>
    <Dialog title={t('color.favorites')} open={open} width={760} onClose={() => { if (menu) setMenu(null); else onClose() }} footer={<span className="color-collection-hint">{words[6]}</span>}>
      <div className="color-collection-toolbar">
        <select aria-label={t('favorite.folder')} value={folderId ?? ''} onChange={event => { setSelected([]); anchor.current = undefined; void run(() => load(Number(event.target.value))) }}>{folders.map(folder => <option key={folder.id} value={folder.id}>{folder.title}</option>)}</select>
        <details className="color-collection-folder-menu"><summary aria-label={t('favorite.folder')}><MoreHorizontal size={16} /></summary><div>{(['create', 'rename', 'delete'] as const).map((action, i) => <button type="button" key={action} onClick={event => { event.currentTarget.closest('details')?.removeAttribute('open'); void run(() => folderAction(action)) }}>{t((['favorite.newFolder', 'favorite.renameFolder', 'favorite.deleteFolder'] as const)[i])}</button>)}</div></details>
        <span className="color-collection-spacer" />
        <button className="dialog-button" onClick={() => { setSelecting(!selecting); setSelected([]) }}>{selecting ? words[4] : words[3]}</button>
        <button className="dialog-button" onClick={() => setEditing({ name: `Color-${currentValue}`, value: currentValue, description: '' })}>{t('color.favorite')}</button>
      </div>
      {selecting && <div className="color-collection-selection">
        <button className="toolbar-button" onClick={() => setSelected(records.map(item => item.id))}>{words[5]}</button><span>{selected.length} / {records.length}</span>
        <span className="color-collection-spacer" />
        {([-1, 1] as const).map((direction, i) => <button className="toolbar-button" key={direction} disabled={!selected.length || selected.includes(records[direction === -1 ? 0 : records.length - 1]?.id)} onClick={() => void run(() => move(selected, direction))}>{words[i + 1]}</button>)}
        <button className="toolbar-button" disabled={!selected.length} onClick={() => void run(() => remove(selected))}>{t('common.action.delete')}</button>
      </div>}
      {error && !editing && <p role="alert" className="result-status--error">{error}</p>}
      <div className="color-collection-grid">
        {!records.length && <p className="history-empty">{t('favorite.empty')}</p>}
        {records.map(record => <article className={`color-collection-card${selected.includes(record.id) ? ' color-collection-card--selected' : ''}`} key={record.id} onContextMenu={event => { event.preventDefault(); openMenu(record, event.clientX, event.clientY) }}>
          <button className="color-collection-apply" aria-label={`${record.name} ${record.value}`} aria-pressed={selecting ? selected.includes(record.id) : undefined} onClick={event => choose(record, event)}>
            <span className="color-collection-swatch" style={{ backgroundColor: previewColor(record.value) }} />
            <span className="color-collection-caption"><strong>{record.name}</strong><code>{record.value}</code></span>
            {selecting && selected.includes(record.id) && <span className="color-collection-check"><Check size={14} /></span>}
          </button>
          <button className="color-collection-more" aria-label={`${words[0]} · ${record.name}`} onClick={event => { const rect = event.currentTarget.getBoundingClientRect(); openMenu(record, rect.right, rect.bottom) }}><MoreHorizontal size={16} /></button>
        </article>)}
      </div>
      {menu && open && toolActive && createPortal(<div ref={menuRef} className="quick-note-tree-menu color-collection-context-menu" role="menu" style={{ left: menu.left, top: menu.top }}>
        <button role="menuitem" disabled={!editRecord} onClick={() => { if (editRecord) setEditing(editRecord); setMenu(null) }}>{words[0]}</button>
        {([-1, 1] as const).map((direction, i) => <button role="menuitem" key={direction} disabled={menu.ids.includes(records[direction === -1 ? 0 : records.length - 1]?.id)} onClick={() => void run(() => move(menu.ids, direction))}>{words[i + 1]}</button>)}
        <button role="menuitem" onClick={() => void run(() => remove(menu.ids))}>{t('common.action.delete')}</button>
      </div>, document.body)}
    </Dialog>
    <Dialog title={words[0]} open={open && editing !== null} width={440} onClose={() => setEditing(null)} footer={<><button className="dialog-button" onClick={() => setEditing(null)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" disabled={!editing?.name.trim() || !editing.value.trim() || !folderId} onClick={() => void run(async () => { if (!editing) return; const value = formatColor(parseColor(editing.value), 'HEX_UPPER'); await window.mootool.saveFavorite({ ...editing, value, kind: 'color', folderId }); setEditing(null); await load() })}>{t('common.save')}</button></>}>
      {editing && <><div className="color-favorite-preview"><span style={{ background: previewColor(editing.value) }} /><strong>{editing.value}</strong></div>
        <label className="color-favorite-field"><span>{t('common.name')}</span><input aria-label={t('common.name')} value={editing.name} onChange={event => setEditing({ ...editing, name: event.target.value })} /></label>
        <label className="color-favorite-field"><span>HEX / RGB</span><input aria-label="HEX / RGB" value={editing.value} onChange={event => setEditing({ ...editing, value: event.target.value })} /></label>
        <label className="color-favorite-field"><span>{words[7]}</span><input aria-label={words[7]} value={editing.description} onChange={event => setEditing({ ...editing, description: event.target.value })} /></label>
        {error && <p role="alert" className="result-status--error">{error}</p>}</>}
    </Dialog>
  </>
}
function previewColor(value: string) {
  try { return formatColor(parseColor(value), 'HEX_UPPER') } catch { return 'transparent' }
}
