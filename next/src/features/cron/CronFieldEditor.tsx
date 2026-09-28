import { useState } from 'react'
import type { CronFields } from './cronTools'

export const cronLabels = {
  'zh-CN': { every: '每个值', unspecified: '不指定', range: '范围', step: '从指定值开始，每隔', list: '指定值（可多选）', nearest: '最近工作日', last: '每月最后一天', lastWork: '每月最后工作日', nth: '每月第 N 个星期', lastWeek: '每月最后一个星期', custom: '自定义', from: '开始', to: '结束', interval: '间隔', weekday: '星期（1=周日，7=周六）', ordinal: '第几次', resolve: '反解析到界面', examples: '常用表达式', locale: '描述语言', convert: '转自然语言', noRuns: '没有未来执行时间', copy: '复制表达式' },
  'en-US': { every: 'Every value', unspecified: 'Unspecified', range: 'Range', step: 'Start / interval', list: 'Select values', nearest: 'Nearest weekday', last: 'Last day of month', lastWork: 'Last weekday of month', nth: 'Nth weekday of month', lastWeek: 'Last weekday occurrence', custom: 'Custom', from: 'Start', to: 'End', interval: 'Interval', weekday: 'Weekday (1=Sun, 7=Sat)', ordinal: 'Occurrence', resolve: 'Resolve to builder', examples: 'Common expressions', locale: 'Description language', convert: 'Describe', noRuns: 'No future executions', copy: 'Copy expression' },
  'ja-JP': { every: 'すべて', unspecified: '指定なし', range: '範囲', step: '開始値 / 間隔', list: '値を選択（複数可）', nearest: '最寄りの平日', last: '月末日', lastWork: '月末の平日', nth: '毎月第 N 曜日', lastWeek: '毎月最後の曜日', custom: 'カスタム', from: '開始', to: '終了', interval: '間隔', weekday: '曜日（1=日、7=土）', ordinal: '第何回', resolve: '画面に反映', examples: 'よく使う式', locale: '説明の言語', convert: '自然言語に変換', noRuns: '今後の実行はありません', copy: '式をコピー' }
} as const

type Mode = 'every' | 'unspecified' | 'range' | 'step' | 'list' | 'nearest' | 'last' | 'lastWork' | 'nth' | 'lastWeek' | 'custom'
function modeOf(value: string): Mode {
  if (value === '*') return 'every'
  if (value === '?' || value === '') return 'unspecified'
  if (value === 'L') return 'last'
  if (value === 'LW') return 'lastWork'
  if (/^\d+W$/.test(value)) return 'nearest'
  if (/^\d+L$/.test(value)) return 'lastWeek'
  if (/^\d+#\d+$/.test(value)) return 'nth'
  if (/^\d+-\d+$/.test(value)) return 'range'
  if (/^\d+\/\d+$/.test(value)) return 'step'
  if (/^\d+(,\d+)*$/.test(value)) return 'list'
  return 'custom'
}

export function CronFieldEditor({ field, value, language, onChange }: {
  field: keyof CronFields; value: string; language: keyof typeof cronLabels; onChange: (value: string) => void
}) {
  const labels = cronLabels[language]
  const min = ['day', 'month', 'week'].includes(field) ? 1 : field === 'year' ? 1970 : 0
  const max = field === 'year' ? 2199 : field === 'month' ? 12 : field === 'day' ? 31 : field === 'week' ? 7 : field === 'hour' ? 23 : 59
  const [custom, setCustom] = useState(false)
  const mode = custom ? 'custom' : modeOf(value)
  const numbers = value.match(/\d+/g)?.map(Number) ?? []
  const first = numbers[0] ?? min
  const second = numbers[1] ?? (mode === 'range' ? max : 1)
  const modes: Mode[] = ['every', ...(['day', 'week', 'year'].includes(field) ? ['unspecified' as const] : []), 'range', 'step', 'list', ...(field === 'day' ? ['nearest', 'last', 'lastWork'] as const : []), ...(field === 'week' ? ['nth', 'lastWeek'] as const : []), 'custom']
  function choose(next: Mode) {
    setCustom(next === 'custom')
    const defaults: Record<Mode, string> = { every: '*', unspecified: field === 'year' ? '' : '?', range: `${min}-${max}`, step: `${min}/1`, list: String(min), nearest: '1W', last: 'L', lastWork: 'LW', nth: '2#1', lastWeek: '6L', custom: value }
    onChange(defaults[next])
  }
  function numeric(label: string, number: number, low: number, high: number, update: (number: number) => void) {
    return <label>{label}<input type="number" aria-label={label} min={low} max={high} value={number} onChange={event => { const n = Number(event.target.value); if (Number.isInteger(n) && n >= low && n <= high) update(n) }} /></label>
  }
  return <div className="cron-field-editor">
    <div className="cron-mode-options">{modes.map(item => <label key={item}><input type="radio" name={`cron-mode-${field}`} checked={mode === item} onChange={() => choose(item)} />{labels[item]}</label>)}</div>
    <div className="cron-mode-values">
      {(mode === 'range' || mode === 'step') && <>{numeric(labels.from, first, min, max, n => onChange(`${n}${mode === 'range' ? '-' : '/'}${mode === 'range' ? Math.max(n, second) : second}`))}{numeric(mode === 'range' ? labels.to : labels.interval, second, mode === 'range' ? first : 1, mode === 'range' ? max : max - min + 1, n => onChange(`${first}${mode === 'range' ? '-' : '/'}${n}`))}</>}
      {mode === 'nearest' && numeric(labels.nearest, first, 1, 31, n => onChange(`${n}W`))}
      {(mode === 'nth' || mode === 'lastWeek') && numeric(labels.weekday, first, 1, 7, n => onChange(mode === 'nth' ? `${n}#${second}` : `${n}L`))}
      {mode === 'nth' && numeric(labels.ordinal, second, 1, 5, n => onChange(`${first}#${n}`))}
      {mode === 'list' && field !== 'year' && <div className="cron-value-grid">{Array.from({ length: max - min + 1 }, (_, i) => i + min).map(n => <label key={n}><input type="checkbox" checked={numbers.includes(n)} onChange={event => { const next = event.target.checked ? [...numbers, n] : numbers.filter(v => v !== n); if (next.length) onChange(next.sort((a, b) => a - b).join(',')) }} />{n}</label>)}</div>}
      {(mode === 'custom' || (mode === 'list' && field === 'year')) && <input aria-label={labels.custom} value={value} onChange={event => onChange(event.target.value)} />}
    </div>
  </div>
}
