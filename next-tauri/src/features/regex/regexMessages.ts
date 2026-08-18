import { defineMessages } from '../../app/localizedMessages'

export const regexMessages = defineMessages({
  'zh-CN': {
    'session.label': '会话', 'session.summary': '/{flags} · {count} 个匹配{error}', 'session.error': ' · 表达式错误',
    'tab.test': '表达式测试', 'tab.common': '常用正则', 'action.clear': '清空测试内容', 'field.pattern': '正则表达式',
    'action.copied': '已复制', 'action.copy': '复制', 'action.run': '运行', 'aria.flags': '正则标志',
    'flag.global': 'g · 全局', 'flag.ignoreCase': 'i · 忽略大小写', 'flag.multiline': 'm · 多行',
    'flag.dotAll': 's · 点匹配换行', 'pane.source': '测试文本', 'pane.characters': '{count} 字符', 'aria.source': 'Regex 测试文本',
    'result.invalid': '无效正则：{error}', 'result.matches': '{count} 个匹配', 'result.waiting': '等待运行',
    'result.empty': '没有匹配项', 'result.instructions': '输入表达式后按 Enter 或点击“运行”', 'field.replace': '替换表达式',
    'action.applyReplace': '应用替换', 'search.placeholder': '搜索名称或表达式', 'status.error': '请修正表达式后重新运行',
    'status.ready': 'JavaScript RegExp · 本地执行', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'report.error': 'Regex 状态上报失败：{error}', 'host.loading': '正在加载 Regex…',
    'common.phone': '中国大陆手机号', 'common.email': '电子邮箱', 'common.url': 'HTTP(S) URL', 'common.domain': '域名',
    'common.ipv4': 'IPv4', 'common.ipv6': 'IPv6（完整）', 'common.account': '账号（字母开头）', 'common.htmlId': 'HTML id 属性值',
    'common.color': '十六进制颜色', 'common.image': '图片 URL', 'common.magnet': 'Magnet 链接', 'common.chinese': '中文字符',
    'common.alnum': '字母与数字', 'common.len3to20': '长度 3–20', 'common.letters26': '英文字母', 'common.wordUnderscore': '单词字符',
    'common.cnEnNum': '中英文、数字与下划线', 'common.integer': '整数', 'common.positiveInt': '正整数',
    'common.nonNegativeInt': '非负整数', 'common.float': '浮点数'
  },
  'en-US': {
    'session.label': 'Session', 'session.summary': '/{flags} · {count} matches{error}', 'session.error': ' · Expression error',
    'tab.test': 'Expression test', 'tab.common': 'Common patterns', 'action.clear': 'Clear test content', 'field.pattern': 'Regular expression',
    'action.copied': 'Copied', 'action.copy': 'Copy', 'action.run': 'Run', 'aria.flags': 'Regex flags',
    'flag.global': 'g · Global', 'flag.ignoreCase': 'i · Ignore case', 'flag.multiline': 'm · Multiline',
    'flag.dotAll': 's · Dot matches newline', 'pane.source': 'Test text', 'pane.characters': '{count} chars', 'aria.source': 'Regex test text',
    'result.invalid': 'Invalid regex: {error}', 'result.matches': '{count} matches', 'result.waiting': 'Waiting to run',
    'result.empty': 'No matches', 'result.instructions': 'Enter an expression, then press Enter or click “Run”', 'field.replace': 'Replacement expression',
    'action.applyReplace': 'Apply replacement', 'search.placeholder': 'Search names or expressions', 'status.error': 'Correct the expression and run again',
    'status.ready': 'JavaScript RegExp · Local execution', 'error.clipboard': 'Copy failed; check clipboard permission',
    'report.error': 'Regex status reporting failed: {error}', 'host.loading': 'Loading Regex…',
    'common.phone': 'Mainland China mobile number', 'common.email': 'Email address', 'common.url': 'HTTP(S) URL', 'common.domain': 'Domain name',
    'common.ipv4': 'IPv4', 'common.ipv6': 'IPv6 (full)', 'common.account': 'Account (starts with a letter)', 'common.htmlId': 'HTML id value',
    'common.color': 'Hex color', 'common.image': 'Image URL', 'common.magnet': 'Magnet link', 'common.chinese': 'Chinese characters',
    'common.alnum': 'Letters and digits', 'common.len3to20': 'Length 3–20', 'common.letters26': 'English letters', 'common.wordUnderscore': 'Word characters',
    'common.cnEnNum': 'Chinese, English, digits, underscore', 'common.integer': 'Integer', 'common.positiveInt': 'Positive integer',
    'common.nonNegativeInt': 'Non-negative integer', 'common.float': 'Floating-point number'
  },
  'ja-JP': {
    'session.label': 'セッション', 'session.summary': '/{flags} · 一致 {count} 件{error}', 'session.error': ' · 式エラー',
    'tab.test': '式のテスト', 'tab.common': 'よく使う正規表現', 'action.clear': 'テスト内容を消去', 'field.pattern': '正規表現',
    'action.copied': 'コピー済み', 'action.copy': 'コピー', 'action.run': '実行', 'aria.flags': '正規表現フラグ',
    'flag.global': 'g · グローバル', 'flag.ignoreCase': 'i · 大文字小文字を無視', 'flag.multiline': 'm · 複数行',
    'flag.dotAll': 's · ドットで改行にも一致', 'pane.source': 'テストテキスト', 'pane.characters': '{count} 文字', 'aria.source': 'Regex テストテキスト',
    'result.invalid': '無効な正規表現：{error}', 'result.matches': '{count} 件一致', 'result.waiting': '実行待ち',
    'result.empty': '一致する項目はありません', 'result.instructions': '式を入力し Enter または「実行」をクリックしてください', 'field.replace': '置換式',
    'action.applyReplace': '置換を適用', 'search.placeholder': '名前または式を検索', 'status.error': '式を修正して再実行してください',
    'status.ready': 'JavaScript RegExp · ローカル実行', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'report.error': 'Regex 状態の報告に失敗しました：{error}', 'host.loading': 'Regex を読み込み中…',
    'common.phone': '中国本土の携帯電話番号', 'common.email': 'メールアドレス', 'common.url': 'HTTP(S) URL', 'common.domain': 'ドメイン名',
    'common.ipv4': 'IPv4', 'common.ipv6': 'IPv6（完全）', 'common.account': 'アカウント（英字で開始）', 'common.htmlId': 'HTML id 属性値',
    'common.color': '16 進カラー', 'common.image': '画像 URL', 'common.magnet': 'Magnet リンク', 'common.chinese': '中国語文字',
    'common.alnum': '英字と数字', 'common.len3to20': '長さ 3～20', 'common.letters26': '英字', 'common.wordUnderscore': '単語文字',
    'common.cnEnNum': '中英文字、数字、アンダースコア', 'common.integer': '整数', 'common.positiveInt': '正の整数',
    'common.nonNegativeInt': '非負整数', 'common.float': '浮動小数点数'
  }
})
