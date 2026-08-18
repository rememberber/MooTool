import { defineMessages } from '../../app/localizedMessages'

export const uaMessages = defineMessages({
  'zh-CN': {
    'title': 'UA 分析', 'session.label': '会话', 'session.summary': '{browser} · {os} · {device}',
    'action.copy': '复制', 'action.copied': '已复制', 'action.clear': '清空', 'action.parse': '解析 User-Agent',
    'input.label': 'User-Agent 输入', 'result.correct': '请修正 User-Agent',
    'fact.browser': '浏览器', 'fact.engine': '渲染引擎', 'fact.os': '操作系统', 'fact.cpu': 'CPU 架构',
    'fact.deviceType': '设备类型', 'fact.device': '设备', 'value.unknown': '未知',
    'device.bot': '机器人', 'device.mobile': '移动设备', 'device.tablet': '平板', 'device.wearable': '可穿戴设备',
    'device.console': '游戏主机', 'device.smarttv': '智能电视', 'device.embedded': '嵌入式设备', 'device.desktop': '桌面设备',
    'status.ready': '浏览器、引擎、系统、设备与 Bot 分类已解析', 'status.local': 'UAParser.js 1.x · MIT · 本地处理',
    'error.empty': 'User-Agent 不能为空', 'error.tooLong': 'User-Agent 长度不能超过 16 KiB',
    'error.copyFailed': '复制失败，请检查剪贴板权限', 'report.error': 'UA 状态上报失败：{error}',
    'host.loading': '正在加载 UA 分析…'
  },
  'en-US': {
    'title': 'UA Analyzer', 'session.label': 'Session', 'session.summary': '{browser} · {os} · {device}',
    'action.copy': 'Copy', 'action.copied': 'Copied', 'action.clear': 'Clear', 'action.parse': 'Parse User-Agent',
    'input.label': 'User-Agent input', 'result.correct': 'Correct the User-Agent',
    'fact.browser': 'Browser', 'fact.engine': 'Rendering engine', 'fact.os': 'Operating system', 'fact.cpu': 'CPU architecture',
    'fact.deviceType': 'Device type', 'fact.device': 'Device', 'value.unknown': 'Unknown',
    'device.bot': 'Bot', 'device.mobile': 'Mobile', 'device.tablet': 'Tablet', 'device.wearable': 'Wearable',
    'device.console': 'Console', 'device.smarttv': 'Smart TV', 'device.embedded': 'Embedded', 'device.desktop': 'Desktop',
    'status.ready': 'Browser, engine, system, device, and Bot classification parsed', 'status.local': 'UAParser.js 1.x · MIT · local processing',
    'error.empty': 'User-Agent cannot be empty', 'error.tooLong': 'User-Agent cannot exceed 16 KiB',
    'error.copyFailed': 'Copy failed. Check clipboard permission.', 'report.error': 'UA status reporting failed: {error}',
    'host.loading': 'Loading UA analyzer…'
  },
  'ja-JP': {
    'title': 'UA 解析', 'session.label': 'セッション', 'session.summary': '{browser} · {os} · {device}',
    'action.copy': 'コピー', 'action.copied': 'コピー済み', 'action.clear': 'クリア', 'action.parse': 'User-Agent を解析',
    'input.label': 'User-Agent 入力', 'result.correct': 'User-Agent を修正してください',
    'fact.browser': 'ブラウザー', 'fact.engine': 'レンダリングエンジン', 'fact.os': 'オペレーティングシステム', 'fact.cpu': 'CPU アーキテクチャ',
    'fact.deviceType': 'デバイス種別', 'fact.device': 'デバイス', 'value.unknown': '不明',
    'device.bot': 'ボット', 'device.mobile': 'モバイル', 'device.tablet': 'タブレット', 'device.wearable': 'ウェアラブル',
    'device.console': 'ゲーム機', 'device.smarttv': 'スマートテレビ', 'device.embedded': '組み込み機器', 'device.desktop': 'デスクトップ',
    'status.ready': 'ブラウザー、エンジン、システム、デバイス、Bot 分類を解析しました', 'status.local': 'UAParser.js 1.x · MIT · ローカル処理',
    'error.empty': 'User-Agent は空にできません', 'error.tooLong': 'User-Agent は 16 KiB 以下にしてください',
    'error.copyFailed': 'コピーに失敗しました。クリップボード権限を確認してください。', 'report.error': 'UA 状態の報告に失敗しました：{error}',
    'host.loading': 'UA 解析を読み込み中…'
  }
})
