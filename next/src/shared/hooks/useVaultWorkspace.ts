import { useEffect, useEffectEvent, useState } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import type { VaultKind, VaultWorkspace } from '@/shared/contracts/vaultWorkspace'
import { useToast } from '@/shared/feedback/ToastProvider'

export function useVaultWorkspace<File>(
  kind: VaultKind,
  open: () => Promise<VaultWorkspace<File>>,
  activePath: string,
  onReset: (rootKey: string) => void,
  onOpen: (workspace: VaultWorkspace<File>, rootKey: string) => void
) {
  const { settings, ready: settingsReady } = useSettings()
  const toast = useToast()
  const customRoot = kind === 'json' ? settings.vault.jsonPath : settings.vault.quickNotePath
  const rootKey = JSON.stringify([kind, customRoot, customRoot ? '' : settings.data.directory])
  const [loaded, setLoaded] = useState<{ key: string; rootDirectory: string } | null>(null)
  const reset = useEffectEvent(onReset)
  const accept = useEffectEvent(onOpen)
  const ready = settingsReady && loaded?.key === rootKey

  useEffect(() => {
    if (!settingsReady) return
    let cancelled = false
    reset(rootKey)
    void open().then((workspace) => {
      if (cancelled) return
      accept(workspace, rootKey)
      setLoaded({ key: rootKey, rootDirectory: workspace.rootDirectory })
    }).catch((error) => { if (!cancelled) toast.error(error instanceof Error ? error.message : String(error)) })
    return () => { cancelled = true }
  }, [open, rootKey, settingsReady, toast])

  useEffect(() => {
    if (!ready || !activePath || !loaded) return
    void window.mootool.rememberVaultFile({ kind, rootDirectory: loaded.rootDirectory, relativePath: activePath })
      .catch((error) => toast.error(error instanceof Error ? error.message : String(error)))
  }, [activePath, kind, loaded, ready, toast])

  return { ready, rootKey }
}
