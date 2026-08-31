import { Download, RefreshCw, RotateCw, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useI18n } from '../../app/i18n'
import { runtimeApi } from '../../platform/api/runtimeApi'
import { productUpdateApi } from '../../platform/api/updateApi'
import type { RuntimeInfo } from '../../platform/contracts/runtime'
import type { ProductUpdateCheck, ProductUpdateEvent } from '../../platform/contracts/update'
import { errorMessage, toProductError } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'

type UpdatePhase = 'idle' | 'checking' | 'installing' | 'cancelling' | 'installed'

export function UpdateCard({ disabled = false }: { disabled?: boolean }) {
  const { t } = useI18n()
  const dialog = useDesktopDialog()
  const [runtime, setRuntime] = useState<RuntimeInfo>()
  const [result, setResult] = useState<ProductUpdateCheck>()
  const [phase, setPhase] = useState<UpdatePhase>('idle')
  const [status, setStatus] = useState('')
  const [downloaded, setDownloaded] = useState(0)
  const [total, setTotal] = useState<number | null>(null)

  useEffect(() => {
    void runtimeApi.getInfo().then(setRuntime).catch((cause: unknown) => {
      setStatus(errorMessage(cause, 'settings.update.runtime'))
    })
  }, [])

  const percent = useMemo(() => total && total > 0
    ? Math.min(100, Math.round((downloaded / total) * 100))
    : null, [downloaded, total])
  const busy = disabled || phase === 'checking' || phase === 'installing' || phase === 'cancelling'

  async function checkForUpdates(): Promise<void> {
    setPhase('checking')
    setStatus('')
    setResult(undefined)
    try {
      const next = await productUpdateApi.check()
      setResult(next)
    } catch (cause) {
      setStatus(`${t('settings.updateFailed')}: ${errorMessage(cause, 'settings.update.check')}`)
    } finally {
      setPhase('idle')
    }
  }

  async function installAndRestart(): Promise<void> {
    if (!result || result.status !== 'available') return
    if (!await dialog.confirm(t('settings.updateConfirm', { version: result.latestVersion ?? '' }))) return
    setPhase('installing')
    setStatus('')
    setDownloaded(0)
    setTotal(null)
    try {
      await productUpdateApi.install(handleProgress)
      setPhase('installed')
      setStatus(t('settings.updateInstalled'))
      await productUpdateApi.relaunch()
    } catch (cause) {
      const error = toProductError(cause)
      if (error.code === 'cancelled') {
        setStatus(t('settings.updateCancelled'))
      } else {
        setStatus(`${t('settings.updateInstallFailed')}: ${errorMessage(cause, 'settings.update.install')}`)
      }
      setPhase('idle')
    }
  }

  function handleProgress(event: ProductUpdateEvent): void {
    if (event.event === 'progress') {
      setDownloaded(event.data.downloadedBytes)
      setTotal(event.data.contentLength)
    } else if (event.event === 'cancelled') {
      setStatus(t('settings.updateCancelled'))
    } else if (event.event === 'installed') {
      setStatus(t('settings.updateInstalled'))
    }
  }

  async function cancelUpdate(): Promise<void> {
    setPhase('cancelling')
    try {
      const cancelled = await productUpdateApi.cancel()
      if (!cancelled) setPhase('installing')
    } catch (cause) {
      setStatus(errorMessage(cause, 'settings.update.cancel'))
      setPhase('installing')
    }
  }

  return (
    <div className="settings-update-card">
      <div className="settings-update-summary">
        <div>
          <strong>{t('settings.updateTitle')}</strong>
          <span>{t('settings.updateBody')}</span>
        </div>
        <div className="settings-update-version">
          <span>{t('settings.currentVersion')}</span>
          <strong>{runtime?.version ?? '…'}</strong>
          {runtime && <small>{runtime.platform} · {runtime.architecture}</small>}
        </div>
      </div>

      <div className="settings-update-actions">
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void checkForUpdates()}>
          <RefreshCw className={phase === 'checking' ? 'spin' : undefined} />
          {phase === 'checking' ? t('settings.updateChecking') : t('settings.updateCheck')}
        </button>
        {result?.status === 'available' && phase !== 'installing' && phase !== 'cancelling' && (
          <button className="primary-button" type="button" disabled={disabled} onClick={() => void installAndRestart()}>
            <Download />{t('settings.updateInstallRestart')}
          </button>
        )}
        {(phase === 'installing' || phase === 'cancelling') && (
          <button className="secondary-button" type="button" disabled={phase === 'cancelling'} onClick={() => void cancelUpdate()}>
            <X />{phase === 'cancelling' ? t('settings.updateCancelling') : t('settings.updateCancel')}
          </button>
        )}
      </div>

      {result && (
        <div className={`settings-update-result settings-update-result--${result.status}`} aria-live="polite">
          <strong>{result.status === 'available'
            ? t('settings.updateAvailable', { version: result.latestVersion ?? '' })
            : result.status === 'upToDate'
              ? t('settings.updateLatest')
              : t('settings.updateInactive')}</strong>
          {result.publishedAt && <small>{new Date(result.publishedAt).toLocaleString()}</small>}
          {result.releaseNotes && <pre>{result.releaseNotes}</pre>}
        </div>
      )}

      {(phase === 'installing' || phase === 'cancelling') && (
        <div className="settings-update-progress" aria-live="polite">
          <div><span style={{ width: `${percent ?? 8}%` }} /></div>
          <span><RotateCw className="spin" />{percent === null
            ? t('settings.updateDownloadingBytes', { downloaded: formatBytes(downloaded) })
            : t('settings.updateDownloading', { percent })}</span>
        </div>
      )}
      {status && <div className="settings-update-status" aria-live="polite">{status}</div>}
    </div>
  )
}

function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KiB`
  return `${(value / 1024 / 1024).toFixed(1)} MiB`
}
