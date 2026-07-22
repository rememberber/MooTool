import type { AppLanguage } from '@/shared/contracts/settings'

export const languages = ['zh-CN', 'en-US', 'ja-JP'] as const satisfies readonly AppLanguage[]

export type Language = AppLanguage

export type MessageKey = keyof typeof messages['zh-CN']

export const languageLabels: Record<Language, string> = {
  'zh-CN': '中',
  'en-US': 'EN',
  'ja-JP': '日'
}

export const messages = {
  'zh-CN': {
    'common.toast.dismiss': '关闭通知',
    'common.resizePane': '调整分栏宽度',
    'common.loading': '正在加载…',
    'common.close': '关闭',
    'common.cancel': '取消',
    'findReplace.find': '查找',
    'findReplace.findPlaceholder': '查找',
    'findReplace.replacePlaceholder': '替换为',
    'findReplace.replace': '替换',
    'findReplace.replaceAll': '替换全部',
    'findReplace.regex': '使用正则',
    'findReplace.matchCase': '区分大小写',
    'findReplace.wholeWord': '全词匹配',
    'findReplace.foundPrefix': '共找到:',
    'findReplace.replacedPrefix': '已替换:',
    'findReplace.previous': '上一处',
    'findReplace.next': '下一处',
    'findReplace.noMatches': '未找到匹配内容',
    'app.nav.tools': '工具导航',
    'app.nav.home': '主页',
    'app.nav.quickNote': '随手记',
    'app.nav.textDiff': '文本对比',
    'app.nav.reformat': '格式化',
    'app.nav.json': 'JSON',
    'app.nav.java': '代码运行',
    'app.nav.ymlProperties': '配置文件转换',
    'app.nav.protobuf': 'Protobuf',
    'app.nav.variables': '环境变量',
    'app.nav.host': 'Host',
    'app.nav.net': '网络/IP',
    'app.nav.uaParse': 'UA 分析',
    'app.nav.timeConvert': '时间转换',
    'app.nav.translation': '翻译',
    'app.nav.calculator': '计算器',
    'app.nav.hardware': '系统信息',
    'app.nav.encode': '编码解码',
    'app.nav.crypto': '加解密/随机',
    'app.nav.qrCode': '二维码',
    'app.nav.http': 'HTTP 请求',
    'app.nav.diff': '文本 Diff',
    'app.nav.regex': '正则',
    'app.nav.colorBoard': '调色板',
    'app.nav.image': '图片助手',
    'app.nav.pdf': 'PDF',
    'app.nav.cron': 'Cron',
    'app.group.text': '笔记与文本',
    'app.group.dev': '开发与格式',
    'app.group.network': '网络与请求',
    'app.group.encode': '编码与加密',
    'app.group.daily': '实用工具',
    'app.group.system': '系统信息',
    'app.nav.recent': '最近',
    'app.nav.settings': '设置',
    'app.nav.search': '搜索',
    'app.nav.collapse': '收起导航栏',
    'app.nav.expand': '展开导航栏',
    'app.nav.manageGroups': '管理分组',
    'app.group.all': '全部工具',
    'app.group.manage.title': '管理功能分组',
    'app.group.manage.new': '新建分组',
    'app.group.manage.defaultName': '新分组 {number}',
    'app.group.manage.empty': '暂无自定义分组，点击“新建分组”开始创建。',
    'app.group.manage.name': '分组名称',
    'app.group.manage.tools': '选择工具',
    'app.group.manage.delete': '删除分组',
    'app.group.manage.deleteConfirm': '确定删除分组“{name}”吗？',
    'app.group.manage.nameRequired': '请输入分组名称。',
    'app.group.manage.toolRequired': '请至少选择一个工具。',
    'app.group.manage.saveFailed': '分组保存失败',
    'app.recent.empty': '暂无最近使用',
    'app.search.title': '搜索工具',
    'app.search.placeholder': '搜索功能…',
    'app.search.empty': '未找到匹配的功能',
    'app.search.close': '关闭搜索',
    'app.home.website': '打开 MooTool 主页',
    'app.home.tagline': '给开发者准备的桌面小工具',
    'app.home.author': 'Proudly by RememBerBer 周波',
    'app.home.about.title': '关于',
    'app.home.about.line1': '你好！感谢使用 MooTool。「Moo」是我女儿的小名。',
    'app.home.about.line2': '现在用 Swing 做项目的 Java 开发者越来越少了，但我依然热爱它。',
    'app.home.about.line2Note': '虽然你现在使用的已经不是 Swing 版了 😛',
    'app.home.about.line3': '所以每当有一点空闲时间，我都会投入到开发中。',
    'app.home.about.line4': '最终做出了一些成果，尽管网上已有很多类似工具。',
    'app.home.about.line5': '希望你使用它的愉快程度，不亚于我开发它时的投入。',
    'app.home.source.title': '源码',
    'app.home.help.title': '帮助我们做得更好',
    'app.home.help.issue': '提交问题或建议',
    'app.home.thanks.title': '致谢',
    'app.home.otherWorks.title': '其他作品',
    'app.home.wePush.desc': '专注批量推送的小而美的工具',
    'app.home.mooInfo.desc': 'OSHI 的可视化实现，查看系统与硬件信息',
    'app.home.sponsor.title': '赞助',
    'app.home.sponsor.prompt': '帮我续Tokens',
    'app.home.sponsor.tip': '感谢您的鼓励和支持',
    'app.home.contributors.title': '贡献者',
    'app.home.contributors.thanks': '感谢每一位参与 MooTool 的贡献者。',
    'app.placeholder': '这个工具页会按 JSON 工作台的结构继续迁移。',
    'toolWindow.detach': '在独立窗口中打开',
    'toolWindow.dock': '收回到功能区',
    'toolWindow.focus': '定位窗口',
    'toolWindow.detachedTitle': '{tool} 已在独立窗口中打开',
    'toolWindow.detachedDescription': '关闭独立窗口后，此功能会自动回到这里。',
    'toolWindow.invalid': '无法打开这个工具窗口。',
    'settings.title': '设置',
    'settings.close': '关闭设置',
    'settings.saveFailed': '设置保存失败',
    'settings.language': '语言',
    'settings.category.general': '常规',
    'settings.category.appearance': '外观',
    'settings.category.layout': '布局与习惯',
    'settings.category.editor': '编辑器',
    'settings.category.network': '网络',
    'settings.category.data': '数据与备份',
    'settings.category.vault': 'Vault',
    'settings.category.runtime': '运行环境',
    'settings.category.tools': '工具默认值',
    'settings.category.shortcuts': '快捷键',
    'settings.category.about': '关于与更新',
    'settings.group.application': '应用行为',
    'settings.group.theme': '主题与字体',
    'settings.group.navigation': '功能导航',
    'settings.group.editor': '编辑体验',
    'settings.group.proxy': 'HTTP 代理',
    'settings.group.timeouts': '超时与取消',
    'settings.group.storage': '数据目录',
    'settings.group.backup': '本地备份',
    'settings.backup.all': '完整备份',
    'settings.backup.database': '数据文件',
    'settings.backup.settings': '配置文件',
    'settings.backup.images': '图片目录',
    'settings.backup.export': '导出',
    'settings.backup.open': '打开目录',
    'settings.backup.success': '备份已导出',
    'settings.group.migration': '旧版数据迁移',
    'settings.migration.source': 'Java 版数据目录',
    'settings.migration.scan': '扫描',
    'settings.migration.ready': '发现 {count} 项可迁移数据',
    'settings.migration.databaseFound': '数据库已找到',
    'settings.migration.databaseMissing': '未找到旧数据库',
    'settings.migration.configFound': '配置已找到',
    'settings.migration.configMissing': '未找到旧配置',
    'settings.migration.import': '开始迁移',
    'settings.migration.importing': '正在迁移…',
    'settings.migration.confirmTitle': '迁移 Java 版数据',
    'settings.migration.confirmBody': '将迁移 {count} 项数据。MooTool Next 会先创建完整备份，不会修改 Java 版源文件。',
    'settings.migration.success': '已迁移 {count} 项数据',
    'settings.migration.alreadyMigrated': '该来源已经迁移完成',
    'settings.migration.warning.remotes': '两个旧 Vault 使用不同远程仓库，远程地址不会自动合并。',
    'settings.migration.warning.secrets': '旧配置中的代理密码和 Git Token 已跳过，请在设置中重新安全保存。',
    'app.migrationHint.title': '迁移 Java 版数据',
    'app.migrationHint.body': '若你之前使用过 Java 版 MooTool，可在「设置 → 数据与备份」中迁移历史数据。迁移不会修改或删除 Java 版原始数据。',
    'app.migrationHint.openSettings': '打开设置',
    'app.migrationHint.dismiss': '知道了',
    'settings.group.vaultPaths': 'Vault 路径',
    'settings.group.git': 'Git 同步',
    'settings.group.runtimes': '本机运行时',
    'settings.group.toolDefaults': '默认参数',
    'settings.group.shortcuts': '应用快捷键',
    'settings.autoCheckUpdates': '启动时检查更新',
    'settings.autoDownloadUpdates': '自动静默下载新版',
    'settings.startMaximized': '启动时最大化',
    'settings.trayEnabled': '启用系统托盘',
    'settings.closeBehavior': '关闭主窗口时',
    'settings.close.ask': '每次询问',
    'settings.close.hide': '隐藏',
    'settings.close.quit': '退出',
    'settings.interfaceStyle': '界面风格',
    'settings.interfaceStyle.modern': '现代主题',
    'settings.interfaceStyle.quiet': '安静主题',
    'settings.theme': '颜色模式',
    'settings.theme.system': '跟随系统',
    'settings.theme.light': '浅色',
    'settings.theme.dark': '深色',
    'settings.accentColor': '强调色',
    'settings.fontSize': '全局字号',
    'settings.unifiedBackground': '统一工作区背景',
    'settings.navigationStyle': '导航样式',
    'settings.navigation.classic': '经典',
    'settings.navigation.card': '卡片',
    'settings.navigation.grouped': '分组',
    'settings.navigation.toolsTitle': '左侧工具列表',
    'settings.navigation.toolsDescription': '选择在“全部工具”区域显示的项目。主页、搜索、最近使用和自定义分组不受影响。',
    'settings.navigation.showAll': '全部显示',
    'settings.navigation.hideAll': '全部隐藏',
    'settings.showRecent': '显示最近使用',
    'settings.compactNavigation': '紧凑导航',
    'settings.showSeparators': '显示分割线',
    'settings.hideNavigationTitles': '隐藏导航标题',
    'settings.sqlDialect': 'SQL 方言',
    'settings.jsonFontSize': 'JSON 字号',
    'settings.quickNoteFontSize': '随手记字号',
    'settings.softWrap': '编辑器自动换行',
    'settings.proxyEnabled': '使用 HTTP 代理',
    'settings.proxyHost': 'Host',
    'settings.proxyPort': '端口',
    'settings.proxyUsername': '用户名',
    'settings.proxyPassword': '密码',
    'settings.requestTimeout': 'HTTP 请求超时（毫秒）',
    'settings.translationTimeout': '翻译超时（毫秒）',
    'settings.dataDirectory': '数据存储位置',
    'settings.quickNoteVault': '随手记 Vault',
    'settings.jsonVault': 'JSON Vault',
    'settings.gitRemote': '远程仓库地址',
    'settings.gitUsername': 'Git 用户名',
    'settings.gitToken': 'Git 私人令牌',
    'settings.autoCommit': '自动创建 Git 检查点',
    'settings.autoCommitIdleSeconds': '编辑空闲提交（秒）',
    'settings.autoCommitInactiveSeconds': '窗口失焦提交（秒）',
    'settings.autoPullMinutes': '自动拉取间隔（分钟）',
    'settings.hideGitignoredFiles': '隐藏 .gitignore 匹配文件',
    'settings.runtime.detect': '重新检测',
    'settings.runtime.notFound': '未检测到',
    'settings.runtime.auto': '自动从系统环境查找',
    'settings.runtime.path': '路径',
    'settings.qrCodeSize': '二维码尺寸',
    'settings.qrErrorCorrection': '二维码纠错级别',
    'settings.randomStringLength': '随机字符串长度',
    'settings.exportDirectory': '默认导出目录',
    'settings.translationProvider': '默认翻译源',
    'settings.translationSource': '默认源语言',
    'settings.translationTarget': '默认目标语言',
    'settings.shortcut.search': '搜索工具',
    'settings.shortcut.settings': '打开设置',
    'settings.chooseDirectory': '选择目录',
    'settings.version': '版本 {version}',
    'settings.update.check': '检查更新',
    'settings.update.checking': '正在检查…',
    'settings.update.latest': '当前已是最新版本',
    'settings.update.available': '发现新版本 {version}',
    'settings.update.failed': '检查更新失败，请稍后重试',
    'settings.update.target': '{product} · {platform}/{architecture}',
    'settings.update.download': '下载更新',
    'settings.update.downloading': '后台下载中 {percent}%',
    'settings.update.ready': '新版本 {version} 已就绪',
    'settings.update.whatsNew': '更新内容',
    'settings.update.installRestart': '安装并重启',
    'settings.update.manualInstall': '更新已下载，打开 DMG 安装',
    'settings.update.openDownloaded': '打开 DMG',
    'settings.update.downloadFailed': '更新下载失败，请稍后重试',
    'settings.update.installFailed': '无法启动更新安装，请稍后重试',
    'settings.update.noDownload': '暂未找到适用于本机的安装包，请前往发布页查看',
    'settings.update.openRelease': '打开发布页',
    'settings.update.project': '项目主页',
    'settings.secret.stored': '已安全保存',
    'settings.secret.saved': '敏感信息已安全保存',
    'settings.secret.save': '保存',
    'settings.secret.clear': '清除',
    'settings.secret.unavailable': '系统安全存储当前不可用',
    'history.title': '历史记录',
    'history.search': '搜索历史记录…',
    'history.empty': '暂无历史记录',
    'history.clearAll': '全部清空',
    'history.confirmClear': '确定清空该工具的全部历史记录吗？',
    'history.copyInput': '复制输入',
    'history.copyOutput': '复制输出',
    'history.delete': '删除记录',
    'json.title': 'JSON 工作台',
    'json.editor.label': 'JSON 编辑器',
    'json.valid.idle': '等待输入 JSON',
    'json.valid.ok': '有效 JSON · {type}',
    'json.valid.error': 'JSON 解析失败',
    'json.error.empty': '请输入 JSON 内容',
    'json.error.notString': '当前内容不是 JSON 字符串',
    'json.error.duplicateKeys': '发现重复 Key：{paths}',
    'json.error.emptyXml': '请输入 XML 内容',
    'json.error.emptyPath': '请输入 JSON Path',
    'json.error.objectRequired': '该操作需要 JSON Object',
    'json.error.emptyJavaBean': '请输入 JavaBean 类代码',
    'json.error.noJavaFields': '未找到可转换的 Java 字段',
    'json.action.format': '格式化',
    'json.action.compress': '压缩',
    'json.action.wrap': '换行',
    'json.action.nowrap': '单行',
    'json.action.copy': '复制',
    'json.action.copied': '已复制',
    'json.action.clear': '清空',
    'json.action.find': '查找',
    'json.action.import': '导入',
    'json.action.export': '导出',
    'json.action.history': '历史',
    'json.action.more': '更多工具',
    'json.action.escape': 'JSON 字符串转义',
    'json.action.unescape': 'JSON 字符串还原',
    'json.action.escapeText': '字符串转义',
    'json.action.unescapeText': '字符串反转义',
    'json.action.jsonToXml': 'JSON 转 XML',
    'json.action.xmlToJson': 'XML 转 JSON',
    'json.action.beanToJson': 'JavaBean 转 JSON',
    'json.action.jsonToBean': 'JSON 转 JavaBean',
    'json.action.swap': 'Key / Value 互换',
    'json.notice.formatted': '已格式化',
    'json.notice.compressed': '已压缩',
    'json.notice.copied': '已复制到剪贴板',
    'json.notice.copyFailed': '复制失败',
    'json.notice.failed': '处理失败',
    'json.notice.escaped': '已转为 JSON 字符串',
    'json.notice.unescaped': '已还原 JSON 字符串',
    'json.notice.imported': '文件已导入',
    'json.notice.exported': '文件已导出',
    'json.notice.pathApplied': '已应用 JSON Path',
    'json.notice.noMatches': '未找到匹配内容',
    'json.panel.actions': '操作',
    'json.panel.result': '结果',
    'json.panel.format': '格式化选项',
    'json.panel.convert': '转换',
    'json.panel.jsonPath': 'JSON Path',
    'json.format.indent': '缩进空格',
    'json.format.sortKeys': '对 Object Key 排序',
    'json.format.ignoreCase': '排序和重复检查忽略大小写',
    'json.format.duplicateKeys': '格式化前检查重复 Key',
    'json.format.apply': '应用自定义格式',
    'json.find.placeholder': '在 JSON 中查找…',
    'json.find.matches': '{count} 处匹配',
    'json.find.next': '下一处',
    'json.find.close': '关闭查找',
    'json.path.placeholder': '例如 $.store.book[*].title',
    'json.path.query': '查询',
    'json.path.pick': '可视化选择',
    'json.pathPicker.title': '选择 JSON Path',
    'json.pathPicker.use': '使用此路径',
    'json.pathPicker.path': '路径',
    'json.pathPicker.preview': '值预览',
    'json.dialog.input': '输入',
    'json.dialog.output': '输出',
    'json.dialog.run': '转换',
    'json.dialog.className': '根类名',
    'json.dialog.useOutput': '使用输出',
    'json.vault.title': 'JSON Vault',
    'json.vault.new': '新建片段',
    'json.vault.save': '保存片段',
    'json.vault.delete': '删除片段',
    'json.vault.refresh': '刷新 Vault',
    'json.vault.empty': '暂无 JSON 片段',
    'json.vault.fileName': '文件名或相对路径',
    'json.vault.fileNameHint': '例如 drafts/request.json',
    'json.vault.create': '创建',
    'json.vault.saved': '片段已保存',
    'json.vault.created': '片段已创建',
    'json.vault.deleted': '片段已删除',
    'json.vault.confirmDelete': '确定删除“{name}”吗？',
    'json.vault.confirmDiscard': '当前片段尚未保存，确定放弃修改吗？'
    ,'json.vault.newFolder': '新建文件夹'
    ,'json.vault.expandAll': '展开全部'
    ,'json.vault.collapseAll': '折叠全部'
    ,'json.vault.defaultFolder': '新文件夹'
    ,'json.vault.folderName': '文件夹相对路径'
    ,'json.vault.folderCreated': '文件夹已创建'
    ,'json.vault.rename': '重命名'
    ,'json.vault.renameName': '新名称'
    ,'json.vault.renamed': '条目已重命名'
    ,'json.vault.move': '移动'
    ,'json.vault.moveTo': '目标文件夹'
    ,'json.vault.moved': '条目已移动'
    ,'json.vault.duplicate': '复制片段'
    ,'json.vault.duplicated': '片段副本已创建'
    ,'json.vault.openFolder': '在文件管理器中打开 Vault'
    ,'json.vault.sort': '排序方式'
    ,'json.vault.sortName': '按名称'
    ,'json.vault.sortModified': '按修改时间'
    ,'json.vault.more': '更多 Vault 操作'
    ,'json.vault.root': 'Vault 根目录'
    ,'json.git.title': 'JSON Vault Git'
    ,'json.git.open': '打开 Git 面板'
    ,'json.git.unavailable': '未检测到 Git 命令'
    ,'json.git.noRepo': '当前 Vault 尚未初始化 Git 仓库'
    ,'json.git.init': '初始化 Git'
    ,'json.git.refresh': '刷新'
    ,'json.git.fetch': 'Fetch'
    ,'json.git.pull': 'Pull'
    ,'json.git.push': 'Push'
    ,'json.git.branch': '分支 {branch}'
    ,'json.git.sync': '↑{ahead} ↓{behind}'
    ,'json.git.remote': '远程仓库'
    ,'json.git.remotePlaceholder': 'https://... 或 git@host:repo.git'
    ,'json.git.saveRemote': '保存远程地址'
    ,'json.git.removeRemote': '删除远程地址'
    ,'json.git.changes': '未提交变更'
    ,'json.git.history': '提交历史'
    ,'json.git.emptyChanges': '工作区很干净'
    ,'json.git.emptyHistory': '暂无提交记录'
    ,'json.git.diff': '变更详情'
    ,'json.git.noDiff': '选择变更或提交查看 Diff'
    ,'json.git.commitMessage': '提交说明'
    ,'json.git.defaultMessage': 'MooTool JSON checkpoint'
    ,'json.git.commit': '提交全部变更'
    ,'json.git.conflict': '冲突'
    ,'json.git.done': 'Git 操作已完成'
    ,'json.git.discard': '丢弃变更'
    ,'json.git.confirmDiscard': '确定丢弃“{path}”的本地变更吗？此操作无法撤销。'
    ,'json.git.abortMerge': '中止合并 / Rebase'
    ,'json.git.confirmAbort': '确定中止当前合并或 Rebase 并恢复操作前状态吗？'
    ,'json.git.continueOperation': '继续合并 / Rebase'
    ,'json.git.useOurs': '使用本地版本'
    ,'json.git.useTheirs': '使用远程版本'
    ,'time.title': '时间转换'
    ,'time.current': '当前时间'
    ,'time.timestamp': '时间戳 (Unix)'
    ,'time.localTime': '本地时间'
    ,'time.timezone': '时区'
    ,'time.unit.second': '秒 (s)'
    ,'time.unit.millisecond': '毫秒 (ms)'
    ,'time.toLocal': '转为本地时间'
    ,'time.toTimestamp': '转为时间戳'
    ,'time.copy': '复制'
    ,'time.clock': '大屏时钟'
    ,'time.history': '历史'
    ,'time.formatHint': '格式：yyyy-MM-dd HH:mm:ss'
    ,'time.error.timestamp': '请输入有效的 Unix 时间戳'
    ,'time.error.localTime': '请按 yyyy-MM-dd HH:mm:ss 输入有效时间'
    ,'time.notice.toLocal': '已转换为 {zone} 时间'
    ,'time.notice.toTimestamp': '已转换为 Unix 时间戳'
    ,'time.notice.copied': '已复制'
    ,'time.clock.close': '退出大屏时钟'
    ,'common.action.copy': '复制'
    ,'common.action.paste': '粘贴'
    ,'common.action.clear': '清空'
    ,'common.action.history': '历史'
    ,'common.action.swap': '交换'
    ,'common.action.apply': '应用'
    ,'common.action.save': '保存'
    ,'common.action.delete': '删除'
    ,'common.input': '输入'
    ,'common.output': '输出'
    ,'common.result': '结果'
    ,'common.name': '名称'
    ,'common.description': '说明'
    ,'common.yes': '是'
    ,'common.no': '否'
    ,'common.error.process': '处理失败：{message}'
    ,'favorite.title': '收藏夹'
    ,'favorite.add': '收藏当前内容'
    ,'favorite.empty': '暂无收藏'
    ,'favorite.namePlaceholder': '输入收藏名称'
    ,'favorite.saved': '收藏已保存'
    ,'favorite.deleted': '收藏已删除'
    ,'encode.title': '编码解码'
    ,'encode.tab.unicode': 'Native / Unicode'
    ,'encode.tab.url': 'URL 转码'
    ,'encode.tab.hex': 'Native / 16 进制'
    ,'encode.tab.ascii': 'Native / ASCII'
    ,'encode.native': 'Native 文本'
    ,'encode.unicode': 'Unicode 文本'
    ,'encode.url': 'URL 原文'
    ,'encode.encoded': '编码结果'
    ,'encode.hex': '十六进制'
    ,'encode.ascii': 'ASCII'
    ,'encode.toUnicode': '转为 Unicode'
    ,'encode.fromUnicode': '还原 Native'
    ,'encode.urlEncode': 'URL 编码'
    ,'encode.urlDecode': 'URL 解码'
    ,'encode.toHex': '转为 Hex'
    ,'encode.fromHex': '还原文本'
    ,'encode.toAscii': '转为 ASCII'
    ,'encode.fromAscii': '还原文本'
    ,'encode.charset': '字符集'
    ,'encode.asciiDecimal': '十进制'
    ,'encode.asciiHex': '十六进制'
    ,'ua.title': 'UA 分析'
    ,'ua.input': 'User-Agent 输入'
    ,'ua.preset': '选择预设 UA…'
    ,'ua.parse': '解析'
    ,'ua.browser': '浏览器'
    ,'ua.browserVersion': '浏览器版本'
    ,'ua.engine': '渲染引擎'
    ,'ua.engineVersion': '引擎版本'
    ,'ua.os': '操作系统'
    ,'ua.osVersion': '系统版本'
    ,'ua.deviceType': '设备类型'
    ,'ua.deviceBrand': '设备品牌'
    ,'ua.deviceModel': '设备型号'
    ,'ua.mobile': '移动端'
    ,'ua.bot': '爬虫 / Bot'
    ,'ua.empty': '请输入 User-Agent'
    ,'ua.unknown': '未知'
    ,'calculator.title': '计算器'
    ,'calculator.expression': '表达式'
    ,'calculator.calculate': '计算'
    ,'calculator.base': '进制转换'
    ,'calculator.decimal': '十进制'
    ,'calculator.hex': '十六进制'
    ,'calculator.binary': '二进制'
    ,'calculator.number': '数学运算'
    ,'calculator.first': '数值 1'
    ,'calculator.second': '数值 2'
    ,'calculator.gcd': '最大公约数'
    ,'calculator.lcm': '最小公倍数'
    ,'calculator.n': 'n'
    ,'calculator.m': 'm'
    ,'calculator.permutation': '排列 A(n,m)'
    ,'calculator.combination': '组合 C(n,m)'
    ,'calculator.history': '计算记录'
    ,'regex.title': '正则表达式'
    ,'regex.tab.test': '匹配测试'
    ,'regex.tab.common': '常用正则'
    ,'regex.expression': '正则表达式'
    ,'regex.source': '待匹配文本'
    ,'regex.matches': '{count} 个匹配'
    ,'regex.noMatches': '没有匹配结果'
    ,'regex.flag.global': '全局'
    ,'regex.flag.ignoreCase': '忽略大小写'
    ,'regex.flag.multiline': '多行'
    ,'regex.flag.dotAll': '点号匹配换行'
    ,'regex.invalid': '无效正则：{message}'
    ,'regex.common.phone': '中国大陆手机号'
    ,'regex.common.email': '电子邮箱'
    ,'regex.common.domain': '域名'
    ,'regex.common.chinese': '汉字'
    ,'regex.common.integer': '整数'
    ,'regex.common.color': '颜色代码'
    ,'regex.common.ipv4': 'IPv4 地址'
    ,'regex.common.account': '账号校验'
    ,'regex.common.htmlId': 'HTML 标签 id 属性'
    ,'regex.common.jpg': 'JPG 图片链接'
    ,'regex.common.magnet': '磁力链接'
    ,'regex.common.alnum': '英文和数字'
    ,'regex.common.len3to20': '长度为 3-20 的所有字符'
    ,'regex.common.letters26': '26 个英文字母'
    ,'regex.common.wordUnderscore': '数字、字母或下划线'
    ,'regex.common.cnEnNum': '中文、英文、数字和下划线'
    ,'regex.common.noSpecial': '不含特殊字符'
    ,'regex.common.positiveInt': '正整数'
    ,'regex.common.negativeInt': '负整数'
    ,'regex.common.nonNegativeInt': '非负整数'
    ,'regex.common.float': '浮点数'
    ,'cron.title': 'Cron 表达式'
    ,'cron.expression': 'Cron 表达式'
    ,'cron.builder': '表达式构建器'
    ,'cron.second': '秒'
    ,'cron.minute': '分钟'
    ,'cron.hour': '小时'
    ,'cron.day': '日'
    ,'cron.month': '月'
    ,'cron.week': '星期'
    ,'cron.year': '年（可选）'
    ,'cron.parse': '解析运行时间'
    ,'cron.nextRuns': '最近 10 次运行时间'
    ,'cron.humanReadable': '自然语言'
    ,'cron.preset': '常用表达式'
    ,'cron.everyMinute': '每分钟'
    ,'cron.everyHour': '每小时'
    ,'cron.everyDay': '每天午夜'
    ,'cron.weekdays': '工作日上午 9 点'
    ,'cron.invalid': '无法解析 Cron：{message}'
    ,'diff.title': '文本对比'
    ,'diff.sideBySide': '并排对比'
    ,'diff.unified': '统一差异'
    ,'diff.left': '原始文本'
    ,'diff.right': '新文本'
    ,'diff.compare': '对比'
    ,'diff.ignoreWhitespace': '忽略空白差异'
    ,'diff.realtime': '实时对比'
    ,'diff.copy': '复制差异'
    ,'diff.previous': '上一处'
    ,'diff.next': '下一处'
    ,'diff.summary': '新增 {added} · 删除 {removed} · 变更 {changed}'
    ,'diff.identical': '两侧文本一致'
    ,'diff.highlightMode': '高亮模式'
    ,'diff.highlightBoth': '双层高亮'
    ,'diff.highlightCharacters': '仅字符'
    ,'diff.highlightLines': '仅整行'
    ,'diff.displayMode': '显示模式'
    ,'diff.unifiedPanel': '统一差异格式'
    ,'diff.status.ready': '准备就绪'
    ,'diff.status.enterText': '请输入要对比的文本'
    ,'diff.status.characterComplete': '字符差异 {count} 处'
    ,'diff.status.complete': '对比完成，共发现 {count} 处差异'
    ,'diff.status.cleared': '已清空'
    ,'diff.status.swapped': '已交换文本'
    ,'diff.status.copied': '差异结果已复制到剪贴板'
    ,'diff.status.noCopy': '没有差异结果可复制'
    ,'diff.status.navigation': '跳至第 {current}/{total} 处差异'
    ,'config.title': '配置文件转换'
    ,'config.tab.convert': 'YAML / Properties'
    ,'config.tab.validate': 'YAML 校验'
    ,'config.properties': 'Properties'
    ,'config.yaml': 'YAML'
    ,'config.toYaml': '转为 YAML'
    ,'config.toProperties': '转为 Properties'
    ,'config.validate': '校验'
    ,'config.format': '格式化'
    ,'config.valid': 'YAML 格式有效'
    ,'config.invalid': 'YAML 格式无效：{message}'
    ,'common.action.format': '格式化'
    ,'common.export': '导出'
    ,'common.help': '帮助'
    ,'common.import': '导入'
    ,'common.processing': '处理中…'
    ,'common.rename': '重命名'
    ,'common.save': '保存'
    ,'common.saved': '已保存'
    ,'reformat.title': '格式化'
    ,'reformat.tab.text': '文本'
    ,'reformat.tab.file': '文件'
    ,'reformat.type': '类型'
    ,'reformat.indent': '缩进'
    ,'reformat.input': '待格式化内容'
    ,'reformat.chooseFile': '选择文件'
    ,'reformat.noFile': '尚未选择文件'
    ,'reformat.original': '原始内容'
    ,'reformat.result': '格式化结果'
    ,'reformat.formatted': '格式化完成'
    ,'reformat.historySummary': '{type} 格式化'
    ,'crypto.title': '加解密 / 随机'
    ,'crypto.tab.symmetric': '对称加密'
    ,'crypto.tab.asymmetric': '非对称加密'
    ,'crypto.tab.digest': '摘要'
    ,'crypto.tab.base': 'Base64 / Base32'
    ,'crypto.tab.random': '随机生成'
    ,'crypto.algorithm': '算法'
    ,'crypto.key': '密钥'
    ,'crypto.keyHint': '请输入符合算法长度要求的密钥'
    ,'crypto.plainText': '明文'
    ,'crypto.cipherText': '密文'
    ,'crypto.cipherOrSignature': '密文 / 签名'
    ,'crypto.encrypt': '加密'
    ,'crypto.decrypt': '解密'
    ,'crypto.generateKeyPair': '生成密钥对'
    ,'crypto.keyGenerated': '密钥对已生成'
    ,'crypto.publicKey': '公钥'
    ,'crypto.privateKey': '私钥'
    ,'crypto.publicEncrypt': '公钥加密'
    ,'crypto.privateDecrypt': '私钥解密'
    ,'crypto.privateEncrypt': '私钥加密'
    ,'crypto.publicDecrypt': '公钥解密'
    ,'crypto.rsaOnly': '该操作仅支持 RSA'
    ,'crypto.sign': '签名'
    ,'crypto.verify': '验签'
    ,'crypto.verified': '签名验证通过'
    ,'crypto.notVerified': '签名验证失败'
    ,'crypto.digest': '计算摘要'
    ,'crypto.fileDigest': '文件摘要'
    ,'crypto.textDigest': '文本摘要'
    ,'crypto.digestInput': '摘要输入'
    ,'crypto.digestResult': '摘要结果'
    ,'crypto.encode': '编码'
    ,'crypto.decode': '解码'
    ,'crypto.length': '长度'
    ,'crypto.generate': '生成'
    ,'crypto.random.uuid': 'UUID'
    ,'crypto.random.digits': '随机数字'
    ,'crypto.random.string': '随机字符串'
    ,'crypto.random.password': '随机密码'
    ,'color.title': '调色板'
    ,'color.select': '选择颜色'
    ,'color.inputColor': '输入颜色'
    ,'color.picker': '屏幕取色'
    ,'color.freePick': '自由取色'
    ,'color.eyeDropperUnavailable': '当前系统不支持屏幕取色'
    ,'color.format': '格式'
    ,'color.code': '颜色代码'
    ,'color.current': '当前颜色'
    ,'color.compare': '对比色'
    ,'color.operation.invert': '反色'
    ,'color.operation.intersect': '交集'
    ,'color.operation.add': '相加'
    ,'color.operation.difference': '差值'
    ,'color.operation.average': '平均'
    ,'color.operation.swap': '交换'
    ,'color.themeColors': '主题色'
    ,'color.theme': '主题'
    ,'color.theme.default': '默认'
    ,'color.theme.theme1': '主题 1'
    ,'color.theme.theme2': '主题 2'
    ,'color.theme.theme3': '主题 3'
    ,'color.theme.theme4': '主题 4'
    ,'color.theme.theme5': '主题 5'
    ,'color.theme.china': '中国传统色'
    ,'color.standardColors': '标准色'
    ,'color.shiftHint': '按住 Shift 选择对比色。'
    ,'qrcode.title': '二维码'
    ,'qrcode.tab.generate': '生成'
    ,'qrcode.tab.recognize': '识别'
    ,'qrcode.tab.history': '历史'
    ,'qrcode.content': '二维码内容'
    ,'qrcode.size': '尺寸'
    ,'qrcode.correction': '纠错级别'
    ,'qrcode.logo': '中心 Logo'
    ,'qrcode.chooseLogo': '选择 Logo'
    ,'qrcode.level.L': 'L · 约 7%'
    ,'qrcode.level.M': 'M · 约 15%'
    ,'qrcode.level.Q': 'Q · 约 25%'
    ,'qrcode.level.H': 'H · 约 30%'
    ,'qrcode.generate': '生成二维码'
    ,'qrcode.generated': '二维码已生成'
    ,'qrcode.preview': '二维码预览'
    ,'qrcode.chooseImage': '选择二维码图片'
    ,'qrcode.fromClipboard': '从剪贴板读取'
    ,'qrcode.clipboard': '剪贴板图片'
    ,'qrcode.clipboardEmpty': '剪贴板中没有图片'
    ,'qrcode.recognize': '识别二维码'
    ,'qrcode.recognized': '二维码识别完成'
    ,'qrcode.sourceImage': '待识别图片'
    ,'qrcode.result': '识别结果'
    ,'qrcode.history.generate': '生成二维码'
    ,'qrcode.history.recognize': '识别二维码'
    ,'protobuf.title': 'Protobuf'
    ,'protobuf.tab.json': 'JSON / Binary'
    ,'protobuf.tab.wire': 'Wire 解析'
    ,'protobuf.tab.convert': 'Hex / Base64'
    ,'protobuf.definition': 'Proto 定义'
    ,'protobuf.message': '消息类型'
    ,'protobuf.binaryFormat': '二进制格式'
    ,'protobuf.toBinary': '转为 Binary'
    ,'protobuf.toJson': '转为 JSON'
    ,'protobuf.binary': '二进制内容'
    ,'protobuf.wireInput': 'Wire 数据'
    ,'protobuf.decode': '解析'
    ,'protobuf.wireOutput': 'Wire 字段'
    ,'protobuf.hexToBase64': 'Hex 转 Base64'
    ,'protobuf.base64ToHex': 'Base64 转 Hex'
    ,'protobuf.history.jsonToBinary': 'JSON 转 Binary'
    ,'protobuf.history.binaryToJson': 'Binary 转 JSON'
    ,'protobuf.history.format': '格式化 Proto'
    ,'protobuf.history.wire': '解析 Wire'
    ,'protobuf.history.hexToBase64': 'Hex 转 Base64'
    ,'protobuf.history.base64ToHex': 'Base64 转 Hex'
    ,'pdf.title': 'PDF'
    ,'pdf.tab.split': '拆分 PDF'
    ,'pdf.tab.merge': '合并 PDF'
    ,'pdf.addTask': '添加拆分任务'
    ,'pdf.addFile': '添加 PDF'
    ,'pdf.startSplit': '开始拆分'
    ,'pdf.startMerge': '开始合并'
    ,'pdf.selectTask': '请至少选择一个拆分任务'
    ,'pdf.selectTwo': '请至少选择两个 PDF 文件'
    ,'pdf.confirmSplit': '确认开始拆分选中的 PDF 吗？'
    ,'pdf.splitComplete': '拆分完成，共输出 {count} 页'
    ,'pdf.mergeComplete': '合并完成，共 {count} 页'
    ,'pdf.fileName': '文件名'
    ,'pdf.pageRange': '处理页码'
    ,'pdf.mergeRange': '合并页码'
    ,'pdf.rule': '拆分规则'
    ,'pdf.customRule': '输出页码'
    ,'pdf.progress': '状态'
    ,'pdf.output': '最近输出'
    ,'pdf.empty': '尚未添加 PDF 任务'
    ,'pdf.pages': '页'
    ,'pdf.select': '选择'
    ,'pdf.rule.odd': '奇数页'
    ,'pdf.rule.even': '偶数页'
    ,'pdf.rule.custom': '自定义'
    ,'pdf.status.ready': '待处理'
    ,'pdf.status.running': '处理中'
    ,'pdf.status.done': '已完成'
    ,'pdf.status.error': '失败'
    ,'pdf.helpSplitTitle': 'PDF 拆分说明'
    ,'pdf.helpMergeTitle': 'PDF 合并说明'
    ,'pdf.help.split1': '单次最多添加 20 个 PDF 拆分任务。'
    ,'pdf.help.split2': '处理页码支持 1-5、8、10-12 这类范围。'
    ,'pdf.help.split3': '拆分规则可选择奇数页、偶数页或自定义页码。'
    ,'pdf.help.split4': '输出文件默认使用 _split.pdf 后缀。'
    ,'pdf.help.merge1': '至少选择两个 PDF 文件才能合并。'
    ,'pdf.help.merge2': '文件会按列表顺序和填写的页码范围合并。'
    ,'pdf.help.merge3': '当前仅支持未加密 PDF。'
    ,'image.title': '图片助手'
    ,'image.screenshot': '截图'
    ,'image.fromClipboard': '剪贴板'
    ,'image.import': '导入图片'
    ,'image.fromBase64': '从 Base64 导入'
    ,'image.compress': '压缩'
    ,'image.watermark': '水印'
    ,'image.copy': '复制图片'
    ,'image.toBase64': '转 Base64'
    ,'image.toggleList': '显示或隐藏图片列表'
    ,'image.library': '图片库'
    ,'image.empty': '图片库为空'
    ,'image.select': '选择图片'
    ,'image.emptyPreview': '选择或导入一张图片'
    ,'image.zoomIn': '放大'
    ,'image.zoomOut': '缩小'
    ,'image.original': '原始尺寸'
    ,'image.fit': '适应窗口'
    ,'image.base64Import': '导入 Base64 图片'
    ,'image.base64Export': '图片 Base64'
    ,'image.compressTitle': '压缩图片'
    ,'image.watermarkTitle': '添加水印'
    ,'image.startProcess': '开始处理'
    ,'image.selectedCount': '已选择 {count} 张图片'
    ,'image.quality': '质量'
    ,'image.scale': '尺寸比例'
    ,'image.outputFormat': '输出格式'
    ,'image.format.auto': '保持原格式'
    ,'image.watermarkText': '水印文字'
    ,'image.opacity': '透明度'
    ,'image.position': '位置'
    ,'image.fontSize': '字号'
    ,'image.color': '颜色'
    ,'image.diagonal': '文字倾斜显示'
    ,'image.outputMode': '输出方式'
    ,'image.keepOriginal': '保留原图并生成新文件'
    ,'image.overwrite': '覆盖原图'
    ,'image.position.bottom-right': '右下'
    ,'image.position.bottom-left': '左下'
    ,'image.position.top-right': '右上'
    ,'image.position.top-left': '左上'
    ,'image.position.center': '居中'
    ,'image.position.tile': '平铺'
    ,'image.font.auto': '自动'
    ,'image.font.small': '小'
    ,'image.font.medium': '中'
    ,'image.font.large': '大'
    ,'image.crop.x': 'X'
    ,'image.crop.y': 'Y'
    ,'image.crop.width': '宽度'
    ,'image.crop.height': '高度'
    ,'image.captureOverlayHint': '拖拽鼠标框选截图区域'
    ,'image.captureOverlayKeys': '回车确认 · Esc 或右键取消'
    ,'image.captureConfirm': '确认截图'
    ,'image.saveCapture': '保存截图'
    ,'image.clipboardEmpty': '剪贴板中没有图片'
    ,'image.imported': '图片已导入'
    ,'image.captureUnavailable': '未找到可截图的显示器'
    ,'image.saveName': '保存名称'
    ,'image.renamePrompt': '输入新的图片名称'
    ,'image.confirmDelete': '确认删除选中的 {count} 张图片吗？'
    ,'image.exported': '已导出到 {directory}'
    ,'image.processComplete': '已处理 {count} 张图片'
    ,'common.new': '新建'
    ,'common.add': '添加'
    ,'common.stop': '停止'
    ,'common.refresh': '刷新'
    ,'common.search': '搜索'
    ,'common.convert': '转换'
    ,'http.title': 'HTTP 请求'
    ,'http.untitled': '未命名请求'
    ,'http.urlRequired': '请输入请求 URL'
    ,'http.saveName': '请求名称'
    ,'http.confirmDelete': '删除当前请求？'
    ,'http.curlPrompt': '粘贴 cURL 命令'
    ,'http.savedEmpty': '暂无已保存请求'
    ,'http.noUrl': '未设置 URL'
    ,'http.importCurl': '导入 cURL'
    ,'http.copyCurl': '复制为 cURL'
    ,'http.method': '请求方法'
    ,'http.send': '发送'
    ,'http.tab.params': 'Params'
    ,'http.tab.headers': 'Headers'
    ,'http.tab.cookies': 'Cookies'
    ,'http.tab.body': 'Body'
    ,'http.bodyType': 'Body 类型'
    ,'http.response.body': '响应 Body'
    ,'http.response.headers': '响应 Headers'
    ,'http.response.cookies': '响应 Cookies'
    ,'http.responseEmpty': '发送请求后在这里查看响应'
    ,'http.name': 'Name'
    ,'http.value': 'Value'
    ,'http.domain': 'Domain'
    ,'http.path': 'Path'
    ,'http.expires': 'Expires'
    ,'http.enabled': '启用'
    ,'http.addEntry': '添加参数'
    ,'http.history': 'HTTP 请求历史'
    ,'http.error.ABORTED': '请求已取消'
    ,'http.error.TIMEOUT': '请求超时'
    ,'http.error.NETWORK': '网络请求失败'
    ,'http.error.INVALID_REQUEST': '请求参数无效'
    ,'http.error.RESPONSE_TOO_LARGE': '响应超过 10 MB 限制'
    ,'translation.title': '翻译'
    ,'translation.tab.translate': '翻译'
    ,'translation.tab.words': '单词本'
    ,'translation.tab.history': '历史记录'
    ,'translation.exchange': '交换语言与文本'
    ,'translation.provider': '翻译源'
    ,'translation.copy': '复制译文'
    ,'translation.saveWord': '收藏到单词本'
    ,'translation.savedWord': '已保存到单词本'
    ,'translation.sourcePlaceholder': '输入要翻译的文本'
    ,'translation.targetPlaceholder': '译文'
    ,'translation.translating': '翻译中…'
    ,'translation.fallback': '已使用备用翻译源'
    ,'translation.sourceLanguage': '源语言'
    ,'translation.targetLanguage': '目标语言'
    ,'translation.apply': '应用到翻译'
    ,'translation.retranslate': '重新翻译'
    ,'translation.confirmDeleteWord': '删除当前单词？'
    ,'translation.searchWords': '搜索单词、译文或备注'
    ,'translation.remark': '备注（可选）'
    ,'translation.wordEmpty': '选择或新建一个单词'
    ,'translation.searchHistory': '搜索原文、译文或语言'
    ,'translation.lang.auto': '自动检测'
    ,'translation.lang.zh-CN': '中文（简体）'
    ,'translation.lang.en': '英语'
    ,'translation.lang.yue': '粤语'
    ,'translation.lang.wyw': '文言文'
    ,'translation.lang.jp': '日语'
    ,'translation.lang.kor': '韩语'
    ,'translation.lang.fra': '法语'
    ,'translation.lang.spa': '西班牙语'
    ,'translation.lang.th': '泰语'
    ,'translation.lang.ara': '阿拉伯语'
    ,'translation.lang.ru': '俄语'
    ,'translation.lang.pt': '葡萄牙语'
    ,'translation.lang.de': '德语'
    ,'translation.lang.it': '意大利语'
    ,'translation.lang.el': '希腊语'
    ,'translation.lang.nl': '荷兰语'
    ,'translation.lang.pl': '波兰语'
    ,'translation.lang.bul': '保加利亚语'
    ,'translation.lang.est': '爱沙尼亚语'
    ,'translation.lang.dan': '丹麦语'
    ,'translation.lang.fin': '芬兰语'
    ,'translation.lang.cs': '捷克语'
    ,'translation.lang.rom': '罗马尼亚语'
    ,'translation.lang.slo': '斯洛文尼亚语'
    ,'translation.lang.swe': '瑞典语'
    ,'translation.lang.hu': '匈牙利语'
    ,'translation.lang.cht': '繁体中文'
    ,'translation.lang.vie': '越南语'
    ,'host.title': 'Host'
    ,'host.discardChanges': '放弃未保存的修改？'
    ,'host.untitled': '未命名 Host'
    ,'host.namePrompt': 'Host 方案名称'
    ,'host.confirmDelete': '删除当前 Host 方案？'
    ,'host.confirmApply': '将当前内容写入系统 hosts 文件？系统可能请求管理员权限。'
    ,'host.applied': '系统 hosts 已更新'
    ,'host.applying': '正在应用…'
    ,'host.apply': '切换 Host'
    ,'host.empty': '暂无 Host 方案'
    ,'host.import': '导入 Host'
    ,'host.export': '导出 Host'
    ,'host.profileName': '方案名称'
    ,'host.current': '系统当前 hosts'
    ,'host.find': '查找与替换'
    ,'host.findPlaceholder': '查找'
    ,'host.replacePlaceholder': '替换为'
    ,'host.replace': '替换'
    ,'host.replaceAll': '全部替换'
    ,'host.content': 'Host 内容'
    ,'host.placeholder': '输入 hosts 文件内容'
    ,'host.writable': '当前用户可直接写入'
    ,'host.requiresPrivilege': '写入时需要管理员权限'
    ,'net.title': '网络/IP'
    ,'net.running': '正在执行…'
    ,'net.noOutput': '命令执行完成，无输出'
    ,'net.outputPlaceholder': '在左侧运行网络命令，结果会显示在这里'
    ,'net.ipv4Long': 'IPv4 地址与 Long 值互转'
    ,'net.ping': 'PING'
    ,'net.resolve': '通过域名获取 IP'
    ,'net.resolveAction': '解析'
    ,'net.whois': 'WHOIS 查询'
    ,'net.query': '查询'
    ,'net.dns': 'DNS'
    ,'net.flushDns': '刷新 DNS 缓存'
    ,'net.localAddresses': '本机 IP 地址'
    ,'net.error.ABORTED': '命令已停止'
    ,'net.error.TIMEOUT': '命令执行超时'
    ,'net.error.PERMISSION': '系统拒绝执行，请检查权限'
    ,'net.error.UNSUPPORTED': '当前系统不支持该命令'
    ,'net.error.COMMAND_FAILED': '命令执行失败'
    ,'net.error.INVALID_TARGET': '目标地址无效'
    ,'variables.title': '环境变量'
    ,'variables.tab.environment': '系统环境变量'
    ,'variables.tab.runtime': 'Electron 运行属性'
    ,'variables.key': '键'
    ,'variables.value': '值'
    ,'variables.count': '显示 {count} / {total} 项'
    ,'runtime.title': '代码运行'
    ,'runtime.tab.java': 'Java / Groovy'
    ,'runtime.tab.python': 'Python'
    ,'runtime.tab.node': 'Node.js'
    ,'runtime.mode.java': 'Java'
    ,'runtime.mode.groovy': 'Groovy'
    ,'runtime.detect': '检测运行时'
    ,'runtime.detected': '已检测到 {count} 个运行时'
    ,'runtime.missing': '未检测到 {name}'
    ,'runtime.format': '格式化'
    ,'runtime.run': '运行'
    ,'runtime.stop': '停止'
    ,'runtime.clear': '清空输出'
    ,'runtime.history': '运行历史'
    ,'runtime.editor': '代码编辑器'
    ,'runtime.output': '控制台输出'
    ,'runtime.ready': '等待运行代码'
    ,'runtime.running': '正在运行 {name}…'
    ,'runtime.completed': '运行完成'
    ,'runtime.failed': '运行失败'
    ,'runtime.cancelled': '运行已停止'
    ,'runtime.timeout': '运行超时'
    ,'runtime.truncated': '输出超过 2 MB，已截断并停止运行'
    ,'runtime.command': '命令'
    ,'runtime.exitCode': '退出码 {code}'
    ,'runtime.duration': '耗时 {duration} ms'
    ,'runtime.configure': '请在设置中配置 {name} 运行时路径'
    ,'runtime.openSettings': '打开运行环境设置'
    ,'runtime.options': '运行参数'
    ,'runtime.arguments': '程序参数'
    ,'runtime.argumentsPlaceholder': '--name "Moo Tool"'
    ,'runtime.workingDirectory': '工作目录'
    ,'runtime.defaultWorkingDirectory': '使用隔离的临时目录'
    ,'quickNote.title': '随手记'
    ,'quickNote.search': '搜索笔记…'
    ,'quickNote.searchContent': '搜索正文'
    ,'quickNote.sort': '排序'
    ,'quickNote.sort.name': '名称'
    ,'quickNote.sort.modified': '最近修改'
    ,'quickNote.sort.created': '创建时间'
    ,'quickNote.empty': '没有匹配的笔记'
    ,'quickNote.newNote': '新建笔记'
    ,'quickNote.newFolder': '新建文件夹'
    ,'quickNote.expandAll': '展开全部'
    ,'quickNote.collapseAll': '折叠全部'
    ,'quickNote.rename': '重命名'
    ,'quickNote.move': '移动'
    ,'quickNote.duplicate': '创建副本'
    ,'quickNote.delete': '删除'
    ,'quickNote.openVault': '在文件管理器中打开 Vault'
    ,'quickNote.save': '保存'
    ,'quickNote.saved': '笔记已保存'
    ,'quickNote.unsaved': '有未保存更改'
    ,'quickNote.find': '查找与替换'
    ,'quickNote.findPlaceholder': '查找'
    ,'quickNote.replacePlaceholder': '替换为'
    ,'quickNote.nextMatch': '下一个匹配项'
    ,'quickNote.replace': '替换'
    ,'quickNote.replaceAll': '全部替换'
    ,'quickNote.noMatches': '未找到匹配内容'
    ,'quickNote.attachment': '插入图片附件（也可从剪贴板粘贴）'
    ,'quickNote.clipboardEmpty': '剪贴板中没有可用图片'
    ,'quickNote.export': '导出当前笔记'
    ,'quickNote.info': '文档信息'
    ,'quickNote.git': 'Git 同步'
    ,'quickNote.quickReplace': '快速替换'
    ,'quickNote.view.editor': '编辑'
    ,'quickNote.view.split': '分栏'
    ,'quickNote.view.preview': '预览'
    ,'quickNote.syntax': '语法'
    ,'quickNote.font': '字体'
    ,'quickNote.font.system': '系统字体'
    ,'quickNote.font.mono': '等宽字体'
    ,'quickNote.font.serif': '衬线字体'
    ,'quickNote.fontSize': '字号'
    ,'quickNote.wrap': '自动换行'
    ,'quickNote.bulletList': '无序列表'
    ,'quickNote.numberedList': '有序列表'
    ,'quickNote.color': '笔记颜色'
    ,'quickNote.select': '从左侧选择或新建一条笔记'
    ,'quickNote.editorLabel': '笔记内容'
    ,'quickNote.confirmDelete': '确定删除“{name}”吗？文件夹必须为空。'
    ,'quickNote.dialog.createNote': '新建笔记'
    ,'quickNote.dialog.createFolder': '新建文件夹'
    ,'quickNote.dialog.rename': '重命名'
    ,'quickNote.dialog.move': '移动到文件夹'
    ,'quickNote.dialog.name': '名称'
    ,'quickNote.dialog.target': '目标文件夹'
    ,'quickNote.dialog.root': 'Vault 根目录'
    ,'quickNote.create': '创建'
    ,'quickNote.apply': '应用'
    ,'quickNote.path': '路径'
    ,'quickNote.created': '创建时间'
    ,'quickNote.modified': '修改时间'
    ,'quickNote.lines': '行数'
    ,'quickNote.words': '词数'
    ,'quickNote.characters': '字符数'
    ,'quickNote.status': '{lines} 行 · {characters} 字符'
    ,'quickNote.preview.empty': '切换到 Markdown 语法后可预览格式化内容。'
    ,'quickNote.quick.trim': '去除首尾空格'
    ,'quickNote.quick.removeBlankLines': '删除空行'
    ,'quickNote.quick.removeTabs': '删除 Tab'
    ,'quickNote.quick.scientificToNormal': '科学计数转普通数字'
    ,'quickNote.quick.normalToScientific': '普通数字转科学计数'
    ,'quickNote.quick.thousandsToNormal': '去除千分位'
    ,'quickNote.quick.normalToThousands': '添加千分位'
    ,'quickNote.quick.underscoreToCamel': '下划线转驼峰'
    ,'quickNote.quick.camelToUnderscore': '驼峰转下划线'
    ,'quickNote.quick.uppercase': '转大写'
    ,'quickNote.quick.lowercase': '转小写'
    ,'quickNote.quick.linesToComma': '换行转逗号'
    ,'quickNote.quick.linesToSingleQuoted': '换行转单引号列表'
    ,'quickNote.quick.linesToDoubleQuoted': '换行转双引号列表'
    ,'quickNote.quick.commaToLines': '逗号转换行'
    ,'quickNote.quick.tabsToLines': 'Tab 转换行'
    ,'quickNote.quick.clearNewlines': '清除换行'
    ,'quickNote.quick.deduplicateLines': '按行去重'
    ,'quickNote.quick.deduplicateWithCount': '按行去重并计数'
    ,'quickNote.quick.escape': '转义文本'
    ,'quickNote.quick.unescape': '还原转义'
    ,'quickNote.quick.reverseLines': '反转行顺序'
    ,'quickNote.quick.sortAscending': '按行升序'
    ,'quickNote.quick.sortDescending': '按行降序'
    ,'quickNote.git.title': '随手记 Vault Git'
    ,'quickNote.git.defaultMessage': 'MooTool 随手记检查点'
    ,'hardware.title': '系统信息'
    ,'hardware.tab.system': '系统'
    ,'hardware.tab.cpu': '处理器'
    ,'hardware.tab.memory': '内存'
    ,'hardware.tab.storage': '存储'
    ,'hardware.tab.network': '网络'
    ,'hardware.loading': '正在收集系统信息…'
    ,'hardware.empty': '没有可显示的信息'
    ,'hardware.group.operatingSystem': '操作系统'
    ,'hardware.group.processor': '处理器'
    ,'hardware.group.physicalMemory': '物理内存'
    ,'hardware.label.platform': '平台'
    ,'hardware.label.distribution': '发行版'
    ,'hardware.label.kernel': '内核'
    ,'hardware.label.architecture': '架构'
    ,'hardware.label.hostName': '主机名'
    ,'hardware.label.serial': '序列号'
    ,'hardware.label.manufacturer': '制造商'
    ,'hardware.label.model': '型号'
    ,'hardware.label.uptime': '运行时间'
    ,'hardware.label.timeZone': '时区'
    ,'hardware.label.brand': '品牌'
    ,'hardware.label.vendor': '供应商'
    ,'hardware.label.family': '系列'
    ,'hardware.label.physicalCores': '物理核心'
    ,'hardware.label.logicalCores': '逻辑核心'
    ,'hardware.label.performanceCores': '性能核心'
    ,'hardware.label.efficiencyCores': '能效核心'
    ,'hardware.label.baseSpeed': '基础频率'
    ,'hardware.label.maximumSpeed': '最高频率'
    ,'hardware.label.currentLoad': '当前负载'
    ,'hardware.label.total': '总计'
    ,'hardware.label.used': '已用'
    ,'hardware.label.available': '可用'
    ,'hardware.label.active': '活跃'
    ,'hardware.label.usage': '使用率'
    ,'hardware.label.swapTotal': '交换空间总计'
    ,'hardware.label.swapUsed': '交换空间已用'
    ,'hardware.label.device': '设备'
    ,'hardware.label.type': '类型'
    ,'hardware.label.interface': '接口'
    ,'hardware.label.capacity': '容量'
    ,'hardware.label.filesystem': '文件系统'
    ,'hardware.label.ipv4': 'IPv4'
    ,'hardware.label.ipv6': 'IPv6'
    ,'hardware.label.mac': 'MAC'
    ,'hardware.label.mtu': 'MTU'
    ,'hardware.label.speed': '速度'
    ,'hardware.label.status': '状态'
    ,'hardware.label.received': '接收'
    ,'hardware.label.sent': '发送'
  },
  'en-US': {
    'common.toast.dismiss': 'Dismiss notification',
    'common.resizePane': 'Resize panes',
    'common.loading': 'Loading…',
    'common.close': 'Close',
    'common.cancel': 'Cancel',
    'findReplace.find': 'Find',
    'findReplace.findPlaceholder': 'Find',
    'findReplace.replacePlaceholder': 'Replace with',
    'findReplace.replace': 'Replace',
    'findReplace.replaceAll': 'Replace all',
    'findReplace.regex': 'Use regex',
    'findReplace.matchCase': 'Match case',
    'findReplace.wholeWord': 'Whole word',
    'findReplace.foundPrefix': 'Found:',
    'findReplace.replacedPrefix': 'Replaced:',
    'findReplace.previous': 'Previous occurrence',
    'findReplace.next': 'Next occurrence',
    'findReplace.noMatches': 'No matches found',
    'app.nav.tools': 'Tool navigation',
    'app.nav.home': 'Home',
    'app.nav.quickNote': 'Quick Note',
    'app.nav.textDiff': 'Text Diff',
    'app.nav.reformat': 'Reformat',
    'app.nav.json': 'JSON',
    'app.nav.java': 'Code Runner',
    'app.nav.ymlProperties': 'Config Converter',
    'app.nav.protobuf': 'Protobuf',
    'app.nav.variables': 'Environment',
    'app.nav.host': 'Host',
    'app.nav.net': 'Network/IP',
    'app.nav.uaParse': 'UA Parser',
    'app.nav.timeConvert': 'Time',
    'app.nav.translation': 'Translation',
    'app.nav.calculator': 'Calculator',
    'app.nav.hardware': 'System Info',
    'app.nav.encode': 'Encode',
    'app.nav.crypto': 'Crypto/Random',
    'app.nav.qrCode': 'QR Code',
    'app.nav.http': 'HTTP',
    'app.nav.diff': 'Text Diff',
    'app.nav.regex': 'Regex',
    'app.nav.colorBoard': 'Color Board',
    'app.nav.image': 'Images',
    'app.nav.pdf': 'PDF',
    'app.nav.cron': 'Cron',
    'app.group.text': 'Notes & Text',
    'app.group.dev': 'Development & Format',
    'app.group.network': 'Network & Requests',
    'app.group.encode': 'Encoding & Crypto',
    'app.group.daily': 'Utilities',
    'app.group.system': 'System',
    'app.nav.recent': 'Recent',
    'app.nav.settings': 'Settings',
    'app.nav.search': 'Search',
    'app.nav.collapse': 'Collapse sidebar',
    'app.nav.expand': 'Expand sidebar',
    'app.nav.manageGroups': 'Manage groups',
    'app.group.all': 'All tools',
    'app.group.manage.title': 'Manage tool groups',
    'app.group.manage.new': 'New group',
    'app.group.manage.defaultName': 'New group {number}',
    'app.group.manage.empty': 'No custom groups yet. Select New group to create one.',
    'app.group.manage.name': 'Group name',
    'app.group.manage.tools': 'Choose tools',
    'app.group.manage.delete': 'Delete group',
    'app.group.manage.deleteConfirm': 'Delete the group “{name}”?',
    'app.group.manage.nameRequired': 'Enter a group name.',
    'app.group.manage.toolRequired': 'Select at least one tool.',
    'app.group.manage.saveFailed': 'Could not save groups',
    'app.recent.empty': 'No recent tools',
    'app.search.title': 'Search tools',
    'app.search.placeholder': 'Search tools…',
    'app.search.empty': 'No matching tools',
    'app.search.close': 'Close search',
    'app.home.website': 'Open the MooTool website',
    'app.home.tagline': 'Handy desktop toolset for developers',
    'app.home.author': 'Proudly by RememBerBer Zhou Bo',
    'app.home.about.title': 'About',
    'app.home.about.line1': 'Hi! Thanks for using MooTool. "Moo" is named after my daughter.',
    'app.home.about.line2': 'Fewer Java developers build with Swing nowadays, but I still enjoy it.',
    'app.home.about.line2Note': 'Though the version you are using now is no longer the Swing edition 😛',
    'app.home.about.line3': 'So in every bit of free time, I kept working on this project.',
    'app.home.about.line4': 'I finally shipped something, though many similar tools exist on the web.',
    'app.home.about.line5': 'Hope you enjoy using it as much as I enjoyed building it.',
    'app.home.source.title': 'Code',
    'app.home.help.title': 'Help us improve',
    'app.home.help.issue': 'Report an issue or share an idea',
    'app.home.thanks.title': 'Thanks to',
    'app.home.otherWorks.title': 'Other works',
    'app.home.wePush.desc': 'A lightweight tool focused on batch messaging',
    'app.home.mooInfo.desc': 'Visual OSHI implementation for system and hardware information',
    'app.home.sponsor.title': 'Sponsor',
    'app.home.sponsor.prompt': 'Buy me a coffee',
    'app.home.sponsor.tip': 'Thank you for your support',
    'app.home.contributors.title': 'Contributors',
    'app.home.contributors.thanks': 'Thank you to everyone who has contributed to MooTool.',
    'app.placeholder': 'This tool page will follow the JSON workbench structure.',
    'toolWindow.detach': 'Open in a separate window',
    'toolWindow.dock': 'Return to workspace',
    'toolWindow.focus': 'Find window',
    'toolWindow.detachedTitle': '{tool} is open in a separate window',
    'toolWindow.detachedDescription': 'Close the separate window to return this tool here automatically.',
    'toolWindow.invalid': 'This tool window could not be opened.',
    'settings.title': 'Settings',
    'settings.close': 'Close settings',
    'settings.saveFailed': 'Failed to save settings',
    'settings.language': 'Language',
    'settings.category.general': 'General',
    'settings.category.appearance': 'Appearance',
    'settings.category.layout': 'Layout & Habits',
    'settings.category.editor': 'Editor',
    'settings.category.network': 'Network',
    'settings.category.data': 'Data & Backup',
    'settings.category.vault': 'Vault',
    'settings.category.runtime': 'Runtimes',
    'settings.category.tools': 'Tool Defaults',
    'settings.category.shortcuts': 'Shortcuts',
    'settings.category.about': 'About & Updates',
    'settings.group.application': 'Application Behavior',
    'settings.group.theme': 'Theme & Type',
    'settings.group.navigation': 'Tool Navigation',
    'settings.group.editor': 'Editing',
    'settings.group.proxy': 'HTTP Proxy',
    'settings.group.timeouts': 'Timeouts and Cancellation',
    'settings.group.storage': 'Data Directory',
    'settings.group.backup': 'Local Backup',
    'settings.backup.all': 'Full Backup',
    'settings.backup.database': 'Data File',
    'settings.backup.settings': 'Settings File',
    'settings.backup.images': 'Image Directory',
    'settings.backup.export': 'Export',
    'settings.backup.open': 'Open Folder',
    'settings.backup.success': 'Backup exported',
    'settings.group.migration': 'Legacy Data Migration',
    'settings.migration.source': 'Java data directory',
    'settings.migration.scan': 'Scan',
    'settings.migration.ready': '{count} items are ready to migrate',
    'settings.migration.databaseFound': 'Database found',
    'settings.migration.databaseMissing': 'Legacy database not found',
    'settings.migration.configFound': 'Settings found',
    'settings.migration.configMissing': 'Legacy settings not found',
    'settings.migration.import': 'Start Migration',
    'settings.migration.importing': 'Migrating…',
    'settings.migration.confirmTitle': 'Migrate Java Data',
    'settings.migration.confirmBody': 'Migrate {count} items. MooTool Next creates a full backup first and does not modify the Java source files.',
    'settings.migration.success': 'Migrated {count} items',
    'settings.migration.alreadyMigrated': 'This source has already been migrated',
    'settings.migration.warning.remotes': 'The two legacy Vaults use different remotes, so remote URLs will not be merged automatically.',
    'settings.migration.warning.secrets': 'Proxy passwords and Git tokens were skipped. Save them again securely in Settings.',
    'app.migrationHint.title': 'Migrate Java Data',
    'app.migrationHint.body': 'If you previously used the Java edition of MooTool, you can migrate your data in Settings → Data & Backup. Migration does not modify or delete the original Java data.',
    'app.migrationHint.openSettings': 'Open Settings',
    'app.migrationHint.dismiss': 'Got it',
    'settings.group.vaultPaths': 'Vault Paths',
    'settings.group.git': 'Git Sync',
    'settings.group.runtimes': 'Local Runtimes',
    'settings.group.toolDefaults': 'Default Parameters',
    'settings.group.shortcuts': 'Application Shortcuts',
    'settings.autoCheckUpdates': 'Check for updates at startup',
    'settings.autoDownloadUpdates': 'Download new versions silently',
    'settings.startMaximized': 'Start maximized',
    'settings.trayEnabled': 'Enable system tray',
    'settings.closeBehavior': 'When closing the main window',
    'settings.close.ask': 'Ask each time',
    'settings.close.hide': 'Hide',
    'settings.close.quit': 'Quit',
    'settings.interfaceStyle': 'Interface style',
    'settings.interfaceStyle.modern': 'Modern',
    'settings.interfaceStyle.quiet': 'Quiet',
    'settings.theme': 'Color mode',
    'settings.theme.system': 'System',
    'settings.theme.light': 'Light',
    'settings.theme.dark': 'Dark',
    'settings.accentColor': 'Accent color',
    'settings.fontSize': 'Global font size',
    'settings.unifiedBackground': 'Unified workspace background',
    'settings.navigationStyle': 'Navigation style',
    'settings.navigation.classic': 'Classic',
    'settings.navigation.card': 'Cards',
    'settings.navigation.grouped': 'Grouped',
    'settings.navigation.toolsTitle': 'Sidebar tool list',
    'settings.navigation.toolsDescription': 'Choose which items appear under All tools. Home, search, recent tools, and custom groups are not affected.',
    'settings.navigation.showAll': 'Show all',
    'settings.navigation.hideAll': 'Hide all',
    'settings.showRecent': 'Show recent tools',
    'settings.compactNavigation': 'Compact navigation',
    'settings.showSeparators': 'Show separators',
    'settings.hideNavigationTitles': 'Hide navigation titles',
    'settings.sqlDialect': 'SQL dialect',
    'settings.jsonFontSize': 'JSON font size',
    'settings.quickNoteFontSize': 'Quick Note font size',
    'settings.softWrap': 'Wrap editor lines',
    'settings.proxyEnabled': 'Use HTTP proxy',
    'settings.proxyHost': 'Host',
    'settings.proxyPort': 'Port',
    'settings.proxyUsername': 'Username',
    'settings.proxyPassword': 'Password',
    'settings.requestTimeout': 'HTTP request timeout (ms)',
    'settings.translationTimeout': 'Translation timeout (ms)',
    'settings.dataDirectory': 'Data directory',
    'settings.quickNoteVault': 'Quick Note Vault',
    'settings.jsonVault': 'JSON Vault',
    'settings.gitRemote': 'Remote repository',
    'settings.gitUsername': 'Git username',
    'settings.gitToken': 'Git personal token',
    'settings.autoCommit': 'Create Git checkpoints automatically',
    'settings.autoCommitIdleSeconds': 'Checkpoint after editing idle (seconds)',
    'settings.autoCommitInactiveSeconds': 'Checkpoint after window inactive (seconds)',
    'settings.autoPullMinutes': 'Auto-pull interval (minutes)',
    'settings.hideGitignoredFiles': 'Hide files matched by .gitignore',
    'settings.runtime.detect': 'Detect again',
    'settings.runtime.notFound': 'Not found',
    'settings.runtime.auto': 'Find automatically from the system',
    'settings.runtime.path': 'path',
    'settings.qrCodeSize': 'QR code size',
    'settings.qrErrorCorrection': 'QR error correction',
    'settings.randomStringLength': 'Random string length',
    'settings.exportDirectory': 'Default export directory',
    'settings.translationProvider': 'Default translator',
    'settings.translationSource': 'Default source language',
    'settings.translationTarget': 'Default target language',
    'settings.shortcut.search': 'Search tools',
    'settings.shortcut.settings': 'Open settings',
    'settings.chooseDirectory': 'Choose directory',
    'settings.version': 'Version {version}',
    'settings.update.check': 'Check for Updates',
    'settings.update.checking': 'Checking…',
    'settings.update.latest': 'MooTool is up to date',
    'settings.update.available': 'Version {version} is available',
    'settings.update.failed': 'Update check failed. Try again later.',
    'settings.update.target': '{product} · {platform}/{architecture}',
    'settings.update.download': 'Download Update',
    'settings.update.downloading': 'Downloading in background {percent}%',
    'settings.update.ready': 'Version {version} is ready',
    'settings.update.whatsNew': 'What\'s new',
    'settings.update.installRestart': 'Install and Restart',
    'settings.update.manualInstall': 'Update downloaded — open the DMG to install',
    'settings.update.openDownloaded': 'Open DMG',
    'settings.update.downloadFailed': 'The update download failed. Try again later.',
    'settings.update.installFailed': 'Could not start the update installer. Try again later.',
    'settings.update.noDownload': 'No installer matches this device yet. Check the release page.',
    'settings.update.openRelease': 'Open Releases',
    'settings.update.project': 'Project Page',
    'settings.secret.stored': 'Stored securely',
    'settings.secret.saved': 'Sensitive value stored securely',
    'settings.secret.save': 'Save',
    'settings.secret.clear': 'Clear',
    'settings.secret.unavailable': 'Secure system storage is unavailable',
    'history.title': 'History',
    'history.search': 'Search history…',
    'history.empty': 'No history yet',
    'history.clearAll': 'Clear all',
    'history.confirmClear': 'Clear all history for this tool?',
    'history.copyInput': 'Copy input',
    'history.copyOutput': 'Copy output',
    'history.delete': 'Delete record',
    'json.title': 'JSON Workbench',
    'json.editor.label': 'JSON editor',
    'json.valid.idle': 'Waiting for JSON input',
    'json.valid.ok': 'Valid JSON · {type}',
    'json.valid.error': 'Failed to parse JSON',
    'json.error.empty': 'Enter JSON content',
    'json.error.notString': 'Current content is not a JSON string',
    'json.error.duplicateKeys': 'Duplicate keys found: {paths}',
    'json.error.emptyXml': 'Enter XML content',
    'json.error.emptyPath': 'Enter a JSON Path',
    'json.error.objectRequired': 'This operation requires a JSON object',
    'json.error.emptyJavaBean': 'Enter JavaBean class code',
    'json.error.noJavaFields': 'No convertible Java fields found',
    'json.action.format': 'Format',
    'json.action.compress': 'Minify',
    'json.action.wrap': 'Wrap',
    'json.action.nowrap': 'No wrap',
    'json.action.copy': 'Copy',
    'json.action.copied': 'Copied',
    'json.action.clear': 'Clear',
    'json.action.find': 'Find',
    'json.action.import': 'Import',
    'json.action.export': 'Export',
    'json.action.history': 'History',
    'json.action.more': 'More tools',
    'json.action.escape': 'Escape JSON string',
    'json.action.unescape': 'Unescape JSON string',
    'json.action.escapeText': 'Escape text',
    'json.action.unescapeText': 'Unescape text',
    'json.action.jsonToXml': 'JSON to XML',
    'json.action.xmlToJson': 'XML to JSON',
    'json.action.beanToJson': 'JavaBean to JSON',
    'json.action.jsonToBean': 'JSON to JavaBean',
    'json.action.swap': 'Swap Key / Value',
    'json.notice.formatted': 'Formatted',
    'json.notice.compressed': 'Minified',
    'json.notice.copied': 'Copied to clipboard',
    'json.notice.copyFailed': 'Copy failed',
    'json.notice.failed': 'Operation failed',
    'json.notice.escaped': 'Converted to a JSON string',
    'json.notice.unescaped': 'Restored JSON string',
    'json.notice.imported': 'File imported',
    'json.notice.exported': 'File exported',
    'json.notice.pathApplied': 'JSON Path applied',
    'json.notice.noMatches': 'No matches found',
    'json.panel.actions': 'Actions',
    'json.panel.result': 'Result',
    'json.panel.format': 'Format options',
    'json.panel.convert': 'Convert',
    'json.panel.jsonPath': 'JSON Path',
    'json.format.indent': 'Indent spaces',
    'json.format.sortKeys': 'Sort object keys',
    'json.format.ignoreCase': 'Ignore case when sorting and checking duplicates',
    'json.format.duplicateKeys': 'Check duplicate keys before formatting',
    'json.format.apply': 'Apply custom format',
    'json.find.placeholder': 'Find in JSON…',
    'json.find.matches': '{count} matches',
    'json.find.next': 'Next match',
    'json.find.close': 'Close find',
    'json.path.placeholder': 'For example $.store.book[*].title',
    'json.path.query': 'Query',
    'json.path.pick': 'Visual picker',
    'json.pathPicker.title': 'Select JSON Path',
    'json.pathPicker.use': 'Use this path',
    'json.pathPicker.path': 'Path',
    'json.pathPicker.preview': 'Value preview',
    'json.dialog.input': 'Input',
    'json.dialog.output': 'Output',
    'json.dialog.run': 'Convert',
    'json.dialog.className': 'Root class name',
    'json.dialog.useOutput': 'Use output',
    'json.vault.title': 'JSON Vault',
    'json.vault.new': 'New snippet',
    'json.vault.save': 'Save snippet',
    'json.vault.delete': 'Delete snippet',
    'json.vault.refresh': 'Refresh Vault',
    'json.vault.empty': 'No JSON snippets yet',
    'json.vault.fileName': 'File name or relative path',
    'json.vault.fileNameHint': 'For example drafts/request.json',
    'json.vault.create': 'Create',
    'json.vault.saved': 'Snippet saved',
    'json.vault.created': 'Snippet created',
    'json.vault.deleted': 'Snippet deleted',
    'json.vault.confirmDelete': 'Delete "{name}"?',
    'json.vault.confirmDiscard': 'The current snippet has unsaved changes. Discard them?'
    ,'json.vault.newFolder': 'New folder'
    ,'json.vault.expandAll': 'Expand all'
    ,'json.vault.collapseAll': 'Collapse all'
    ,'json.vault.defaultFolder': 'New folder'
    ,'json.vault.folderName': 'Folder relative path'
    ,'json.vault.folderCreated': 'Folder created'
    ,'json.vault.rename': 'Rename'
    ,'json.vault.renameName': 'New name'
    ,'json.vault.renamed': 'Entry renamed'
    ,'json.vault.move': 'Move'
    ,'json.vault.moveTo': 'Destination folder'
    ,'json.vault.moved': 'Entry moved'
    ,'json.vault.duplicate': 'Duplicate snippet'
    ,'json.vault.duplicated': 'Snippet copy created'
    ,'json.vault.openFolder': 'Open Vault in file manager'
    ,'json.vault.sort': 'Sort order'
    ,'json.vault.sortName': 'By name'
    ,'json.vault.sortModified': 'By modified time'
    ,'json.vault.more': 'More Vault actions'
    ,'json.vault.root': 'Vault root'
    ,'json.git.title': 'JSON Vault Git'
    ,'json.git.open': 'Open Git panel'
    ,'json.git.unavailable': 'Git command was not found'
    ,'json.git.noRepo': 'This Vault is not a Git repository yet'
    ,'json.git.init': 'Initialize Git'
    ,'json.git.refresh': 'Refresh'
    ,'json.git.fetch': 'Fetch'
    ,'json.git.pull': 'Pull'
    ,'json.git.push': 'Push'
    ,'json.git.branch': 'Branch {branch}'
    ,'json.git.sync': '↑{ahead} ↓{behind}'
    ,'json.git.remote': 'Remote repository'
    ,'json.git.remotePlaceholder': 'https://... or git@host:repo.git'
    ,'json.git.saveRemote': 'Save remote URL'
    ,'json.git.removeRemote': 'Remove remote'
    ,'json.git.changes': 'Working changes'
    ,'json.git.history': 'Commit history'
    ,'json.git.emptyChanges': 'Working tree is clean'
    ,'json.git.emptyHistory': 'No commits yet'
    ,'json.git.diff': 'Diff'
    ,'json.git.noDiff': 'Select a change or commit to inspect its diff'
    ,'json.git.commitMessage': 'Commit message'
    ,'json.git.defaultMessage': 'MooTool JSON checkpoint'
    ,'json.git.commit': 'Commit all changes'
    ,'json.git.conflict': 'Conflict'
    ,'json.git.done': 'Git operation completed'
    ,'json.git.discard': 'Discard change'
    ,'json.git.confirmDiscard': 'Discard local changes to "{path}"? This cannot be undone.'
    ,'json.git.abortMerge': 'Abort merge / rebase'
    ,'json.git.confirmAbort': 'Abort the current merge or rebase and restore the previous state?'
    ,'json.git.continueOperation': 'Continue merge / rebase'
    ,'json.git.useOurs': 'Use ours'
    ,'json.git.useTheirs': 'Use theirs'
    ,'time.title': 'Time Converter'
    ,'time.current': 'Current time'
    ,'time.timestamp': 'Unix timestamp'
    ,'time.localTime': 'Local time'
    ,'time.timezone': 'Time zone'
    ,'time.unit.second': 'Seconds (s)'
    ,'time.unit.millisecond': 'Milliseconds (ms)'
    ,'time.toLocal': 'Convert to local time'
    ,'time.toTimestamp': 'Convert to timestamp'
    ,'time.copy': 'Copy'
    ,'time.clock': 'Full-screen clock'
    ,'time.history': 'History'
    ,'time.formatHint': 'Format: yyyy-MM-dd HH:mm:ss'
    ,'time.error.timestamp': 'Enter a valid Unix timestamp'
    ,'time.error.localTime': 'Enter a valid time in yyyy-MM-dd HH:mm:ss format'
    ,'time.notice.toLocal': 'Converted to {zone} time'
    ,'time.notice.toTimestamp': 'Converted to Unix timestamp'
    ,'time.notice.copied': 'Copied'
    ,'time.clock.close': 'Exit full-screen clock'
    ,'common.action.copy': 'Copy'
    ,'common.action.paste': 'Paste'
    ,'common.action.clear': 'Clear'
    ,'common.action.history': 'History'
    ,'common.action.swap': 'Swap'
    ,'common.action.apply': 'Apply'
    ,'common.action.save': 'Save'
    ,'common.action.delete': 'Delete'
    ,'common.input': 'Input'
    ,'common.output': 'Output'
    ,'common.result': 'Result'
    ,'common.name': 'Name'
    ,'common.description': 'Description'
    ,'common.yes': 'Yes'
    ,'common.no': 'No'
    ,'common.error.process': 'Processing failed: {message}'
    ,'favorite.title': 'Favorites'
    ,'favorite.add': 'Favorite current value'
    ,'favorite.empty': 'No favorites yet'
    ,'favorite.namePlaceholder': 'Favorite name'
    ,'favorite.saved': 'Favorite saved'
    ,'favorite.deleted': 'Favorite deleted'
    ,'encode.title': 'Encode / Decode'
    ,'encode.tab.unicode': 'Native / Unicode'
    ,'encode.tab.url': 'URL'
    ,'encode.tab.hex': 'Native / Hex'
    ,'encode.tab.ascii': 'Native / ASCII'
    ,'encode.native': 'Native text'
    ,'encode.unicode': 'Unicode text'
    ,'encode.url': 'URL source'
    ,'encode.encoded': 'Encoded result'
    ,'encode.hex': 'Hexadecimal'
    ,'encode.ascii': 'ASCII'
    ,'encode.toUnicode': 'To Unicode'
    ,'encode.fromUnicode': 'To native'
    ,'encode.urlEncode': 'URL encode'
    ,'encode.urlDecode': 'URL decode'
    ,'encode.toHex': 'To hex'
    ,'encode.fromHex': 'To text'
    ,'encode.toAscii': 'To ASCII'
    ,'encode.fromAscii': 'To text'
    ,'encode.charset': 'Charset'
    ,'encode.asciiDecimal': 'Decimal'
    ,'encode.asciiHex': 'Hexadecimal'
    ,'ua.title': 'UA Parser'
    ,'ua.input': 'User-Agent input'
    ,'ua.preset': 'Choose a preset UA…'
    ,'ua.parse': 'Parse'
    ,'ua.browser': 'Browser'
    ,'ua.browserVersion': 'Browser version'
    ,'ua.engine': 'Rendering engine'
    ,'ua.engineVersion': 'Engine version'
    ,'ua.os': 'Operating system'
    ,'ua.osVersion': 'OS version'
    ,'ua.deviceType': 'Device type'
    ,'ua.deviceBrand': 'Device brand'
    ,'ua.deviceModel': 'Device model'
    ,'ua.mobile': 'Mobile'
    ,'ua.bot': 'Crawler / Bot'
    ,'ua.empty': 'Enter a User-Agent'
    ,'ua.unknown': 'Unknown'
    ,'calculator.title': 'Calculator'
    ,'calculator.expression': 'Expression'
    ,'calculator.calculate': 'Calculate'
    ,'calculator.base': 'Base conversion'
    ,'calculator.decimal': 'Decimal'
    ,'calculator.hex': 'Hexadecimal'
    ,'calculator.binary': 'Binary'
    ,'calculator.number': 'Number operations'
    ,'calculator.first': 'Value 1'
    ,'calculator.second': 'Value 2'
    ,'calculator.gcd': 'GCD'
    ,'calculator.lcm': 'LCM'
    ,'calculator.n': 'n'
    ,'calculator.m': 'm'
    ,'calculator.permutation': 'Permutation A(n,m)'
    ,'calculator.combination': 'Combination C(n,m)'
    ,'calculator.history': 'Calculation history'
    ,'regex.title': 'Regular Expression'
    ,'regex.tab.test': 'Match test'
    ,'regex.tab.common': 'Common patterns'
    ,'regex.expression': 'Regular expression'
    ,'regex.source': 'Test text'
    ,'regex.matches': '{count} matches'
    ,'regex.noMatches': 'No matches'
    ,'regex.flag.global': 'Global'
    ,'regex.flag.ignoreCase': 'Ignore case'
    ,'regex.flag.multiline': 'Multiline'
    ,'regex.flag.dotAll': 'Dot matches newline'
    ,'regex.invalid': 'Invalid regular expression: {message}'
    ,'regex.common.phone': 'Mainland China mobile'
    ,'regex.common.email': 'Email address'
    ,'regex.common.domain': 'Domain name'
    ,'regex.common.chinese': 'Chinese characters'
    ,'regex.common.integer': 'Integer'
    ,'regex.common.color': 'Color code'
    ,'regex.common.ipv4': 'IPv4 address'
    ,'regex.common.account': 'Account name'
    ,'regex.common.htmlId': 'HTML id attribute'
    ,'regex.common.jpg': 'JPG image URL'
    ,'regex.common.magnet': 'Magnet link'
    ,'regex.common.alnum': 'Letters and numbers'
    ,'regex.common.len3to20': 'Any 3-20 characters'
    ,'regex.common.letters26': 'Latin letters'
    ,'regex.common.wordUnderscore': 'Letters, numbers or underscore'
    ,'regex.common.cnEnNum': 'Chinese, letters, numbers and underscore'
    ,'regex.common.noSpecial': 'No special characters'
    ,'regex.common.positiveInt': 'Positive integer'
    ,'regex.common.negativeInt': 'Negative integer'
    ,'regex.common.nonNegativeInt': 'Non-negative integer'
    ,'regex.common.float': 'Floating-point number'
    ,'cron.title': 'Cron Expression'
    ,'cron.expression': 'Cron expression'
    ,'cron.builder': 'Expression builder'
    ,'cron.second': 'Second'
    ,'cron.minute': 'Minute'
    ,'cron.hour': 'Hour'
    ,'cron.day': 'Day'
    ,'cron.month': 'Month'
    ,'cron.week': 'Weekday'
    ,'cron.year': 'Year (optional)'
    ,'cron.parse': 'Parse schedule'
    ,'cron.nextRuns': 'Next 10 runs'
    ,'cron.humanReadable': 'Natural language'
    ,'cron.preset': 'Common expressions'
    ,'cron.everyMinute': 'Every minute'
    ,'cron.everyHour': 'Every hour'
    ,'cron.everyDay': 'Daily at midnight'
    ,'cron.weekdays': 'Weekdays at 9 AM'
    ,'cron.invalid': 'Cannot parse Cron: {message}'
    ,'diff.title': 'Text Diff'
    ,'diff.sideBySide': 'Side by side'
    ,'diff.unified': 'Unified'
    ,'diff.left': 'Original text'
    ,'diff.right': 'New text'
    ,'diff.compare': 'Compare'
    ,'diff.ignoreWhitespace': 'Ignore whitespace'
    ,'diff.realtime': 'Live comparison'
    ,'diff.copy': 'Copy diff'
    ,'diff.previous': 'Previous difference'
    ,'diff.next': 'Next difference'
    ,'diff.summary': 'Added {added} · removed {removed} · changed {changed}'
    ,'diff.identical': 'Both texts are identical'
    ,'diff.highlightMode': 'Highlight mode'
    ,'diff.highlightBoth': 'Dual highlight'
    ,'diff.highlightCharacters': 'Character only'
    ,'diff.highlightLines': 'Line only'
    ,'diff.displayMode': 'Display mode'
    ,'diff.unifiedPanel': 'Unified diff'
    ,'diff.status.ready': 'Ready'
    ,'diff.status.enterText': 'Enter text to compare'
    ,'diff.status.characterComplete': '{count} character difference(s)'
    ,'diff.status.complete': 'Compare complete, {count} difference(s) found'
    ,'diff.status.cleared': 'Cleared'
    ,'diff.status.swapped': 'Texts swapped'
    ,'diff.status.copied': 'Diff copied to clipboard'
    ,'diff.status.noCopy': 'No diff result to copy'
    ,'diff.status.navigation': 'Jump to difference {current}/{total}'
    ,'config.title': 'Config Converter'
    ,'config.tab.convert': 'YAML / Properties'
    ,'config.tab.validate': 'YAML validation'
    ,'config.properties': 'Properties'
    ,'config.yaml': 'YAML'
    ,'config.toYaml': 'To YAML'
    ,'config.toProperties': 'To Properties'
    ,'config.validate': 'Validate'
    ,'config.format': 'Format'
    ,'config.valid': 'YAML is valid'
    ,'config.invalid': 'Invalid YAML: {message}'
    ,'common.action.format': 'Format'
    ,'common.export': 'Export'
    ,'common.help': 'Help'
    ,'common.import': 'Import'
    ,'common.processing': 'Processing…'
    ,'common.rename': 'Rename'
    ,'common.save': 'Save'
    ,'common.saved': 'Saved'
    ,'reformat.title': 'Reformat'
    ,'reformat.tab.text': 'Text'
    ,'reformat.tab.file': 'File'
    ,'reformat.type': 'Type'
    ,'reformat.indent': 'Indent'
    ,'reformat.input': 'Content to format'
    ,'reformat.chooseFile': 'Choose file'
    ,'reformat.noFile': 'No file selected'
    ,'reformat.original': 'Original'
    ,'reformat.result': 'Formatted result'
    ,'reformat.formatted': 'Formatting complete'
    ,'reformat.historySummary': '{type} formatting'
    ,'crypto.title': 'Crypto / Random'
    ,'crypto.tab.symmetric': 'Symmetric'
    ,'crypto.tab.asymmetric': 'Asymmetric'
    ,'crypto.tab.digest': 'Digest'
    ,'crypto.tab.base': 'Base64 / Base32'
    ,'crypto.tab.random': 'Random'
    ,'crypto.algorithm': 'Algorithm'
    ,'crypto.key': 'Key'
    ,'crypto.keyHint': 'Enter a key with the length required by the algorithm'
    ,'crypto.plainText': 'Plain text'
    ,'crypto.cipherText': 'Cipher text'
    ,'crypto.cipherOrSignature': 'Cipher text / signature'
    ,'crypto.encrypt': 'Encrypt'
    ,'crypto.decrypt': 'Decrypt'
    ,'crypto.generateKeyPair': 'Generate key pair'
    ,'crypto.keyGenerated': 'Key pair generated'
    ,'crypto.publicKey': 'Public key'
    ,'crypto.privateKey': 'Private key'
    ,'crypto.publicEncrypt': 'Public-key encrypt'
    ,'crypto.privateDecrypt': 'Private-key decrypt'
    ,'crypto.privateEncrypt': 'Private-key encrypt'
    ,'crypto.publicDecrypt': 'Public-key decrypt'
    ,'crypto.rsaOnly': 'This operation only supports RSA'
    ,'crypto.sign': 'Sign'
    ,'crypto.verify': 'Verify'
    ,'crypto.verified': 'Signature verified'
    ,'crypto.notVerified': 'Signature verification failed'
    ,'crypto.digest': 'Calculate digest'
    ,'crypto.fileDigest': 'File digest'
    ,'crypto.textDigest': 'Text digest'
    ,'crypto.digestInput': 'Digest input'
    ,'crypto.digestResult': 'Digest result'
    ,'crypto.encode': 'Encode'
    ,'crypto.decode': 'Decode'
    ,'crypto.length': 'Length'
    ,'crypto.generate': 'Generate'
    ,'crypto.random.uuid': 'UUID'
    ,'crypto.random.digits': 'Random digits'
    ,'crypto.random.string': 'Random string'
    ,'crypto.random.password': 'Random password'
    ,'color.title': 'Color Board'
    ,'color.select': 'Select color'
    ,'color.inputColor': 'Enter color'
    ,'color.picker': 'Screen picker'
    ,'color.freePick': 'Color picker'
    ,'color.eyeDropperUnavailable': 'Screen color picking is unavailable on this system'
    ,'color.format': 'Format'
    ,'color.code': 'Color code'
    ,'color.current': 'Current color'
    ,'color.compare': 'Comparison color'
    ,'color.operation.invert': 'Invert'
    ,'color.operation.intersect': 'Intersect'
    ,'color.operation.add': 'Add'
    ,'color.operation.difference': 'Difference'
    ,'color.operation.average': 'Average'
    ,'color.operation.swap': 'Swap'
    ,'color.themeColors': 'Theme colors'
    ,'color.theme': 'Theme'
    ,'color.theme.default': 'Default'
    ,'color.theme.theme1': 'Theme 1'
    ,'color.theme.theme2': 'Theme 2'
    ,'color.theme.theme3': 'Theme 3'
    ,'color.theme.theme4': 'Theme 4'
    ,'color.theme.theme5': 'Theme 5'
    ,'color.theme.china': 'Traditional Chinese'
    ,'color.standardColors': 'Standard colors'
    ,'color.shiftHint': 'Hold Shift to select the comparison color.'
    ,'qrcode.title': 'QR Code'
    ,'qrcode.tab.generate': 'Generate'
    ,'qrcode.tab.recognize': 'Recognize'
    ,'qrcode.tab.history': 'History'
    ,'qrcode.content': 'QR content'
    ,'qrcode.size': 'Size'
    ,'qrcode.correction': 'Error correction'
    ,'qrcode.logo': 'Center logo'
    ,'qrcode.chooseLogo': 'Choose logo'
    ,'qrcode.level.L': 'L · about 7%'
    ,'qrcode.level.M': 'M · about 15%'
    ,'qrcode.level.Q': 'Q · about 25%'
    ,'qrcode.level.H': 'H · about 30%'
    ,'qrcode.generate': 'Generate QR code'
    ,'qrcode.generated': 'QR code generated'
    ,'qrcode.preview': 'QR preview'
    ,'qrcode.chooseImage': 'Choose QR image'
    ,'qrcode.fromClipboard': 'Read from clipboard'
    ,'qrcode.clipboard': 'Clipboard image'
    ,'qrcode.clipboardEmpty': 'No image in the clipboard'
    ,'qrcode.recognize': 'Recognize QR code'
    ,'qrcode.recognized': 'QR recognition complete'
    ,'qrcode.sourceImage': 'Source image'
    ,'qrcode.result': 'Recognition result'
    ,'qrcode.history.generate': 'Generate QR code'
    ,'qrcode.history.recognize': 'Recognize QR code'
    ,'protobuf.title': 'Protobuf'
    ,'protobuf.tab.json': 'JSON / Binary'
    ,'protobuf.tab.wire': 'Wire Decoder'
    ,'protobuf.tab.convert': 'Hex / Base64'
    ,'protobuf.definition': 'Proto definition'
    ,'protobuf.message': 'Message type'
    ,'protobuf.binaryFormat': 'Binary format'
    ,'protobuf.toBinary': 'To Binary'
    ,'protobuf.toJson': 'To JSON'
    ,'protobuf.binary': 'Binary content'
    ,'protobuf.wireInput': 'Wire data'
    ,'protobuf.decode': 'Decode'
    ,'protobuf.wireOutput': 'Wire fields'
    ,'protobuf.hexToBase64': 'Hex to Base64'
    ,'protobuf.base64ToHex': 'Base64 to Hex'
    ,'protobuf.history.jsonToBinary': 'JSON to Binary'
    ,'protobuf.history.binaryToJson': 'Binary to JSON'
    ,'protobuf.history.format': 'Format Proto'
    ,'protobuf.history.wire': 'Decode Wire'
    ,'protobuf.history.hexToBase64': 'Hex to Base64'
    ,'protobuf.history.base64ToHex': 'Base64 to Hex'
    ,'pdf.title': 'PDF'
    ,'pdf.tab.split': 'Split PDF'
    ,'pdf.tab.merge': 'Merge PDF'
    ,'pdf.addTask': 'Add split task'
    ,'pdf.addFile': 'Add PDF'
    ,'pdf.startSplit': 'Start split'
    ,'pdf.startMerge': 'Start merge'
    ,'pdf.selectTask': 'Select at least one split task'
    ,'pdf.selectTwo': 'Select at least two PDF files'
    ,'pdf.confirmSplit': 'Start splitting the selected PDFs?'
    ,'pdf.splitComplete': 'Split complete, {count} pages written'
    ,'pdf.mergeComplete': 'Merge complete, {count} pages'
    ,'pdf.fileName': 'File name'
    ,'pdf.pageRange': 'Input pages'
    ,'pdf.mergeRange': 'Merge pages'
    ,'pdf.rule': 'Split rule'
    ,'pdf.customRule': 'Output pages'
    ,'pdf.progress': 'Status'
    ,'pdf.output': 'Recent output'
    ,'pdf.empty': 'No PDF tasks added'
    ,'pdf.pages': 'pages'
    ,'pdf.select': 'Select'
    ,'pdf.rule.odd': 'Odd pages'
    ,'pdf.rule.even': 'Even pages'
    ,'pdf.rule.custom': 'Custom'
    ,'pdf.status.ready': 'Ready'
    ,'pdf.status.running': 'Running'
    ,'pdf.status.done': 'Done'
    ,'pdf.status.error': 'Error'
    ,'pdf.helpSplitTitle': 'PDF split help'
    ,'pdf.helpMergeTitle': 'PDF merge help'
    ,'pdf.help.split1': 'Up to 20 PDF split tasks can be added at once.'
    ,'pdf.help.split2': 'Page ranges support values such as 1-5, 8, and 10-12.'
    ,'pdf.help.split3': 'Choose odd, even, or custom output pages.'
    ,'pdf.help.split4': 'Output files use the _split.pdf suffix by default.'
    ,'pdf.help.merge1': 'Select at least two PDF files to merge.'
    ,'pdf.help.merge2': 'Files are merged in list order using their page ranges.'
    ,'pdf.help.merge3': 'Only unencrypted PDF files are currently supported.'
    ,'image.title': 'Image Assistant'
    ,'image.screenshot': 'Capture'
    ,'image.fromClipboard': 'Clipboard'
    ,'image.import': 'Import images'
    ,'image.fromBase64': 'Import Base64'
    ,'image.compress': 'Compress'
    ,'image.watermark': 'Watermark'
    ,'image.copy': 'Copy image'
    ,'image.toBase64': 'To Base64'
    ,'image.toggleList': 'Show or hide image list'
    ,'image.library': 'Image library'
    ,'image.empty': 'Image library is empty'
    ,'image.select': 'Select image'
    ,'image.emptyPreview': 'Select or import an image'
    ,'image.zoomIn': 'Zoom in'
    ,'image.zoomOut': 'Zoom out'
    ,'image.original': 'Original size'
    ,'image.fit': 'Fit window'
    ,'image.base64Import': 'Import Base64 image'
    ,'image.base64Export': 'Image Base64'
    ,'image.compressTitle': 'Compress images'
    ,'image.watermarkTitle': 'Add watermark'
    ,'image.startProcess': 'Start processing'
    ,'image.selectedCount': '{count} images selected'
    ,'image.quality': 'Quality'
    ,'image.scale': 'Scale'
    ,'image.outputFormat': 'Output format'
    ,'image.format.auto': 'Keep original format'
    ,'image.watermarkText': 'Watermark text'
    ,'image.opacity': 'Opacity'
    ,'image.position': 'Position'
    ,'image.fontSize': 'Font size'
    ,'image.color': 'Color'
    ,'image.diagonal': 'Rotate watermark text'
    ,'image.outputMode': 'Output mode'
    ,'image.keepOriginal': 'Keep original and create a new file'
    ,'image.overwrite': 'Overwrite original'
    ,'image.position.bottom-right': 'Bottom right'
    ,'image.position.bottom-left': 'Bottom left'
    ,'image.position.top-right': 'Top right'
    ,'image.position.top-left': 'Top left'
    ,'image.position.center': 'Center'
    ,'image.position.tile': 'Tile'
    ,'image.font.auto': 'Auto'
    ,'image.font.small': 'Small'
    ,'image.font.medium': 'Medium'
    ,'image.font.large': 'Large'
    ,'image.crop.x': 'X'
    ,'image.crop.y': 'Y'
    ,'image.crop.width': 'Width'
    ,'image.crop.height': 'Height'
    ,'image.captureOverlayHint': 'Drag to select a capture area'
    ,'image.captureOverlayKeys': 'Enter to confirm · Esc or right-click to cancel'
    ,'image.captureConfirm': 'Confirm capture'
    ,'image.saveCapture': 'Save capture'
    ,'image.clipboardEmpty': 'No image in the clipboard'
    ,'image.imported': 'Image imported'
    ,'image.captureUnavailable': 'No display is available for capture'
    ,'image.saveName': 'Save name'
    ,'image.renamePrompt': 'Enter a new image name'
    ,'image.confirmDelete': 'Delete the selected {count} images?'
    ,'image.exported': 'Exported to {directory}'
    ,'image.processComplete': 'Processed {count} images'
    ,'common.new': 'New'
    ,'common.add': 'Add'
    ,'common.stop': 'Stop'
    ,'common.refresh': 'Refresh'
    ,'common.search': 'Search'
    ,'common.convert': 'Convert'
    ,'http.title': 'HTTP Request'
    ,'http.untitled': 'Untitled request'
    ,'http.urlRequired': 'Enter a request URL'
    ,'http.saveName': 'Request name'
    ,'http.confirmDelete': 'Delete this request?'
    ,'http.curlPrompt': 'Paste a cURL command'
    ,'http.savedEmpty': 'No saved requests'
    ,'http.noUrl': 'No URL'
    ,'http.importCurl': 'Import cURL'
    ,'http.copyCurl': 'Copy as cURL'
    ,'http.method': 'Method'
    ,'http.send': 'Send'
    ,'http.tab.params': 'Params'
    ,'http.tab.headers': 'Headers'
    ,'http.tab.cookies': 'Cookies'
    ,'http.tab.body': 'Body'
    ,'http.bodyType': 'Body type'
    ,'http.response.body': 'Response Body'
    ,'http.response.headers': 'Response Headers'
    ,'http.response.cookies': 'Response Cookies'
    ,'http.responseEmpty': 'Send a request to inspect its response'
    ,'http.name': 'Name'
    ,'http.value': 'Value'
    ,'http.domain': 'Domain'
    ,'http.path': 'Path'
    ,'http.expires': 'Expires'
    ,'http.enabled': 'Enabled'
    ,'http.addEntry': 'Add an entry'
    ,'http.history': 'HTTP Request History'
    ,'http.error.ABORTED': 'Request cancelled'
    ,'http.error.TIMEOUT': 'Request timed out'
    ,'http.error.NETWORK': 'Network request failed'
    ,'http.error.INVALID_REQUEST': 'Invalid request'
    ,'http.error.RESPONSE_TOO_LARGE': 'Response exceeds the 10 MB limit'
    ,'translation.title': 'Translation'
    ,'translation.tab.translate': 'Translate'
    ,'translation.tab.words': 'Word Book'
    ,'translation.tab.history': 'History'
    ,'translation.exchange': 'Swap languages and text'
    ,'translation.provider': 'Translator'
    ,'translation.copy': 'Copy translation'
    ,'translation.saveWord': 'Save to word book'
    ,'translation.savedWord': 'Saved to word book'
    ,'translation.sourcePlaceholder': 'Enter text to translate'
    ,'translation.targetPlaceholder': 'Translation'
    ,'translation.translating': 'Translating…'
    ,'translation.fallback': 'fallback used'
    ,'translation.sourceLanguage': 'Source language'
    ,'translation.targetLanguage': 'Target language'
    ,'translation.apply': 'Apply to translation'
    ,'translation.retranslate': 'Translate again'
    ,'translation.confirmDeleteWord': 'Delete this word?'
    ,'translation.searchWords': 'Search words, translations, or notes'
    ,'translation.remark': 'Note (optional)'
    ,'translation.wordEmpty': 'Select or create a word'
    ,'translation.searchHistory': 'Search source, translation, or language'
    ,'translation.lang.auto': 'Detect language'
    ,'translation.lang.zh-CN': 'Chinese (Simplified)'
    ,'translation.lang.en': 'English'
    ,'translation.lang.yue': 'Cantonese'
    ,'translation.lang.wyw': 'Classical Chinese'
    ,'translation.lang.jp': 'Japanese'
    ,'translation.lang.kor': 'Korean'
    ,'translation.lang.fra': 'French'
    ,'translation.lang.spa': 'Spanish'
    ,'translation.lang.th': 'Thai'
    ,'translation.lang.ara': 'Arabic'
    ,'translation.lang.ru': 'Russian'
    ,'translation.lang.pt': 'Portuguese'
    ,'translation.lang.de': 'German'
    ,'translation.lang.it': 'Italian'
    ,'translation.lang.el': 'Greek'
    ,'translation.lang.nl': 'Dutch'
    ,'translation.lang.pl': 'Polish'
    ,'translation.lang.bul': 'Bulgarian'
    ,'translation.lang.est': 'Estonian'
    ,'translation.lang.dan': 'Danish'
    ,'translation.lang.fin': 'Finnish'
    ,'translation.lang.cs': 'Czech'
    ,'translation.lang.rom': 'Romanian'
    ,'translation.lang.slo': 'Slovenian'
    ,'translation.lang.swe': 'Swedish'
    ,'translation.lang.hu': 'Hungarian'
    ,'translation.lang.cht': 'Chinese (Traditional)'
    ,'translation.lang.vie': 'Vietnamese'
    ,'host.title': 'Host'
    ,'host.discardChanges': 'Discard unsaved changes?'
    ,'host.untitled': 'Untitled Host'
    ,'host.namePrompt': 'Host profile name'
    ,'host.confirmDelete': 'Delete this Host profile?'
    ,'host.confirmApply': 'Write this content to the system hosts file? Administrator permission may be requested.'
    ,'host.applied': 'System hosts updated'
    ,'host.applying': 'Applying…'
    ,'host.apply': 'Switch Host'
    ,'host.empty': 'No Host profiles'
    ,'host.import': 'Import Host'
    ,'host.export': 'Export Host'
    ,'host.profileName': 'Profile name'
    ,'host.current': 'Current system hosts'
    ,'host.find': 'Find and replace'
    ,'host.findPlaceholder': 'Find'
    ,'host.replacePlaceholder': 'Replace with'
    ,'host.replace': 'Replace'
    ,'host.replaceAll': 'Replace all'
    ,'host.content': 'Host content'
    ,'host.placeholder': 'Enter hosts file content'
    ,'host.writable': 'Writable by the current user'
    ,'host.requiresPrivilege': 'Administrator permission required to write'
    ,'net.title': 'Network/IP'
    ,'net.running': 'Running…'
    ,'net.noOutput': 'Command completed without output'
    ,'net.outputPlaceholder': 'Run a network command to view its output here'
    ,'net.ipv4Long': 'IPv4 and Long Conversion'
    ,'net.ping': 'PING'
    ,'net.resolve': 'Resolve Host to IP'
    ,'net.resolveAction': 'Resolve'
    ,'net.whois': 'WHOIS Lookup'
    ,'net.query': 'Query'
    ,'net.dns': 'DNS'
    ,'net.flushDns': 'Flush DNS Cache'
    ,'net.localAddresses': 'Local IP Addresses'
    ,'net.error.ABORTED': 'Command stopped'
    ,'net.error.TIMEOUT': 'Command timed out'
    ,'net.error.PERMISSION': 'Permission denied'
    ,'net.error.UNSUPPORTED': 'This command is unavailable on the current system'
    ,'net.error.COMMAND_FAILED': 'Command failed'
    ,'net.error.INVALID_TARGET': 'Invalid target'
    ,'variables.title': 'Environment Variables'
    ,'variables.tab.environment': 'System Environment'
    ,'variables.tab.runtime': 'Electron Runtime Properties'
    ,'variables.key': 'Key'
    ,'variables.value': 'Value'
    ,'variables.count': 'Showing {count} of {total}'
    ,'runtime.title': 'Code Runner'
    ,'runtime.tab.java': 'Java / Groovy'
    ,'runtime.tab.python': 'Python'
    ,'runtime.tab.node': 'Node.js'
    ,'runtime.mode.java': 'Java'
    ,'runtime.mode.groovy': 'Groovy'
    ,'runtime.detect': 'Detect runtimes'
    ,'runtime.detected': '{count} runtimes detected'
    ,'runtime.missing': '{name} was not detected'
    ,'runtime.format': 'Format'
    ,'runtime.run': 'Run'
    ,'runtime.stop': 'Stop'
    ,'runtime.clear': 'Clear output'
    ,'runtime.history': 'Run history'
    ,'runtime.editor': 'Code editor'
    ,'runtime.output': 'Console output'
    ,'runtime.ready': 'Run code to see output'
    ,'runtime.running': 'Running {name}…'
    ,'runtime.completed': 'Run completed'
    ,'runtime.failed': 'Run failed'
    ,'runtime.cancelled': 'Run stopped'
    ,'runtime.timeout': 'Run timed out'
    ,'runtime.truncated': 'Output exceeded 2 MB and the run was stopped'
    ,'runtime.command': 'Command'
    ,'runtime.exitCode': 'Exit code {code}'
    ,'runtime.duration': '{duration} ms'
    ,'runtime.configure': 'Configure the {name} runtime path in Settings'
    ,'runtime.openSettings': 'Open runtime settings'
    ,'runtime.options': 'Run Options'
    ,'runtime.arguments': 'Program arguments'
    ,'runtime.argumentsPlaceholder': '--name "Moo Tool"'
    ,'runtime.workingDirectory': 'Working directory'
    ,'runtime.defaultWorkingDirectory': 'Use an isolated temporary directory'
    ,'quickNote.title': 'Quick Note'
    ,'quickNote.search': 'Search notes…'
    ,'quickNote.searchContent': 'Search contents'
    ,'quickNote.sort': 'Sort'
    ,'quickNote.sort.name': 'Name'
    ,'quickNote.sort.modified': 'Last modified'
    ,'quickNote.sort.created': 'Created'
    ,'quickNote.empty': 'No matching notes'
    ,'quickNote.newNote': 'New note'
    ,'quickNote.newFolder': 'New folder'
    ,'quickNote.expandAll': 'Expand all'
    ,'quickNote.collapseAll': 'Collapse all'
    ,'quickNote.rename': 'Rename'
    ,'quickNote.move': 'Move'
    ,'quickNote.duplicate': 'Duplicate'
    ,'quickNote.delete': 'Delete'
    ,'quickNote.openVault': 'Open Vault in file manager'
    ,'quickNote.save': 'Save'
    ,'quickNote.saved': 'Note saved'
    ,'quickNote.unsaved': 'Unsaved changes'
    ,'quickNote.find': 'Find and replace'
    ,'quickNote.findPlaceholder': 'Find'
    ,'quickNote.replacePlaceholder': 'Replace with'
    ,'quickNote.nextMatch': 'Next match'
    ,'quickNote.replace': 'Replace'
    ,'quickNote.replaceAll': 'Replace all'
    ,'quickNote.noMatches': 'No matches found'
    ,'quickNote.attachment': 'Insert image attachment (or paste from clipboard)'
    ,'quickNote.clipboardEmpty': 'No usable image in the clipboard'
    ,'quickNote.export': 'Export current note'
    ,'quickNote.info': 'Document information'
    ,'quickNote.git': 'Git sync'
    ,'quickNote.quickReplace': 'Quick replace'
    ,'quickNote.view.editor': 'Editor'
    ,'quickNote.view.split': 'Split'
    ,'quickNote.view.preview': 'Preview'
    ,'quickNote.syntax': 'Syntax'
    ,'quickNote.font': 'Font'
    ,'quickNote.font.system': 'System font'
    ,'quickNote.font.mono': 'Monospace'
    ,'quickNote.font.serif': 'Serif'
    ,'quickNote.fontSize': 'Font size'
    ,'quickNote.wrap': 'Soft wrap'
    ,'quickNote.bulletList': 'Bulleted list'
    ,'quickNote.numberedList': 'Numbered list'
    ,'quickNote.color': 'Note color'
    ,'quickNote.select': 'Select or create a note from the left'
    ,'quickNote.editorLabel': 'Note content'
    ,'quickNote.confirmDelete': 'Delete “{name}”? Folders must be empty.'
    ,'quickNote.dialog.createNote': 'New note'
    ,'quickNote.dialog.createFolder': 'New folder'
    ,'quickNote.dialog.rename': 'Rename'
    ,'quickNote.dialog.move': 'Move to folder'
    ,'quickNote.dialog.name': 'Name'
    ,'quickNote.dialog.target': 'Target folder'
    ,'quickNote.dialog.root': 'Vault root'
    ,'quickNote.create': 'Create'
    ,'quickNote.apply': 'Apply'
    ,'quickNote.path': 'Path'
    ,'quickNote.created': 'Created'
    ,'quickNote.modified': 'Modified'
    ,'quickNote.lines': 'Lines'
    ,'quickNote.words': 'Words'
    ,'quickNote.characters': 'Characters'
    ,'quickNote.status': '{lines} lines · {characters} characters'
    ,'quickNote.preview.empty': 'Choose Markdown syntax to preview formatted content.'
    ,'quickNote.quick.trim': 'Trim whitespace'
    ,'quickNote.quick.removeBlankLines': 'Remove blank lines'
    ,'quickNote.quick.removeTabs': 'Remove tabs'
    ,'quickNote.quick.scientificToNormal': 'Scientific to decimal'
    ,'quickNote.quick.normalToScientific': 'Decimal to scientific'
    ,'quickNote.quick.thousandsToNormal': 'Remove thousands separators'
    ,'quickNote.quick.normalToThousands': 'Add thousands separators'
    ,'quickNote.quick.underscoreToCamel': 'Snake case to camel case'
    ,'quickNote.quick.camelToUnderscore': 'Camel case to snake case'
    ,'quickNote.quick.uppercase': 'Uppercase'
    ,'quickNote.quick.lowercase': 'Lowercase'
    ,'quickNote.quick.linesToComma': 'Lines to comma list'
    ,'quickNote.quick.linesToSingleQuoted': 'Lines to single-quoted list'
    ,'quickNote.quick.linesToDoubleQuoted': 'Lines to double-quoted list'
    ,'quickNote.quick.commaToLines': 'Comma list to lines'
    ,'quickNote.quick.tabsToLines': 'Tabs to lines'
    ,'quickNote.quick.clearNewlines': 'Remove newlines'
    ,'quickNote.quick.deduplicateLines': 'Deduplicate lines'
    ,'quickNote.quick.deduplicateWithCount': 'Deduplicate with counts'
    ,'quickNote.quick.escape': 'Escape text'
    ,'quickNote.quick.unescape': 'Unescape text'
    ,'quickNote.quick.reverseLines': 'Reverse lines'
    ,'quickNote.quick.sortAscending': 'Sort lines A–Z'
    ,'quickNote.quick.sortDescending': 'Sort lines Z–A'
    ,'quickNote.git.title': 'Quick Note Vault Git'
    ,'quickNote.git.defaultMessage': 'MooTool Quick Note checkpoint'
    ,'hardware.title': 'System Information'
    ,'hardware.tab.system': 'System'
    ,'hardware.tab.cpu': 'Processor'
    ,'hardware.tab.memory': 'Memory'
    ,'hardware.tab.storage': 'Storage'
    ,'hardware.tab.network': 'Network'
    ,'hardware.loading': 'Collecting system information…'
    ,'hardware.empty': 'No information available'
    ,'hardware.group.operatingSystem': 'Operating System'
    ,'hardware.group.processor': 'Processor'
    ,'hardware.group.physicalMemory': 'Physical Memory'
    ,'hardware.label.platform': 'Platform'
    ,'hardware.label.distribution': 'Distribution'
    ,'hardware.label.kernel': 'Kernel'
    ,'hardware.label.architecture': 'Architecture'
    ,'hardware.label.hostName': 'Host name'
    ,'hardware.label.serial': 'Serial'
    ,'hardware.label.manufacturer': 'Manufacturer'
    ,'hardware.label.model': 'Model'
    ,'hardware.label.uptime': 'Uptime'
    ,'hardware.label.timeZone': 'Time zone'
    ,'hardware.label.brand': 'Brand'
    ,'hardware.label.vendor': 'Vendor'
    ,'hardware.label.family': 'Family'
    ,'hardware.label.physicalCores': 'Physical cores'
    ,'hardware.label.logicalCores': 'Logical cores'
    ,'hardware.label.performanceCores': 'Performance cores'
    ,'hardware.label.efficiencyCores': 'Efficiency cores'
    ,'hardware.label.baseSpeed': 'Base speed'
    ,'hardware.label.maximumSpeed': 'Maximum speed'
    ,'hardware.label.currentLoad': 'Current load'
    ,'hardware.label.total': 'Total'
    ,'hardware.label.used': 'Used'
    ,'hardware.label.available': 'Available'
    ,'hardware.label.active': 'Active'
    ,'hardware.label.usage': 'Usage'
    ,'hardware.label.swapTotal': 'Swap total'
    ,'hardware.label.swapUsed': 'Swap used'
    ,'hardware.label.device': 'Device'
    ,'hardware.label.type': 'Type'
    ,'hardware.label.interface': 'Interface'
    ,'hardware.label.capacity': 'Capacity'
    ,'hardware.label.filesystem': 'Filesystem'
    ,'hardware.label.ipv4': 'IPv4'
    ,'hardware.label.ipv6': 'IPv6'
    ,'hardware.label.mac': 'MAC'
    ,'hardware.label.mtu': 'MTU'
    ,'hardware.label.speed': 'Speed'
    ,'hardware.label.status': 'Status'
    ,'hardware.label.received': 'Received'
    ,'hardware.label.sent': 'Sent'
  },
  'ja-JP': {
    'common.toast.dismiss': '通知を閉じる',
    'common.resizePane': 'ペイン幅を調整',
    'common.loading': '読み込み中…',
    'common.close': '閉じる',
    'common.cancel': 'キャンセル',
    'findReplace.find': '検索',
    'findReplace.findPlaceholder': '検索',
    'findReplace.replacePlaceholder': '置換文字列',
    'findReplace.replace': '置換',
    'findReplace.replaceAll': 'すべて置換',
    'findReplace.regex': '正規表現を使用',
    'findReplace.matchCase': '大文字と小文字を区別',
    'findReplace.wholeWord': '単語単位',
    'findReplace.foundPrefix': '合計:',
    'findReplace.replacedPrefix': '置換済み:',
    'findReplace.previous': '前の一致',
    'findReplace.next': '次の一致',
    'findReplace.noMatches': '一致する内容がありません',
    'app.nav.tools': 'ツールナビゲーション',
    'app.nav.home': 'ホーム',
    'app.nav.quickNote': 'クイックノート',
    'app.nav.textDiff': 'テキスト比較',
    'app.nav.reformat': 'フォーマット',
    'app.nav.json': 'JSON',
    'app.nav.java': 'コード実行',
    'app.nav.ymlProperties': '設定ファイル変換',
    'app.nav.protobuf': 'Protobuf',
    'app.nav.variables': '環境変数',
    'app.nav.host': 'Host',
    'app.nav.net': 'ネットワーク/IP',
    'app.nav.uaParse': 'UA 解析',
    'app.nav.timeConvert': '時刻変換',
    'app.nav.translation': '翻訳',
    'app.nav.calculator': '計算機',
    'app.nav.hardware': 'システム情報',
    'app.nav.encode': 'エンコード',
    'app.nav.crypto': '暗号化/ランダム',
    'app.nav.qrCode': 'QRコード',
    'app.nav.http': 'HTTP リクエスト',
    'app.nav.diff': 'テキスト Diff',
    'app.nav.regex': '正規表現',
    'app.nav.colorBoard': 'カラーパネル',
    'app.nav.image': '画像処理',
    'app.nav.pdf': 'PDF',
    'app.nav.cron': 'Cron',
    'app.group.text': 'ノートとテキスト',
    'app.group.dev': '開発とフォーマット',
    'app.group.network': 'ネットワークとリクエスト',
    'app.group.encode': 'エンコードと暗号化',
    'app.group.daily': 'ユーティリティ',
    'app.group.system': 'システム',
    'app.nav.recent': '最近',
    'app.nav.settings': '設定',
    'app.nav.search': '検索',
    'app.nav.collapse': 'サイドバーを折りたたむ',
    'app.nav.expand': 'サイドバーを展開',
    'app.nav.manageGroups': 'グループを管理',
    'app.group.all': 'すべてのツール',
    'app.group.manage.title': 'ツールグループを管理',
    'app.group.manage.new': '新しいグループ',
    'app.group.manage.defaultName': '新しいグループ {number}',
    'app.group.manage.empty': 'カスタムグループはありません。「新しいグループ」から作成できます。',
    'app.group.manage.name': 'グループ名',
    'app.group.manage.tools': 'ツールを選択',
    'app.group.manage.delete': 'グループを削除',
    'app.group.manage.deleteConfirm': 'グループ「{name}」を削除しますか？',
    'app.group.manage.nameRequired': 'グループ名を入力してください。',
    'app.group.manage.toolRequired': 'ツールを1つ以上選択してください。',
    'app.group.manage.saveFailed': 'グループを保存できませんでした',
    'app.recent.empty': '最近使ったツールはありません',
    'app.search.title': 'ツールを検索',
    'app.search.placeholder': '機能を検索…',
    'app.search.empty': '一致する機能がありません',
    'app.search.close': '検索を閉じる',
    'app.home.website': 'MooTool のホームページを開く',
    'app.home.tagline': '開発者向けの便利なデスクトップツールセット',
    'app.home.author': 'Proudly by RememBerBer Zhou Bo',
    'app.home.about.title': 'MooTool について',
    'app.home.about.line1': 'MooTool をご利用いただきありがとうございます。「Moo」は作者の娘の愛称です。',
    'app.home.about.line2': 'Swing で開発する Java 開発者は減りましたが、今でもその開発を楽しんでいます。',
    'app.home.about.line2Note': 'もっとも、今お使いのものはもう Swing 版ではありません 😛',
    'app.home.about.line3': '空いた時間を少しずつ、このプロジェクトの開発に注いできました。',
    'app.home.about.line4': 'Web 上に似たツールが多くある中でも、ひとつの形にすることができました。',
    'app.home.about.line5': '開発したときと同じくらい、楽しく使っていただければ幸いです。',
    'app.home.source.title': 'ソースコード',
    'app.home.help.title': '改善にご協力ください',
    'app.home.help.issue': '問題やアイデアを共有',
    'app.home.thanks.title': '謝辞',
    'app.home.otherWorks.title': 'その他の作品',
    'app.home.wePush.desc': '一括配信に特化した軽量ツール',
    'app.home.mooInfo.desc': 'システムとハードウェア情報を表示する OSHI の可視化実装',
    'app.home.sponsor.title': 'スポンサー',
    'app.home.sponsor.prompt': 'コーヒーをごちそうする',
    'app.home.sponsor.tip': 'ご支援ありがとうございます',
    'app.home.contributors.title': 'コントリビューター',
    'app.home.contributors.thanks': 'MooTool に貢献してくださった皆さまに感謝します。',
    'app.placeholder': 'このツールページは JSON ワークベンチの構成で移行します。',
    'toolWindow.detach': '別ウィンドウで開く',
    'toolWindow.dock': 'ワークスペースに戻す',
    'toolWindow.focus': 'ウィンドウを表示',
    'toolWindow.detachedTitle': '{tool} は別ウィンドウで開いています',
    'toolWindow.detachedDescription': '別ウィンドウを閉じると、このツールは自動的にここへ戻ります。',
    'toolWindow.invalid': 'このツールウィンドウを開けませんでした。',
    'settings.title': '設定',
    'settings.close': '設定を閉じる',
    'settings.saveFailed': '設定を保存できませんでした',
    'settings.language': '言語',
    'settings.category.general': '一般',
    'settings.category.appearance': '外観',
    'settings.category.layout': 'レイアウトと操作',
    'settings.category.editor': 'エディター',
    'settings.category.network': 'ネットワーク',
    'settings.category.data': 'データとバックアップ',
    'settings.category.vault': 'Vault',
    'settings.category.runtime': '実行環境',
    'settings.category.tools': 'ツールの既定値',
    'settings.category.shortcuts': 'ショートカット',
    'settings.category.about': '情報と更新',
    'settings.group.application': 'アプリの動作',
    'settings.group.theme': 'テーマとフォント',
    'settings.group.navigation': '機能ナビゲーション',
    'settings.group.editor': '編集',
    'settings.group.proxy': 'HTTP プロキシ',
    'settings.group.timeouts': 'タイムアウトとキャンセル',
    'settings.group.storage': 'データディレクトリ',
    'settings.group.backup': 'ローカルバックアップ',
    'settings.backup.all': '完全バックアップ',
    'settings.backup.database': 'データファイル',
    'settings.backup.settings': '設定ファイル',
    'settings.backup.images': '画像ディレクトリ',
    'settings.backup.export': 'エクスポート',
    'settings.backup.open': 'フォルダーを開く',
    'settings.backup.success': 'バックアップをエクスポートしました',
    'settings.group.migration': '旧版データの移行',
    'settings.migration.source': 'Java 版データディレクトリ',
    'settings.migration.scan': 'スキャン',
    'settings.migration.ready': '{count} 件の移行可能なデータが見つかりました',
    'settings.migration.databaseFound': 'データベースが見つかりました',
    'settings.migration.databaseMissing': '旧データベースが見つかりません',
    'settings.migration.configFound': '設定が見つかりました',
    'settings.migration.configMissing': '旧設定が見つかりません',
    'settings.migration.import': '移行を開始',
    'settings.migration.importing': '移行中…',
    'settings.migration.confirmTitle': 'Java 版データを移行',
    'settings.migration.confirmBody': '{count} 件のデータを移行します。MooTool Next は先に完全バックアップを作成し、Java 版の元ファイルは変更しません。',
    'settings.migration.success': '{count} 件のデータを移行しました',
    'settings.migration.alreadyMigrated': 'この移行元はすでに移行済みです',
    'settings.migration.warning.remotes': '旧 Vault のリモートが異なるため、リモート URL は自動統合されません。',
    'settings.migration.warning.secrets': 'プロキシパスワードと Git Token は移行されません。設定で安全に保存し直してください。',
    'app.migrationHint.title': 'Java 版データを移行',
    'app.migrationHint.body': '以前に Java 版 MooTool を使っていた場合、「設定 → データとバックアップ」から履歴データを移行できます。移行しても Java 版の元データは変更・削除されません。',
    'app.migrationHint.openSettings': '設定を開く',
    'app.migrationHint.dismiss': '了解',
    'settings.group.vaultPaths': 'Vault パス',
    'settings.group.git': 'Git 同期',
    'settings.group.runtimes': 'ローカル実行環境',
    'settings.group.toolDefaults': '既定のパラメーター',
    'settings.group.shortcuts': 'アプリのショートカット',
    'settings.autoCheckUpdates': '起動時に更新を確認',
    'settings.autoDownloadUpdates': '新しいバージョンを自動ダウンロード',
    'settings.startMaximized': '最大化して起動',
    'settings.trayEnabled': 'システムトレイを有効化',
    'settings.closeBehavior': 'メインウィンドウを閉じるとき',
    'settings.close.ask': '毎回確認',
    'settings.close.hide': '隠す',
    'settings.close.quit': '終了',
    'settings.interfaceStyle': 'インターフェーススタイル',
    'settings.interfaceStyle.modern': 'モダン',
    'settings.interfaceStyle.quiet': 'クワイエット',
    'settings.theme': 'カラーモード',
    'settings.theme.system': 'システム',
    'settings.theme.light': 'ライト',
    'settings.theme.dark': 'ダーク',
    'settings.accentColor': 'アクセントカラー',
    'settings.fontSize': '全体の文字サイズ',
    'settings.unifiedBackground': 'ワークスペースの背景を統一',
    'settings.navigationStyle': 'ナビゲーション形式',
    'settings.navigation.classic': 'クラシック',
    'settings.navigation.card': 'カード',
    'settings.navigation.grouped': 'グループ',
    'settings.navigation.toolsTitle': 'サイドバーのツール一覧',
    'settings.navigation.toolsDescription': '「すべてのツール」に表示する項目を選択します。ホーム、検索、最近使ったツール、カスタムグループには影響しません。',
    'settings.navigation.showAll': 'すべて表示',
    'settings.navigation.hideAll': 'すべて非表示',
    'settings.showRecent': '最近使ったツールを表示',
    'settings.compactNavigation': 'コンパクトナビゲーション',
    'settings.showSeparators': '区切り線を表示',
    'settings.hideNavigationTitles': 'ナビゲーションのタイトルを隠す',
    'settings.sqlDialect': 'SQL 方言',
    'settings.jsonFontSize': 'JSON の文字サイズ',
    'settings.quickNoteFontSize': 'クイックノートの文字サイズ',
    'settings.softWrap': 'エディターで折り返す',
    'settings.proxyEnabled': 'HTTP プロキシを使用',
    'settings.proxyHost': 'Host',
    'settings.proxyPort': 'ポート',
    'settings.proxyUsername': 'ユーザー名',
    'settings.proxyPassword': 'パスワード',
    'settings.requestTimeout': 'HTTP タイムアウト（ミリ秒）',
    'settings.translationTimeout': '翻訳タイムアウト（ミリ秒）',
    'settings.dataDirectory': 'データ保存場所',
    'settings.quickNoteVault': 'クイックノート Vault',
    'settings.jsonVault': 'JSON Vault',
    'settings.gitRemote': 'リモートリポジトリ',
    'settings.gitUsername': 'Git ユーザー名',
    'settings.gitToken': 'Git 個人トークン',
    'settings.autoCommit': 'Git チェックポイントを自動作成',
    'settings.autoCommitIdleSeconds': '編集アイドル後に保存（秒）',
    'settings.autoCommitInactiveSeconds': 'ウィンドウ非アクティブ後に保存（秒）',
    'settings.autoPullMinutes': '自動 pull 間隔（分）',
    'settings.hideGitignoredFiles': '.gitignore 対象ファイルを隠す',
    'settings.runtime.detect': '再検出',
    'settings.runtime.notFound': '見つかりません',
    'settings.runtime.auto': 'システムから自動検出',
    'settings.runtime.path': 'パス',
    'settings.qrCodeSize': 'QR コードサイズ',
    'settings.qrErrorCorrection': 'QR 誤り訂正レベル',
    'settings.randomStringLength': 'ランダム文字列の長さ',
    'settings.exportDirectory': '既定の出力先',
    'settings.translationProvider': '既定の翻訳サービス',
    'settings.translationSource': '既定の翻訳元言語',
    'settings.translationTarget': '既定の翻訳先言語',
    'settings.shortcut.search': 'ツールを検索',
    'settings.shortcut.settings': '設定を開く',
    'settings.chooseDirectory': 'ディレクトリを選択',
    'settings.version': 'バージョン {version}',
    'settings.update.check': 'アップデートを確認',
    'settings.update.checking': '確認中…',
    'settings.update.latest': '現在のバージョンは最新です',
    'settings.update.available': '新しいバージョン {version} があります',
    'settings.update.failed': 'アップデートを確認できませんでした。後でもう一度お試しください。',
    'settings.update.target': '{product} · {platform}/{architecture}',
    'settings.update.download': 'アップデートをダウンロード',
    'settings.update.downloading': 'バックグラウンドでダウンロード中 {percent}%',
    'settings.update.ready': 'バージョン {version} の準備ができました',
    'settings.update.whatsNew': '更新内容',
    'settings.update.installRestart': 'インストールして再起動',
    'settings.update.manualInstall': 'ダウンロード済みです。DMG を開いてインストールしてください',
    'settings.update.openDownloaded': 'DMG を開く',
    'settings.update.downloadFailed': 'アップデートのダウンロードに失敗しました。後でもう一度お試しください。',
    'settings.update.installFailed': 'アップデートのインストールを開始できませんでした。後でもう一度お試しください。',
    'settings.update.noDownload': 'このデバイスに対応するインストーラーはまだありません。リリースページをご確認ください。',
    'settings.update.openRelease': 'リリースページを開く',
    'settings.update.project': 'プロジェクトページ',
    'settings.secret.stored': '安全に保存済み',
    'settings.secret.saved': '機密情報を安全に保存しました',
    'settings.secret.save': '保存',
    'settings.secret.clear': '消去',
    'settings.secret.unavailable': 'システムの安全なストレージを利用できません',
    'history.title': '履歴',
    'history.search': '履歴を検索…',
    'history.empty': '履歴はありません',
    'history.clearAll': 'すべて消去',
    'history.confirmClear': 'このツールの履歴をすべて消去しますか？',
    'history.copyInput': '入力をコピー',
    'history.copyOutput': '出力をコピー',
    'history.delete': '履歴を削除',
    'json.title': 'JSON ワークベンチ',
    'json.editor.label': 'JSON エディター',
    'json.valid.idle': 'JSON 入力待ち',
    'json.valid.ok': '有効な JSON · {type}',
    'json.valid.error': 'JSON の解析に失敗しました',
    'json.error.empty': 'JSON を入力してください',
    'json.error.notString': '現在の内容は JSON 文字列ではありません',
    'json.error.duplicateKeys': '重複したキーがあります：{paths}',
    'json.error.emptyXml': 'XML を入力してください',
    'json.error.emptyPath': 'JSON Path を入力してください',
    'json.error.objectRequired': 'この操作には JSON Object が必要です',
    'json.error.emptyJavaBean': 'JavaBean クラスコードを入力してください',
    'json.error.noJavaFields': '変換できる Java フィールドが見つかりません',
    'json.action.format': '整形',
    'json.action.compress': '圧縮',
    'json.action.wrap': '折返し',
    'json.action.nowrap': '一行',
    'json.action.copy': 'コピー',
    'json.action.copied': 'コピー済み',
    'json.action.clear': 'クリア',
    'json.action.find': '検索',
    'json.action.import': '読み込み',
    'json.action.export': '書き出し',
    'json.action.history': '履歴',
    'json.action.more': 'その他のツール',
    'json.action.escape': 'JSON 文字列へエスケープ',
    'json.action.unescape': 'JSON 文字列を復元',
    'json.action.escapeText': '文字列をエスケープ',
    'json.action.unescapeText': '文字列をアンエスケープ',
    'json.action.jsonToXml': 'JSON から XML',
    'json.action.xmlToJson': 'XML から JSON',
    'json.action.beanToJson': 'JavaBean から JSON',
    'json.action.jsonToBean': 'JSON から JavaBean',
    'json.action.swap': 'Key / Value 入れ替え',
    'json.notice.formatted': '整形しました',
    'json.notice.compressed': '圧縮しました',
    'json.notice.copied': 'クリップボードにコピーしました',
    'json.notice.copyFailed': 'コピーに失敗しました',
    'json.notice.failed': '処理に失敗しました',
    'json.notice.escaped': 'JSON 文字列に変換しました',
    'json.notice.unescaped': 'JSON 文字列を復元しました',
    'json.notice.imported': 'ファイルを読み込みました',
    'json.notice.exported': 'ファイルを書き出しました',
    'json.notice.pathApplied': 'JSON Path を適用しました',
    'json.notice.noMatches': '一致する内容はありません',
    'json.panel.actions': '操作',
    'json.panel.result': '結果',
    'json.panel.format': 'フォーマット設定',
    'json.panel.convert': '変換',
    'json.panel.jsonPath': 'JSON Path',
    'json.format.indent': 'インデント空白数',
    'json.format.sortKeys': 'Object Key をソート',
    'json.format.ignoreCase': 'ソートと重複検査で大文字小文字を無視',
    'json.format.duplicateKeys': 'フォーマット前に重複 Key を検査',
    'json.format.apply': 'カスタム整形を適用',
    'json.find.placeholder': 'JSON 内を検索…',
    'json.find.matches': '{count} 件',
    'json.find.next': '次の一致',
    'json.find.close': '検索を閉じる',
    'json.path.placeholder': '例: $.store.book[*].title',
    'json.path.query': '検索',
    'json.path.pick': 'ビジュアル選択',
    'json.pathPicker.title': 'JSON Path を選択',
    'json.pathPicker.use': 'このパスを使用',
    'json.pathPicker.path': 'パス',
    'json.pathPicker.preview': '値のプレビュー',
    'json.dialog.input': '入力',
    'json.dialog.output': '出力',
    'json.dialog.run': '変換',
    'json.dialog.className': 'ルートクラス名',
    'json.dialog.useOutput': '出力を使用',
    'json.vault.title': 'JSON Vault',
    'json.vault.new': '新規スニペット',
    'json.vault.save': 'スニペットを保存',
    'json.vault.delete': 'スニペットを削除',
    'json.vault.refresh': 'Vault を更新',
    'json.vault.empty': 'JSON スニペットはありません',
    'json.vault.fileName': 'ファイル名または相対パス',
    'json.vault.fileNameHint': '例: drafts/request.json',
    'json.vault.create': '作成',
    'json.vault.saved': 'スニペットを保存しました',
    'json.vault.created': 'スニペットを作成しました',
    'json.vault.deleted': 'スニペットを削除しました',
    'json.vault.confirmDelete': '「{name}」を削除しますか？',
    'json.vault.confirmDiscard': '現在のスニペットに未保存の変更があります。破棄しますか？'
    ,'json.vault.newFolder': '新規フォルダー'
    ,'json.vault.expandAll': 'すべて展開'
    ,'json.vault.collapseAll': 'すべて折りたたむ'
    ,'json.vault.defaultFolder': '新規フォルダー'
    ,'json.vault.folderName': 'フォルダーの相対パス'
    ,'json.vault.folderCreated': 'フォルダーを作成しました'
    ,'json.vault.rename': '名前を変更'
    ,'json.vault.renameName': '新しい名前'
    ,'json.vault.renamed': '項目の名前を変更しました'
    ,'json.vault.move': '移動'
    ,'json.vault.moveTo': '移動先フォルダー'
    ,'json.vault.moved': '項目を移動しました'
    ,'json.vault.duplicate': 'スニペットを複製'
    ,'json.vault.duplicated': 'スニペットのコピーを作成しました'
    ,'json.vault.openFolder': 'ファイルマネージャーで Vault を開く'
    ,'json.vault.sort': '並び順'
    ,'json.vault.sortName': '名前順'
    ,'json.vault.sortModified': '更新日時順'
    ,'json.vault.more': 'その他の Vault 操作'
    ,'json.vault.root': 'Vault ルート'
    ,'json.git.title': 'JSON Vault Git'
    ,'json.git.open': 'Git パネルを開く'
    ,'json.git.unavailable': 'Git コマンドが見つかりません'
    ,'json.git.noRepo': 'この Vault はまだ Git リポジトリではありません'
    ,'json.git.init': 'Git を初期化'
    ,'json.git.refresh': '更新'
    ,'json.git.fetch': 'Fetch'
    ,'json.git.pull': 'Pull'
    ,'json.git.push': 'Push'
    ,'json.git.branch': 'ブランチ {branch}'
    ,'json.git.sync': '↑{ahead} ↓{behind}'
    ,'json.git.remote': 'リモートリポジトリ'
    ,'json.git.remotePlaceholder': 'https://... または git@host:repo.git'
    ,'json.git.saveRemote': 'リモート URL を保存'
    ,'json.git.removeRemote': 'リモートを削除'
    ,'json.git.changes': '未コミットの変更'
    ,'json.git.history': 'コミット履歴'
    ,'json.git.emptyChanges': '作業ツリーはクリーンです'
    ,'json.git.emptyHistory': 'コミットはありません'
    ,'json.git.diff': '変更詳細'
    ,'json.git.noDiff': '変更またはコミットを選択して Diff を表示'
    ,'json.git.commitMessage': 'コミットメッセージ'
    ,'json.git.defaultMessage': 'MooTool JSON checkpoint'
    ,'json.git.commit': 'すべての変更をコミット'
    ,'json.git.conflict': '競合'
    ,'json.git.done': 'Git 操作が完了しました'
    ,'json.git.discard': '変更を破棄'
    ,'json.git.confirmDiscard': '「{path}」のローカル変更を破棄しますか？この操作は元に戻せません。'
    ,'json.git.abortMerge': 'マージ / リベースを中止'
    ,'json.git.confirmAbort': '現在のマージまたはリベースを中止し、操作前の状態に戻しますか？'
    ,'json.git.continueOperation': 'マージ / リベースを続行'
    ,'json.git.useOurs': 'ローカル版を使用'
    ,'json.git.useTheirs': 'リモート版を使用'
    ,'time.title': '時刻変換'
    ,'time.current': '現在時刻'
    ,'time.timestamp': 'Unix タイムスタンプ'
    ,'time.localTime': 'ローカル時刻'
    ,'time.timezone': 'タイムゾーン'
    ,'time.unit.second': '秒 (s)'
    ,'time.unit.millisecond': 'ミリ秒 (ms)'
    ,'time.toLocal': 'ローカル時刻に変換'
    ,'time.toTimestamp': 'タイムスタンプに変換'
    ,'time.copy': 'コピー'
    ,'time.clock': '全画面時計'
    ,'time.history': '履歴'
    ,'time.formatHint': '形式: yyyy-MM-dd HH:mm:ss'
    ,'time.error.timestamp': '有効な Unix タイムスタンプを入力してください'
    ,'time.error.localTime': 'yyyy-MM-dd HH:mm:ss 形式の時刻を入力してください'
    ,'time.notice.toLocal': '{zone} の時刻に変換しました'
    ,'time.notice.toTimestamp': 'Unix タイムスタンプに変換しました'
    ,'time.notice.copied': 'コピーしました'
    ,'time.clock.close': '全画面時計を閉じる'
    ,'common.action.copy': 'コピー'
    ,'common.action.paste': '貼り付け'
    ,'common.action.clear': 'クリア'
    ,'common.action.history': '履歴'
    ,'common.action.swap': '入れ替え'
    ,'common.action.apply': '適用'
    ,'common.action.save': '保存'
    ,'common.action.delete': '削除'
    ,'common.input': '入力'
    ,'common.output': '出力'
    ,'common.result': '結果'
    ,'common.name': '名前'
    ,'common.description': '説明'
    ,'common.yes': 'はい'
    ,'common.no': 'いいえ'
    ,'common.error.process': '処理に失敗しました: {message}'
    ,'favorite.title': 'お気に入り'
    ,'favorite.add': '現在の内容をお気に入りに追加'
    ,'favorite.empty': 'お気に入りはありません'
    ,'favorite.namePlaceholder': 'お気に入り名'
    ,'favorite.saved': 'お気に入りを保存しました'
    ,'favorite.deleted': 'お気に入りを削除しました'
    ,'encode.title': 'エンコード / デコード'
    ,'encode.tab.unicode': 'Native / Unicode'
    ,'encode.tab.url': 'URL 変換'
    ,'encode.tab.hex': 'Native / 16進数'
    ,'encode.tab.ascii': 'Native / ASCII'
    ,'encode.native': 'Native テキスト'
    ,'encode.unicode': 'Unicode テキスト'
    ,'encode.url': 'URL 元テキスト'
    ,'encode.encoded': 'エンコード結果'
    ,'encode.hex': '16進数'
    ,'encode.ascii': 'ASCII'
    ,'encode.toUnicode': 'Unicode へ'
    ,'encode.fromUnicode': 'Native へ'
    ,'encode.urlEncode': 'URL エンコード'
    ,'encode.urlDecode': 'URL デコード'
    ,'encode.toHex': 'Hex へ'
    ,'encode.fromHex': 'テキストへ'
    ,'encode.toAscii': 'ASCII へ'
    ,'encode.fromAscii': 'テキストへ'
    ,'encode.charset': '文字コード'
    ,'encode.asciiDecimal': '10進数'
    ,'encode.asciiHex': '16進数'
    ,'ua.title': 'UA 解析'
    ,'ua.input': 'User-Agent 入力'
    ,'ua.preset': 'プリセット UA を選択…'
    ,'ua.parse': '解析'
    ,'ua.browser': 'ブラウザー'
    ,'ua.browserVersion': 'ブラウザー版'
    ,'ua.engine': 'レンダリングエンジン'
    ,'ua.engineVersion': 'エンジン版'
    ,'ua.os': 'OS'
    ,'ua.osVersion': 'OS バージョン'
    ,'ua.deviceType': 'デバイス種別'
    ,'ua.deviceBrand': 'デバイスブランド'
    ,'ua.deviceModel': 'デバイスモデル'
    ,'ua.mobile': 'モバイル'
    ,'ua.bot': 'クローラー / Bot'
    ,'ua.empty': 'User-Agent を入力してください'
    ,'ua.unknown': '不明'
    ,'calculator.title': '計算機'
    ,'calculator.expression': '式'
    ,'calculator.calculate': '計算'
    ,'calculator.base': '進数変換'
    ,'calculator.decimal': '10進数'
    ,'calculator.hex': '16進数'
    ,'calculator.binary': '2進数'
    ,'calculator.number': '数値演算'
    ,'calculator.first': '値 1'
    ,'calculator.second': '値 2'
    ,'calculator.gcd': '最大公約数'
    ,'calculator.lcm': '最小公倍数'
    ,'calculator.n': 'n'
    ,'calculator.m': 'm'
    ,'calculator.permutation': '順列 A(n,m)'
    ,'calculator.combination': '組合せ C(n,m)'
    ,'calculator.history': '計算履歴'
    ,'regex.title': '正規表現'
    ,'regex.tab.test': 'マッチテスト'
    ,'regex.tab.common': 'よく使う正規表現'
    ,'regex.expression': '正規表現'
    ,'regex.source': 'テストテキスト'
    ,'regex.matches': '{count} 件一致'
    ,'regex.noMatches': '一致なし'
    ,'regex.flag.global': 'グローバル'
    ,'regex.flag.ignoreCase': '大文字小文字を無視'
    ,'regex.flag.multiline': '複数行'
    ,'regex.flag.dotAll': 'ドットで改行に一致'
    ,'regex.invalid': '無効な正規表現: {message}'
    ,'regex.common.phone': '中国本土の携帯番号'
    ,'regex.common.email': 'メールアドレス'
    ,'regex.common.domain': 'ドメイン名'
    ,'regex.common.chinese': '漢字'
    ,'regex.common.integer': '整数'
    ,'regex.common.color': 'カラーコード'
    ,'regex.common.ipv4': 'IPv4 アドレス'
    ,'regex.common.account': 'アカウント名'
    ,'regex.common.htmlId': 'HTML id 属性'
    ,'regex.common.jpg': 'JPG 画像 URL'
    ,'regex.common.magnet': 'マグネットリンク'
    ,'regex.common.alnum': '英字と数字'
    ,'regex.common.len3to20': '3〜20 文字'
    ,'regex.common.letters26': '英字'
    ,'regex.common.wordUnderscore': '英数字またはアンダースコア'
    ,'regex.common.cnEnNum': '中国語、英数字、アンダースコア'
    ,'regex.common.noSpecial': '特殊文字を含まない'
    ,'regex.common.positiveInt': '正の整数'
    ,'regex.common.negativeInt': '負の整数'
    ,'regex.common.nonNegativeInt': '非負整数'
    ,'regex.common.float': '浮動小数点数'
    ,'cron.title': 'Cron 式'
    ,'cron.expression': 'Cron 式'
    ,'cron.builder': '式ビルダー'
    ,'cron.second': '秒'
    ,'cron.minute': '分'
    ,'cron.hour': '時'
    ,'cron.day': '日'
    ,'cron.month': '月'
    ,'cron.week': '曜日'
    ,'cron.year': '年（任意）'
    ,'cron.parse': '実行時刻を解析'
    ,'cron.nextRuns': '次の 10 回'
    ,'cron.humanReadable': '自然言語'
    ,'cron.preset': 'よく使う式'
    ,'cron.everyMinute': '毎分'
    ,'cron.everyHour': '毎時'
    ,'cron.everyDay': '毎日午前0時'
    ,'cron.weekdays': '平日午前9時'
    ,'cron.invalid': 'Cron を解析できません: {message}'
    ,'diff.title': 'テキスト比較'
    ,'diff.sideBySide': '並列表示'
    ,'diff.unified': 'Unified 表示'
    ,'diff.left': '元のテキスト'
    ,'diff.right': '新しいテキスト'
    ,'diff.compare': '比較'
    ,'diff.ignoreWhitespace': '空白差分を無視'
    ,'diff.realtime': 'リアルタイム比較'
    ,'diff.copy': '差分をコピー'
    ,'diff.previous': '前の差分'
    ,'diff.next': '次の差分'
    ,'diff.summary': '追加 {added} · 削除 {removed} · 変更 {changed}'
    ,'diff.identical': '両方のテキストは同一です'
    ,'diff.highlightMode': 'ハイライトモード'
    ,'diff.highlightBoth': '二重ハイライト'
    ,'diff.highlightCharacters': '文字のみ'
    ,'diff.highlightLines': '行のみ'
    ,'diff.displayMode': '表示モード'
    ,'diff.unifiedPanel': '統一差分形式'
    ,'diff.status.ready': '準備完了'
    ,'diff.status.enterText': '比較するテキストを入力してください'
    ,'diff.status.characterComplete': '文字差分 {count} 件'
    ,'diff.status.complete': '比較完了、{count} 件の差分があります'
    ,'diff.status.cleared': 'クリアしました'
    ,'diff.status.swapped': 'テキストを入れ替えました'
    ,'diff.status.copied': '差分結果をクリップボードにコピーしました'
    ,'diff.status.noCopy': 'コピーできる差分結果がありません'
    ,'diff.status.navigation': '差分 {current}/{total} へ移動'
    ,'config.title': '設定ファイル変換'
    ,'config.tab.convert': 'YAML / Properties'
    ,'config.tab.validate': 'YAML 検証'
    ,'config.properties': 'Properties'
    ,'config.yaml': 'YAML'
    ,'config.toYaml': 'YAML へ'
    ,'config.toProperties': 'Properties へ'
    ,'config.validate': '検証'
    ,'config.format': '整形'
    ,'config.valid': 'YAML は有効です'
    ,'config.invalid': '無効な YAML: {message}'
    ,'common.action.format': '整形'
    ,'common.export': 'エクスポート'
    ,'common.help': 'ヘルプ'
    ,'common.import': 'インポート'
    ,'common.processing': '処理中…'
    ,'common.rename': '名前を変更'
    ,'common.save': '保存'
    ,'common.saved': '保存しました'
    ,'reformat.title': 'フォーマット'
    ,'reformat.tab.text': 'テキスト'
    ,'reformat.tab.file': 'ファイル'
    ,'reformat.type': '種類'
    ,'reformat.indent': 'インデント'
    ,'reformat.input': '整形する内容'
    ,'reformat.chooseFile': 'ファイルを選択'
    ,'reformat.noFile': 'ファイルが選択されていません'
    ,'reformat.original': '元の内容'
    ,'reformat.result': '整形結果'
    ,'reformat.formatted': '整形が完了しました'
    ,'reformat.historySummary': '{type} フォーマット'
    ,'crypto.title': '暗号化 / ランダム'
    ,'crypto.tab.symmetric': '共通鍵暗号'
    ,'crypto.tab.asymmetric': '公開鍵暗号'
    ,'crypto.tab.digest': 'ダイジェスト'
    ,'crypto.tab.base': 'Base64 / Base32'
    ,'crypto.tab.random': 'ランダム生成'
    ,'crypto.algorithm': 'アルゴリズム'
    ,'crypto.key': 'キー'
    ,'crypto.keyHint': 'アルゴリズムに合う長さのキーを入力してください'
    ,'crypto.plainText': '平文'
    ,'crypto.cipherText': '暗号文'
    ,'crypto.cipherOrSignature': '暗号文 / 署名'
    ,'crypto.encrypt': '暗号化'
    ,'crypto.decrypt': '復号'
    ,'crypto.generateKeyPair': 'キーペアを生成'
    ,'crypto.keyGenerated': 'キーペアを生成しました'
    ,'crypto.publicKey': '公開鍵'
    ,'crypto.privateKey': '秘密鍵'
    ,'crypto.publicEncrypt': '公開鍵で暗号化'
    ,'crypto.privateDecrypt': '秘密鍵で復号'
    ,'crypto.privateEncrypt': '秘密鍵で暗号化'
    ,'crypto.publicDecrypt': '公開鍵で復号'
    ,'crypto.rsaOnly': 'この操作は RSA のみ対応しています'
    ,'crypto.sign': '署名'
    ,'crypto.verify': '検証'
    ,'crypto.verified': '署名を検証しました'
    ,'crypto.notVerified': '署名の検証に失敗しました'
    ,'crypto.digest': 'ダイジェスト計算'
    ,'crypto.fileDigest': 'ファイルダイジェスト'
    ,'crypto.textDigest': 'テキストダイジェスト'
    ,'crypto.digestInput': '入力'
    ,'crypto.digestResult': 'ダイジェスト結果'
    ,'crypto.encode': 'エンコード'
    ,'crypto.decode': 'デコード'
    ,'crypto.length': '長さ'
    ,'crypto.generate': '生成'
    ,'crypto.random.uuid': 'UUID'
    ,'crypto.random.digits': 'ランダム数字'
    ,'crypto.random.string': 'ランダム文字列'
    ,'crypto.random.password': 'ランダムパスワード'
    ,'color.title': 'カラーボード'
    ,'color.select': '色を選択'
    ,'color.inputColor': '色を入力'
    ,'color.picker': '画面から採色'
    ,'color.freePick': 'カラーピッカー'
    ,'color.eyeDropperUnavailable': 'このシステムでは画面からの採色を利用できません'
    ,'color.format': '形式'
    ,'color.code': 'カラーコード'
    ,'color.current': '現在の色'
    ,'color.compare': '比較色'
    ,'color.operation.invert': '反転'
    ,'color.operation.intersect': '交差'
    ,'color.operation.add': '加算'
    ,'color.operation.difference': '差分'
    ,'color.operation.average': '平均'
    ,'color.operation.swap': '入れ替え'
    ,'color.themeColors': 'テーマカラー'
    ,'color.theme': 'テーマ'
    ,'color.theme.default': 'デフォルト'
    ,'color.theme.theme1': 'テーマ 1'
    ,'color.theme.theme2': 'テーマ 2'
    ,'color.theme.theme3': 'テーマ 3'
    ,'color.theme.theme4': 'テーマ 4'
    ,'color.theme.theme5': 'テーマ 5'
    ,'color.theme.china': '中国伝統色'
    ,'color.standardColors': '標準色'
    ,'color.shiftHint': 'Shift を押しながら比較色を選択します。'
    ,'qrcode.title': 'QR コード'
    ,'qrcode.tab.generate': '生成'
    ,'qrcode.tab.recognize': '認識'
    ,'qrcode.tab.history': '履歴'
    ,'qrcode.content': 'QR コードの内容'
    ,'qrcode.size': 'サイズ'
    ,'qrcode.correction': '誤り訂正レベル'
    ,'qrcode.logo': '中央ロゴ'
    ,'qrcode.chooseLogo': 'ロゴを選択'
    ,'qrcode.level.L': 'L · 約 7%'
    ,'qrcode.level.M': 'M · 約 15%'
    ,'qrcode.level.Q': 'Q · 約 25%'
    ,'qrcode.level.H': 'H · 約 30%'
    ,'qrcode.generate': 'QR コードを生成'
    ,'qrcode.generated': 'QR コードを生成しました'
    ,'qrcode.preview': 'QR プレビュー'
    ,'qrcode.chooseImage': 'QR 画像を選択'
    ,'qrcode.fromClipboard': 'クリップボードから読込'
    ,'qrcode.clipboard': 'クリップボード画像'
    ,'qrcode.clipboardEmpty': 'クリップボードに画像がありません'
    ,'qrcode.recognize': 'QR コードを認識'
    ,'qrcode.recognized': 'QR コードを認識しました'
    ,'qrcode.sourceImage': '認識する画像'
    ,'qrcode.result': '認識結果'
    ,'qrcode.history.generate': 'QR コード生成'
    ,'qrcode.history.recognize': 'QR コード認識'
    ,'protobuf.title': 'Protobuf'
    ,'protobuf.tab.json': 'JSON / Binary'
    ,'protobuf.tab.wire': 'Wire 解析'
    ,'protobuf.tab.convert': 'Hex / Base64'
    ,'protobuf.definition': 'Proto 定義'
    ,'protobuf.message': 'メッセージ型'
    ,'protobuf.binaryFormat': 'バイナリ形式'
    ,'protobuf.toBinary': 'Binary へ'
    ,'protobuf.toJson': 'JSON へ'
    ,'protobuf.binary': 'バイナリ内容'
    ,'protobuf.wireInput': 'Wire データ'
    ,'protobuf.decode': '解析'
    ,'protobuf.wireOutput': 'Wire フィールド'
    ,'protobuf.hexToBase64': 'Hex から Base64'
    ,'protobuf.base64ToHex': 'Base64 から Hex'
    ,'protobuf.history.jsonToBinary': 'JSON から Binary'
    ,'protobuf.history.binaryToJson': 'Binary から JSON'
    ,'protobuf.history.format': 'Proto を整形'
    ,'protobuf.history.wire': 'Wire を解析'
    ,'protobuf.history.hexToBase64': 'Hex から Base64'
    ,'protobuf.history.base64ToHex': 'Base64 から Hex'
    ,'pdf.title': 'PDF'
    ,'pdf.tab.split': 'PDF 分割'
    ,'pdf.tab.merge': 'PDF 結合'
    ,'pdf.addTask': '分割タスクを追加'
    ,'pdf.addFile': 'PDF を追加'
    ,'pdf.startSplit': '分割開始'
    ,'pdf.startMerge': '結合開始'
    ,'pdf.selectTask': '分割タスクを選択してください'
    ,'pdf.selectTwo': 'PDF を 2 件以上選択してください'
    ,'pdf.confirmSplit': '選択した PDF を分割しますか？'
    ,'pdf.splitComplete': '分割完了、{count} ページを出力しました'
    ,'pdf.mergeComplete': '結合完了、全 {count} ページ'
    ,'pdf.fileName': 'ファイル名'
    ,'pdf.pageRange': '対象ページ'
    ,'pdf.mergeRange': '結合ページ'
    ,'pdf.rule': '分割ルール'
    ,'pdf.customRule': '出力ページ'
    ,'pdf.progress': '状態'
    ,'pdf.output': '最近の出力'
    ,'pdf.empty': 'PDF タスクがありません'
    ,'pdf.pages': 'ページ'
    ,'pdf.select': '選択'
    ,'pdf.rule.odd': '奇数ページ'
    ,'pdf.rule.even': '偶数ページ'
    ,'pdf.rule.custom': 'カスタム'
    ,'pdf.status.ready': '待機中'
    ,'pdf.status.running': '処理中'
    ,'pdf.status.done': '完了'
    ,'pdf.status.error': '失敗'
    ,'pdf.helpSplitTitle': 'PDF 分割ヘルプ'
    ,'pdf.helpMergeTitle': 'PDF 結合ヘルプ'
    ,'pdf.help.split1': '一度に最大 20 件の PDF 分割タスクを追加できます。'
    ,'pdf.help.split2': 'ページ範囲は 1-5、8、10-12 のように指定できます。'
    ,'pdf.help.split3': '奇数、偶数、またはカスタム出力ページを選択できます。'
    ,'pdf.help.split4': '出力ファイルには既定で _split.pdf が付きます。'
    ,'pdf.help.merge1': 'PDF を 2 件以上選択して結合します。'
    ,'pdf.help.merge2': '一覧順とページ範囲に従って結合します。'
    ,'pdf.help.merge3': '現在は暗号化されていない PDF のみ対応します。'
    ,'image.title': '画像アシスタント'
    ,'image.screenshot': 'スクリーンショット'
    ,'image.fromClipboard': 'クリップボード'
    ,'image.import': '画像をインポート'
    ,'image.fromBase64': 'Base64 からインポート'
    ,'image.compress': '圧縮'
    ,'image.watermark': '透かし'
    ,'image.copy': '画像をコピー'
    ,'image.toBase64': 'Base64 へ'
    ,'image.toggleList': '画像一覧の表示切替'
    ,'image.library': '画像ライブラリ'
    ,'image.empty': '画像ライブラリは空です'
    ,'image.select': '画像を選択'
    ,'image.emptyPreview': '画像を選択またはインポートしてください'
    ,'image.zoomIn': '拡大'
    ,'image.zoomOut': '縮小'
    ,'image.original': '元のサイズ'
    ,'image.fit': 'ウィンドウに合わせる'
    ,'image.base64Import': 'Base64 画像をインポート'
    ,'image.base64Export': '画像 Base64'
    ,'image.compressTitle': '画像を圧縮'
    ,'image.watermarkTitle': '透かしを追加'
    ,'image.startProcess': '処理開始'
    ,'image.selectedCount': '{count} 枚の画像を選択中'
    ,'image.quality': '品質'
    ,'image.scale': 'サイズ比率'
    ,'image.outputFormat': '出力形式'
    ,'image.format.auto': '元の形式を維持'
    ,'image.watermarkText': '透かし文字'
    ,'image.opacity': '透明度'
    ,'image.position': '位置'
    ,'image.fontSize': '文字サイズ'
    ,'image.color': '色'
    ,'image.diagonal': '透かし文字を傾ける'
    ,'image.outputMode': '出力方法'
    ,'image.keepOriginal': '元画像を残して新規作成'
    ,'image.overwrite': '元画像を上書き'
    ,'image.position.bottom-right': '右下'
    ,'image.position.bottom-left': '左下'
    ,'image.position.top-right': '右上'
    ,'image.position.top-left': '左上'
    ,'image.position.center': '中央'
    ,'image.position.tile': 'タイル'
    ,'image.font.auto': '自動'
    ,'image.font.small': '小'
    ,'image.font.medium': '中'
    ,'image.font.large': '大'
    ,'image.crop.x': 'X'
    ,'image.crop.y': 'Y'
    ,'image.crop.width': '幅'
    ,'image.crop.height': '高さ'
    ,'image.captureOverlayHint': 'ドラッグして撮影範囲を選択'
    ,'image.captureOverlayKeys': 'Enter で確定 · Esc または右クリックでキャンセル'
    ,'image.captureConfirm': 'スクリーンショットを確定'
    ,'image.saveCapture': 'スクリーンショットを保存'
    ,'image.clipboardEmpty': 'クリップボードに画像がありません'
    ,'image.imported': '画像をインポートしました'
    ,'image.captureUnavailable': 'キャプチャ可能な画面がありません'
    ,'image.saveName': '保存名'
    ,'image.renamePrompt': '新しい画像名を入力'
    ,'image.confirmDelete': '選択した {count} 枚の画像を削除しますか？'
    ,'image.exported': '{directory} にエクスポートしました'
    ,'image.processComplete': '{count} 枚の画像を処理しました'
    ,'common.new': '新規'
    ,'common.add': '追加'
    ,'common.stop': '停止'
    ,'common.refresh': '更新'
    ,'common.search': '検索'
    ,'common.convert': '変換'
    ,'http.title': 'HTTP リクエスト'
    ,'http.untitled': '無題のリクエスト'
    ,'http.urlRequired': 'リクエスト URL を入力してください'
    ,'http.saveName': 'リクエスト名'
    ,'http.confirmDelete': 'このリクエストを削除しますか？'
    ,'http.curlPrompt': 'cURL コマンドを貼り付け'
    ,'http.savedEmpty': '保存済みリクエストはありません'
    ,'http.noUrl': 'URL 未設定'
    ,'http.importCurl': 'cURL をインポート'
    ,'http.copyCurl': 'cURL としてコピー'
    ,'http.method': 'メソッド'
    ,'http.send': '送信'
    ,'http.tab.params': 'Params'
    ,'http.tab.headers': 'Headers'
    ,'http.tab.cookies': 'Cookies'
    ,'http.tab.body': 'Body'
    ,'http.bodyType': 'Body タイプ'
    ,'http.response.body': 'レスポンス Body'
    ,'http.response.headers': 'レスポンス Headers'
    ,'http.response.cookies': 'レスポンス Cookies'
    ,'http.responseEmpty': 'リクエストを送信するとレスポンスが表示されます'
    ,'http.name': 'Name'
    ,'http.value': 'Value'
    ,'http.domain': 'Domain'
    ,'http.path': 'Path'
    ,'http.expires': 'Expires'
    ,'http.enabled': '有効'
    ,'http.addEntry': '項目を追加'
    ,'http.history': 'HTTP リクエスト履歴'
    ,'http.error.ABORTED': 'リクエストをキャンセルしました'
    ,'http.error.TIMEOUT': 'リクエストがタイムアウトしました'
    ,'http.error.NETWORK': 'ネットワークリクエストに失敗しました'
    ,'http.error.INVALID_REQUEST': 'リクエストが無効です'
    ,'http.error.RESPONSE_TOO_LARGE': 'レスポンスが 10 MB を超えています'
    ,'translation.title': '翻訳'
    ,'translation.tab.translate': '翻訳'
    ,'translation.tab.words': '単語帳'
    ,'translation.tab.history': '履歴'
    ,'translation.exchange': '言語とテキストを交換'
    ,'translation.provider': '翻訳サービス'
    ,'translation.copy': '翻訳をコピー'
    ,'translation.saveWord': '単語帳に保存'
    ,'translation.savedWord': '単語帳に保存しました'
    ,'translation.sourcePlaceholder': '翻訳するテキストを入力'
    ,'translation.targetPlaceholder': '翻訳結果'
    ,'translation.translating': '翻訳中…'
    ,'translation.fallback': '代替サービスを使用'
    ,'translation.sourceLanguage': '翻訳元言語'
    ,'translation.targetLanguage': '翻訳先言語'
    ,'translation.apply': '翻訳に適用'
    ,'translation.retranslate': '再翻訳'
    ,'translation.confirmDeleteWord': 'この単語を削除しますか？'
    ,'translation.searchWords': '単語、翻訳、メモを検索'
    ,'translation.remark': 'メモ（任意）'
    ,'translation.wordEmpty': '単語を選択または作成してください'
    ,'translation.searchHistory': '原文、翻訳、言語を検索'
    ,'translation.lang.auto': '自動検出'
    ,'translation.lang.zh-CN': '中国語（簡体字）'
    ,'translation.lang.en': '英語'
    ,'translation.lang.yue': '広東語'
    ,'translation.lang.wyw': '漢文'
    ,'translation.lang.jp': '日本語'
    ,'translation.lang.kor': '韓国語'
    ,'translation.lang.fra': 'フランス語'
    ,'translation.lang.spa': 'スペイン語'
    ,'translation.lang.th': 'タイ語'
    ,'translation.lang.ara': 'アラビア語'
    ,'translation.lang.ru': 'ロシア語'
    ,'translation.lang.pt': 'ポルトガル語'
    ,'translation.lang.de': 'ドイツ語'
    ,'translation.lang.it': 'イタリア語'
    ,'translation.lang.el': 'ギリシャ語'
    ,'translation.lang.nl': 'オランダ語'
    ,'translation.lang.pl': 'ポーランド語'
    ,'translation.lang.bul': 'ブルガリア語'
    ,'translation.lang.est': 'エストニア語'
    ,'translation.lang.dan': 'デンマーク語'
    ,'translation.lang.fin': 'フィンランド語'
    ,'translation.lang.cs': 'チェコ語'
    ,'translation.lang.rom': 'ルーマニア語'
    ,'translation.lang.slo': 'スロベニア語'
    ,'translation.lang.swe': 'スウェーデン語'
    ,'translation.lang.hu': 'ハンガリー語'
    ,'translation.lang.cht': '中国語（繁体字）'
    ,'translation.lang.vie': 'ベトナム語'
    ,'host.title': 'Host'
    ,'host.discardChanges': '未保存の変更を破棄しますか？'
    ,'host.untitled': '無題の Host'
    ,'host.namePrompt': 'Host プロファイル名'
    ,'host.confirmDelete': 'この Host プロファイルを削除しますか？'
    ,'host.confirmApply': 'システムの hosts ファイルに書き込みますか？管理者権限が要求される場合があります。'
    ,'host.applied': 'システム hosts を更新しました'
    ,'host.applying': '適用中…'
    ,'host.apply': 'Host を切り替え'
    ,'host.empty': 'Host プロファイルはありません'
    ,'host.import': 'Host をインポート'
    ,'host.export': 'Host をエクスポート'
    ,'host.profileName': 'プロファイル名'
    ,'host.current': '現在のシステム hosts'
    ,'host.find': '検索と置換'
    ,'host.findPlaceholder': '検索'
    ,'host.replacePlaceholder': '置換文字列'
    ,'host.replace': '置換'
    ,'host.replaceAll': 'すべて置換'
    ,'host.content': 'Host 内容'
    ,'host.placeholder': 'hosts ファイルの内容を入力'
    ,'host.writable': '現在のユーザーで書き込み可能'
    ,'host.requiresPrivilege': '書き込みには管理者権限が必要です'
    ,'net.title': 'ネットワーク/IP'
    ,'net.running': '実行中…'
    ,'net.noOutput': 'コマンドは出力なしで完了しました'
    ,'net.outputPlaceholder': 'ネットワークコマンドの結果がここに表示されます'
    ,'net.ipv4Long': 'IPv4 と Long の変換'
    ,'net.ping': 'PING'
    ,'net.resolve': 'ホスト名から IP を取得'
    ,'net.resolveAction': '解決'
    ,'net.whois': 'WHOIS 検索'
    ,'net.query': '検索'
    ,'net.dns': 'DNS'
    ,'net.flushDns': 'DNS キャッシュを消去'
    ,'net.localAddresses': 'ローカル IP アドレス'
    ,'net.error.ABORTED': 'コマンドを停止しました'
    ,'net.error.TIMEOUT': 'コマンドがタイムアウトしました'
    ,'net.error.PERMISSION': '権限が拒否されました'
    ,'net.error.UNSUPPORTED': '現在のシステムでは使用できません'
    ,'net.error.COMMAND_FAILED': 'コマンドに失敗しました'
    ,'net.error.INVALID_TARGET': '対象が無効です'
    ,'variables.title': '環境変数'
    ,'variables.tab.environment': 'システム環境変数'
    ,'variables.tab.runtime': 'Electron ランタイムプロパティ'
    ,'variables.key': 'キー'
    ,'variables.value': '値'
    ,'variables.count': '{count} / {total} 件を表示'
    ,'runtime.title': 'コード実行'
    ,'runtime.tab.java': 'Java / Groovy'
    ,'runtime.tab.python': 'Python'
    ,'runtime.tab.node': 'Node.js'
    ,'runtime.mode.java': 'Java'
    ,'runtime.mode.groovy': 'Groovy'
    ,'runtime.detect': 'ランタイムを検出'
    ,'runtime.detected': '{count} 個のランタイムを検出しました'
    ,'runtime.missing': '{name} が見つかりません'
    ,'runtime.format': '整形'
    ,'runtime.run': '実行'
    ,'runtime.stop': '停止'
    ,'runtime.clear': '出力をクリア'
    ,'runtime.history': '実行履歴'
    ,'runtime.editor': 'コードエディター'
    ,'runtime.output': 'コンソール出力'
    ,'runtime.ready': 'コードを実行すると結果が表示されます'
    ,'runtime.running': '{name} を実行中…'
    ,'runtime.completed': '実行が完了しました'
    ,'runtime.failed': '実行に失敗しました'
    ,'runtime.cancelled': '実行を停止しました'
    ,'runtime.timeout': '実行がタイムアウトしました'
    ,'runtime.truncated': '出力が 2 MB を超えたため停止しました'
    ,'runtime.command': 'コマンド'
    ,'runtime.exitCode': '終了コード {code}'
    ,'runtime.duration': '{duration} ms'
    ,'runtime.configure': '設定で {name} ランタイムのパスを指定してください'
    ,'runtime.openSettings': 'ランタイム設定を開く'
    ,'runtime.options': '実行オプション'
    ,'runtime.arguments': 'プログラム引数'
    ,'runtime.argumentsPlaceholder': '--name "Moo Tool"'
    ,'runtime.workingDirectory': '作業ディレクトリ'
    ,'runtime.defaultWorkingDirectory': '分離された一時ディレクトリを使用'
    ,'quickNote.title': 'クイックノート'
    ,'quickNote.search': 'ノートを検索…'
    ,'quickNote.searchContent': '本文を検索'
    ,'quickNote.sort': '並び順'
    ,'quickNote.sort.name': '名前'
    ,'quickNote.sort.modified': '更新日時'
    ,'quickNote.sort.created': '作成日時'
    ,'quickNote.empty': '一致するノートはありません'
    ,'quickNote.newNote': '新規ノート'
    ,'quickNote.newFolder': '新規フォルダー'
    ,'quickNote.expandAll': 'すべて展開'
    ,'quickNote.collapseAll': 'すべて折りたたむ'
    ,'quickNote.rename': '名前を変更'
    ,'quickNote.move': '移動'
    ,'quickNote.duplicate': '複製'
    ,'quickNote.delete': '削除'
    ,'quickNote.openVault': 'ファイルマネージャーで Vault を開く'
    ,'quickNote.save': '保存'
    ,'quickNote.saved': 'ノートを保存しました'
    ,'quickNote.unsaved': '未保存の変更があります'
    ,'quickNote.find': '検索と置換'
    ,'quickNote.findPlaceholder': '検索'
    ,'quickNote.replacePlaceholder': '置換文字列'
    ,'quickNote.nextMatch': '次の一致'
    ,'quickNote.replace': '置換'
    ,'quickNote.replaceAll': 'すべて置換'
    ,'quickNote.noMatches': '一致する内容がありません'
    ,'quickNote.attachment': '画像を添付（クリップボードから貼り付け可能）'
    ,'quickNote.clipboardEmpty': 'クリップボードに利用可能な画像がありません'
    ,'quickNote.export': '現在のノートをエクスポート'
    ,'quickNote.info': '文書情報'
    ,'quickNote.git': 'Git 同期'
    ,'quickNote.quickReplace': 'クイック置換'
    ,'quickNote.view.editor': '編集'
    ,'quickNote.view.split': '分割'
    ,'quickNote.view.preview': 'プレビュー'
    ,'quickNote.syntax': '構文'
    ,'quickNote.font': 'フォント'
    ,'quickNote.font.system': 'システムフォント'
    ,'quickNote.font.mono': '等幅フォント'
    ,'quickNote.font.serif': 'セリフ体'
    ,'quickNote.fontSize': '文字サイズ'
    ,'quickNote.wrap': '自動折り返し'
    ,'quickNote.bulletList': '箇条書き'
    ,'quickNote.numberedList': '番号付きリスト'
    ,'quickNote.color': 'ノートの色'
    ,'quickNote.select': '左側からノートを選択または作成してください'
    ,'quickNote.editorLabel': 'ノート本文'
    ,'quickNote.confirmDelete': '「{name}」を削除しますか？フォルダーは空である必要があります。'
    ,'quickNote.dialog.createNote': '新規ノート'
    ,'quickNote.dialog.createFolder': '新規フォルダー'
    ,'quickNote.dialog.rename': '名前を変更'
    ,'quickNote.dialog.move': 'フォルダーへ移動'
    ,'quickNote.dialog.name': '名前'
    ,'quickNote.dialog.target': '移動先フォルダー'
    ,'quickNote.dialog.root': 'Vault ルート'
    ,'quickNote.create': '作成'
    ,'quickNote.apply': '適用'
    ,'quickNote.path': 'パス'
    ,'quickNote.created': '作成日時'
    ,'quickNote.modified': '更新日時'
    ,'quickNote.lines': '行数'
    ,'quickNote.words': '単語数'
    ,'quickNote.characters': '文字数'
    ,'quickNote.status': '{lines} 行 · {characters} 文字'
    ,'quickNote.preview.empty': 'Markdown 構文を選択すると書式付き内容をプレビューできます。'
    ,'quickNote.quick.trim': '前後の空白を削除'
    ,'quickNote.quick.removeBlankLines': '空行を削除'
    ,'quickNote.quick.removeTabs': 'Tab を削除'
    ,'quickNote.quick.scientificToNormal': '指数表記を通常表記へ'
    ,'quickNote.quick.normalToScientific': '通常表記を指数表記へ'
    ,'quickNote.quick.thousandsToNormal': '桁区切りを削除'
    ,'quickNote.quick.normalToThousands': '桁区切りを追加'
    ,'quickNote.quick.underscoreToCamel': 'スネークケースをキャメルケースへ'
    ,'quickNote.quick.camelToUnderscore': 'キャメルケースをスネークケースへ'
    ,'quickNote.quick.uppercase': '大文字に変換'
    ,'quickNote.quick.lowercase': '小文字に変換'
    ,'quickNote.quick.linesToComma': '改行をカンマへ'
    ,'quickNote.quick.linesToSingleQuoted': '改行を単一引用符リストへ'
    ,'quickNote.quick.linesToDoubleQuoted': '改行を二重引用符リストへ'
    ,'quickNote.quick.commaToLines': 'カンマを改行へ'
    ,'quickNote.quick.tabsToLines': 'Tab を改行へ'
    ,'quickNote.quick.clearNewlines': '改行を削除'
    ,'quickNote.quick.deduplicateLines': '行を重複排除'
    ,'quickNote.quick.deduplicateWithCount': '行を重複排除して集計'
    ,'quickNote.quick.escape': 'テキストをエスケープ'
    ,'quickNote.quick.unescape': 'エスケープを解除'
    ,'quickNote.quick.reverseLines': '行順を反転'
    ,'quickNote.quick.sortAscending': '行を昇順に並べ替え'
    ,'quickNote.quick.sortDescending': '行を降順に並べ替え'
    ,'quickNote.git.title': 'クイックノート Vault Git'
    ,'quickNote.git.defaultMessage': 'MooTool クイックノート チェックポイント'
    ,'hardware.title': 'システム情報'
    ,'hardware.tab.system': 'システム'
    ,'hardware.tab.cpu': 'プロセッサ'
    ,'hardware.tab.memory': 'メモリ'
    ,'hardware.tab.storage': 'ストレージ'
    ,'hardware.tab.network': 'ネットワーク'
    ,'hardware.loading': 'システム情報を収集中…'
    ,'hardware.empty': '表示できる情報がありません'
    ,'hardware.group.operatingSystem': 'オペレーティングシステム'
    ,'hardware.group.processor': 'プロセッサ'
    ,'hardware.group.physicalMemory': '物理メモリ'
    ,'hardware.label.platform': 'プラットフォーム'
    ,'hardware.label.distribution': 'ディストリビューション'
    ,'hardware.label.kernel': 'カーネル'
    ,'hardware.label.architecture': 'アーキテクチャ'
    ,'hardware.label.hostName': 'ホスト名'
    ,'hardware.label.serial': 'シリアル番号'
    ,'hardware.label.manufacturer': 'メーカー'
    ,'hardware.label.model': 'モデル'
    ,'hardware.label.uptime': '稼働時間'
    ,'hardware.label.timeZone': 'タイムゾーン'
    ,'hardware.label.brand': 'ブランド'
    ,'hardware.label.vendor': 'ベンダー'
    ,'hardware.label.family': 'ファミリー'
    ,'hardware.label.physicalCores': '物理コア'
    ,'hardware.label.logicalCores': '論理コア'
    ,'hardware.label.performanceCores': '高性能コア'
    ,'hardware.label.efficiencyCores': '高効率コア'
    ,'hardware.label.baseSpeed': '基本速度'
    ,'hardware.label.maximumSpeed': '最大速度'
    ,'hardware.label.currentLoad': '現在の負荷'
    ,'hardware.label.total': '合計'
    ,'hardware.label.used': '使用済み'
    ,'hardware.label.available': '利用可能'
    ,'hardware.label.active': 'アクティブ'
    ,'hardware.label.usage': '使用率'
    ,'hardware.label.swapTotal': 'スワップ合計'
    ,'hardware.label.swapUsed': 'スワップ使用量'
    ,'hardware.label.device': 'デバイス'
    ,'hardware.label.type': 'タイプ'
    ,'hardware.label.interface': 'インターフェース'
    ,'hardware.label.capacity': '容量'
    ,'hardware.label.filesystem': 'ファイルシステム'
    ,'hardware.label.ipv4': 'IPv4'
    ,'hardware.label.ipv6': 'IPv6'
    ,'hardware.label.mac': 'MAC'
    ,'hardware.label.mtu': 'MTU'
    ,'hardware.label.speed': '速度'
    ,'hardware.label.status': '状態'
    ,'hardware.label.received': '受信'
    ,'hardware.label.sent': '送信'
  }
} as const
