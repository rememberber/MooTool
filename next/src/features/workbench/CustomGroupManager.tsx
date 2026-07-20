import { FolderPlus, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { toolById, toolGroups } from '@/app/toolRegistry'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import type { CustomToolGroup } from '@/shared/contracts/settings'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type CustomGroupManagerProps = {
  open: boolean
  onClose: () => void
}

export function CustomGroupManager({ open, onClose }: CustomGroupManagerProps) {
  const { settings, updateSettings } = useSettings()
  const { t } = useI18n()
  const toast = useToast()
  const [groups, setGroups] = useState<CustomToolGroup[]>([])
  const [selectedId, setSelectedId] = useState<string>()
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    const nextGroups = settings.layout.customGroups.map((group) => ({ ...group, toolIds: [...group.toolIds] }))
    setGroups(nextGroups)
    setSelectedId(nextGroups[0]?.id)
  }, [open, settings.layout.customGroups])

  const selectedGroup = groups.find((group) => group.id === selectedId)
  const invalidGroup = useMemo(() => groups.find((group) => !group.name.trim() || group.toolIds.length === 0), [groups])

  function createGroup(): void {
    const id = crypto.randomUUID()
    const group: CustomToolGroup = {
      id,
      name: t('app.group.manage.defaultName', { number: String(groups.length + 1) }),
      toolIds: []
    }
    setGroups((current) => [...current, group])
    setSelectedId(id)
  }

  function updateSelected(patch: Partial<Pick<CustomToolGroup, 'name' | 'toolIds'>>): void {
    if (!selectedId) return
    setGroups((current) => current.map((group) => group.id === selectedId ? { ...group, ...patch } : group))
  }

  function toggleTool(toolId: CustomToolGroup['toolIds'][number]): void {
    if (!selectedGroup) return
    const toolIds = selectedGroup.toolIds.includes(toolId)
      ? selectedGroup.toolIds.filter((id) => id !== toolId)
      : [...selectedGroup.toolIds, toolId]
    updateSelected({ toolIds })
  }

  function deleteSelected(): void {
    if (!selectedGroup || !window.confirm(t('app.group.manage.deleteConfirm', { name: selectedGroup.name }))) return
    const selectedIndex = groups.findIndex((group) => group.id === selectedGroup.id)
    const nextGroups = groups.filter((group) => group.id !== selectedGroup.id)
    setGroups(nextGroups)
    setSelectedId(nextGroups[Math.min(selectedIndex, nextGroups.length - 1)]?.id)
  }

  async function save(): Promise<void> {
    if (invalidGroup || saving) return
    setSaving(true)
    try {
      await updateSettings({
        layout: {
          customGroups: groups.map((group) => ({ ...group, name: group.name.trim(), toolIds: [...group.toolIds] }))
        }
      })
      onClose()
    } catch {
      toast.error(t('app.group.manage.saveFailed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog
      title={t('app.group.manage.title')}
      open={open}
      width={720}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button dialog-button--danger" type="button" disabled={!selectedGroup || saving} onClick={deleteSelected}>
            <Trash2 size={14} />{t('app.group.manage.delete')}
          </button>
          <button className="dialog-button" type="button" disabled={saving} onClick={onClose}>{t('common.cancel')}</button>
          <button className="dialog-button dialog-button--primary" type="button" disabled={Boolean(invalidGroup) || saving} onClick={() => { void save() }}>
            {t('common.save')}
          </button>
        </>
      )}
    >
      <div className="custom-group-manager">
        <aside className="custom-group-manager__list">
          <button className="custom-group-manager__new" type="button" onClick={createGroup}>
            <FolderPlus size={15} />{t('app.group.manage.new')}
          </button>
          <div className="custom-group-manager__groups">
            {groups.length === 0 ? (
              <p>{t('app.group.manage.empty')}</p>
            ) : groups.map((group) => (
              <button
                className={group.id === selectedId ? 'custom-group-manager__group custom-group-manager__group--active' : 'custom-group-manager__group'}
                type="button"
                key={group.id}
                onClick={() => setSelectedId(group.id)}
              >
                <span>{group.name || t('app.group.manage.name')}</span>
                <small>{group.toolIds.length}</small>
              </button>
            ))}
          </div>
        </aside>

        <section className="custom-group-manager__editor">
          {selectedGroup ? (
            <>
              <label className="custom-group-manager__name">
                <span>{t('app.group.manage.name')}</span>
                <input autoFocus value={selectedGroup.name} maxLength={64} onChange={(event) => updateSelected({ name: event.target.value })} />
                {!selectedGroup.name.trim() && <small>{t('app.group.manage.nameRequired')}</small>}
              </label>
              <fieldset>
                <legend>{t('app.group.manage.tools')}</legend>
                <div className="custom-group-manager__tools">
                  {toolGroups.map((group) => (
                    <section key={group.id}>
                      <h3>{t(group.titleKey)}</h3>
                      {group.toolIds.map((toolId) => {
                        const tool = toolById.get(toolId)!
                        const Icon = tool.icon
                        return (
                          <label key={toolId}>
                            <input type="checkbox" checked={selectedGroup.toolIds.includes(toolId)} onChange={() => toggleTool(toolId)} />
                            <Icon size={15} />
                            <span>{t(tool.titleKey)}</span>
                          </label>
                        )
                      })}
                    </section>
                  ))}
                </div>
                {selectedGroup.toolIds.length === 0 && <small className="custom-group-manager__validation">{t('app.group.manage.toolRequired')}</small>}
              </fieldset>
            </>
          ) : (
            <div className="custom-group-manager__placeholder">{t('app.group.manage.empty')}</div>
          )}
        </section>
      </div>
    </Dialog>
  )
}
