const fail = (key: 'invalidTable' | 'tableHeaders' | 'tableLimit' | 'unsupportedTable' | 'complexHtml'): never => { throw new Error(`app.command.${key}`) }

export function parseClipboardTable(input: string): string[][] {
  // Excel uses TSV; CSV is useful for copied exports. Detect outside quoted headers.
  let quoted = false
  let tabs = 0
  let commas = 0
  for (let index = 0; index < input.length; index++) {
    const char = input[index]
    if (char === '"') {
      if (quoted && input[index + 1] === '"') index++
      else quoted = !quoted
    } else if (!quoted) {
      if (char === '\r' || char === '\n') break
      if (char === '\t') tabs++
      if (char === ',') commas++
    }
  }
  const delimiter = tabs > 0 || commas === 0 ? '\t' : ','
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let inQuotes = false
  let closed = false
  const cell = () => { row.push(field); field = ''; closed = false; if (row.length > 200) fail('tableLimit') }
  const line = () => { cell(); rows.push(row); row = []; if (rows.length > 5001) fail('tableLimit') }
  for (let index = 0; index < input.length; index++) {
    const char = input[index]
    if (inQuotes) {
      if (char === '"') {
        if (input[index + 1] === '"') { field += '"'; index++ }
        else { inQuotes = false; closed = true }
      } else if (char === '\r' && input[index + 1] === '\n') { field += '\n'; index++ }
      else field += char
    } else if (char === delimiter) cell()
    else if (char === '\n' || char === '\r') {
      line()
      if (char === '\r' && input[index + 1] === '\n') index++
    } else if (char === '"') {
      if (field || closed) fail('invalidTable')
      inQuotes = true
    } else {
      if (closed) fail('invalidTable')
      field += char
    }
  }
  if (inQuotes) fail('invalidTable')
  if (field || closed || row.length) line()
  return rows
}

export function tableRowsToJson(rows: string[][]): string {
  if (rows.length < 2) fail('invalidTable')
  if (rows.length > 5001 || rows[0].length > 200) fail('tableLimit')
  const headers = rows[0].map(value => value.trim())
  if (headers.some(value => !value) || new Set(headers).size !== headers.length) fail('tableHeaders')
  if (rows.some(row => row.length !== headers.length)) fail('invalidTable')
  return JSON.stringify(rows.slice(1).map(row => Object.fromEntries(headers.map((header, index) => [header, row[index]]))), null, 2)
}

function htmlFragment(html: string): DocumentFragment {
  // Template contents stay inert: never insert copied markup into the live page.
  const template = document.createElement('template')
  template.innerHTML = html
  template.content.querySelectorAll('script,style,noscript,template,iframe,object,embed,form,input,button,svg,math').forEach(element => element.remove())
  if (template.content.querySelectorAll('*').length > 5000) fail('complexHtml')
  return template.content
}

function htmlTableRows(table: HTMLTableElement): string[][] {
  if (table.querySelector('table')) fail('unsupportedTable')
  const rows = Array.from(table.rows, row => Array.from(row.cells, cell => {
    if (cell.colSpan !== 1 || cell.rowSpan !== 1) fail('unsupportedTable')
    const copy = cell.cloneNode(true) as HTMLElement
    copy.querySelectorAll('br').forEach(br => br.replaceWith('\n'))
    return (copy.textContent ?? '').replace(/\u00a0/g, ' ').trim()
  }))
  if (rows.length > 5001 || rows.some(row => row.length > 200)) fail('tableLimit')
  return rows
}

export function clipboardTableToJson(text: string, html?: string): string {
  const table = html ? htmlFragment(html).querySelector('table') : null
  return tableRowsToJson(table ? htmlTableRows(table) : parseClipboardTable(text))
}

const escapeMarkdown = (text: string) => text.replace(/\\/g, '\\\\').replace(/[`*_{}\[\]<>#+.!|~\-]/g, '\\$&')
const longestTicks = (text: string) => Math.max(0, ...Array.from(text.matchAll(/`+/g), match => match[0].length))

export function clipboardHtmlToMarkdown(html: string): string {
  const fragment = htmlFragment(html)
  function children(node: Node, depth: number): string {
    if (depth > 100) fail('complexHtml')
    const parts = Array.from(node.childNodes, child => render(child, depth + 1))
    return parts.reduce((result, part, index) => {
      if (/^\s+$/.test(part) && (result.endsWith('\n') || parts[index + 1]?.startsWith('\n'))) return result
      return result + (result.endsWith('\n\n') && part.startsWith('\n\n') ? part.slice(2) : part)
    }, '')
  }
  function emphasis(content: string, marker: string): string {
    if (!content.trim()) return content
    return (content.match(/^\s*/)?.[0] ?? '') + marker + content.trim() + marker + (content.match(/\s*$/)?.[0] ?? '')
  }
  function render(node: Node, depth: number): string {
    if (node.nodeType === 3) return escapeMarkdown((node.textContent ?? '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' '))
    if (node.nodeType !== 1) return ''
    const element = node as HTMLElement
    const tag = element.tagName.toLowerCase()
    if (tag === 'br') return '  \n'
    if (tag === 'hr') return '\n\n---\n\n'
    if (tag === 'img') return escapeMarkdown(element.getAttribute('alt') ?? '')
    if (tag === 'pre') {
      const code = element.textContent ?? ''
      const fence = '`'.repeat(Math.max(3, longestTicks(code) + 1))
      return `\n\n${fence}\n${code.replace(/\n$/, '')}\n${fence}\n\n`
    }
    if (tag === 'code') {
      const code = (element.textContent ?? '').replace(/[\r\n]/g, ' ')
      const fence = '`'.repeat(longestTicks(code) + 1)
      const padding = /^[` ]|[` ]$/.test(code) ? ' ' : ''
      return `${fence}${padding}${code}${padding}${fence}`
    }
    if (tag === 'table') {
      const rows = htmlTableRows(element as HTMLTableElement)
      if (!rows.length) return ''
      if (rows.some(row => row.length !== rows[0].length)) fail('invalidTable')
      const rowText = (row: string[]) => `| ${row.map(cell => escapeMarkdown(cell).replace(/\n/g, '<br>')).join(' | ')} |`
      return `\n\n${rowText(rows[0])}\n| ${rows[0].map(() => '---').join(' | ')} |\n${rows.slice(1).map(rowText).join('\n')}\n\n`
    }
    if (tag === 'ul' || tag === 'ol') {
      const start = Number(element.getAttribute('start'))
      const offset = Number.isSafeInteger(start) && start > 0 && start < 1_000_000 ? start : 1
      return '\n\n' + Array.from(element.children).filter(child => child.tagName === 'LI').map((child, index) => {
        const prefix = tag === 'ol' ? `${offset + index}. ` : '- '
        return prefix + children(child, depth).trim().replace(/\n/g, '\n' + ' '.repeat(prefix.length))
      }).join('\n') + '\n\n'
    }
    const content = children(node, depth)
    if (/^h[1-6]$/.test(tag)) return `\n\n${'#'.repeat(Number(tag[1]))} ${content.trim()}\n\n`
    if (['p', 'div', 'section', 'article', 'header', 'footer'].includes(tag)) return `\n\n${content.trim()}\n\n`
    if (tag === 'blockquote') return '\n\n' + content.trim().split('\n').map(line => `> ${line}`).join('\n') + '\n\n'
    if (tag === 'strong' || tag === 'b') return emphasis(content, '**')
    if (tag === 'em' || tag === 'i') return emphasis(content, '*')
    if (tag === 'del' || tag === 's') return emphasis(content, '~~')
    if (tag === 'a') {
      const href = element.getAttribute('href')?.trim() ?? ''
      try {
        const url = new URL(href)
        if (['https:', 'http:', 'mailto:'].includes(url.protocol)) return `[${content}](<${href.replace(/[<>\s\\]/g, char => encodeURIComponent(char))}>)`
      } catch { /* Unsupported or relative links retain their visible text. */ }
    }
    return content
  }
  // Preserve whitespace inside code blocks; block rendering already adds separators.
  return children(fragment, 0).trim()
}
