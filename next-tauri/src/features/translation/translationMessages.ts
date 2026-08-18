import { defineMessages } from '../../app/localizedMessages'

export const translationMessages = defineMessages({
  'zh-CN': {
    'title': '翻译', 'session.label': '会话', 'session.summary': '{source} → {target} · {count} 字符',
    'notice.ready': '输入内容后将自动翻译', 'notice.translating': '正在翻译…', 'notice.complete': '翻译完成 · {provider}', 'notice.completeFallback': '翻译完成 · {provider}（备用源）',
    'notice.fallback': '（备用源）', 'notice.copied': '译文已复制', 'notice.copyFailed': '复制失败，请检查剪贴板权限',
    'notice.savedWord': '已收藏到 Tauri 本地单词本', 'operation.translate': '翻译文本', 'language.auto': '自动检测',
    'tab.translate': '翻译', 'tab.words': '单词本', 'tab.history': '历史记录', 'action.exchange': '交换语言与文本',
    'option.provider': '翻译源', 'action.copy': '复制译文', 'action.favorite': '收藏', 'action.clear': '清空',
    'aria.source': '待翻译文本', 'aria.result': '翻译结果', 'status.translating': '翻译中…', 'status.fallback': '备用源',
    'status.result': '译文', 'footer.capabilities': 'Rust 网络请求 · Google/Bing 故障切换 · SQLite 单词本与历史',
    'report.error': '翻译状态上报失败：{error}', 'aria.sourceLanguage': '源语言', 'aria.targetLanguage': '目标语言',
    'confirm.deleteWord': '删除当前单词？', 'search.words': '搜索单词、译文或备注', 'action.new': '新建',
    'action.delete': '删除', 'action.apply': '应用到翻译', 'field.source': '原文', 'field.target': '译文',
    'field.remark': '备注（可选）', 'action.save': '保存', 'words.empty': '选择或新建一个单词',
    'search.history': '搜索原文、译文或语言', 'confirm.clearHistory': '清空全部翻译历史？',
    'action.clearAll': '清空全部', 'history.empty': '暂无翻译历史', 'host.loading': '正在加载翻译工作台…'
  },
  'en-US': {
    'title': 'Translate', 'session.label': 'Session', 'session.summary': '{source} → {target} · {count} chars',
    'notice.ready': 'Translation starts automatically when you enter text', 'notice.translating': 'Translating…', 'notice.complete': 'Translation completed · {provider}', 'notice.completeFallback': 'Translation completed · {provider} (fallback)',
    'notice.fallback': ' (fallback)', 'notice.copied': 'Translation copied', 'notice.copyFailed': 'Copy failed; check clipboard permission',
    'notice.savedWord': 'Saved to the local Tauri word book', 'operation.translate': 'Translate text', 'language.auto': 'Auto detect',
    'tab.translate': 'Translate', 'tab.words': 'Word book', 'tab.history': 'History', 'action.exchange': 'Swap languages and text',
    'option.provider': 'Provider', 'action.copy': 'Copy translation', 'action.favorite': 'Favorite', 'action.clear': 'Clear',
    'aria.source': 'Text to translate', 'aria.result': 'Translation result', 'status.translating': 'Translating…', 'status.fallback': 'Fallback',
    'status.result': 'Translation', 'footer.capabilities': 'Rust networking · Google/Bing failover · SQLite word book and history',
    'report.error': 'Translation status reporting failed: {error}', 'aria.sourceLanguage': 'Source language', 'aria.targetLanguage': 'Target language',
    'confirm.deleteWord': 'Delete the current word?', 'search.words': 'Search words, translations, or notes', 'action.new': 'New',
    'action.delete': 'Delete', 'action.apply': 'Use in translation', 'field.source': 'Source text', 'field.target': 'Translation',
    'field.remark': 'Notes (optional)', 'action.save': 'Save', 'words.empty': 'Select or create a word',
    'search.history': 'Search source, translation, or language', 'confirm.clearHistory': 'Clear all translation history?',
    'action.clearAll': 'Clear all', 'history.empty': 'No translation history', 'host.loading': 'Loading translation workbench…'
  },
  'ja-JP': {
    'title': '翻訳', 'session.label': 'セッション', 'session.summary': '{source} → {target} · {count} 文字',
    'notice.ready': '内容を入力すると自動的に翻訳します', 'notice.translating': '翻訳中…', 'notice.complete': '翻訳完了 · {provider}', 'notice.completeFallback': '翻訳完了 · {provider}（予備ソース）',
    'notice.fallback': '（予備ソース）', 'notice.copied': '訳文をコピーしました', 'notice.copyFailed': 'コピーに失敗しました。クリップボード権限を確認してください',
    'notice.savedWord': 'Tauri ローカル単語帳へ保存しました', 'operation.translate': 'テキストを翻訳', 'language.auto': '自動検出',
    'tab.translate': '翻訳', 'tab.words': '単語帳', 'tab.history': '履歴', 'action.exchange': '言語とテキストを入れ替え',
    'option.provider': '翻訳元', 'action.copy': '訳文をコピー', 'action.favorite': 'お気に入り', 'action.clear': 'クリア',
    'aria.source': '翻訳するテキスト', 'aria.result': '翻訳結果', 'status.translating': '翻訳中…', 'status.fallback': '予備ソース',
    'status.result': '訳文', 'footer.capabilities': 'Rust ネットワーク · Google/Bing フェイルオーバー · SQLite 単語帳と履歴',
    'report.error': '翻訳状態の報告に失敗しました：{error}', 'aria.sourceLanguage': '原文の言語', 'aria.targetLanguage': '翻訳先の言語',
    'confirm.deleteWord': '現在の単語を削除しますか？', 'search.words': '単語、訳文、メモを検索', 'action.new': '新規',
    'action.delete': '削除', 'action.apply': '翻訳に適用', 'field.source': '原文', 'field.target': '訳文',
    'field.remark': 'メモ（任意）', 'action.save': '保存', 'words.empty': '単語を選択または作成してください',
    'search.history': '原文、訳文、言語を検索', 'confirm.clearHistory': '翻訳履歴をすべて消去しますか？',
    'action.clearAll': 'すべて消去', 'history.empty': '翻訳履歴はありません', 'host.loading': '翻訳ワークベンチを読み込み中…'
  }
})
