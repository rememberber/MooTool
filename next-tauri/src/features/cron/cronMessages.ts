import { defineMessages } from '../../app/localizedMessages'

export const cronMessages = defineMessages({
  'zh-CN': {
    'session.label': '会话', 'session.summary': '{expression} · {count} 个执行时间', 'action.parse': '解析', 'builder.title': '字段构建器',
    'field.second': '秒', 'field.minute': '分', 'field.hour': '时', 'field.day': '日', 'field.month': '月', 'field.week': '周',
    'field.year': '年（可选）', 'preset.minute': '每分钟', 'preset.hour': '每小时', 'preset.day': '每天午夜',
    'preset.weekdays': '工作日 09:00', 'preset.month': '每月 1 日', 'description.empty': '点击“解析”查看自然语言说明',
    'runs.title': '未来 10 次执行时间', 'runs.empty': '解析有效表达式后显示执行计划', 'status.error': '表达式需要修正',
    'status.ready': 'Quartz 6/7 字段 · IANA 时区', 'error.requiredFields': 'Cron 的前 6 个字段不能为空',
    'error.fieldCount': 'Quartz Cron 必须包含 6 或 7 个字段', 'error.runCount': '执行时间数量必须在 1–50 之间',
    'error.invalidZone': '无效时区：{zone}', 'error.insufficientRuns': '支持的年份范围内没有足够的执行时间',
    'report.error': 'Cron 状态上报失败：{error}', 'host.loading': '正在加载 Cron…'
  },
  'en-US': {
    'session.label': 'Session', 'session.summary': '{expression} · {count} run times', 'action.parse': 'Parse', 'builder.title': 'Field builder',
    'field.second': 'Second', 'field.minute': 'Minute', 'field.hour': 'Hour', 'field.day': 'Day', 'field.month': 'Month', 'field.week': 'Weekday',
    'field.year': 'Year (optional)', 'preset.minute': 'Every minute', 'preset.hour': 'Every hour', 'preset.day': 'Daily at midnight',
    'preset.weekdays': 'Weekdays at 09:00', 'preset.month': 'First day of each month', 'description.empty': 'Click “Parse” for a natural-language description',
    'runs.title': 'Next 10 run times', 'runs.empty': 'Parse a valid expression to display its schedule', 'status.error': 'The expression needs correction',
    'status.ready': 'Quartz 6/7 fields · IANA timezone', 'error.requiredFields': 'The first 6 Cron fields cannot be empty',
    'error.fieldCount': 'Quartz Cron must contain 6 or 7 fields', 'error.runCount': 'Run count must be from 1 to 50',
    'error.invalidZone': 'Invalid timezone: {zone}', 'error.insufficientRuns': 'Not enough run times exist in the supported year range',
    'report.error': 'Cron status reporting failed: {error}', 'host.loading': 'Loading Cron…'
  },
  'ja-JP': {
    'session.label': 'セッション', 'session.summary': '{expression} · 実行時刻 {count} 件', 'action.parse': '解析', 'builder.title': 'フィールドビルダー',
    'field.second': '秒', 'field.minute': '分', 'field.hour': '時', 'field.day': '日', 'field.month': '月', 'field.week': '曜日',
    'field.year': '年（任意）', 'preset.minute': '毎分', 'preset.hour': '毎時', 'preset.day': '毎日午前 0 時',
    'preset.weekdays': '平日 09:00', 'preset.month': '毎月 1 日', 'description.empty': '「解析」をクリックすると自然言語の説明を表示します',
    'runs.title': '次の 10 回の実行時刻', 'runs.empty': '有効な式を解析すると実行予定を表示します', 'status.error': '式を修正してください',
    'status.ready': 'Quartz 6/7 フィールド · IANA タイムゾーン', 'error.requiredFields': 'Cron の先頭 6 フィールドを空にはできません',
    'error.fieldCount': 'Quartz Cron は 6 または 7 フィールド必要です', 'error.runCount': '実行時刻の件数は 1～50 にしてください',
    'error.invalidZone': '無効なタイムゾーンです：{zone}', 'error.insufficientRuns': '対応する年の範囲内に十分な実行時刻がありません',
    'report.error': 'Cron 状態の報告に失敗しました：{error}', 'host.loading': 'Cron を読み込み中…'
  }
})
