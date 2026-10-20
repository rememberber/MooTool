import { defineMessages } from '../../app/localizedMessages'

export const encodeMessages = defineMessages({
  'zh-CN': {
    'title': '编码解码', 'session.label': '会话', 'session.summary': '{tab} · {left} → {right} 字符',
    'sample.unicode': 'MooTool 编码转换 🐮', 'sample.url': 'https://mootool.app/search?q=编码',
    'sample.base64': 'MooTool Next Tauri · 你好', 'sample.hex': 'MooTool 你好', 'sample.ascii': 'MooTool 牛',
    'notice.ready': '{tab} 转换器已就绪', 'notice.done': '{action}完成', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'error.unicodeRange': 'Unicode 码点越界：{value}', 'error.urlEscape': '无效 URL 转义：{value}', 'error.base64': 'Base64 输入无效',
    'error.hex': 'Hex 输入必须由完整的十六进制字节组成', 'error.codePoint': '无效码点：{value}', 'error.codePointRange': '码点越界：{value}',
    'aria.types': '编码类型', 'tab.unicode': 'Native / Unicode', 'tab.url': 'URL 转码', 'tab.hex': 'Native / 16 进制', 'tab.ascii': 'Native / ASCII',
    'field.charset': '字符集', 'field.codePointFormat': '码点格式', 'option.decimal': '十进制',
    'option.hexadecimal': '十六进制', 'action.clear': '清空当前转换', 'action.copied': '已复制', 'action.copy': '复制',
    'pane.metrics': '{characters} 字符 · {bytes} 字节', 'unicode.left': 'Native 文本', 'unicode.right': 'Unicode 文本',
    'unicode.forward': '转为 Unicode', 'unicode.reverse': '还原 Native', 'url.left': 'URL 原文', 'url.right': '编码结果',
    'url.forward': 'URL 编码', 'url.reverse': 'URL 解码', 'base64.left': 'UTF-8 文本', 'base64.right': 'Base64',
    'base64.forward': '编码', 'base64.reverse': '解码', 'hex.left': 'Native 文本', 'hex.right': '十六进制',
    'hex.forward': '转为 Hex', 'hex.reverse': '还原文本', 'ascii.left': 'Native 文本', 'ascii.right': 'ASCII',
    'ascii.forward': '转为 ASCII', 'ascii.reverse': '还原文本', 'footer.capabilities': 'UTF-8 / GB2312 · 本地转换',
    'report.error': '编码状态上报失败：{error}', 'host.loading': '正在加载编码解码…'
  },
  'en-US': {
    'title': 'Encode / Decode', 'session.label': 'Session', 'session.summary': '{tab} · {left} → {right} chars',
    'sample.unicode': 'MooTool encoding conversion 🐮', 'sample.url': 'https://mootool.app/search?q=encoding',
    'sample.base64': 'MooTool Next Tauri · Hello', 'sample.hex': 'MooTool Hello', 'sample.ascii': 'MooTool Cow',
    'notice.ready': '{tab} converter is ready', 'notice.done': '{action} completed', 'error.clipboard': 'Copy failed; check clipboard permission',
    'error.unicodeRange': 'Unicode code point out of range: {value}', 'error.urlEscape': 'Invalid URL escape: {value}', 'error.base64': 'Invalid Base64 input',
    'error.hex': 'Hex input must contain complete hexadecimal bytes', 'error.codePoint': 'Invalid code point: {value}', 'error.codePointRange': 'Code point out of range: {value}',
    'aria.types': 'Encoding type', 'tab.unicode': 'Native / Unicode', 'tab.url': 'URL', 'tab.hex': 'Native / Hex', 'tab.ascii': 'Native / ASCII',
    'field.charset': 'Charset', 'field.codePointFormat': 'Code point format', 'option.decimal': 'Decimal',
    'option.hexadecimal': 'Hexadecimal', 'action.clear': 'Clear current conversion', 'action.copied': 'Copied', 'action.copy': 'Copy',
    'pane.metrics': '{characters} chars · {bytes} bytes', 'unicode.left': 'Native text', 'unicode.right': 'Unicode text',
    'unicode.forward': 'To Unicode', 'unicode.reverse': 'To native', 'url.left': 'URL source', 'url.right': 'Encoded result',
    'url.forward': 'URL encode', 'url.reverse': 'URL decode', 'base64.left': 'UTF-8 text', 'base64.right': 'Base64',
    'base64.forward': 'Encode', 'base64.reverse': 'Decode', 'hex.left': 'Native text', 'hex.right': 'Hexadecimal',
    'hex.forward': 'To hex', 'hex.reverse': 'To text', 'ascii.left': 'Native text', 'ascii.right': 'ASCII',
    'ascii.forward': 'To ASCII', 'ascii.reverse': 'To text', 'footer.capabilities': 'UTF-8 / GB2312 · Local conversion',
    'report.error': 'Encoding status reporting failed: {error}', 'host.loading': 'Loading Encode / Decode…'
  },
  'ja-JP': {
    'title': 'エンコード / デコード', 'session.label': 'セッション', 'session.summary': '{tab} · {left} → {right} 文字',
    'sample.unicode': 'MooTool エンコード変換 🐮', 'sample.url': 'https://mootool.app/search?q=エンコード',
    'sample.base64': 'MooTool Next Tauri · こんにちは', 'sample.hex': 'MooTool こんにちは', 'sample.ascii': 'MooTool 牛',
    'notice.ready': '{tab} 変換の準備ができました', 'notice.done': '{action}が完了しました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'error.unicodeRange': 'Unicode コードポイントが範囲外です：{value}', 'error.urlEscape': '無効な URL エスケープです：{value}', 'error.base64': 'Base64 入力が無効です',
    'error.hex': 'Hex 入力は完全な 16 進バイトで構成してください', 'error.codePoint': '無効なコードポイントです：{value}', 'error.codePointRange': 'コードポイントが範囲外です：{value}',
    'aria.types': 'エンコード形式', 'tab.unicode': 'Native / Unicode', 'tab.url': 'URL 変換', 'tab.hex': 'Native / 16進数', 'tab.ascii': 'Native / ASCII',
    'field.charset': '文字セット', 'field.codePointFormat': 'コードポイント形式', 'option.decimal': '10進数',
    'option.hexadecimal': '16進数', 'action.clear': '現在の変換を消去', 'action.copied': 'コピー済み', 'action.copy': 'コピー',
    'pane.metrics': '{characters} 文字 · {bytes} バイト', 'unicode.left': 'Native テキスト', 'unicode.right': 'Unicode テキスト',
    'unicode.forward': 'Unicode へ', 'unicode.reverse': 'Native へ', 'url.left': 'URL 元テキスト', 'url.right': 'エンコード結果',
    'url.forward': 'URL エンコード', 'url.reverse': 'URL デコード', 'base64.left': 'UTF-8 テキスト', 'base64.right': 'Base64',
    'base64.forward': 'エンコード', 'base64.reverse': 'デコード', 'hex.left': 'Native テキスト', 'hex.right': '16進数',
    'hex.forward': 'Hex へ', 'hex.reverse': 'テキストへ', 'ascii.left': 'Native テキスト', 'ascii.right': 'ASCII',
    'ascii.forward': 'ASCII へ', 'ascii.reverse': 'テキストへ', 'footer.capabilities': 'UTF-8 / GB2312 · ローカル変換',
    'report.error': 'エンコード状態の報告に失敗しました：{error}', 'host.loading': 'エンコード / デコードを読み込み中…'
  }
})
