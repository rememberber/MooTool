import { defineMessages } from '../../app/localizedMessages'

export const toolFavoriteMessages = defineMessages({
  'zh-CN': {
    'label': '业务收藏', 'empty': '选择收藏配置', 'save': '收藏当前配置', 'delete': '删除收藏',
    'prompt.name': '收藏名称', 'confirm.delete': '删除收藏“{name}”？', 'error.payload': '收藏数据无法读取'
  },
  'en-US': {
    'label': 'Presets', 'empty': 'Choose a saved preset', 'save': 'Save current preset', 'delete': 'Delete preset',
    'prompt.name': 'Preset name', 'confirm.delete': 'Delete preset “{name}”?', 'error.payload': 'The saved preset cannot be read'
  },
  'ja-JP': {
    'label': 'プリセット', 'empty': '保存済みプリセットを選択', 'save': '現在の設定を保存', 'delete': 'プリセットを削除',
    'prompt.name': 'プリセット名', 'confirm.delete': 'プリセット「{name}」を削除しますか？', 'error.payload': '保存済みプリセットを読み込めません'
  }
})
