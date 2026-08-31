import { DatabaseBackup, FolderInput, RotateCw, ShieldCheck } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useI18n } from '../../app/i18n'
import { productImportApi } from '../../platform/api/productImportApi'
import type {
  ProductImportCounts,
  ProductImportPreview,
  ProductImportResult,
  ProductImportSource
} from '../../platform/contracts/productImport'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'

type ImportPhase = 'idle' | 'scanning' | 'importing'

export function ProductImportCard({ disabled = false }: { disabled?: boolean }) {
  const { t } = useI18n()
  const dialog = useDesktopDialog()
  const [phase, setPhase] = useState<ImportPhase>('idle')
  const [preview, setPreview] = useState<ProductImportPreview>()
  const [result, setResult] = useState<ProductImportResult>()
  const [status, setStatus] = useState('')
  const busy = disabled || phase !== 'idle'
  const entries = useMemo(() => preview ? countEntries(preview.items) : [], [preview])

  async function chooseAndPreview(sourceProduct: ProductImportSource): Promise<void> {
    const directory = await productImportApi.chooseSourceDirectory()
    if (!directory) return
    setPhase('scanning')
    setStatus('')
    setPreview(undefined)
    setResult(undefined)
    try {
      setPreview(await productImportApi.preview(sourceProduct, directory))
    } catch (cause) {
      setStatus(`${t('settings.importScanFailed')}: ${errorMessage(cause, 'settings.product-import.preview')}`)
    } finally {
      setPhase('idle')
    }
  }

  async function runImport(): Promise<void> {
    if (!preview || preview.alreadyImported) return
    const product = preview.sourceProduct === 'java'
      ? t('settings.importJavaName')
      : t('settings.importElectronName')
    if (!await dialog.confirm(t('settings.importConfirm', { product, count: preview.totalItems }))) return
    setPhase('importing')
    setStatus('')
    try {
      const next = await productImportApi.run(
        preview.sourceProduct,
        preview.sourceDirectory,
        preview.fingerprint
      )
      setResult(next)
      setPreview({ ...next.preview, alreadyImported: true })
      setStatus(t('settings.importCompleted', { count: total(next.imported) }))
    } catch (cause) {
      setStatus(`${t('settings.importFailed')}: ${errorMessage(cause, 'settings.product-import.run')}`)
    } finally {
      setPhase('idle')
    }
  }

  return (
    <div className="settings-import-card">
      <div className="settings-import-heading">
        <div><strong>{t('settings.importTitle')}</strong><span>{t('settings.importBody')}</span></div>
        <ShieldCheck />
      </div>
      <div className="settings-import-actions">
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void chooseAndPreview('java')}>
          <FolderInput />{t('settings.importJava')}
        </button>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => void chooseAndPreview('nextElectron')}>
          <FolderInput />{t('settings.importElectron')}
        </button>
        {phase !== 'idle' && <span><RotateCw className="spin" />{phase === 'scanning' ? t('settings.importScanning') : t('settings.importRunning')}</span>}
      </div>

      {preview && (
        <div className="settings-import-preview">
          <header>
            <div>
              <strong>{preview.sourceProduct === 'java' ? t('settings.importJavaName') : t('settings.importElectronName')}</strong>
              <code title={preview.sourceDirectory}>{preview.sourceDirectory}</code>
            </div>
            <span>{t('settings.importItemCount', { count: preview.totalItems })}</span>
          </header>
          <div className="settings-import-counts">
            {entries.length
              ? entries.map(([key, count]) => <span key={key}>{t(`settings.importCount.${key}`)} <strong>{count}</strong></span>)
              : <span>{t('settings.importEmpty')}</span>}
          </div>
          <ul>
            {preview.warnings.map((warning) => <li key={warning}>{warningText(warning, t)}</li>)}
          </ul>
          {preview.alreadyImported
            ? <div className="settings-import-duplicate">{t('settings.importAlready')}</div>
            : <button className="primary-button" type="button" disabled={busy || preview.totalItems === 0} onClick={() => void runImport()}>
              <DatabaseBackup />{t('settings.importStart')}
            </button>}
        </div>
      )}

      {result && (
        <div className="settings-import-result">
          <strong>{t('settings.importResultTitle')}</strong>
          <span>{t('settings.importBackupPath')}<code>{result.backupPath}</code></span>
          <span>{t('settings.importReportPath')}<code>{result.reportPath}</code></span>
          {result.importedVaultPath && <span>{t('settings.importVaultPath')}<code>{result.importedVaultPath}</code></span>}
        </div>
      )}
      {status && <div className="settings-import-status" aria-live="polite">{status}</div>}
    </div>
  )
}

type CountKey = keyof ProductImportCounts

function countEntries(counts: ProductImportCounts): Array<[CountKey, number]> {
  return (Object.entries(counts) as Array<[CountKey, number]>).filter(([, count]) => count > 0)
}

function total(counts: ProductImportCounts): number {
  return Object.values(counts).reduce((sum, count) => sum + count, 0)
}

function warningText(warning: string, t: ReturnType<typeof useI18n>['t']): string {
  const known: Record<string, Parameters<typeof t>[0]> = {
    secretsSkipped: 'settings.importWarningSecrets',
    sourceRemainsReadOnly: 'settings.importWarningReadOnly',
    databaseNotFound: 'settings.importWarningDatabase',
    settingsNotFound: 'settings.importWarningSettings'
  }
  return known[warning] ? t(known[warning]) : warning
}
