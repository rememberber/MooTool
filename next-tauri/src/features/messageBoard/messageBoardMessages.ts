import { defineMessages } from '../../app/localizedMessages'

export const messageBoardMessages = defineMessages({
  'zh-CN': {
    'title': '留言板', 'session.label': '会话', 'notice.ready': '留言展示牌已就绪', 'display.empty': '请输入展示内容',
    'notice.presenting': '展示模式已开启', 'error.empty': '展示内容不能为空', 'notice.duplicate': '该内容已经保存在常用留言中',
    'notice.saved': '已保存为 Tauri 本地常用留言', 'confirm.delete': '从常用留言中删除这条内容吗？',
    'notice.deleted': '常用留言已删除', 'notice.applied': '已应用常用留言', 'display.awake': '显示器保持唤醒',
    'display.mode': '展示模式', 'action.exit': '退出展示', 'action.start': '开始展示', 'content.title': '展示内容',
    'content.placeholder': '输入希望全屏展示的留言…', 'preset.away': '暂时离开，有事请留言', 'preset.closed': '今日已结束营业',
    'preset.break': '休息一下，马上回来', 'preset.focus': '正在专注，请勿打扰', 'preset.meeting': '会议进行中',
    'preset.quiet': '请保持安静，谢谢', 'style.title': '样式', 'theme.sunbeam': '暖阳', 'theme.coral': '珊瑚',
    'theme.cobalt': '钴蓝', 'theme.forest': '森林', 'theme.paper': '纸张', 'theme.midnight': '午夜',
    'align.left': '左对齐', 'align.center': '居中', 'size.label': '字号比例', 'preview.hint': '双击展示画面或按 Esc 可退出',
    'saved.title': '常用留言', 'action.saveCurrent': '保存当前内容', 'action.delete': '删除',
    'saved.empty': '还没有常用留言；可以把当前内容保存在 Tauri 独立数据库中。',
    'footer.capabilities': '全屏展示 · 自动适配字号 · 展示时防休眠', 'report.error': '留言板状态上报失败：{error}',
    'host.loading': '正在加载留言板…'
  },
  'en-US': {
    'title': 'Message Board', 'session.label': 'Session', 'notice.ready': 'Message display is ready', 'display.empty': 'Enter display text',
    'notice.presenting': 'Presentation mode started', 'error.empty': 'Display text cannot be empty', 'notice.duplicate': 'This message is already saved',
    'notice.saved': 'Saved to local Tauri messages', 'confirm.delete': 'Delete this saved message?',
    'notice.deleted': 'Saved message deleted', 'notice.applied': 'Saved message applied', 'display.awake': 'Display kept awake',
    'display.mode': 'Presentation mode', 'action.exit': 'Exit presentation', 'action.start': 'Start presentation', 'content.title': 'Display text',
    'content.placeholder': 'Enter a message to show full screen…', 'preset.away': 'Away for now — please leave a message', 'preset.closed': 'Closed for today',
    'preset.break': 'Taking a break — back soon', 'preset.focus': 'Focus in progress — please do not disturb', 'preset.meeting': 'Meeting in progress',
    'preset.quiet': 'Please keep quiet, thank you', 'style.title': 'Style', 'theme.sunbeam': 'Sunbeam', 'theme.coral': 'Coral',
    'theme.cobalt': 'Cobalt', 'theme.forest': 'Forest', 'theme.paper': 'Paper', 'theme.midnight': 'Midnight',
    'align.left': 'Left', 'align.center': 'Center', 'size.label': 'Text scale', 'preview.hint': 'Double-click the display or press Esc to exit',
    'saved.title': 'Saved messages', 'action.saveCurrent': 'Save current text', 'action.delete': 'Delete',
    'saved.empty': 'No saved messages yet. Save the current text in the independent Tauri database.',
    'footer.capabilities': 'Full-screen display · Auto-fit text · Prevent sleep while presenting', 'report.error': 'Message Board status reporting failed: {error}',
    'host.loading': 'Loading Message Board…'
  },
  'ja-JP': {
    'title': '伝言板', 'session.label': 'セッション', 'notice.ready': 'メッセージ表示の準備ができました', 'display.empty': '表示内容を入力してください',
    'notice.presenting': '表示モードを開始しました', 'error.empty': '表示内容を空にはできません', 'notice.duplicate': 'この内容はすでに保存されています',
    'notice.saved': 'Tauri ローカル定型文へ保存しました', 'confirm.delete': 'この定型文を削除しますか？',
    'notice.deleted': '定型文を削除しました', 'notice.applied': '定型文を適用しました', 'display.awake': 'ディスプレイのスリープを防止中',
    'display.mode': '表示モード', 'action.exit': '表示を終了', 'action.start': '表示を開始', 'content.title': '表示内容',
    'content.placeholder': '全画面で表示するメッセージを入力…', 'preset.away': '一時離席中です。ご用件をお残しください', 'preset.closed': '本日の営業は終了しました',
    'preset.break': '休憩中です。すぐ戻ります', 'preset.focus': '集中中です。邪魔しないでください', 'preset.meeting': '会議中です',
    'preset.quiet': '静かにしてください。ありがとうございます', 'style.title': 'スタイル', 'theme.sunbeam': '陽だまり', 'theme.coral': 'コーラル',
    'theme.cobalt': 'コバルト', 'theme.forest': 'フォレスト', 'theme.paper': 'ペーパー', 'theme.midnight': 'ミッドナイト',
    'align.left': '左揃え', 'align.center': '中央', 'size.label': '文字サイズ', 'preview.hint': '画面をダブルクリックするか Esc で終了',
    'saved.title': '定型文', 'action.saveCurrent': '現在の内容を保存', 'action.delete': '削除',
    'saved.empty': '定型文はまだありません。現在の内容を Tauri 独立データベースへ保存できます。',
    'footer.capabilities': '全画面表示 · 文字サイズ自動調整 · 表示中のスリープ防止', 'report.error': '伝言板状態の報告に失敗しました：{error}',
    'host.loading': '伝言板を読み込み中…'
  }
})
