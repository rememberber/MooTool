import { defineMessages } from '../../app/localizedMessages'

export const timestampMessages = defineMessages({
  'zh-CN': {
    'title': '时间转换', 'session.label': '会话', 'unit.second': '秒', 'unit.millisecond': '毫秒', 'notice.ready': '时间转换器已就绪',
    'notice.toLocal': '已转换为 {zone} 本地时间', 'notice.toTimestamp': '已转换为 Unix {unit}时间戳', 'notice.now': '已使用当前时间',
    'error.clipboard': '复制失败，请检查剪贴板权限', 'error.timestampInteger': '时间戳必须是整数', 'error.safeRange': '时间戳超出安全范围',
    'error.invalidTimestamp': '无效时间戳：{detail}', 'error.localFormat': '本地时间必须使用 YYYY-MM-DD HH:mm:ss，并且在所选时区中有效',
    'error.invalidZone': '无效时区：{zone}', 'timezone.invalid': '无效', 'current.title': '当前时间 · {zone}', 'current.unix': 'Unix 秒',
    'current.local': '本地时间', 'field.timezone': 'IANA 时区', 'field.timestamp': 'Unix 时间戳', 'action.toLocal': '转本地时间',
    'action.toTimestamp': '转时间戳', 'action.now': '使用现在', 'action.copyValue': '复制{label}', 'field.local': '本地时间 · {zone}', 'detail.weekday': '星期',
    'detail.offset': 'UTC 偏移', 'footer.capabilities': 'Unix 秒/毫秒自动识别 · IANA 时区 · DST 感知',
    'report.error': '时间转换状态上报失败：{error}', 'host.loading': '正在加载时间转换…'
  },
  'en-US': {
    'title': 'Time Converter', 'session.label': 'Session', 'unit.second': 'seconds', 'unit.millisecond': 'milliseconds', 'notice.ready': 'Time converter is ready',
    'notice.toLocal': 'Converted to {zone} local time', 'notice.toTimestamp': 'Converted to Unix {unit} timestamp', 'notice.now': 'Current time applied',
    'error.clipboard': 'Copy failed; check clipboard permission', 'error.timestampInteger': 'Timestamp must be an integer', 'error.safeRange': 'Timestamp exceeds the safe range',
    'error.invalidTimestamp': 'Invalid timestamp: {detail}', 'error.localFormat': 'Local time must use YYYY-MM-DD HH:mm:ss and be valid in the selected timezone',
    'error.invalidZone': 'Invalid timezone: {zone}', 'timezone.invalid': 'invalid', 'current.title': 'Current time · {zone}', 'current.unix': 'Unix seconds',
    'current.local': 'Local time', 'field.timezone': 'IANA timezone', 'field.timestamp': 'Unix timestamp', 'action.toLocal': 'To local time',
    'action.toTimestamp': 'To timestamp', 'action.now': 'Use now', 'action.copyValue': 'Copy {label}', 'field.local': 'Local time · {zone}', 'detail.weekday': 'Weekday',
    'detail.offset': 'UTC offset', 'footer.capabilities': 'Automatic Unix second/millisecond detection · IANA timezones · DST aware',
    'report.error': 'Time Converter status reporting failed: {error}', 'host.loading': 'Loading Time Converter…'
  },
  'ja-JP': {
    'title': '時刻変換', 'session.label': 'セッション', 'unit.second': '秒', 'unit.millisecond': 'ミリ秒', 'notice.ready': '時刻変換の準備ができました',
    'notice.toLocal': '{zone} のローカル時刻へ変換しました', 'notice.toTimestamp': 'Unix {unit}タイムスタンプへ変換しました', 'notice.now': '現在時刻を使用しました',
    'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください', 'error.timestampInteger': 'タイムスタンプは整数にしてください', 'error.safeRange': 'タイムスタンプが安全な範囲を超えています',
    'error.invalidTimestamp': '無効なタイムスタンプです：{detail}', 'error.localFormat': 'ローカル時刻は YYYY-MM-DD HH:mm:ss を使用し、選択したタイムゾーンで有効な必要があります',
    'error.invalidZone': '無効なタイムゾーンです：{zone}', 'timezone.invalid': '無効', 'current.title': '現在時刻 · {zone}', 'current.unix': 'Unix 秒',
    'current.local': 'ローカル時刻', 'field.timezone': 'IANA タイムゾーン', 'field.timestamp': 'Unix タイムスタンプ', 'action.toLocal': 'ローカル時刻へ',
    'action.toTimestamp': 'タイムスタンプへ', 'action.now': '現在時刻を使用', 'action.copyValue': '{label} をコピー', 'field.local': 'ローカル時刻 · {zone}', 'detail.weekday': '曜日',
    'detail.offset': 'UTC オフセット', 'footer.capabilities': 'Unix 秒/ミリ秒の自動判定 · IANA タイムゾーン · DST 対応',
    'report.error': '時刻変換状態の報告に失敗しました：{error}', 'host.loading': '時刻変換を読み込み中…'
  }
})
