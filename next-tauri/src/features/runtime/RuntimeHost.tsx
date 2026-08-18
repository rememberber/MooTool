import { lazy, Suspense } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { ManagedToolHost } from '../toolWebview/ManagedToolHost'
import { runtimeMessages } from './runtimeMessages'
const RuntimeSurface = lazy(async () => ({ default: (await import('./RuntimeSurface')).RuntimeSurface }))
export function RuntimeHost({ active }: { active: boolean }) { const { t } = useLocalizedMessages(runtimeMessages); return <ManagedToolHost active={active} toolId="runtime" title={t('title')}><Suspense fallback={<div className="tool-webview-placeholder">{t('host.loading')}</div>}><RuntimeSurface /></Suspense></ManagedToolHost> }
