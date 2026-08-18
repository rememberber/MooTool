import { ArrowDown, ArrowUp, FolderPlus, Save, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useI18n } from '../../app/i18n'
import { productToolCatalog, toolGroups } from '../../app/toolCatalog'
import type { CustomToolGroup } from '../../platform/contracts/settings'
import { errorMessage } from '../../shared/errors'
import { useSettings } from '../settings/SettingsProvider'

export function CustomGroupDialog({ open, onClose }: { open: boolean; onClose(): void }) {
  const { settings, save, ready } = useSettings()
  const { t, toolTitle, groupTitle } = useI18n()
  const [groups, setGroups] = useState<CustomToolGroup[]>([])
  const [selectedId, setSelectedId] = useState<string>()
  const [busy, setBusy] = useState(false)
  const [status, setStatus] = useState('')

  useEffect(() => {
    if (!open) return
    const next = settings.layout.customGroups.map((group) => ({ ...group, toolIds: [...group.toolIds] }))
    setGroups(next)
    setSelectedId(next[0]?.id)
    setStatus('')
  }, [open, settings.layout.customGroups])

  const selected = groups.find((group) => group.id === selectedId)
  const invalid = useMemo(() => groups.some((group) => (
    !group.name.trim() || group.name.trim().length > 40 || group.toolIds.length === 0
  )), [groups])

  if (!open) return null

  function createGroup(): void {
    if (groups.length >= 12) return
    const group: CustomToolGroup = {
      id: `group-${crypto.randomUUID()}`,
      name: t('groups.defaultName', { number: groups.length + 1 }),
      toolIds: []
    }
    setGroups((current) => [...current, group])
    setSelectedId(group.id)
  }

  function updateSelected(patch: Partial<Pick<CustomToolGroup, 'name' | 'toolIds'>>): void {
    if (!selected) return
    setGroups((current) => current.map((group) => group.id === selected.id
      ? { ...group, ...patch }
      : group))
  }

  function toggleTool(toolId: string): void {
    if (!selected) return
    updateSelected({
      toolIds: selected.toolIds.includes(toolId)
        ? selected.toolIds.filter((item) => item !== toolId)
        : [...selected.toolIds, toolId]
    })
  }

  function moveSelected(offset: -1 | 1): void {
    if (!selected) return
    const index = groups.findIndex((group) => group.id === selected.id)
    const target = index + offset
    if (target < 0 || target >= groups.length) return
    const next = [...groups]
    ;[next[index], next[target]] = [next[target], next[index]]
    setGroups(next)
  }

  function deleteSelected(): void {
    if (!selected) return
    const index = groups.findIndex((group) => group.id === selected.id)
    const next = groups.filter((group) => group.id !== selected.id)
    setGroups(next)
    setSelectedId(next[Math.min(index, next.length - 1)]?.id)
  }

  async function persist(): Promise<void> {
    if (!ready || invalid) return
    setBusy(true)
    setStatus('')
    try {
      await save((current) => ({
        ...current,
        layout: {
          ...current.layout,
          customGroups: groups.map((group) => ({
            ...group,
            name: group.name.trim(),
            toolIds: [...group.toolIds]
          }))
        }
      }))
      onClose()
    } catch (cause) {
      setStatus(`${t('groups.saveFailed')}: ${errorMessage(cause)}`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="custom-groups-backdrop" role="presentation">
      <section className="custom-groups-dialog" role="dialog" aria-modal="true" aria-labelledby="custom-groups-title">
        <header>
          <div>
            <span className="eyebrow">TAURI WORKSPACE</span>
            <h2 id="custom-groups-title">{t('groups.title')}</h2>
            <p>{t('groups.subtitle')}</p>
          </div>
          <button className="icon-button" type="button" aria-label={t('groups.cancel')} onClick={onClose}><X /></button>
        </header>

        <div className="custom-groups-layout">
          <aside>
            <button className="secondary-button custom-groups-add" type="button" disabled={groups.length >= 12} onClick={createGroup}>
              <FolderPlus />{t('groups.add')}
            </button>
            {groups.length >= 12 && <small>{t('groups.maximum')}</small>}
            <div className="custom-groups-list">
              {groups.length === 0
                ? <p>{t('groups.empty')}</p>
                : groups.map((group, index) => (
                  <button
                    className={group.id === selectedId ? 'custom-groups-item custom-groups-item--active' : 'custom-groups-item'}
                    type="button"
                    key={group.id}
                    onClick={() => setSelectedId(group.id)}
                  >
                    <strong>{group.name || t('groups.unnamed')}</strong>
                    <span>{t('groups.selectedCount', { count: group.toolIds.length })}</span>
                    <small>{index + 1}</small>
                  </button>
                ))}
            </div>
          </aside>

          <div className="custom-groups-editor">
            {selected ? (
              <>
                <div className="custom-groups-name-row">
                  <label>
                    <span>{t('groups.name')}</span>
                    <input maxLength={40} value={selected.name} onChange={(event) => updateSelected({ name: event.target.value })} />
                  </label>
                  <div>
                    <button type="button" title={t('groups.moveUp')} disabled={groups[0]?.id === selected.id} onClick={() => moveSelected(-1)}><ArrowUp /></button>
                    <button type="button" title={t('groups.moveDown')} disabled={groups.at(-1)?.id === selected.id} onClick={() => moveSelected(1)}><ArrowDown /></button>
                    <button type="button" title={t('groups.delete')} onClick={deleteSelected}><Trash2 /></button>
                  </div>
                </div>
                {!selected.name.trim() && <small className="custom-groups-validation">{t('groups.nameRequired')}</small>}
                <div className="custom-groups-tools">
                  {toolGroups.map((builtInGroup) => (
                    <section key={builtInGroup}>
                      <h3>{groupTitle(builtInGroup)}</h3>
                      <div>
                        {productToolCatalog.filter((tool) => tool.group === builtInGroup).map((tool) => (
                          <label key={tool.id}>
                            <input type="checkbox" checked={selected.toolIds.includes(tool.id)} onChange={() => toggleTool(tool.id)} />
                            <tool.icon />
                            <span>{toolTitle(tool)}</span>
                          </label>
                        ))}
                      </div>
                    </section>
                  ))}
                </div>
                {selected.toolIds.length === 0 && <small className="custom-groups-validation">{t('groups.toolRequired')}</small>}
              </>
            ) : <div className="custom-groups-placeholder">{t('groups.empty')}</div>}
          </div>
        </div>

        <footer>
          <span className="custom-groups-status">{status}</span>
          <button className="secondary-button" type="button" disabled={busy} onClick={onClose}>{t('groups.cancel')}</button>
          <button className="primary-button" type="button" disabled={busy || invalid} onClick={() => void persist()}><Save />{t('groups.save')}</button>
        </footer>
      </section>
    </div>
  )
}
