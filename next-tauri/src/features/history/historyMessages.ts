import { defineMessages } from '../../app/localizedMessages'

export const historyMessages = defineMessages({
  'zh-CN': {
    'title': '操作历史', 'aria.dialog': '操作历史', 'description': '统一记录工具打开、处理结果和数据维护动作。',
    'action.close': '关闭', 'search.placeholder': '搜索工具、动作或摘要', 'status.all': '全部状态',
    'status.info': '信息', 'status.success': '成功', 'status.error': '失败', 'action.clear': '清空',
    'confirm.clear': '清空全部操作历史？', 'action.delete': '删除记录', 'empty': '没有匹配的操作历史',
    'footer.limit': '最多保留 {count} 条', 'footer.count': '{count} 条记录', 'tool.systemData': '数据与备份',
    'action.restore': '恢复到工具', 'action.copy': '复制', 'payload.input': '输入', 'payload.output': '输出', 'payload.empty': '此记录没有可恢复内容'
  },
  'en-US': {
    'title': 'Operation history', 'aria.dialog': 'Operation history', 'description': 'A unified record of tool launches, processing results, and data maintenance actions.',
    'action.close': 'Close', 'search.placeholder': 'Search tools, actions, or summaries', 'status.all': 'All statuses',
    'status.info': 'Info', 'status.success': 'Success', 'status.error': 'Error', 'action.clear': 'Clear',
    'confirm.clear': 'Clear all operation history?', 'action.delete': 'Delete record', 'empty': 'No matching operation history',
    'footer.limit': 'Keep up to {count}', 'footer.count': '{count} records', 'tool.systemData': 'Data & Backup',
    'action.restore': 'Restore in tool', 'action.copy': 'Copy', 'payload.input': 'Input', 'payload.output': 'Output', 'payload.empty': 'This record has no restorable payload'
  },
  'ja-JP': {
    'title': '操作履歴', 'aria.dialog': '操作履歴', 'description': 'ツールの起動、処理結果、データ保守操作をまとめて記録します。',
    'action.close': '閉じる', 'search.placeholder': 'ツール、操作、概要を検索', 'status.all': 'すべての状態',
    'status.info': '情報', 'status.success': '成功', 'status.error': 'エラー', 'action.clear': 'クリア',
    'confirm.clear': 'すべての操作履歴を消去しますか？', 'action.delete': '記録を削除', 'empty': '一致する操作履歴はありません',
    'footer.limit': '最大 {count} 件を保持', 'footer.count': '{count} 件', 'tool.systemData': 'データとバックアップ',
    'action.restore': 'ツールに復元', 'action.copy': 'コピー', 'payload.input': '入力', 'payload.output': '出力', 'payload.empty': '復元可能な内容がありません'
  }
})
