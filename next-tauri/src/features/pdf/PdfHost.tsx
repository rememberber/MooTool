import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { pdfMessages } from './pdfMessages'

const PdfSurface = lazy(async () => ({ default: (await import('./PdfSurface')).PdfSurface }))

export function PdfHost({ active }: { active: boolean }) {
  const { t } = useLocalizedMessages(pdfMessages)
  return <ManagedToolHost active={active} toolId="pdf" title={t('title')}><Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}><PdfSurface /></Suspense></ManagedToolHost>
}
