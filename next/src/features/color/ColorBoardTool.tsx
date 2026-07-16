import { ArrowLeftRight, Copy, Eye, History, Palette, Star } from 'lucide-react'
import { useMemo, useState } from 'react'
import { FavoriteDialog } from '@/features/favorites/FavoriteDialog'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  applyColorOperation,
  bestTextColor,
  colorThemes,
  formatColor,
  parseColor,
  standardColors,
  type ColorFormat,
  type ColorOperation,
  type ColorThemeId,
  type RgbColor
} from './colorTools'

type EyeDropperResult = { sRGBHex: string }
type EyeDropperConstructor = new () => { open: () => Promise<EyeDropperResult> }

export function ColorBoardTool() {
  const { t } = useI18n()
  const actions = useToolActions('colorBoard')
  const [primary, setPrimary] = useState<RgbColor>(() => parseColor('#DE8F7D'))
  const [secondary, setSecondary] = useState<RgbColor>(() => parseColor('#4F83CC'))
  const [format, setFormat] = useState<ColorFormat>('HEX_UPPER')
  const [code, setCode] = useState('#DE8F7D')
  const [theme, setTheme] = useState<ColorThemeId>('default')
  const [historyOpen, setHistoryOpen] = useState(false)
  const [favoritesOpen, setFavoritesOpen] = useState(false)
  const selectedTheme = useMemo(() => colorThemes.find((item) => item.id === theme) ?? colorThemes[0], [theme])
  const primaryHex = formatColor(primary, 'HEX_UPPER')
  const secondaryHex = formatColor(secondary, 'HEX_UPPER')

  function selectColor(value: string, asSecondary = false, operation = t('color.select')): void {
    try {
      const next = parseColor(value)
      if (asSecondary) setSecondary(next)
      else {
        const before = primaryHex
        setPrimary(next)
        setCode(formatColor(next, format))
        void actions.saveHistory(operation, before, formatColor(next, 'HEX_UPPER'), operation)
      }
    } catch (error) { actions.reportError(error) }
  }

  function changeFormat(nextFormat: ColorFormat): void {
    setFormat(nextFormat)
    setCode(formatColor(primary, nextFormat))
  }

  function applyCode(): void {
    selectColor(code, false, t('color.inputColor'))
  }

  function runOperation(operation: ColorOperation): void {
    const output = applyColorOperation(operation, primary, secondary)
    setPrimary(output)
    setCode(formatColor(output, format))
    void actions.saveHistory(t(`color.operation.${operation}` as 'color.operation.invert'), `${primaryHex} / ${secondaryHex}`, formatColor(output, 'HEX_UPPER'), operation)
  }

  function swap(): void {
    const before = `${primaryHex} / ${secondaryHex}`
    setPrimary(secondary)
    setSecondary(primary)
    setCode(formatColor(secondary, format))
    void actions.saveHistory(t('color.operation.swap'), before, `${secondaryHex} / ${primaryHex}`, 'swap')
  }

  async function pickScreenColor(): Promise<void> {
    try {
      const Constructor = (window as unknown as { EyeDropper?: EyeDropperConstructor }).EyeDropper
      if (!Constructor) throw new Error(t('color.eyeDropperUnavailable'))
      const result = await new Constructor().open()
      selectColor(result.sRGBHex, false, t('color.picker'))
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') return
      actions.reportError(error)
    }
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('color.title')} actions={<><button className="toolbar-button" type="button" onClick={() => setFavoritesOpen(true)}><Star size={14} />{t('favorite.title')}</button><button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button></>} />
      <div className="local-tool-shell color-workspace">
        <div className="p4-toolbar color-toolbar">
          <button className="dialog-button" type="button" onClick={() => { void pickScreenColor() }}><Eye size={14} />{t('color.picker')}</button>
          <label className="color-native-input"><span>{t('color.freePick')}</span><input type="color" value={primaryHex} onChange={(event) => selectColor(event.target.value, false, t('color.freePick'))} /></label>
          <label className="compact-field"><span>{t('color.format')}</span><select value={format} onChange={(event) => changeFormat(event.target.value as ColorFormat)}><option value="HEX_UPPER">HTML</option><option value="HEX_LOWER">html</option><option value="RGB">RGB</option></select></label>
          <label className="color-code-input"><span>{t('color.code')}</span><input value={code} spellCheck={false} onChange={(event) => setCode(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') applyCode() }} /></label>
          <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.action.copy')} onClick={() => { void actions.copy(code) }}><Copy size={14} /></button>
        </div>
        <ResizableColumns className="color-board-layout" columns={2} defaultSizes={[0.34, 0.66]} minPaneWidths={[240, 420]} storageKey="color-board">
          <section className="color-current-panel">
            <div className="color-preview" style={{ background: primaryHex, color: bestTextColor(primary) }}><span>{t('color.current')}</span><strong>{primaryHex}</strong><small>{formatColor(primary, 'RGB')}</small></div>
            <div className="color-compare-row"><span>{t('color.compare')}</span><label className="color-chip color-chip--large" aria-label={t('color.compare')} style={{ background: secondaryHex }}><input type="color" value={secondaryHex} aria-label={t('color.compare')} onChange={(event) => setSecondary(parseColor(event.target.value))} /></label><code>{secondaryHex}</code><button className="dialog-button" type="button" onClick={swap}><ArrowLeftRight size={14} />{t('color.operation.swap')}</button></div>
            <div className="color-operations">{(['invert', 'intersect', 'add', 'difference', 'average'] as ColorOperation[]).map((operation) => <button className="dialog-button" type="button" key={operation} onClick={() => runOperation(operation)}>{t(`color.operation.${operation}` as 'color.operation.invert')}</button>)}</div>
          </section>
          <section className="color-palette-panel">
            <header><h2>{t('color.themeColors')}</h2><label><Palette size={14} /><select aria-label={t('color.theme')} value={theme} onChange={(event) => setTheme(event.target.value as ColorThemeId)}>{colorThemes.map((item) => <option key={item.id} value={item.id}>{t(`color.theme.${item.id}` as 'color.theme.default')}</option>)}</select></label></header>
            <div className="theme-main-colors">{selectedTheme.main.map((color) => <ColorChip key={color} color={color} onSelect={(secondarySelection) => selectColor(color, secondarySelection)} />)}</div>
            <div className="theme-shade-grid">{selectedTheme.shades.flatMap((row, column) => row.map((color) => <ColorChip key={`${column}-${color}`} color={color} onSelect={(secondarySelection) => selectColor(color, secondarySelection)} />))}</div>
            <h2>{t('color.standardColors')}</h2>
            <div className="standard-colors">{standardColors.map((color) => <ColorChip key={color} color={color} onSelect={(secondarySelection) => selectColor(color, secondarySelection)} />)}</div>
            <p className="color-shift-hint">{t('color.shiftHint')}</p>
          </section>
        </ResizableColumns>
      </div>
      <FavoriteDialog kind="color" open={favoritesOpen} currentValue={primaryHex} onClose={() => setFavoritesOpen(false)} onApply={(value) => selectColor(value, false, t('favorite.title'))} />
      <HistoryDialog funcType="colorBoard" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => selectColor(value)} onApplyRecord={(record) => {
        const color = (record.outputText || record.inputText).match(/#[0-9a-fA-F]{6}/)?.[0]
        if (color) selectColor(color, false, record.summary)
      }} />
    </section>
  )
}

function ColorChip({ color, onSelect }: { color: string; onSelect: (secondary: boolean) => void }) {
  return <button className="color-chip" type="button" aria-label={color} title={color} style={{ background: color }} onClick={(event) => onSelect(event.shiftKey)} />
}
