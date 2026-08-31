import { defineMessages } from '../../app/localizedMessages'

export const toolWebviewMessages = defineMessages({
  'zh-CN': {
    'placement.closed': '未启动', 'placement.docked': '已停靠', 'placement.detached': '独立窗口',
    'loads': '加载 {count}', 'waiting.state': '等待工具状态上报', 'action.start': '启动', 'action.detach': '分离',
    'action.dock': '收回', 'action.close': '关闭{title}', 'busy.start': '正在启动', 'busy.detach': '正在分离',
    'busy.dock': '正在收回', 'busy.close': '正在关闭', 'area': '{title}原生 WebView 区域',
    'detached.title': '{title}已分离', 'detached.detail': '同一个工具会话正在独立原生窗口中运行。',
    'closed.title': '{title}已关闭', 'closed.detail': '点击“启动”创建新的工具会话。',
    'session.label': '会话：', 'session.waiting': '等待{title}上报', 'state.label': '状态：',
    'action.manage': '管理{title}窗口', 'debug.placement': '位置', 'debug.loads': '加载次数', 'debug.session': '会话',
    'error.containerNotReady': '工具 WebView 容器尚未就绪'
  },
  'en-US': {
    'placement.closed': 'Not started', 'placement.docked': 'Docked', 'placement.detached': 'Detached window',
    'loads': 'Loads {count}', 'waiting.state': 'Waiting for tool status', 'action.start': 'Start', 'action.detach': 'Detach',
    'action.dock': 'Dock', 'action.close': 'Close {title}', 'busy.start': 'Starting', 'busy.detach': 'Detaching',
    'busy.dock': 'Docking', 'busy.close': 'Closing', 'area': '{title} native WebView area',
    'detached.title': '{title} detached', 'detached.detail': 'The same tool session is running in a separate native window.',
    'closed.title': '{title} closed', 'closed.detail': 'Click “Start” to create a new tool session.',
    'session.label': 'Session:', 'session.waiting': 'Waiting for {title} report', 'state.label': 'State:',
    'action.manage': 'Manage {title} window', 'debug.placement': 'Placement', 'debug.loads': 'Loads', 'debug.session': 'Session',
    'error.containerNotReady': 'The tool WebView container is not ready'
  },
  'ja-JP': {
    'placement.closed': '未起動', 'placement.docked': 'ドッキング中', 'placement.detached': '別ウィンドウ',
    'loads': '読み込み {count}', 'waiting.state': 'ツール状態の報告を待機中', 'action.start': '起動', 'action.detach': '切り離す',
    'action.dock': '戻す', 'action.close': '{title} を閉じる', 'busy.start': '起動中', 'busy.detach': '切り離し中',
    'busy.dock': '戻しています', 'busy.close': '終了中', 'area': '{title} ネイティブ WebView 領域',
    'detached.title': '{title} を切り離しました', 'detached.detail': '同じツールセッションが別のネイティブウィンドウで実行中です。',
    'closed.title': '{title} は閉じています', 'closed.detail': '「起動」をクリックして新しいツールセッションを作成します。',
    'session.label': 'セッション：', 'session.waiting': '{title} の報告を待機中', 'state.label': '状態：',
    'action.manage': '{title} ウィンドウを管理', 'debug.placement': '位置', 'debug.loads': '読み込み回数', 'debug.session': 'セッション',
    'error.containerNotReady': 'ツール WebView コンテナーの準備ができていません'
  }
})
