import { lazy, StrictMode, Suspense } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import { useLocalizedMessages } from './app/localizedMessages'
import { surfaceMessages } from './app/surfaceMessages'
import { CalculatorToolSurface } from './features/calculator/CalculatorToolSurface'
import { SettingsProvider } from './features/settings/SettingsProvider'
import { SettingsSurface } from './features/settings/SettingsSurface'
import { ToolProbePage } from './features/webviewLab/ToolProbePage'
import './styles.css'

const surface = new URLSearchParams(window.location.search).get('surface')
const EditorLabToolSurface = lazy(async () => {
  const module = await import('./features/editorLab/EditorLabToolSurface')
  return { default: module.EditorLabToolSurface }
})
const JsonToolSurface = lazy(async () => {
  const module = await import('./features/json/JsonToolSurface')
  return { default: module.JsonToolSurface }
})
const TextDiffSurface = lazy(async () => {
  const module = await import('./features/textDiff/TextDiffSurface')
  return { default: module.TextDiffSurface }
})
const ReformatSurface = lazy(async () => {
  const module = await import('./features/reformat/ReformatSurface')
  return { default: module.ReformatSurface }
})
const EncodeSurface = lazy(async () => {
  const module = await import('./features/encode/EncodeSurface')
  return { default: module.EncodeSurface }
})
const RegexSurface = lazy(async () => {
  const module = await import('./features/regex/RegexSurface')
  return { default: module.RegexSurface }
})
const ConfigSurface = lazy(async () => {
  const module = await import('./features/config/ConfigSurface')
  return { default: module.ConfigSurface }
})
const CronSurface = lazy(async () => {
  const module = await import('./features/cron/CronSurface')
  return { default: module.CronSurface }
})
const TimestampSurface = lazy(async () => {
  const module = await import('./features/timestamp/TimestampSurface')
  return { default: module.TimestampSurface }
})
const UaSurface = lazy(async () => {
  const module = await import('./features/ua/UaSurface')
  return { default: module.UaSurface }
})
const CryptoSurface = lazy(async () => {
  const module = await import('./features/crypto/CryptoSurface')
  return { default: module.CryptoSurface }
})
const QrcodeSurface = lazy(async () => {
  const module = await import('./features/qrcode/QrcodeSurface')
  return { default: module.QrcodeSurface }
})
const ProtobufSurface = lazy(async () => {
  const module = await import('./features/protobuf/ProtobufSurface')
  return { default: module.ProtobufSurface }
})
const ColorSurface = lazy(async () => {
  const module = await import('./features/color/ColorSurface')
  return { default: module.ColorSurface }
})
const QuickNoteSurface = lazy(async () => {
  const module = await import('./features/quickNote/QuickNoteSurface')
  return { default: module.QuickNoteSurface }
})
const MessageBoardSurface = lazy(async () => {
  const module = await import('./features/messageBoard/MessageBoardSurface')
  return { default: module.MessageBoardSurface }
})
const NetworkSurface = lazy(async () => {
  const module = await import('./features/network/NetworkSurface')
  return { default: module.NetworkSurface }
})
const VariablesSurface = lazy(async () => {
  const module = await import('./features/variables/VariablesSurface')
  return { default: module.VariablesSurface }
})
const SystemSurface = lazy(async () => {
  const module = await import('./features/system/SystemSurface')
  return { default: module.SystemSurface }
})
const HostSurface = lazy(async () => ({ default: (await import('./features/host/HostSurface')).HostSurface }))
const HttpSurface = lazy(async () => ({ default: (await import('./features/http/HttpSurface')).HttpSurface }))
const RuntimeSurface = lazy(async () => ({ default: (await import('./features/runtime/RuntimeSurface')).RuntimeSurface }))
const TranslationSurface = lazy(async () => ({ default: (await import('./features/translation/TranslationSurface')).TranslationSurface }))
const ImageSurface = lazy(async () => ({ default: (await import('./features/image/ImageSurface')).ImageSurface }))
const PdfSurface = lazy(async () => ({ default: (await import('./features/pdf/PdfSurface')).PdfSurface }))

function SurfaceFallback() {
  const { t } = useLocalizedMessages(surfaceMessages)
  return <div className="tool-webview-placeholder">{t('loading')}</div>
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SettingsProvider>
      {surface === 'calculator'
        ? <CalculatorToolSurface />
        : surface === 'editor-lab'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <EditorLabToolSurface />
              </Suspense>
            )
        : surface === 'json'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <JsonToolSurface />
              </Suspense>
            )
        : surface === 'settings'
          ? <SettingsSurface />
        : surface === 'text-diff'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <TextDiffSurface />
              </Suspense>
            )
        : surface === 'reformat'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <ReformatSurface />
              </Suspense>
            )
        : surface === 'encode'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <EncodeSurface />
              </Suspense>
            )
        : surface === 'regex'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <RegexSurface />
              </Suspense>
            )
        : surface === 'config'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <ConfigSurface />
              </Suspense>
            )
        : surface === 'cron'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <CronSurface />
              </Suspense>
            )
        : surface === 'timestamp'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <TimestampSurface />
              </Suspense>
            )
        : surface === 'ua'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <UaSurface />
              </Suspense>
            )
        : surface === 'crypto'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <CryptoSurface />
              </Suspense>
            )
        : surface === 'qrcode'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <QrcodeSurface />
              </Suspense>
            )
        : surface === 'protobuf'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <ProtobufSurface />
              </Suspense>
            )
        : surface === 'color'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <ColorSurface />
              </Suspense>
            )
        : surface === 'quick-note'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <QuickNoteSurface />
              </Suspense>
            )
        : surface === 'message-board'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <MessageBoardSurface />
              </Suspense>
            )
        : surface === 'network'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <NetworkSurface />
              </Suspense>
            )
        : surface === 'variables'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <VariablesSurface />
              </Suspense>
            )
        : surface === 'system'
          ? (
              <Suspense fallback={<SurfaceFallback />}>
                <SystemSurface />
              </Suspense>
            )
        : surface === 'host' ? <Suspense fallback={<SurfaceFallback />}><HostSurface /></Suspense>
        : surface === 'http' ? <Suspense fallback={<SurfaceFallback />}><HttpSurface /></Suspense>
        : surface === 'runtime' ? <Suspense fallback={<SurfaceFallback />}><RuntimeSurface /></Suspense>
        : surface === 'translation' ? <Suspense fallback={<SurfaceFallback />}><TranslationSurface /></Suspense>
        : surface === 'image' ? <Suspense fallback={<SurfaceFallback />}><ImageSurface /></Suspense>
        : surface === 'pdf' ? <Suspense fallback={<SurfaceFallback />}><PdfSurface /></Suspense>
        : surface === 'tool-probe'
          ? <ToolProbePage />
          : <App />}
    </SettingsProvider>
  </StrictMode>
)
