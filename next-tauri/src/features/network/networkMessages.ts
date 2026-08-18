import { defineMessages } from '../../app/localizedMessages'

export const networkMessages = defineMessages({
  'zh-CN': {
    'title': '网络 / IP', 'session.label': '会话', 'notice.ready': 'IPv4 / CIDR 计算器已就绪', 'notice.analyzed': 'CIDR 信息已计算',
    'notice.converted': 'IPv4 整数转换完成', 'notice.copied': '{value} 已复制', 'error.clipboard': '复制失败，请检查剪贴板权限',
    'error.ipv4Input': '请输入 IPv4 或 CIDR，例如 192.168.1.10/24', 'error.octetRange': 'IPv4 每段必须在 0–255 之间',
    'error.prefixRange': 'CIDR 前缀必须在 0–32 之间', 'error.integerFormat': 'IPv4 整数必须是十进制非负整数',
    'error.integerRange': 'IPv4 整数必须在 0–4294967295 之间', 'category.private': '私有地址', 'category.loopback': '环回地址',
    'category.linkLocal': '链路本地地址', 'category.multicast': '组播地址', 'category.limitedBroadcast': '受限广播地址',
    'category.publicReserved': '公网 / 保留地址', 'action.calculate': '计算', 'field.integer': '整数转 IPv4', 'action.convert': '转换',
    'action.copyCidr': '复制 CIDR', 'fact.address': 'IP 地址', 'fact.category': '地址类型', 'fact.netmask': '子网掩码',
    'fact.wildcard': '通配掩码', 'fact.network': '网络地址', 'fact.broadcast': '广播地址', 'fact.firstHost': '首个主机',
    'fact.lastHost': '最后主机', 'fact.total': '地址总数', 'fact.usable': '可用主机', 'fact.integer': '无符号整数',
    'fact.binary': '二进制', 'footer.capabilities': 'IPv4 · CIDR · 子网 · 整数转换 · 本地计算',
    'report.error': '网络状态上报失败：{error}', 'host.loading': '正在加载网络 / IP 工具…'
  },
  'en-US': {
    'title': 'Network / IP', 'session.label': 'Session', 'notice.ready': 'IPv4 / CIDR calculator is ready', 'notice.analyzed': 'CIDR information calculated',
    'notice.converted': 'IPv4 integer conversion completed', 'notice.copied': '{value} copied', 'error.clipboard': 'Copy failed; check clipboard permission',
    'error.ipv4Input': 'Enter an IPv4 address or CIDR, such as 192.168.1.10/24', 'error.octetRange': 'Each IPv4 octet must be from 0 to 255',
    'error.prefixRange': 'CIDR prefix must be from 0 to 32', 'error.integerFormat': 'IPv4 integer must be a non-negative decimal integer',
    'error.integerRange': 'IPv4 integer must be from 0 to 4294967295', 'category.private': 'Private address', 'category.loopback': 'Loopback address',
    'category.linkLocal': 'Link-local address', 'category.multicast': 'Multicast address', 'category.limitedBroadcast': 'Limited broadcast address',
    'category.publicReserved': 'Public / reserved address', 'action.calculate': 'Calculate', 'field.integer': 'Integer to IPv4', 'action.convert': 'Convert',
    'action.copyCidr': 'Copy CIDR', 'fact.address': 'IP address', 'fact.category': 'Address type', 'fact.netmask': 'Netmask',
    'fact.wildcard': 'Wildcard mask', 'fact.network': 'Network address', 'fact.broadcast': 'Broadcast address', 'fact.firstHost': 'First host',
    'fact.lastHost': 'Last host', 'fact.total': 'Total addresses', 'fact.usable': 'Usable hosts', 'fact.integer': 'Unsigned integer',
    'fact.binary': 'Binary', 'footer.capabilities': 'IPv4 · CIDR · Subnet · Integer conversion · Local calculation',
    'report.error': 'Network status reporting failed: {error}', 'host.loading': 'Loading Network / IP tools…'
  },
  'ja-JP': {
    'title': 'ネットワーク / IP', 'session.label': 'セッション', 'notice.ready': 'IPv4 / CIDR 計算の準備ができました', 'notice.analyzed': 'CIDR 情報を計算しました',
    'notice.converted': 'IPv4 整数変換が完了しました', 'notice.copied': '{value} をコピーしました', 'error.clipboard': 'コピーに失敗しました。クリップボード権限を確認してください',
    'error.ipv4Input': 'IPv4 または CIDR を入力してください（例：192.168.1.10/24）', 'error.octetRange': 'IPv4 の各オクテットは 0～255 にしてください',
    'error.prefixRange': 'CIDR プレフィックスは 0～32 にしてください', 'error.integerFormat': 'IPv4 整数は 10 進の非負整数にしてください',
    'error.integerRange': 'IPv4 整数は 0～4294967295 にしてください', 'category.private': 'プライベートアドレス', 'category.loopback': 'ループバックアドレス',
    'category.linkLocal': 'リンクローカルアドレス', 'category.multicast': 'マルチキャストアドレス', 'category.limitedBroadcast': '制限付きブロードキャストアドレス',
    'category.publicReserved': 'パブリック / 予約済みアドレス', 'action.calculate': '計算', 'field.integer': '整数から IPv4', 'action.convert': '変換',
    'action.copyCidr': 'CIDR をコピー', 'fact.address': 'IP アドレス', 'fact.category': 'アドレス種別', 'fact.netmask': 'サブネットマスク',
    'fact.wildcard': 'ワイルドカードマスク', 'fact.network': 'ネットワークアドレス', 'fact.broadcast': 'ブロードキャストアドレス', 'fact.firstHost': '最初のホスト',
    'fact.lastHost': '最後のホスト', 'fact.total': '総アドレス数', 'fact.usable': '利用可能ホスト', 'fact.integer': '符号なし整数',
    'fact.binary': 'バイナリ', 'footer.capabilities': 'IPv4 · CIDR · サブネット · 整数変換 · ローカル計算',
    'report.error': 'ネットワーク状態の報告に失敗しました：{error}', 'host.loading': 'ネットワーク / IP ツールを読み込み中…'
  }
})
