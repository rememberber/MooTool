import { ArrowLeftRight, Equal, History } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { combination, convertBase, evaluateExpression, gcd, lcm, permutation } from './calculatorTools'

export function CalculatorTool() {
  const { t } = useI18n()
  const actions = useToolActions('calculator')
  const [expression, setExpression] = useState('2 * (3 + 4)')
  const [result, setResult] = useState('14')
  const [decimal, setDecimal] = useState('255')
  const [hex, setHex] = useState('ff')
  const [binary, setBinary] = useState('11111111')
  const [first, setFirst] = useState('54')
  const [second, setSecond] = useState('24')
  const [n, setN] = useState('5')
  const [m, setM] = useState('2')
  const [log, setLog] = useState<string[]>(['2 * (3 + 4) = 14'])
  const [historyOpen, setHistoryOpen] = useState(false)

  function run(summary: string, input: string, calculate: () => string): void {
    try {
      const output = calculate()
      setResult(output)
      setLog((items) => [`${summary}: ${input} = ${output}`, ...items].slice(0, 12))
      void actions.saveHistory(summary, input, output)
    } catch (error) {
      actions.reportError(error)
    }
  }

  function evaluate(): void {
    run(t('calculator.expression'), expression, () => evaluateExpression(expression))
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('calculator.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell calculator-workspace">
        <section className="calculator-expression">
          <label htmlFor="calculator-expression">{t('calculator.expression')}</label>
          <div><input id="calculator-expression" value={expression} onChange={(event) => setExpression(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }} /><button className="primary-command" type="button" onClick={evaluate}><Equal size={16} />{t('calculator.calculate')}</button></div>
          <output>{result}</output>
        </section>
        <ResizableColumns className="calculator-grid" columns={3} defaultSizes={[1, 1, 0.8]} minPaneWidths={[220, 220, 200]} minimumWidth={760} storageKey="calculator-panels">
          <section className="operation-panel">
            <h2>{t('calculator.base')}</h2>
            <LabeledInput label={t('calculator.decimal')} value={decimal} onChange={setDecimal} />
            <div className="inline-command-row"><button type="button" onClick={() => run('DEC → HEX', decimal, () => { const value = convertBase(decimal, 10, 16); setHex(value); return value })}>DEC → HEX</button><button type="button" onClick={() => run('DEC → BIN', decimal, () => { const value = convertBase(decimal, 10, 2); setBinary(value); return value })}>DEC → BIN</button></div>
            <LabeledInput label={t('calculator.hex')} value={hex} onChange={setHex} />
            <button className="panel-command" type="button" onClick={() => run('HEX → DEC', hex, () => { const value = convertBase(hex, 16, 10); setDecimal(value); return value })}>HEX → DEC</button>
            <LabeledInput label={t('calculator.binary')} value={binary} onChange={setBinary} />
            <button className="panel-command" type="button" onClick={() => run('BIN → DEC', binary, () => { const value = convertBase(binary, 2, 10); setDecimal(value); return value })}>BIN → DEC</button>
          </section>
          <section className="operation-panel">
            <h2>{t('calculator.number')}</h2>
            <div className="paired-inputs"><LabeledInput label={t('calculator.first')} value={first} onChange={setFirst} /><LabeledInput label={t('calculator.second')} value={second} onChange={setSecond} /></div>
            <div className="inline-command-row"><button type="button" onClick={() => run(t('calculator.gcd'), `${first}, ${second}`, () => gcd(first, second))}>{t('calculator.gcd')}</button><button type="button" onClick={() => run(t('calculator.lcm'), `${first}, ${second}`, () => lcm(first, second))}>{t('calculator.lcm')}</button></div>
            <div className="paired-inputs"><LabeledInput label={t('calculator.n')} value={n} onChange={setN} /><LabeledInput label={t('calculator.m')} value={m} onChange={setM} /></div>
            <div className="inline-command-row"><button type="button" onClick={() => run(t('calculator.permutation'), `${n}, ${m}`, () => permutation(n, m))}>{t('calculator.permutation')}</button><button type="button" onClick={() => run(t('calculator.combination'), `${n}, ${m}`, () => combination(n, m))}>{t('calculator.combination')}</button></div>
          </section>
          <section className="calculator-log"><h2>{t('calculator.history')}</h2>{log.map((item, index) => <p key={`${index}-${item}`}>{item}</p>)}</section>
        </ResizableColumns>
      </div>
      <HistoryDialog funcType="calculator" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setResult(value)} onApplyRecord={(record) => { setExpression(record.inputText); setResult(record.outputText) }} />
    </section>
  )
}

function LabeledInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <label className="labeled-input"><span>{label}</span><input value={value} onChange={(event) => onChange(event.target.value)} /></label>
}
