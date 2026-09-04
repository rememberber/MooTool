import { defineMessages } from '../../app/localizedMessages'

export const textDiffMessages = defineMessages({
  'zh-CN': {
    'title': '文本对比', 'session.label': '会话', 'session.summary': '{changed} 修改 · {added} 新增 · {removed} 删除',
    'sample.left': 'server:\n  port: 8080\n  compression: false\ndatabase:\n  host: localhost\n  poolSize: 8\nmessage: 你好，MooTool',
    'sample.right': 'server:\n  port: 9090\n  compression: true\ndatabase:\n  host: db.internal\n  poolSize: 8\n  timeout: 30\nmessage: 你好，MooTool Next Tauri',
    'error.clipboard': '无法写入剪贴板，请检查系统权限', 'aria.options': '文本对比选项', 'action.sample': '示例',
    'action.swap': '交换', 'action.find': '查找', 'option.whitespace': '忽略空白', 'option.case': '忽略大小写',
    'aria.viewMode': '差异视图', 'view.split': '并排', 'view.unified': '统一', 'view.unifiedHeading': '统一差异',
    'action.previousDifference': '上一个差异', 'action.nextDifference': '下一个差异',
    'option.context': '上下文', 'unit.lines': '{count} 行', 'option.all': '全部', 'action.copied': '已复制',
    'action.copyDiff': '复制 Diff', 'action.clear': '清空两侧文本', 'pane.original': '原始文本', 'pane.target': '目标文本',
    'pane.characters': '{count} 字符', 'aria.summary': '对比统计', 'summary.identical': '内容一致', 'summary.different': '发现差异',
    'summary.changed': '修改', 'summary.added': '新增', 'summary.removed': '删除', 'summary.unchanged': '未变',
    'aria.result': '文本差异结果', 'result.limit': '结果过大，已省略 {count} 行', 'result.collapsed': '已折叠 {count} 行未变内容',
    'result.empty': '在上方输入两份文本以开始对比', 'footer.capabilities': 'Myers 序列差异 · Unicode 词级高亮 · 本地处理',
    'report.error': '文本对比状态上报失败：{error}', 'row.added': '新增行', 'row.removed': '删除行',
    'host.loading': '正在加载文本对比…'
  },
  'en-US': {
    'title': 'Text Diff', 'session.label': 'Session', 'session.summary': '{changed} changed · {added} added · {removed} removed',
    'sample.left': 'server:\n  port: 8080\n  compression: false\ndatabase:\n  host: localhost\n  poolSize: 8\nmessage: Hello, MooTool',
    'sample.right': 'server:\n  port: 9090\n  compression: true\ndatabase:\n  host: db.internal\n  poolSize: 8\n  timeout: 30\nmessage: Hello, MooTool Next Tauri',
    'error.clipboard': 'Unable to write to the clipboard; check system permission', 'aria.options': 'Text comparison options', 'action.sample': 'Sample',
    'action.swap': 'Swap', 'action.find': 'Find', 'option.whitespace': 'Ignore whitespace', 'option.case': 'Ignore case',
    'aria.viewMode': 'Diff view', 'view.split': 'Split', 'view.unified': 'Unified', 'view.unifiedHeading': 'Unified diff',
    'action.previousDifference': 'Previous difference', 'action.nextDifference': 'Next difference',
    'option.context': 'Context', 'unit.lines': '{count} lines', 'option.all': 'All', 'action.copied': 'Copied',
    'action.copyDiff': 'Copy Diff', 'action.clear': 'Clear both texts', 'pane.original': 'Original', 'pane.target': 'Target',
    'pane.characters': '{count} chars', 'aria.summary': 'Comparison statistics', 'summary.identical': 'Contents match', 'summary.different': 'Differences found',
    'summary.changed': 'Changed', 'summary.added': 'Added', 'summary.removed': 'Removed', 'summary.unchanged': 'Unchanged',
    'aria.result': 'Text diff result', 'result.limit': 'Result is too large; omitted {count} lines', 'result.collapsed': 'Collapsed {count} unchanged lines',
    'result.empty': 'Enter two texts above to compare them', 'footer.capabilities': 'Myers sequence diff · Unicode word highlighting · Local processing',
    'report.error': 'Text Diff status reporting failed: {error}', 'row.added': 'Added line', 'row.removed': 'Removed line',
    'host.loading': 'Loading Text Diff…'
  },
  'ja-JP': {
    'title': 'テキスト比較', 'session.label': 'セッション', 'session.summary': '変更 {changed} · 追加 {added} · 削除 {removed}',
    'sample.left': 'server:\n  port: 8080\n  compression: false\ndatabase:\n  host: localhost\n  poolSize: 8\nmessage: こんにちは、MooTool',
    'sample.right': 'server:\n  port: 9090\n  compression: true\ndatabase:\n  host: db.internal\n  poolSize: 8\n  timeout: 30\nmessage: こんにちは、MooTool Next Tauri',
    'error.clipboard': 'クリップボードへ書き込めません。システム権限を確認してください', 'aria.options': 'テキスト比較オプション', 'action.sample': 'サンプル',
    'action.swap': '入れ替え', 'action.find': '検索', 'option.whitespace': '空白を無視', 'option.case': '大文字小文字を無視',
    'aria.viewMode': '差分表示', 'view.split': '左右', 'view.unified': '統合', 'view.unifiedHeading': '統合差分',
    'action.previousDifference': '前の差分', 'action.nextDifference': '次の差分',
    'option.context': 'コンテキスト', 'unit.lines': '{count} 行', 'option.all': 'すべて', 'action.copied': 'コピー済み',
    'action.copyDiff': 'Diff をコピー', 'action.clear': '両方のテキストを消去', 'pane.original': '元のテキスト', 'pane.target': '対象テキスト',
    'pane.characters': '{count} 文字', 'aria.summary': '比較統計', 'summary.identical': '内容は同じです', 'summary.different': '差分があります',
    'summary.changed': '変更', 'summary.added': '追加', 'summary.removed': '削除', 'summary.unchanged': '変更なし',
    'aria.result': 'テキスト差分結果', 'result.limit': '結果が大きすぎるため {count} 行を省略しました', 'result.collapsed': '変更のない {count} 行を折りたたみました',
    'result.empty': '上に 2 つのテキストを入力して比較してください', 'footer.capabilities': 'Myers シーケンス差分 · Unicode 単語ハイライト · ローカル処理',
    'report.error': 'テキスト比較状態の報告に失敗しました：{error}', 'row.added': '追加行', 'row.removed': '削除行',
    'host.loading': 'テキスト比較を読み込み中…'
  }
})
