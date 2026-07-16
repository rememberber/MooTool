import { ArrowLeft, ArrowRight } from 'lucide-react'
import { useMemo, useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { TextPane, ToolHeaderButtons, ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { asciiToText, fromUnicode, hexToText, textToAscii, textToHex, toUnicode, urlDecode, urlEncode, type AsciiFormat, type UrlCharset } from './encodeTools'

type EncodeTab = 'unicode' | 'url' | 'hex' | 'ascii'
type Pair = { left: string; right: string }

const initialPairs: Record<EncodeTab, Pair> = {
  unicode: { left: 'MooTool 编码转换', right: '' },
  url: { left: 'https://mootool.app/search?q=编码', right: '' },
  hex: { left: 'MooTool', right: '' },
  ascii: { left: 'MooTool', right: '' }
}

export function EncodeTool() {
  const { t } = useI18n()
  const actions = useToolActions('encode')
  const [tab, setTab] = useState<EncodeTab>('unicode')
  const [pairs, setPairs] = useState(initialPairs)
  const [charset, setCharset] = useState<UrlCharset>('utf-8')
  const [asciiFormat, setAsciiFormat] = useState<AsciiFormat>('decimal')
  const [historyOpen, setHistoryOpen] = useState(false)
  const labels = useMemo(() => getTabLabels(tab, t), [tab, t])
  const pair = pairs[tab]

  function setPair(patch: Partial<Pair>): void {
    setPairs((current) => ({ ...current, [tab]: { ...current[tab], ...patch } }))
  }

  function convert(direction: 'forward' | 'reverse'): void {
    try {
      const input = direction === 'forward' ? pair.left : pair.right
      const output = runConversion(tab, direction, input, charset, asciiFormat)
      setPair(direction === 'forward' ? { right: output } : { left: output })
      const summary = direction === 'forward' ? labels.forward : labels.reverse
      void actions.saveHistory(summary, input, output, JSON.stringify({ tab, direction, charset, asciiFormat }))
    } catch (error) {
      actions.reportError(error)
    }
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('encode.title')} actions={<ToolHeaderButtons onHistory={() => setHistoryOpen(true)} onClear={() => setPairs({ ...initialPairs, [tab]: { left: '', right: '' } })} />} />
      <div className="local-tool-shell">
        <ToolTabs tabs={[
          { id: 'unicode', label: t('encode.tab.unicode') },
          { id: 'url', label: t('encode.tab.url') },
          { id: 'hex', label: t('encode.tab.hex') },
          { id: 'ascii', label: t('encode.tab.ascii') }
        ]} active={tab} onChange={setTab} />
        <div className="io-workspace">
          <TextPane label={labels.left} value={pair.left} onChange={(left) => setPair({ left })} />
          <div className="io-actions">
            <button className="io-action-button" type="button" onClick={() => convert('forward')}><ArrowRight size={15} />{labels.forward}</button>
            <button className="io-action-button" type="button" onClick={() => convert('reverse')}><ArrowLeft size={15} />{labels.reverse}</button>
            {tab === 'url' && <label className="compact-select">{t('encode.charset')}<select value={charset} onChange={(event) => setCharset(event.target.value as UrlCharset)}><option value="utf-8">UTF-8</option><option value="gb2312">GB2312</option></select></label>}
            {tab === 'ascii' && <label className="compact-select">ASCII<select value={asciiFormat} onChange={(event) => setAsciiFormat(event.target.value as AsciiFormat)}><option value="decimal">{t('encode.asciiDecimal')}</option><option value="hex">{t('encode.asciiHex')}</option></select></label>}
          </div>
          <TextPane label={labels.right} value={pair.right} onChange={(right) => setPair({ right })} />
        </div>
      </div>
      <HistoryDialog
        funcType="encode"
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        onApply={() => undefined}
        onApplyRecord={(record) => {
          try {
            const meta = JSON.parse(record.extraData ?? '{}') as { tab?: EncodeTab; direction?: 'forward' | 'reverse'; charset?: UrlCharset; asciiFormat?: AsciiFormat }
            const nextTab = meta.tab ?? 'unicode'
            setTab(nextTab)
            setPairs((current) => ({ ...current, [nextTab]: meta.direction === 'reverse' ? { left: record.outputText, right: record.inputText } : { left: record.inputText, right: record.outputText } }))
            if (meta.charset) setCharset(meta.charset)
            if (meta.asciiFormat) setAsciiFormat(meta.asciiFormat)
          } catch {
            setPair({ right: record.outputText })
          }
        }}
      />
    </section>
  )
}

function runConversion(tab: EncodeTab, direction: 'forward' | 'reverse', value: string, charset: UrlCharset, asciiFormat: AsciiFormat): string {
  if (tab === 'unicode') return direction === 'forward' ? toUnicode(value) : fromUnicode(value)
  if (tab === 'url') return direction === 'forward' ? urlEncode(value, charset) : urlDecode(value, charset)
  if (tab === 'hex') return direction === 'forward' ? textToHex(value) : hexToText(value)
  return direction === 'forward' ? textToAscii(value, asciiFormat) : asciiToText(value)
}

function getTabLabels(tab: EncodeTab, t: ReturnType<typeof useI18n>['t']): { left: string; right: string; forward: string; reverse: string } {
  if (tab === 'unicode') return { left: t('encode.native'), right: t('encode.unicode'), forward: t('encode.toUnicode'), reverse: t('encode.fromUnicode') }
  if (tab === 'url') return { left: t('encode.url'), right: t('encode.encoded'), forward: t('encode.urlEncode'), reverse: t('encode.urlDecode') }
  if (tab === 'hex') return { left: t('encode.native'), right: t('encode.hex'), forward: t('encode.toHex'), reverse: t('encode.fromHex') }
  return { left: t('encode.native'), right: t('encode.ascii'), forward: t('encode.toAscii'), reverse: t('encode.fromAscii') }
}
