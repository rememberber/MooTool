import {
  Binary,
  Braces,
  Calculator,
  Clock3,
  Code2,
  Diff,
  FileCode2,
  FileImage,
  FileText,
  Globe2,
  HardDrive,
  Image,
  KeyRound,
  Languages,
  MessageSquareText,
  Network,
  NotebookPen,
  Palette,
  PanelsTopLeft,
  QrCode,
  Regex,
  Rocket,
  ScanSearch,
  ServerCog,
  ShieldCheck,
  TextCursorInput,
  Variable,
  type LucideIcon
} from 'lucide-react'

export type ToolId =
  | 'home'
  | 'quick-note'
  | 'text-diff'
  | 'reformat'
  | 'json'
  | 'config'
  | 'runtime'
  | 'protobuf'
  | 'variables'
  | 'http'
  | 'host'
  | 'network'
  | 'ua'
  | 'encode'
  | 'crypto'
  | 'regex'
  | 'cron'
  | 'qrcode'
  | 'timestamp'
  | 'message-board'
  | 'translation'
  | 'calculator'
  | 'color'
  | 'image'
  | 'pdf'
  | 'system'
  | 'editor-lab'
  | 'webview-lab'

export interface ToolDefinition {
  id: ToolId
  title: string
  group: '文本与配置' | '开发工具' | '网络工具' | '编码工具' | '实用工具' | '系统工具'
  icon: LucideIcon
  ready: boolean
  engineeringOnly: boolean
  keywords: string[]
}

export const toolCatalog: ToolDefinition[] = [
  {
    ...tool('quick-note', '随手记', '文本与配置', NotebookPen, ['note', 'markdown']),
    ready: true
  },
  {
    ...tool('text-diff', '文本对比', '文本与配置', Diff, ['diff', 'compare']),
    ready: true
  },
  {
    ...tool('reformat', '格式化', '文本与配置', FileCode2, ['format', 'nginx', 'java', 'xml', 'html']),
    ready: true
  },
  {
    ...tool('json', 'JSON', '文本与配置', Braces, ['json', 'path', 'format', 'minify']),
    ready: true
  },
  {
    ...tool('config', 'YAML / Properties', '文本与配置', FileText, ['yaml', 'properties']),
    ready: true
  },
  { ...tool('runtime', '代码运行', '开发工具', Code2, ['code', 'run']), ready: true },
  {
    ...tool('protobuf', 'Protobuf', '开发工具', Binary, ['proto']),
    ready: true
  },
  {
    ...tool('variables', '环境变量', '开发工具', Variable, ['env']),
    ready: true
  },
  { ...tool('http', 'HTTP', '网络工具', Globe2, ['request', 'api']), ready: true },
  { ...tool('host', 'Host', '网络工具', ServerCog, ['hosts', 'dns']), ready: true },
  {
    ...tool('network', '网络 / IP', '网络工具', Network, ['ip', 'port']),
    ready: true
  },
  {
    ...tool('ua', 'UA 分析', '网络工具', ScanSearch, ['user agent', 'browser']),
    ready: true
  },
  {
    ...tool('encode', '编码解码', '编码工具', Rocket, ['base64', 'unicode', 'url', 'hex', 'ascii']),
    ready: true
  },
  {
    ...tool('crypto', '加解密 / 随机', '编码工具', KeyRound, ['hash', 'aes', 'uuid']),
    ready: true
  },
  {
    ...tool('regex', 'Regex', '编码工具', Regex, ['regexp', '正则']),
    ready: true
  },
  {
    ...tool('cron', 'Cron', '编码工具', Clock3, ['schedule']),
    ready: true
  },
  {
    ...tool('qrcode', '二维码', '编码工具', QrCode, ['qr']),
    ready: true
  },
  {
    ...tool('timestamp', '时间转换', '实用工具', Clock3, ['timestamp', 'date', 'timezone']),
    ready: true
  },
  {
    ...tool('message-board', '留言板', '实用工具', MessageSquareText, ['message']),
    ready: true
  },
  { ...tool('translation', '翻译', '实用工具', Languages, ['translate']), ready: true },
  {
    ...tool('calculator', '计算器', '实用工具', Calculator, ['calc', 'math', '进制']),
    ready: true
  },
  {
    ...tool('color', '调色板', '实用工具', Palette, ['color', 'picker']),
    ready: true
  },
  { ...tool('image', '图片工具', '实用工具', Image, ['capture', 'resize']), ready: true },
  { ...tool('pdf', 'PDF', '实用工具', FileImage, ['pdf', 'merge']), ready: true },
  {
    ...tool('system', '硬件与系统', '系统工具', HardDrive, ['hardware', 'system']),
    ready: true
  },
  {
    ...tool('editor-lab', 'CodeMirror 实验台', '系统工具', TextCursorInput, ['codemirror', 'editor', 'ime', 'p0']),
    ready: true,
    engineeringOnly: true
  },
  {
    ...tool('webview-lab', 'WebView 实验台', '系统工具', PanelsTopLeft, ['webview', 'reparent', 'p0']),
    ready: true,
    engineeringOnly: true
  }
]

export const productToolCatalog = toolCatalog.filter((tool) => !tool.engineeringOnly)
export const navigationToolCatalog = import.meta.env.DEV ? toolCatalog : productToolCatalog
export const toolGroups = [...new Set(productToolCatalog.map((item) => item.group))]

export const homeTool = {
  id: 'home' as const,
  title: 'MooTool',
  icon: ShieldCheck
}

function tool(
  id: Exclude<ToolId, 'home'>,
  title: string,
  group: ToolDefinition['group'],
  icon: LucideIcon,
  keywords: string[]
): ToolDefinition {
  return { id, title, group, icon, keywords, ready: false, engineeringOnly: false }
}
