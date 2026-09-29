import { useToolDraft } from '@/shared/hooks/useToolDraft'
import { ArrowDown, ArrowUp, Equal, History } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { combination, convertBase, evaluateExpression, gcd, lcm, permutation } from './calculatorTools'

export function CalculatorTool() {
  const { t } = useI18n()
  const actions = useToolActions('calculator')
  const { draft, setField } = useToolDraft('calculator', {
    expression: '2 * (3 + 4)',
    result: '14',
    decimal: '255',
    hex: 'ff',
    binary: '11111111',
    gcdFirst: '54',
    gcdSecond: '24',
    lcmFirst: '54',
    lcmSecond: '24',
    permutationN: '5',
    permutationM: '2',
    combinationN: '5',
    combinationM: '2',
    log: ['2 * (3 + 4) = 14']
  })
  const { expression, result, decimal, hex, binary, gcdFirst, gcdSecond, lcmFirst, lcmSecond, permutationN, permutationM, combinationN, combinationM, log } = draft
  const setExpression = (value: React.SetStateAction<typeof draft.expression>) => setField('expression', value)
  const setResult = (value: React.SetStateAction<typeof draft.result>) => setField('result', value)
  const setDecimal = (value: React.SetStateAction<typeof draft.decimal>) => setField('decimal', value)
  const setHex = (value: React.SetStateAction<typeof draft.hex>) => setField('hex', value)
  const setBinary = (value: React.SetStateAction<typeof draft.binary>) => setField('binary', value)
  const setGcdFirst = (value: React.SetStateAction<typeof draft.gcdFirst>) => setField('gcdFirst', value)
  const setGcdSecond = (value: React.SetStateAction<typeof draft.gcdSecond>) => setField('gcdSecond', value)
  const setLcmFirst = (value: React.SetStateAction<typeof draft.lcmFirst>) => setField('lcmFirst', value)
  const setLcmSecond = (value: React.SetStateAction<typeof draft.lcmSecond>) => setField('lcmSecond', value)
  const setPermutationN = (value: React.SetStateAction<typeof draft.permutationN>) => setField('permutationN', value)
  const setPermutationM = (value: React.SetStateAction<typeof draft.permutationM>) => setField('permutationM', value)
  const setCombinationN = (value: React.SetStateAction<typeof draft.combinationN>) => setField('combinationN', value)
  const setCombinationM = (value: React.SetStateAction<typeof draft.combinationM>) => setField('combinationM', value)
  const setLog = (value: React.SetStateAction<typeof draft.log>) => setField('log', value)
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
      <ToolPageHeader title={t('calculator.title')} />
      <div className="local-tool-shell calculator-workspace">
        <ResizableColumns className="calculator-layout" columns={2} defaultSizes={[1, 1]} minPaneWidths={[360, 320]} minimumWidth={680} storageKey="calculator-panels">
          <div className="calculator-controls">
            <section className="calculator-section calculator-arithmetic">
              <div className="embedded-tool-heading">
                <h2>{t('calculator.arithmetic')}</h2>
                <WorkspaceDragZone />
                <button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>
              </div>
              <div className="calculator-expression-row">
                <input id="calculator-expression" aria-label={t('calculator.expression')} value={expression} onChange={(event) => setExpression(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }} />
                <button className="primary-command calculator-evaluate-button" type="button" aria-label={t('calculator.calculate')} onClick={evaluate}><Equal size={16} /></button>
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
