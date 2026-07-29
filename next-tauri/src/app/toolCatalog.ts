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
  | 'webview-lab'

export interface ToolDefinition {
  id: ToolId
  title: string
  group: '文本与配置' | '开发工具' | '网络工具' | '编码工具' | '实用工具' | '系统工具'
  icon: LucideIcon
  ready: boolean
  keywords: string[]
}

export const toolCatalog: ToolDefinition[] = [
  tool('quick-note', '随手记', '文本与配置', NotebookPen, ['note', 'markdown']),
  tool('text-diff', '文本对比', '文本与配置', Diff, ['diff', 'compare']),
  tool('reformat', '格式化', '文本与配置', FileCode2, ['format', 'sql', 'xml']),
  tool('json', 'JSON', '文本与配置', Braces, ['json', 'path']),
  tool('config', 'YAML / Properties', '文本与配置', FileText, ['yaml', 'properties']),
  tool('runtime', '代码运行', '开发工具', Code2, ['code', 'run']),
  tool('protobuf', 'Protobuf', '开发工具', Binary, ['proto']),
  tool('variables', '环境变量', '开发工具', Variable, ['env']),
  tool('http', 'HTTP', '网络工具', Globe2, ['request', 'api']),
  tool('host', 'Host', '网络工具', ServerCog, ['hosts', 'dns']),
  tool('network', '网络 / IP', '网络工具', Network, ['ip', 'port']),
  tool('ua', 'UA 分析', '网络工具', ScanSearch, ['user agent', 'browser']),
  tool('encode', '编码解码', '编码工具', Rocket, ['base64', 'unicode']),
  tool('crypto', '加解密 / 随机', '编码工具', KeyRound, ['hash', 'aes', 'uuid']),
  tool('regex', 'Regex', '编码工具', Regex, ['regexp', '正则']),
  tool('cron', 'Cron', '编码工具', Clock3, ['schedule']),
  tool('qrcode', '二维码', '编码工具', QrCode, ['qr']),
  tool('timestamp', '时间转换', '实用工具', Clock3, ['timestamp', 'date']),
  tool('message-board', '留言板', '实用工具', MessageSquareText, ['message']),
  tool('translation', '翻译', '实用工具', Languages, ['translate']),
  {
    ...tool('calculator', '计算器', '实用工具', Calculator, ['calc', 'math', '进制']),
    ready: true
  },
  tool('color', '调色板', '实用工具', Palette, ['color', 'picker']),
  tool('image', '图片工具', '实用工具', Image, ['capture', 'resize']),
  tool('pdf', 'PDF', '实用工具', FileImage, ['pdf', 'merge']),
  tool('system', '硬件与系统', '系统工具', HardDrive, ['hardware', 'system']),
  {
    ...tool('webview-lab', 'WebView 实验台', '系统工具', PanelsTopLeft, ['webview', 'reparent', 'p0']),
    ready: true
  }
]

export const toolGroups = [...new Set(toolCatalog.map((item) => item.group))]

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
  return { id, title, group, icon, keywords, ready: false }
}
