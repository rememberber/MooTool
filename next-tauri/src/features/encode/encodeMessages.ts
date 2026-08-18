import { defineMessages } from '../../app/localizedMessages'

export const encodeMessages = defineMessages({
  'zh-CN': {
    'title': '编码解码', 'session.label': '会话', 'session.summary': '{tab} · {left} → {right} 字符',
    'sample.unicode': 'MooTool 编码转换 🐮', 'sample.url': 'https://mootool.app/search?q=编码',
    'sample.base64': 'MooTool Next Tauri · 你好', 'sample.hex': 'MooTool 你好', 'sample.ascii': 'MooTool 牛',
    'notice.ready': '{tab} 转换器已就绪', 'notice.done': '{action}完成', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'error.unicodeRange': 'Unicode 码点越界：{value}', 'error.urlEscape': '无效 URL 转义：{value}', 'error.base64': 'Base64 输入无效',
    'error.hex': 'Hex 输入必须由完整的十六进制字节组成', 'error.codePoint': '无效码点：{value}', 'error.codePointRange': '码点越界：{value}',
    'aria.types': '编码类型', 'field.charset': '字符集', 'field.codePointFormat': '码点格式', 'option.decimal': '十进制',
    'option.hexadecimal': '十六进制', 'action.clear': '清空当前转换', 'action.copied': '已复制', 'action.copy': '复制',
    'pane.metrics': '{characters} 字符 · {bytes} 字节', 'unicode.left': '原始文本', 'unicode.right': 'Unicode 转义',
    'unicode.forward': '转 Unicode', 'unicode.reverse': '还原文本', 'url.left': 'URL / 文本', 'url.right': '百分号编码',
    'url.forward': 'URL 编码', 'url.reverse': 'URL 解码', 'base64.left': 'UTF-8 文本', 'base64.right': 'Base64',
    'base64.forward': '编码', 'base64.reverse': '解码', 'hex.left': 'UTF-8 文本', 'hex.right': '十六进制字节',
    'hex.forward': '转 Hex', 'hex.reverse': '还原文本', 'ascii.left': '原始文本', 'ascii.right': 'Unicode 码点',
    'ascii.forward': '转码点', 'ascii.reverse': '还原文本', 'footer.capabilities': 'UTF-8 / GB2312 · 本地转换',
    'report.error': '编码状态上报失败：{error}', 'host.loading': '正在加载编码解码…'
  },
  'en-US': {
    'title': 'Encode / Decode', 'session.label': 'Session', 'session.summary': '{tab} · {left} → {right} chars',
    'sample.unicode': 'MooTool encoding conversion 🐮', 'sample.url': 'https://mootool.app/search?q=encoding',
    'sample.base64': 'MooTool Next Tauri · Hello', 'sample.hex': 'MooTool Hello', 'sample.ascii': 'MooTool Cow',
    'notice.ready': '{tab} converter is ready', 'notice.done': '{action} completed', 'error.clipboard': 'Copy failed; check clipboard permission',
    'error.unicodeRange': 'Unicode code point out of range: {value}', 'error.urlEscape': 'Invalid URL escape: {value}', 'error.base64': 'Invalid Base64 input',
    'error.hex': 'Hex input must contain complete hexadecimal bytes', 'error.codePoint': 'Invalid code point: {value}', 'error.codePointRange': 'Code point out of range: {value}',
    'aria.types': 'Encoding type', 'field.charset': 'Charset', 'field.codePointFormat': 'Code point format', 'option.decimal': 'Decimal',
    'option.hexadecimal': 'Hexadecimal', 'action.clear': 'Clear current conversion', 'action.copied': 'Copied', 'action.copy': 'Copy',
    'pane.metrics': '{characters} chars · {bytes} bytes', 'unicode.left': 'Source text', 'unicode.right': 'Unicode escapes',
    'unicode.forward': 'To Unicode', 'unicode.reverse': 'Restore text', 'url.left': 'URL / Text', 'url.right': 'Percent encoding',
    'url.forward': 'URL encode', 'url.reverse': 'URL decode', 'base64.left': 'UTF-8 text', 'base64.right': 'Base64',
    'base64.forward': 'Encode', 'base64.reverse': 'Decode', 'hex.left': 'UTF-8 text', 'hex.right': 'Hex bytes',
    'hex.forward': 'To Hex', 'hex.reverse': 'Restore text', 'ascii.left': 'Source text', 'ascii.right': 'Unicode code points',
    'ascii.forward': 'To code points', 'ascii.reverse': 'Restore text', 'footer.capabilities': 'UTF-8 / GB2312 · Local conversion',
    'report.error': 'Encoding status reporting failed: {error}', 'host.loading': 'Loading Encode / Decode…'
  },
  'ja-JP': {
    'title': 'エンコード / デコード', 'session.label': 'セッション', 'session.summary': '{tab} · {left} → {right} 文字',
    'sample.unicode': 'MooTool エンコード変換 🐮', 'sample.url': 'https://mootool.app/search?q=エンコード',
    'sample.base64': 'MooTool Next Tauri · こんにちは', 'sample.hex': 'MooTool こんにちは', 'sample.ascii': 'MooTool 牛',
    'notice.ready': '{tab} 変換の準備ができました', 'notice.done': '{action}が完了しました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'error.unicodeRange': 'Unicode コードポイントが範囲外です：{value}', 'error.urlEscape': '無効な URL エスケープです：{value}', 'error.base64': 'Base64 入力が無効です',
    'error.hex': 'Hex 入力は完全な 16 進バイトで構成してください', 'error.codePoint': '無効なコードポイントです：{value}', 'error.codePointRange': 'コードポイントが範囲外です：{value}',
    'aria.types': 'エンコード形式', 'field.charset': '文字セット', 'field.codePointFormat': 'コードポイント形式', 'option.decimal': '10 進数',
    'option.hexadecimal': '16 進数', 'action.clear': '現在の変換を消去', 'action.copied': 'コピー済み', 'action.copy': 'コピー',
    'pane.metrics': '{characters} 文字 · {bytes} バイト', 'unicode.left': '元のテキスト', 'unicode.right': 'Unicode エスケープ',
    'unicode.forward': 'Unicode へ', 'unicode.reverse': 'テキストを復元', 'url.left': 'URL / テキスト', 'url.right': 'パーセントエンコード',
    'url.forward': 'URL エンコード', 'url.reverse': 'URL デコード', 'base64.left': 'UTF-8 テキスト', 'base64.right': 'Base64',
    'base64.forward': 'エンコード', 'base64.reverse': 'デコード', 'hex.left': 'UTF-8 テキスト', 'hex.right': '16 進バイト',
    'hex.forward': 'Hex へ', 'hex.reverse': 'テキストを復元', 'ascii.left': '元のテキスト', 'ascii.right': 'Unicode コードポイント',
    'ascii.forward': 'コードポイントへ', 'ascii.reverse': 'テキストを復元', 'footer.capabilities': 'UTF-8 / GB2312 · ローカル変換',
    'report.error': 'エンコード状態の報告に失敗しました：{error}', 'host.loading': 'エンコード / デコードを読み込み中…'
  }
})
