import { defineMessages } from '../../app/localizedMessages'

export const protobufMessages = defineMessages({
  'zh-CN': {
    'session.label': '会话', 'session.summary': '{message} · {format} · {count} 字符', 'session.noMessage': '未选类型',
    'notice.ready': 'Protobuf 编解码器已就绪', 'notice.encoded': 'JSON 已编码为 Protobuf', 'notice.decoded': 'Protobuf 已解码为 JSON',
    'notice.format': '二进制显示已切换为 {format}', 'notice.inspected': 'Wire 字段结构已解析', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'error.jsonParse': 'JSON 解析失败：{detail}', 'error.jsonObject': '消息 JSON 顶层必须是对象', 'error.messageConvert': '消息转换失败：{detail}',
    'error.messageValidate': '消息校验失败：{detail}', 'error.decode': 'Protobuf 解码失败：{detail}', 'error.wireTag': 'Wire 数据包含无效 tag 0',
    'error.hex': 'Hex 必须由完整字节组成', 'error.base64': 'Base64 格式无效', 'error.binaryLimit': '二进制数据不能超过 16 MiB',
    'error.schemaEmpty': '.proto 定义不能为空', 'error.schemaLimit': '.proto 定义不能超过 2 MiB', 'error.schemaParse': '.proto 解析失败：{detail}',
    'error.messageRequired': '请选择消息类型', 'error.messageMissing': '未找到消息类型：{name}', 'field.message': '消息类型',
    'field.noMessages': '未解析到消息', 'field.noPackage': '无 package', 'pane.schema': '.proto 定义', 'pane.messages': '{count} messages',
    'pane.json': 'JSON 消息', 'action.encode': '编码', 'aria.json': 'Protobuf JSON 输入', 'action.decode': '解码',
    'pane.binary': '{format} 二进制', 'action.copied': '已复制', 'action.copy': '复制', 'aria.binary': 'Protobuf 二进制',
    'action.inspect': '解析字段', 'wire.empty': '空消息（0 bytes）', 'wire.hint': '编码或粘贴二进制后，可查看无需 schema 的字段号、Wire Type 与字节长度。',
    'footer.capabilities': 'protobuf.js · proto2 / proto3 · 64-bit 字符串保真', 'report.error': 'Protobuf 状态上报失败：{error}',
    'host.loading': '正在加载 Protobuf 工作台…'
  },
  'en-US': {
    'session.label': 'Session', 'session.summary': '{message} · {format} · {count} chars', 'session.noMessage': 'No type selected',
    'notice.ready': 'Protobuf codec is ready', 'notice.encoded': 'JSON encoded as Protobuf', 'notice.decoded': 'Protobuf decoded as JSON',
    'notice.format': 'Binary display switched to {format}', 'notice.inspected': 'Wire field structure parsed', 'error.clipboard': 'Copy failed; check clipboard permission',
    'error.jsonParse': 'JSON parsing failed: {detail}', 'error.jsonObject': 'Message JSON must have an object at the top level', 'error.messageConvert': 'Message conversion failed: {detail}',
    'error.messageValidate': 'Message validation failed: {detail}', 'error.decode': 'Protobuf decoding failed: {detail}', 'error.wireTag': 'Wire data contains invalid tag 0',
    'error.hex': 'Hex must contain complete bytes', 'error.base64': 'Invalid Base64 format', 'error.binaryLimit': 'Binary data cannot exceed 16 MiB',
    'error.schemaEmpty': '.proto definition cannot be empty', 'error.schemaLimit': '.proto definition cannot exceed 2 MiB', 'error.schemaParse': '.proto parsing failed: {detail}',
    'error.messageRequired': 'Select a message type', 'error.messageMissing': 'Message type not found: {name}', 'field.message': 'Message type',
    'field.noMessages': 'No messages parsed', 'field.noPackage': 'No package', 'pane.schema': '.proto definition', 'pane.messages': '{count} messages',
    'pane.json': 'JSON message', 'action.encode': 'Encode', 'aria.json': 'Protobuf JSON input', 'action.decode': 'Decode',
    'pane.binary': '{format} binary', 'action.copied': 'Copied', 'action.copy': 'Copy', 'aria.binary': 'Protobuf binary',
    'action.inspect': 'Parse fields', 'wire.empty': 'Empty message (0 bytes)', 'wire.hint': 'Encode or paste binary to inspect field numbers, Wire Types, and byte lengths without a schema.',
    'footer.capabilities': 'protobuf.js · proto2 / proto3 · Lossless 64-bit strings', 'report.error': 'Protobuf status reporting failed: {error}',
    'host.loading': 'Loading Protobuf workbench…'
  },
  'ja-JP': {
    'session.label': 'セッション', 'session.summary': '{message} · {format} · {count} 文字', 'session.noMessage': '型未選択',
    'notice.ready': 'Protobuf コーデックの準備ができました', 'notice.encoded': 'JSON を Protobuf へエンコードしました', 'notice.decoded': 'Protobuf を JSON へデコードしました',
    'notice.format': 'バイナリ表示を {format} へ切り替えました', 'notice.inspected': 'Wire フィールド構造を解析しました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'error.jsonParse': 'JSON の解析に失敗しました：{detail}', 'error.jsonObject': 'メッセージ JSON のトップレベルはオブジェクトにしてください', 'error.messageConvert': 'メッセージ変換に失敗しました：{detail}',
    'error.messageValidate': 'メッセージ検証に失敗しました：{detail}', 'error.decode': 'Protobuf のデコードに失敗しました：{detail}', 'error.wireTag': 'Wire データに無効な tag 0 が含まれています',
    'error.hex': 'Hex は完全なバイトで構成してください', 'error.base64': 'Base64 形式が無効です', 'error.binaryLimit': 'バイナリデータは 16 MiB 以下にしてください',
    'error.schemaEmpty': '.proto 定義を空にはできません', 'error.schemaLimit': '.proto 定義は 2 MiB 以下にしてください', 'error.schemaParse': '.proto の解析に失敗しました：{detail}',
    'error.messageRequired': 'メッセージ型を選択してください', 'error.messageMissing': 'メッセージ型が見つかりません：{name}', 'field.message': 'メッセージ型',
    'field.noMessages': 'メッセージを解析できませんでした', 'field.noPackage': 'package なし', 'pane.schema': '.proto 定義', 'pane.messages': '{count} messages',
    'pane.json': 'JSON メッセージ', 'action.encode': 'エンコード', 'aria.json': 'Protobuf JSON 入力', 'action.decode': 'デコード',
    'pane.binary': '{format} バイナリ', 'action.copied': 'コピー済み', 'action.copy': 'コピー', 'aria.binary': 'Protobuf バイナリ',
    'action.inspect': 'フィールドを解析', 'wire.empty': '空のメッセージ（0 bytes）', 'wire.hint': 'バイナリをエンコードまたは貼り付けると、schema なしでフィールド番号、Wire Type、バイト長を確認できます。',
    'footer.capabilities': 'protobuf.js · proto2 / proto3 · 64-bit 文字列を保持', 'report.error': 'Protobuf 状態の報告に失敗しました：{error}',
    'host.loading': 'Protobuf ワークベンチを読み込み中…'
  }
})
