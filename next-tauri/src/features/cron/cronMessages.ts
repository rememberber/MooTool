import { defineMessages } from '../../app/localizedMessages'

export const cronFieldLabels = {
  'zh-CN': { every: '每个值', unspecified: '不指定', range: '范围', step: '从指定值开始，每隔', list: '指定值（可多选）', nearest: '最近工作日', last: '每月最后一天', lastWork: '每月最后工作日', nth: '每月第 N 个星期', lastWeek: '每月最后一个星期', custom: '自定义', from: '开始', to: '结束', interval: '间隔', weekday: '星期（1=周日，7=周六）', ordinal: '第几次' },
  'en-US': { every: 'Every value', unspecified: 'Unspecified', range: 'Range', step: 'Start / interval', list: 'Select values', nearest: 'Nearest weekday', last: 'Last day of month', lastWork: 'Last weekday of month', nth: 'Nth weekday of month', lastWeek: 'Last weekday occurrence', custom: 'Custom', from: 'Start', to: 'End', interval: 'Interval', weekday: 'Weekday (1=Sun, 7=Sat)', ordinal: 'Occurrence' },
  'ja-JP': { every: 'すべて', unspecified: '指定なし', range: '範囲', step: '開始値 / 間隔', list: '値を選択（複数可）', nearest: '最寄りの平日', last: '月末日', lastWork: '月末の平日', nth: '毎月第 N 曜日', lastWeek: '毎月最後の曜日', custom: 'カスタム', from: '開始', to: '終了', interval: '間隔', weekday: '曜日（1=日、7=土）', ordinal: '第何回' }
} as const

export const cronMessages = defineMessages({
  'zh-CN': {
    'session.label': '会话', 'session.summary': '{expression} · {count} 个执行时间', 'action.parse': '解析', 'builder.title': '字段构建器',
    'field.second': '秒', 'field.minute': '分', 'field.hour': '时', 'field.day': '日', 'field.month': '月', 'field.week': '周',
    'field.year': '年（可选）', 'preset.minute': '每分钟', 'preset.hour': '每小时', 'preset.day': '每天午夜',
    'preset.weekdays': '工作日 09:00', 'preset.month': '每月 1 日', 'description.empty': '点击“解析”查看自然语言说明',
    'action.examples': '常用表达式', 'action.resolve': '反解析到界面', 'action.convert': '转自然语言', 'action.copy': '复制表达式', 'description.locale': '描述语言',
    'language.zh': '中文', 'language.en': 'English', 'language.ja': '日本語',
    'runs.title': '最近 10 次运行时间', 'runs.history': '最近 10 次运行时间', 'runs.empty': '没有未来执行时间', 'error.invalid': '无法解析 Cron：{message}', 'status.error': '表达式需要修正', 'action.history': '历史', 'favorite.title': '收藏夹',
    'status.ready': 'Quartz 6/7 字段 · IANA 时区', 'error.requiredFields': 'Cron 的前 6 个字段不能为空',
    'error.fieldCount': 'Quartz Cron 必须包含 6 或 7 个字段', 'error.runCount': '执行时间数量必须在 1–50 之间',
    'error.invalidZone': '无效时区：{zone}', 'error.insufficientRuns': '支持的年份范围内没有足够的执行时间',
    'error.lastOffset': '月末偏移必须在 0–30 天之间',
    'report.error': 'Cron 状态上报失败：{error}', 'host.loading': '正在加载 Cron…'
  },
  'en-US': {
    'session.label': 'Session', 'session.summary': '{expression} · {count} run times', 'action.parse': 'Parse', 'builder.title': 'Field builder',
    'field.second': 'Second', 'field.minute': 'Minute', 'field.hour': 'Hour', 'field.day': 'Day', 'field.month': 'Month', 'field.week': 'Weekday',
    'field.year': 'Year (optional)', 'preset.minute': 'Every minute', 'preset.hour': 'Every hour', 'preset.day': 'Daily at midnight',
    'preset.weekdays': 'Weekdays at 09:00', 'preset.month': 'First day of each month', 'description.empty': 'Click “Parse” for a natural-language description',
    'action.examples': 'Common expressions', 'action.resolve': 'Resolve to builder', 'action.convert': 'Describe', 'action.copy': 'Copy expression', 'description.locale': 'Description language',
    'language.zh': '中文', 'language.en': 'English', 'language.ja': '日本語',
    'runs.title': 'Next 10 runs', 'runs.history': 'Next 10 runs', 'runs.empty': 'No future executions', 'error.invalid': 'Cannot parse Cron: {message}', 'status.error': 'The expression needs correction', 'action.history': 'History', 'favorite.title': 'Favorites',
    'status.ready': 'Quartz 6/7 fields · IANA timezone', 'error.requiredFields': 'The first 6 Cron fields cannot be empty',
    'error.fieldCount': 'Quartz Cron must contain 6 or 7 fields', 'error.runCount': 'Run count must be from 1 to 50',
    'error.invalidZone': 'Invalid timezone: {zone}', 'error.insufficientRuns': 'Not enough run times exist in the supported year range',
    'error.lastOffset': 'Last-day offset must be from 0 to 30',
    'report.error': 'Cron status reporting failed: {error}', 'host.loading': 'Loading Cron…'
  },
  'ja-JP': {
    'session.label': 'セッション', 'session.summary': '{expression} · 実行時刻 {count} 件', 'action.parse': '解析', 'builder.title': 'フィールドビルダー',
    'field.second': '秒', 'field.minute': '分', 'field.hour': '時', 'field.day': '日', 'field.month': '月', 'field.week': '曜日',
    'field.year': '年（任意）', 'preset.minute': '毎分', 'preset.hour': '毎時', 'preset.day': '毎日午前 0 時',
    'preset.weekdays': '平日 09:00', 'preset.month': '毎月 1 日', 'description.empty': '「解析」をクリックすると自然言語の説明を表示します',
    'action.examples': 'よく使う式', 'action.resolve': '画面に反映', 'action.convert': '自然言語に変換', 'action.copy': '式をコピー', 'description.locale': '説明の言語',
    'language.zh': '中文', 'language.en': 'English', 'language.ja': '日本語',
    'runs.title': '次の 10 回', 'runs.history': '次の 10 回', 'runs.empty': '今後の実行はありません', 'error.invalid': 'Cron を解析できません: {message}', 'status.error': '式を修正してください', 'action.history': '履歴', 'favorite.title': 'お気に入り',
    'status.ready': 'Quartz 6/7 フィールド · IANA タイムゾーン', 'error.requiredFields': 'Cron の先頭 6 フィールドを空にはできません',
    'error.fieldCount': 'Quartz Cron は 6 または 7 フィールド必要です', 'error.runCount': '実行時刻の件数は 1～50 にしてください',
    'error.invalidZone': '無効なタイムゾーンです：{zone}', 'error.insufficientRuns': '対応する年の範囲内に十分な実行時刻がありません',
    'error.lastOffset': '月末からの日数は 0～30 にしてください',
    'report.error': 'Cron 状態の報告に失敗しました：{error}', 'host.loading': 'Cron を読み込み中…'
  }
})
