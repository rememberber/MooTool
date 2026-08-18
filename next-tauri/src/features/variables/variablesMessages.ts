import { defineMessages } from '../../app/localizedMessages'

export const variablesMessages = defineMessages({
  'zh-CN': {
    'title': '环境变量', 'session.label': '会话', 'session.summary': '{count} 项 · {sensitive} 项敏感 · {state}',
    'state.revealed': '已显示', 'state.redacted': '已脱敏', 'notice.loading': '正在读取 Tauri 进程环境…',
    'notice.revealed': '敏感变量已临时显示，请注意屏幕共享与剪贴板', 'notice.redacted': '环境变量已读取，敏感值默认脱敏',
    'error.revealFirst': '请先显式显示敏感值再复制', 'notice.copied': '{name} 已复制', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'search.placeholder': '搜索名称或值', 'summary.sensitive': '{count} 项敏感变量', 'action.redact': '重新脱敏',
    'action.reveal': '显示敏感值', 'action.refresh': '刷新', 'column.name': '名称', 'column.value': '值', 'column.action': '操作',
    'badge.sensitive': '敏感', 'action.copied': '已复制', 'action.copy': '复制', 'empty': '没有匹配的环境变量',
    'footer.capabilities': '只读 · 默认脱敏 · 不写入系统环境', 'report.error': '环境变量状态上报失败：{error}',
    'host.loading': '正在加载环境变量…'
  },
  'en-US': {
    'title': 'Environment Variables', 'session.label': 'Session', 'session.summary': '{count} entries · {sensitive} sensitive · {state}',
    'state.revealed': 'Revealed', 'state.redacted': 'Redacted', 'notice.loading': 'Reading the Tauri process environment…',
    'notice.revealed': 'Sensitive values are temporarily visible; take care with screen sharing and clipboard use', 'notice.redacted': 'Environment loaded with sensitive values redacted by default',
    'error.revealFirst': 'Explicitly reveal sensitive values before copying', 'notice.copied': '{name} copied', 'error.clipboard': 'Copy failed; check clipboard permission',
    'search.placeholder': 'Search names or values', 'summary.sensitive': '{count} sensitive entries', 'action.redact': 'Redact again',
    'action.reveal': 'Reveal sensitive values', 'action.refresh': 'Refresh', 'column.name': 'Name', 'column.value': 'Value', 'column.action': 'Action',
    'badge.sensitive': 'Sensitive', 'action.copied': 'Copied', 'action.copy': 'Copy', 'empty': 'No matching environment variables',
    'footer.capabilities': 'Read-only · Redacted by default · Never writes the system environment', 'report.error': 'Environment status reporting failed: {error}',
    'host.loading': 'Loading Environment Variables…'
  },
  'ja-JP': {
    'title': '環境変数', 'session.label': 'セッション', 'session.summary': '{count} 件 · 機密 {sensitive} 件 · {state}',
    'state.revealed': '表示中', 'state.redacted': '秘匿済み', 'notice.loading': 'Tauri プロセス環境を読み込み中…',
    'notice.revealed': '機密変数を一時表示しています。画面共有とクリップボードに注意してください', 'notice.redacted': '環境変数を読み込みました。機密値は既定で秘匿されます',
    'error.revealFirst': 'コピーする前に機密値を明示的に表示してください', 'notice.copied': '{name} をコピーしました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'search.placeholder': '名前または値を検索', 'summary.sensitive': '機密変数 {count} 件', 'action.redact': '再び秘匿',
    'action.reveal': '機密値を表示', 'action.refresh': '更新', 'column.name': '名前', 'column.value': '値', 'column.action': '操作',
    'badge.sensitive': '機密', 'action.copied': 'コピー済み', 'action.copy': 'コピー', 'empty': '一致する環境変数はありません',
    'footer.capabilities': '読み取り専用 · 既定で秘匿 · システム環境へ書き込みません', 'report.error': '環境変数状態の報告に失敗しました：{error}',
    'host.loading': '環境変数を読み込み中…'
  }
})
