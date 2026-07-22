import { ArrowDown, ArrowUp, Equal, History } from 'lucide-react'
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
  const [gcdFirst, setGcdFirst] = useState('54')
  const [gcdSecond, setGcdSecond] = useState('24')
  const [lcmFirst, setLcmFirst] = useState('54')
  const [lcmSecond, setLcmSecond] = useState('24')
  const [permutationN, setPermutationN] = useState('5')
  const [permutationM, setPermutationM] = useState('2')
  const [combinationN, setCombinationN] = useState('5')
  const [combinationM, setCombinationM] = useState('2')
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
        <ResizableColumns className="calculator-layout" columns={2} defaultSizes={[1, 1]} minPaneWidths={[360, 320]} minimumWidth={680} storageKey="calculator-panels">
          <div className="calculator-controls">
            <section className="calculator-section calculator-arithmetic">
              <h2>{t('calculator.arithmetic')}</h2>
              <div className="calculator-expression-row">
                <input id="calculator-expression" aria-label={t('calculator.expression')} value={expression} onChange={(event) => setExpression(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }} />
                <button className="primary-command" type="button" onClick={evaluate}><Equal size={16} />{t('calculator.calculate')}</button>
              </div>
            </section>

            <section className="calculator-section calculator-base-panel">
              <h2>{t('calculator.base')}</h2>
              <div className="calculator-base-grid">
                <label htmlFor="calculator-hex">{t('calculator.hex')}</label>
                <input id="calculator-hex" value={hex} onChange={(event) => setHex(event.target.value)} />
                <div className="calculator-base-actions">
                  <button className="panel-command" type="button" aria-label="HEX → DEC" onClick={() => run('HEX → DEC', hex, () => { const value = convertBase(hex, 16, 10); setDecimal(value); return value })}><ArrowDown size={15} />{t('common.convert')}</button>
                  <button className="panel-command" type="button" aria-label="DEC → HEX" onClick={() => run('DEC → HEX', decimal, () => { const value = convertBase(decimal, 10, 16); setHex(value); return value })}><ArrowUp size={15} />{t('common.convert')}</button>
                </div>
                <label htmlFor="calculator-decimal">{t('calculator.decimal')}</label>
                <input id="calculator-decimal" value={decimal} onChange={(event) => setDecimal(event.target.value)} />
                <div className="calculator-base-actions">
                  <button className="panel-command" type="button" aria-label="DEC → BIN" onClick={() => run('DEC → BIN', decimal, () => { const value = convertBase(decimal, 10, 2); setBinary(value); return value })}><ArrowDown size={15} />{t('common.convert')}</button>
                  <button className="panel-command" type="button" aria-label="BIN → DEC" onClick={() => run('BIN → DEC', binary, () => { const value = convertBase(binary, 2, 10); setDecimal(value); return value })}><ArrowUp size={15} />{t('common.convert')}</button>
                </div>
                <label htmlFor="calculator-binary">{t('calculator.binary')}</label>
                <input id="calculator-binary" value={binary} onChange={(event) => setBinary(event.target.value)} />
              </div>
            </section>

            <CalculatorOperation title={t('calculator.gcd')} firstLabel={t('calculator.first')} secondLabel={t('calculator.second')} first={gcdFirst} second={gcdSecond} setFirst={setGcdFirst} setSecond={setGcdSecond} actionLabel={t('calculator.calculateGcd')} onAction={() => run(t('calculator.gcd'), `${gcdFirst}, ${gcdSecond}`, () => gcd(gcdFirst, gcdSecond))} />
            <CalculatorOperation title={t('calculator.lcm')} firstLabel={t('calculator.first')} secondLabel={t('calculator.second')} first={lcmFirst} second={lcmSecond} setFirst={setLcmFirst} setSecond={setLcmSecond} actionLabel={t('calculator.calculateLcm')} onAction={() => run(t('calculator.lcm'), `${lcmFirst}, ${lcmSecond}`, () => lcm(lcmFirst, lcmSecond))} />
            <CalculatorOperation title={t('calculator.permutation')} firstLabel={t('calculator.n')} secondLabel={t('calculator.m')} first={permutationN} second={permutationM} setFirst={setPermutationN} setSecond={setPermutationM} actionLabel="A(n,m)" onAction={() => run(t('calculator.permutation'), `${permutationN}, ${permutationM}`, () => permutation(permutationN, permutationM))} />
            <CalculatorOperation title={t('calculator.combination')} firstLabel={t('calculator.n')} secondLabel={t('calculator.m')} first={combinationN} second={combinationM} setFirst={setCombinationN} setSecond={setCombinationM} actionLabel="C(n,m)" onAction={() => run(t('calculator.combination'), `${combinationN}, ${combinationM}`, () => combination(combinationN, combinationM))} />
          </div>

          <section className="calculator-output-panel">
            <output className="calculator-result" aria-label={t('common.result')}>{result}</output>
            <div className="calculator-log"><h2>{t('calculator.history')}</h2>{log.map((item, index) => <p key={`${index}-${item}`}>{item}</p>)}</div>
          </section>
        </ResizableColumns>
      </div>
      <HistoryDialog funcType="calculator" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setResult(value)} onApplyRecord={(record) => { setExpression(record.inputText); setResult(record.outputText) }} />
    </section>
  )
}

function LabeledInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <label className="labeled-input"><span>{label}</span><input value={value} onChange={(event) => onChange(event.target.value)} /></label>
}

function CalculatorOperation({ title, firstLabel, secondLabel, first, second, setFirst, setSecond, actionLabel, onAction }: {
  title: string
  firstLabel: string
  secondLabel: string
  first: string
  second: string
  setFirst: (value: string) => void
  setSecond: (value: string) => void
  actionLabel: string
  onAction: () => void
}) {
  return (
    <section className="calculator-section calculator-operation-panel">
      <h2>{title}</h2>
      <div className="calculator-operation-row">
        <LabeledInput label={firstLabel} value={first} onChange={setFirst} />
        <LabeledInput label={secondLabel} value={second} onChange={setSecond} />
        <button className="panel-command" type="button" onClick={onAction}>{actionLabel}</button>
      </div>
    </section>
  )
}
