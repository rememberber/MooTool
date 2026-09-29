export const regexMessages = {
  'zh-CN': { engine: '正则引擎', java: 'Java 兼容', js: 'JavaScript', hint: 'Java 兼容模式需要 JDK 11+；可在设置 → 运行环境中配置路径。', limited: '结果达到显示上限，仅展示部分匹配。', timeout: '匹配超时，已停止。请简化表达式或缩小文本范围。', previous: '上一个匹配', next: '下一个匹配', working: '匹配中…' },
  'en-US': { engine: 'Regex engine', java: 'Java compatible', js: 'JavaScript', hint: 'Java mode requires JDK 11+. Configure its path in Settings → Runtimes.', limited: 'Display limit reached. Only part of the matches are shown.', timeout: 'Matching timed out and was stopped. Simplify the pattern or shorten the text.', previous: 'Previous match', next: 'Next match', working: 'Matching…' },
  'ja-JP': { engine: '正規表現エンジン', java: 'Java 互換', js: 'JavaScript', hint: 'Java モードには JDK 11+ が必要です。設定 → 実行環境でパスを指定できます。', limited: '表示上限に達しました。一部の一致のみ表示しています。', timeout: '処理がタイムアウトしたため停止しました。式またはテキストを短くしてください。', previous: '前の一致', next: '次の一致', working: '検索中…' }
} as const
