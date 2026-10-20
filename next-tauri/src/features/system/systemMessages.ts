import { defineMessages } from '../../app/localizedMessages'

export const systemMessages = defineMessages({
  'zh-CN': {
    'title': '系统信息', 'session.label': '会话', 'session.summary': '{os} · {arch} · {cores} 线程',
    'session.pending': '等待系统快照', 'notice.loading': '正在通过 Rust 读取系统信息…', 'notice.refreshed': '系统快照已刷新',
    'notice.copied': '系统快照已复制', 'action.copy': '复制全部',
    'notice.sleepPrevented': '已阻止显示器休眠，退出应用或关闭开关后释放', 'notice.sleepRestored': '已恢复系统显示器休眠策略',
    'action.refresh': '刷新', 'tab.system': '系统', 'tab.cpu': '处理器', 'tab.memory': '内存', 'tab.storage': '存储', 'tab.network': '网络', 'empty': '没有可显示的信息', 'label.platform': '平台', 'label.distribution': '发行版', 'label.kernel': '内核', 'label.brand': '品牌', 'label.total': '总计', 'label.usage': '使用率', 'label.swapTotal': '交换空间总计', 'label.swapUsed': '交换空间已用', 'label.device': '设备', 'label.capacity': '容量', 'label.filesystem': '文件系统', 'label.received': '接收', 'label.sent': '发送', 'card.os': '操作系统', 'fact.architecture': '架构', 'fact.hostname': '主机名',
    'card.cpu': '处理器', 'fact.physicalCores': '物理核心', 'fact.logicalCores': '逻辑核心', 'card.memory': '内存',
    'fact.cpuUsage': '当前负载', 'fact.cpuFrequency': '当前频率', 'fact.swap': '交换空间', 'card.storage': '存储', 'card.network': '网络接口', 'count.disks': '{count} 个磁盘/卷', 'count.interfaces': '{count} 个接口',
    'fact.used': '已使用', 'fact.available': '可用', 'card.process': '当前进程', 'fact.uptime': '运行时长',
    'fact.source': '数据来源', 'overview.product': '独立产品', 'overview.policy': '采集策略',
    'overview.policyValue': '只读、按需刷新、不上传', 'overview.sleep': '显示器休眠', 'sleep.preventing': '保持唤醒中',
    'sleep.allowed': '允许休眠', 'duration': '{days} 天 {hours} 小时', 'footer.capabilities': 'sysinfo · 本地只读快照',
    'report.error': '系统状态上报失败：{error}', 'host.loading': '正在读取硬件与系统信息…'
  },
  'en-US': {
    'title': 'System Information', 'session.label': 'Session', 'session.summary': '{os} · {arch} · {cores} threads',
    'session.pending': 'Waiting for system snapshot', 'notice.loading': 'Reading system information through Rust…', 'notice.refreshed': 'System snapshot refreshed',
    'notice.copied': 'System snapshot copied', 'action.copy': 'Copy all',
    'notice.sleepPrevented': 'Display sleep is prevented until the app exits or the switch is disabled', 'notice.sleepRestored': 'System display sleep policy restored',
    'action.refresh': 'Refresh', 'tab.system': 'System', 'tab.cpu': 'Processor', 'tab.memory': 'Memory', 'tab.storage': 'Storage', 'tab.network': 'Network', 'empty': 'Nothing to display', 'label.platform': 'Platform', 'label.distribution': 'Distribution', 'label.kernel': 'Kernel', 'label.brand': 'Brand', 'label.total': 'Total', 'label.usage': 'Usage', 'label.swapTotal': 'Swap total', 'label.swapUsed': 'Swap used', 'label.device': 'Device', 'label.capacity': 'Capacity', 'label.filesystem': 'Filesystem', 'label.received': 'Received', 'label.sent': 'Sent', 'card.os': 'Operating system', 'fact.architecture': 'Architecture', 'fact.hostname': 'Host name',
    'card.cpu': 'Processor', 'fact.physicalCores': 'Physical cores', 'fact.logicalCores': 'Logical threads', 'card.memory': 'Memory',
    'fact.cpuUsage': 'Current usage', 'fact.cpuFrequency': 'Current frequency', 'fact.swap': 'Swap', 'card.storage': 'Storage', 'card.network': 'Network interfaces', 'count.disks': '{count} disks/volumes', 'count.interfaces': '{count} interfaces',
    'fact.used': 'Used', 'fact.available': 'Available', 'card.process': 'Current process', 'fact.uptime': 'Uptime',
    'fact.source': 'Data source', 'overview.product': 'Independent product', 'overview.policy': 'Collection policy',
    'overview.policyValue': 'Read-only, on demand, never uploaded', 'overview.sleep': 'Display sleep', 'sleep.preventing': 'Keeping awake',
    'sleep.allowed': 'Allow sleep', 'duration': '{days} days {hours} hours', 'footer.capabilities': 'sysinfo · Local read-only snapshot',
    'report.error': 'System status reporting failed: {error}', 'host.loading': 'Reading Hardware & System information…'
  },
  'ja-JP': {
    'title': 'システム情報', 'session.label': 'セッション', 'session.summary': '{os} · {arch} · {cores} スレッド',
    'session.pending': 'システムスナップショットを待機中', 'notice.loading': 'Rust でシステム情報を読み込み中…', 'notice.refreshed': 'システムスナップショットを更新しました',
    'notice.copied': 'システムスナップショットをコピーしました', 'action.copy': 'すべてコピー',
    'notice.sleepPrevented': 'アプリ終了またはスイッチ解除までディスプレイのスリープを防止します', 'notice.sleepRestored': 'システムのディスプレイスリープ設定を復元しました',
    'action.refresh': '更新', 'tab.system': 'システム', 'tab.cpu': 'プロセッサ', 'tab.memory': 'メモリ', 'tab.storage': 'ストレージ', 'tab.network': 'ネットワーク', 'empty': '表示できる情報がありません', 'label.platform': 'プラットフォーム', 'label.distribution': 'ディストリビューション', 'label.kernel': 'カーネル', 'label.brand': 'ブランド', 'label.total': '合計', 'label.usage': '使用率', 'label.swapTotal': 'スワップ合計', 'label.swapUsed': 'スワップ使用量', 'label.device': 'デバイス', 'label.capacity': '容量', 'label.filesystem': 'ファイルシステム', 'label.received': '受信', 'label.sent': '送信', 'card.os': 'オペレーティングシステム', 'fact.architecture': 'アーキテクチャ', 'fact.hostname': 'ホスト名',
    'card.cpu': 'プロセッサ', 'fact.physicalCores': '物理コア', 'fact.logicalCores': '論理スレッド', 'card.memory': 'メモリ',
    'fact.cpuUsage': '現在の使用率', 'fact.cpuFrequency': '現在の周波数', 'fact.swap': 'スワップ', 'card.storage': 'ストレージ', 'card.network': 'ネットワークインターフェース', 'count.disks': 'ディスク/ボリューム {count} 件', 'count.interfaces': 'インターフェース {count} 件',
    'fact.used': '使用済み', 'fact.available': '利用可能', 'card.process': '現在のプロセス', 'fact.uptime': '稼働時間',
    'fact.source': 'データソース', 'overview.product': '独立製品', 'overview.policy': '収集ポリシー',
    'overview.policyValue': '読み取り専用、必要時のみ、アップロードなし', 'overview.sleep': 'ディスプレイスリープ', 'sleep.preventing': 'スリープ防止中',
    'sleep.allowed': 'スリープを許可', 'duration': '{days} 日 {hours} 時間', 'footer.capabilities': 'sysinfo · ローカル読み取り専用スナップショット',
    'report.error': 'システム状態の報告に失敗しました：{error}', 'host.loading': 'ハードウェアとシステム情報を読み込み中…'
  }
})
