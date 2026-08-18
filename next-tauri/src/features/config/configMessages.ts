import { defineMessages } from '../../app/localizedMessages'

export const configMessages = defineMessages({
  'zh-CN': {
    'session.label': '会话', 'session.summary': 'YAML {yaml} · Properties {properties} 字符',
    'sample.yaml': 'server:\n  port: 8080\n  compression: true\n  endpoints:\n    - /api\n    - /health\nmessage: 你好，MooTool Next Tauri\n',
    'action.formatYaml': '格式化 YAML', 'action.toProperties': '转 Properties', 'action.toYaml': '转 YAML',
    'action.copy': '复制', 'action.copied': '已复制', 'action.clear': '清空 YAML 和 Properties',
    'validation.valid': 'YAML 有效', 'validation.invalid': 'YAML 无效', 'editor.label': '{type} 编辑器',
    'notice.ready': 'YAML / Properties 转换器已就绪', 'notice.formatted': 'YAML 已格式化',
    'notice.toProperties': '已转换为 Properties', 'notice.toYaml': '已转换为 YAML',
    'notice.copyFailed': '复制失败，请检查剪贴板权限',
    'status.local': '嵌套对象与数组路径 · Java Properties 转义 · 本地处理',
    'error.emptyKey': 'Properties 包含空键名', 'error.rootObject': 'YAML 根节点必须是对象',
    'error.invalidPath': 'Properties 键路径无效或过深', 'error.unsafeKey': 'Properties 键名不安全：{key}',
    'error.arrayPath': 'Properties 数组路径不一致', 'error.maxDepth': 'YAML 嵌套层级过深',
    'report.error': '配置转换状态上报失败：{error}', 'host.loading': '正在加载配置转换…'
  },
  'en-US': {
    'session.label': 'Session', 'session.summary': 'YAML {yaml} · Properties {properties} characters',
    'sample.yaml': 'server:\n  port: 8080\n  compression: true\n  endpoints:\n    - /api\n    - /health\nmessage: Hello, MooTool Next Tauri\n',
    'action.formatYaml': 'Format YAML', 'action.toProperties': 'To Properties', 'action.toYaml': 'To YAML',
    'action.copy': 'Copy', 'action.copied': 'Copied', 'action.clear': 'Clear YAML and Properties',
    'validation.valid': 'Valid YAML', 'validation.invalid': 'Invalid YAML', 'editor.label': '{type} editor',
    'notice.ready': 'YAML / Properties converter is ready', 'notice.formatted': 'YAML formatted',
    'notice.toProperties': 'Converted to Properties', 'notice.toYaml': 'Converted to YAML',
    'notice.copyFailed': 'Copy failed. Check clipboard permission.',
    'status.local': 'Nested object and array paths · Java Properties escaping · local processing',
    'error.emptyKey': 'Properties contains an empty key', 'error.rootObject': 'The YAML root must be an object',
    'error.invalidPath': 'The Properties key path is invalid or too deep', 'error.unsafeKey': 'Unsafe Properties key: {key}',
    'error.arrayPath': 'Inconsistent Properties array path', 'error.maxDepth': 'The YAML nesting depth is too great',
    'report.error': 'Configuration conversion status reporting failed: {error}', 'host.loading': 'Loading configuration converter…'
  },
  'ja-JP': {
    'session.label': 'セッション', 'session.summary': 'YAML {yaml} · Properties {properties} 文字',
    'sample.yaml': 'server:\n  port: 8080\n  compression: true\n  endpoints:\n    - /api\n    - /health\nmessage: こんにちは、MooTool Next Tauri\n',
    'action.formatYaml': 'YAML を整形', 'action.toProperties': 'Properties へ', 'action.toYaml': 'YAML へ',
    'action.copy': 'コピー', 'action.copied': 'コピー済み', 'action.clear': 'YAML と Properties をクリア',
    'validation.valid': '有効な YAML', 'validation.invalid': '無効な YAML', 'editor.label': '{type} エディター',
    'notice.ready': 'YAML / Properties コンバーターの準備ができました', 'notice.formatted': 'YAML を整形しました',
    'notice.toProperties': 'Properties に変換しました', 'notice.toYaml': 'YAML に変換しました',
    'notice.copyFailed': 'コピーに失敗しました。クリップボード権限を確認してください。',
    'status.local': 'オブジェクトと配列のネストパス · Java Properties エスケープ · ローカル処理',
    'error.emptyKey': 'Properties に空のキーがあります', 'error.rootObject': 'YAML のルートはオブジェクトである必要があります',
    'error.invalidPath': 'Properties のキーパスが無効か深すぎます', 'error.unsafeKey': '安全でない Properties キーです：{key}',
    'error.arrayPath': 'Properties の配列パスが一致していません', 'error.maxDepth': 'YAML のネストが深すぎます',
    'report.error': '設定変換状態の報告に失敗しました：{error}', 'host.loading': '設定コンバーターを読み込み中…'
  }
})
