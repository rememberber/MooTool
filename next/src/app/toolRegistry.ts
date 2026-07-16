import {
  ArrowRightLeft,
  Binary,
  Braces,
  Calculator,
  CalendarClock,
  Clock3,
  CodeXml,
  Columns2,
  Cpu,
  FileCog,
  FileText,
  Globe2,
  Home,
  Image,
  Languages,
  LockKeyhole,
  Network,
  NotebookPen,
  Palette,
  Paintbrush,
  QrCode,
  Regex,
  ServerCog,
  SquareTerminal,
  Variable,
  type LucideIcon
} from 'lucide-react'
import { lazy, type ComponentType, type LazyExoticComponent } from 'react'
import type { MessageKey } from '@/shared/i18n/messages'

export const toolIds = [
  'mootool',
  'quickNote',
  'textDiff',
  'reformat',
  'json',
  'java',
  'ymlProperties',
  'protobuf',
  'variables',
  'http',
  'host',
  'net',
  'uaParse',
  'encode',
  'crypto',
  'regex',
  'cron',
  'qrCode',
  'timeConvert',
  'translation',
  'calculator',
  'colorBoard',
  'image',
  'pdf',
  'hardware'
] as const

export type ToolId = (typeof toolIds)[number]
export type ToolGroupId = 'text' | 'dev' | 'network' | 'encode' | 'daily' | 'system'
export type ToolStatus = 'placeholder' | 'in-progress' | 'parity-review' | 'complete'

export type ToolDefinition = {
  id: ToolId
  groupId: ToolGroupId | 'home'
  titleKey: MessageKey
  keywords: string[]
  icon: LucideIcon
  component?: LazyExoticComponent<ComponentType>
  status: ToolStatus
  supportsHistory: boolean
  supportsFavorites: boolean
  settingsSection?: string
}

export type ToolGroupDefinition = {
  id: ToolGroupId
  titleKey: MessageKey
  toolIds: ToolId[]
}

const HomePage = lazy(() => import('@/features/home/HomePage').then((module) => ({ default: module.HomePage })))
const JsonTool = lazy(() => import('@/features/json/JsonTool').then((module) => ({ default: module.JsonTool })))
const TimeConvertTool = lazy(() => import('@/features/time/TimeConvertTool').then((module) => ({ default: module.TimeConvertTool })))
const EncodeTool = lazy(() => import('@/features/encode/EncodeTool').then((module) => ({ default: module.EncodeTool })))
const UaParseTool = lazy(() => import('@/features/ua/UaParseTool').then((module) => ({ default: module.UaParseTool })))
const CalculatorTool = lazy(() => import('@/features/calculator/CalculatorTool').then((module) => ({ default: module.CalculatorTool })))
const RegexTool = lazy(() => import('@/features/regex/RegexTool').then((module) => ({ default: module.RegexTool })))
const CronTool = lazy(() => import('@/features/cron/CronTool').then((module) => ({ default: module.CronTool })))
const TextDiffTool = lazy(() => import('@/features/diff/TextDiffTool').then((module) => ({ default: module.TextDiffTool })))
const ConfigConvertTool = lazy(() => import('@/features/config/ConfigConvertTool').then((module) => ({ default: module.ConfigConvertTool })))
const ReformatTool = lazy(() => import('@/features/reformat/ReformatTool').then((module) => ({ default: module.ReformatTool })))
const ProtobufTool = lazy(() => import('@/features/protobuf/ProtobufTool').then((module) => ({ default: module.ProtobufTool })))
const CryptoTool = lazy(() => import('@/features/crypto/CryptoTool').then((module) => ({ default: module.CryptoTool })))
const QrCodeTool = lazy(() => import('@/features/qrcode/QrCodeTool').then((module) => ({ default: module.QrCodeTool })))
const ColorBoardTool = lazy(() => import('@/features/color/ColorBoardTool').then((module) => ({ default: module.ColorBoardTool })))
const ImageTool = lazy(() => import('@/features/image/ImageTool').then((module) => ({ default: module.ImageTool })))
const PdfTool = lazy(() => import('@/features/pdf/PdfTool').then((module) => ({ default: module.PdfTool })))
const VariablesTool = lazy(() => import('@/features/variables/VariablesTool').then((module) => ({ default: module.VariablesTool })))
const HttpTool = lazy(() => import('@/features/http/HttpTool').then((module) => ({ default: module.HttpTool })))
const HostTool = lazy(() => import('@/features/host/HostTool').then((module) => ({ default: module.HostTool })))
const NetTool = lazy(() => import('@/features/net/NetTool').then((module) => ({ default: module.NetTool })))
const TranslationTool = lazy(() => import('@/features/translation/TranslationTool').then((module) => ({ default: module.TranslationTool })))
const HardwareTool = lazy(() => import('@/features/hardware/HardwareTool').then((module) => ({ default: module.HardwareTool })))
const QuickNoteTool = lazy(() => import('@/features/quickNote/QuickNoteTool').then((module) => ({ default: module.QuickNoteTool })))
const RuntimeTool = lazy(() => import('@/features/runtime/RuntimeTool').then((module) => ({ default: module.RuntimeTool })))

export const toolRegistry: ToolDefinition[] = [
  tool('mootool', 'home', 'app.nav.home', Home, ['home', 'about', '首页', '主页', '关于'], HomePage, 'in-progress'),
  tool('quickNote', 'text', 'app.nav.quickNote', NotebookPen, ['note', 'memo', 'markdown', '随手记', '笔记', '记事'], QuickNoteTool, 'in-progress'),
  tool('textDiff', 'text', 'app.nav.textDiff', Columns2, ['diff', 'compare', 'text', '对比', '比较'], TextDiffTool, 'parity-review'),
  tool('reformat', 'text', 'app.nav.reformat', Paintbrush, ['format', 'java', 'xml', 'html', 'nginx', '格式化'], ReformatTool, 'parity-review'),
  tool('json', 'dev', 'app.nav.json', Braces, ['json', 'xml', 'javabean', 'jsonpath', '格式化'], JsonTool, 'in-progress'),
  tool('java', 'dev', 'app.nav.java', SquareTerminal, ['java', 'groovy', 'python', 'node', 'console', '运行'], RuntimeTool, 'in-progress'),
  tool('ymlProperties', 'dev', 'app.nav.ymlProperties', FileCog, ['yaml', 'yml', 'properties', 'config', '配置', '转换'], ConfigConvertTool, 'parity-review'),
  tool('protobuf', 'dev', 'app.nav.protobuf', Binary, ['protobuf', 'proto', 'wire', '序列化'], ProtobufTool, 'parity-review'),
  tool('variables', 'dev', 'app.nav.variables', Variable, ['env', 'environment', 'variable', '环境变量'], VariablesTool, 'parity-review'),
  tool('http', 'network', 'app.nav.http', Globe2, ['http', 'curl', 'api', 'request', '请求'], HttpTool, 'parity-review'),
  tool('host', 'network', 'app.nav.host', ServerCog, ['host', 'dns', '域名'], HostTool, 'parity-review'),
  tool('net', 'network', 'app.nav.net', Network, ['network', 'ip', 'ping', 'whois', '网络'], NetTool, 'parity-review'),
  tool('uaParse', 'network', 'app.nav.uaParse', CodeXml, ['ua', 'user-agent', 'browser', '浏览器', '分析'], UaParseTool, 'parity-review'),
  tool('encode', 'encode', 'app.nav.encode', ArrowRightLeft, ['encode', 'decode', 'base64', 'url', '编码', '解码'], EncodeTool, 'parity-review'),
  tool('crypto', 'encode', 'app.nav.crypto', LockKeyhole, ['crypto', 'hash', 'md5', 'sha', 'random', '加密', '随机'], CryptoTool, 'parity-review'),
  tool('regex', 'encode', 'app.nav.regex', Regex, ['regex', 'regexp', 'regular', '正则', '匹配'], RegexTool, 'parity-review'),
  tool('cron', 'encode', 'app.nav.cron', CalendarClock, ['cron', 'schedule', '定时', '表达式'], CronTool, 'parity-review'),
  tool('qrCode', 'encode', 'app.nav.qrCode', QrCode, ['qr', 'qrcode', '二维码', '条码'], QrCodeTool, 'parity-review'),
  tool('timeConvert', 'daily', 'app.nav.timeConvert', Clock3, ['time', 'timestamp', 'clock', '时间', '时区', '时间戳'], TimeConvertTool, 'parity-review'),
  tool('translation', 'daily', 'app.nav.translation', Languages, ['translate', 'translation', 'word', '翻译', '单词'], TranslationTool, 'parity-review'),
  tool('calculator', 'daily', 'app.nav.calculator', Calculator, ['calculator', 'calc', 'math', '计算', '表达式'], CalculatorTool, 'parity-review'),
  tool('colorBoard', 'daily', 'app.nav.colorBoard', Palette, ['color', 'palette', 'hex', 'rgb', '调色', '颜色'], ColorBoardTool, 'parity-review'),
  tool('image', 'daily', 'app.nav.image', Image, ['image', 'watermark', 'compress', '图片', '图像'], ImageTool, 'parity-review'),
  tool('pdf', 'daily', 'app.nav.pdf', FileText, ['pdf', 'merge', 'split', '合并', '拆分'], PdfTool, 'parity-review'),
  tool('hardware', 'system', 'app.nav.hardware', Cpu, ['hardware', 'system', 'cpu', 'memory', '系统', '硬件'], HardwareTool, 'parity-review')
]

export const toolGroups: ToolGroupDefinition[] = [
  group('text', 'app.group.text'),
  group('dev', 'app.group.dev'),
  group('network', 'app.group.network'),
  group('encode', 'app.group.encode'),
  group('daily', 'app.group.daily'),
  group('system', 'app.group.system')
]

export const toolById = new Map(toolRegistry.map((definition) => [definition.id, definition]))

export function isToolId(value: string): value is ToolId {
  return toolById.has(value as ToolId)
}

function tool(
  id: ToolId,
  groupId: ToolDefinition['groupId'],
  titleKey: MessageKey,
  icon: LucideIcon,
  keywords: string[],
  component?: LazyExoticComponent<ComponentType>,
  status: ToolStatus = 'placeholder'
): ToolDefinition {
  return {
    id,
    groupId,
    titleKey,
    keywords,
    icon,
    component,
    status,
    supportsHistory: id !== 'mootool' && id !== 'hardware',
    supportsFavorites: ['regex', 'cron', 'colorBoard'].includes(id)
  }
}

function group(id: ToolGroupId, titleKey: MessageKey): ToolGroupDefinition {
  return {
    id,
    titleKey,
    toolIds: toolRegistry.filter((definition) => definition.groupId === id).map((definition) => definition.id)
  }
}
