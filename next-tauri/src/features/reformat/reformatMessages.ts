import { defineMessages } from '../../app/localizedMessages'

export const reformatMessages = defineMessages({
  'zh-CN': {
    'title': '格式化', 'session.label': '会话', 'session.summary': '{type} · {length} 字符 · {indent} 空格',
    'toolbar.label': '格式化操作', 'type.label': '格式化类型', 'indent.label': '缩进',
    'action.format': '格式化', 'action.formatting': '格式化中…', 'action.import': '导入文件',
    'action.copy': '复制结果', 'action.copied': '已复制', 'action.clear': '清空', 'action.export': '导出结果',
    'editor.pending': '待格式化内容', 'editor.original': '原文', 'editor.result': '格式化结果', 'editor.stats': '{lines} 行 · {length} 字符',
    'editor.label': '{type} 格式化编辑器', 'notice.ready': '{type} 格式化器已就绪',
    'notice.formatted': '{type} 格式化完成', 'notice.loaded': '已载入 {file}', 'notice.cleared': '内容已清空', 'notice.exported': '格式化结果已导出：{path}',
    'notice.copyFailed': '复制失败，请检查剪贴板权限', 'status.local': '本地处理 · Prettier / MooTool Nginx formatter',
    'error.unclosedBlock': 'Nginx 配置中的大括号未闭合', 'error.unexpectedClosingBrace': 'Nginx 配置包含多余的右大括号',
    'error.unclosedString': 'Nginx 配置中的字符串未闭合', 'report.error': '格式化状态上报失败：{error}',
    'host.loading': '正在加载格式化工具…'
  },
  'en-US': {
    'title': 'Formatter', 'session.label': 'Session', 'session.summary': '{type} · {length} characters · {indent} spaces',
    'toolbar.label': 'Formatting actions', 'type.label': 'Format type', 'indent.label': 'Indent',
    'action.format': 'Format', 'action.formatting': 'Formatting…', 'action.import': 'Import file',
    'action.copy': 'Copy result', 'action.copied': 'Copied', 'action.clear': 'Clear', 'action.export': 'Export result',
    'editor.pending': 'Content to format', 'editor.original': 'Original', 'editor.result': 'Formatted result', 'editor.stats': '{lines} lines · {length} characters',
    'editor.label': '{type} formatter editor', 'notice.ready': '{type} formatter is ready',
    'notice.formatted': '{type} formatting completed', 'notice.loaded': 'Loaded {file}', 'notice.cleared': 'Content cleared', 'notice.exported': 'Formatted result exported: {path}',
    'notice.copyFailed': 'Copy failed. Check clipboard permission.', 'status.local': 'Local processing · Prettier / MooTool Nginx formatter',
    'error.unclosedBlock': 'The braces in the Nginx configuration are not closed', 'error.unexpectedClosingBrace': 'The Nginx configuration contains an extra closing brace',
    'error.unclosedString': 'A string in the Nginx configuration is not closed', 'report.error': 'Formatter status reporting failed: {error}',
    'host.loading': 'Loading formatter…'
  },
  'ja-JP': {
    'title': 'フォーマッター', 'session.label': 'セッション', 'session.summary': '{type} · {length} 文字 · {indent} スペース',
    'toolbar.label': 'フォーマット操作', 'type.label': 'フォーマット種別', 'indent.label': 'インデント',
    'action.format': 'フォーマット', 'action.formatting': 'フォーマット中…', 'action.import': 'ファイルを読み込む',
    'action.copy': '結果をコピー', 'action.copied': 'コピー済み', 'action.clear': 'クリア', 'action.export': '結果を書き出す',
    'editor.pending': 'フォーマットする内容', 'editor.original': '元の内容', 'editor.result': 'フォーマット結果', 'editor.stats': '{lines} 行 · {length} 文字',
    'editor.label': '{type} フォーマッターエディター', 'notice.ready': '{type} フォーマッターの準備ができました',
    'notice.formatted': '{type} のフォーマットが完了しました', 'notice.loaded': '{file} を読み込みました', 'notice.cleared': '内容をクリアしました', 'notice.exported': 'フォーマット結果を書き出しました：{path}',
    'notice.copyFailed': 'コピーに失敗しました。クリップボード権限を確認してください。', 'status.local': 'ローカル処理 · Prettier / MooTool Nginx formatter',
    'error.unclosedBlock': 'Nginx 設定の波括弧が閉じられていません', 'error.unexpectedClosingBrace': 'Nginx 設定に余分な閉じ波括弧があります',
    'error.unclosedString': 'Nginx 設定の文字列が閉じられていません', 'report.error': 'フォーマッター状態の報告に失敗しました：{error}',
    'host.loading': 'フォーマッターを読み込み中…'
  }
})
