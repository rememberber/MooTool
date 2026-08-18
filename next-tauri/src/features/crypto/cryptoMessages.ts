import { defineMessages } from '../../app/localizedMessages'

export const cryptoMessages = defineMessages({
  'zh-CN': {
    'title': '加解密 / 随机', 'session.label': '会话', 'session.summary': '{tab} · {count} 字符输出',
    'tab.digest': '摘要 / HMAC', 'tab.aes': 'AES-GCM', 'tab.random': '安全随机', 'notice.ready': '{tab}已就绪',
    'notice.hmacDone': 'HMAC-SHA256 计算完成', 'notice.digestDone': '{algorithm} 摘要计算完成',
    'notice.encrypted': 'AES-256-GCM 加密完成', 'notice.decrypted': 'AES-256-GCM 解密并校验完成',
    'notice.uuid': 'UUID v4 已生成', 'notice.random': '安全随机值已生成', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'error.hmacSecret': 'HMAC 密钥不能为空', 'error.invalidCiphertext': '不是 MooTool AES-GCM v1 密文',
    'error.decryptFailed': '解密失败：密钥错误或密文已损坏', 'error.randomLength': '随机长度必须是 1–4096 的整数',
    'error.passphraseEmpty': '加密口令不能为空', 'error.passphraseLong': '加密口令不能超过 4096 个字符',
    'field.digest': '摘要', 'option.compatible': '兼容', 'field.hmac': 'HMAC 密钥（可选）', 'placeholder.hmac': '填写后使用 HMAC-SHA256',
    'field.passphrase': '加密口令', 'placeholder.passphrase': 'PBKDF2 派生本地密钥', 'field.type': '类型',
    'random.password': '强密码', 'random.alphanumeric': '字母数字', 'random.digits': '纯数字', 'field.length': '长度',
    'pane.settings': '生成设置', 'pane.aesInput': '明文或密文', 'pane.source': '原文', 'random.secure': '使用系统加密安全随机源',
    'random.detail': '拒绝取模偏差，生成结果仅在本机内存中处理。', 'action.generate': '立即生成',
    'aria.input': '加解密输入', 'action.calculate': '计算', 'action.encrypt': '加密', 'action.decrypt': '解密',
    'pane.output': '输出', 'action.copied': '已复制', 'action.copy': '复制', 'aria.output': '加解密输出',
    'report.error': '加密状态上报失败：{error}', 'host.loading': '正在加载加解密 / 随机…'
  },
  'en-US': {
    'title': 'Crypto / Random', 'session.label': 'Session', 'session.summary': '{tab} · {count} output chars',
    'tab.digest': 'Digest / HMAC', 'tab.aes': 'AES-GCM', 'tab.random': 'Secure Random', 'notice.ready': '{tab} ready',
    'notice.hmacDone': 'HMAC-SHA256 completed', 'notice.digestDone': '{algorithm} digest completed',
    'notice.encrypted': 'AES-256-GCM encryption completed', 'notice.decrypted': 'AES-256-GCM decryption and verification completed',
    'notice.uuid': 'UUID v4 generated', 'notice.random': 'Secure random value generated', 'error.clipboard': 'Copy failed; check clipboard permission',
    'error.hmacSecret': 'HMAC secret cannot be empty', 'error.invalidCiphertext': 'Not a MooTool AES-GCM v1 ciphertext',
    'error.decryptFailed': 'Decryption failed: wrong passphrase or damaged ciphertext', 'error.randomLength': 'Random length must be an integer from 1 to 4096',
    'error.passphraseEmpty': 'Passphrase cannot be empty', 'error.passphraseLong': 'Passphrase cannot exceed 4096 characters',
    'field.digest': 'Digest', 'option.compatible': 'legacy', 'field.hmac': 'HMAC secret (optional)', 'placeholder.hmac': 'Uses HMAC-SHA256 when set',
    'field.passphrase': 'Passphrase', 'placeholder.passphrase': 'Derive a local key with PBKDF2', 'field.type': 'Type',
    'random.password': 'Strong password', 'random.alphanumeric': 'Alphanumeric', 'random.digits': 'Digits only', 'field.length': 'Length',
    'pane.settings': 'Generation settings', 'pane.aesInput': 'Plaintext or ciphertext', 'pane.source': 'Source', 'random.secure': 'Uses the system cryptographic random source',
    'random.detail': 'Avoids modulo bias; generated values stay in local memory.', 'action.generate': 'Generate now',
    'aria.input': 'Crypto input', 'action.calculate': 'Calculate', 'action.encrypt': 'Encrypt', 'action.decrypt': 'Decrypt',
    'pane.output': 'Output', 'action.copied': 'Copied', 'action.copy': 'Copy', 'aria.output': 'Crypto output',
    'report.error': 'Crypto status reporting failed: {error}', 'host.loading': 'Loading Crypto / Random…'
  },
  'ja-JP': {
    'title': '暗号化 / ランダム', 'session.label': 'セッション', 'session.summary': '{tab} · 出力 {count} 文字',
    'tab.digest': 'ダイジェスト / HMAC', 'tab.aes': 'AES-GCM', 'tab.random': '安全な乱数', 'notice.ready': '{tab} の準備ができました',
    'notice.hmacDone': 'HMAC-SHA256 の計算が完了しました', 'notice.digestDone': '{algorithm} ダイジェストの計算が完了しました',
    'notice.encrypted': 'AES-256-GCM の暗号化が完了しました', 'notice.decrypted': 'AES-256-GCM の復号と検証が完了しました',
    'notice.uuid': 'UUID v4 を生成しました', 'notice.random': '安全なランダム値を生成しました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'error.hmacSecret': 'HMAC キーを空にはできません', 'error.invalidCiphertext': 'MooTool AES-GCM v1 暗号文ではありません',
    'error.decryptFailed': '復号に失敗しました：パスフレーズが違うか暗号文が破損しています', 'error.randomLength': '乱数の長さは 1～4096 の整数にしてください',
    'error.passphraseEmpty': 'パスフレーズを空にはできません', 'error.passphraseLong': 'パスフレーズは 4096 文字以内にしてください',
    'field.digest': 'ダイジェスト', 'option.compatible': '互換', 'field.hmac': 'HMAC キー（任意）', 'placeholder.hmac': '入力すると HMAC-SHA256 を使用',
    'field.passphrase': 'パスフレーズ', 'placeholder.passphrase': 'PBKDF2 でローカルキーを導出', 'field.type': '種類',
    'random.password': '強力なパスワード', 'random.alphanumeric': '英数字', 'random.digits': '数字のみ', 'field.length': '長さ',
    'pane.settings': '生成設定', 'pane.aesInput': '平文または暗号文', 'pane.source': '原文', 'random.secure': 'システムの暗号学的乱数源を使用',
    'random.detail': '剰余バイアスを避け、生成結果はローカルメモリ内だけで処理します。', 'action.generate': '今すぐ生成',
    'aria.input': '暗号入力', 'action.calculate': '計算', 'action.encrypt': '暗号化', 'action.decrypt': '復号',
    'pane.output': '出力', 'action.copied': 'コピー済み', 'action.copy': 'コピー', 'aria.output': '暗号出力',
    'report.error': '暗号状態の報告に失敗しました：{error}', 'host.loading': '暗号化 / ランダムを読み込み中…'
  }
})
