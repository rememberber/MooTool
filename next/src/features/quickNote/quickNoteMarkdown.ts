import DOMPurify from 'dompurify'
import { marked, type Tokens } from 'marked'

class OutlineTokenizer extends marked.Tokenizer {
  readonly remainingLengths = new WeakMap<Tokens.Heading, number>()

  heading(source: string): Tokens.Heading | undefined {
    const token = super.heading(source)
    if (token) this.remainingLengths.set(token, source.length)
    return token
  }

  lheading(source: string): Tokens.Heading | undefined {
    const token = super.lheading(source)
    if (token) this.remainingLengths.set(token, source.length)
    return token
  }
}

export function renderQuickNoteMarkdown(content: string, showOutline: boolean) {
  const headings: { offset: number; level: number; label: string }[] = []
  if (!showOutline) return { html: marked.parse(content, { async: false }), headings }

  // Match the line endings used by Marked and CodeMirror so the offsets work in both views.
  const source = content.replace(/\r\n?/g, '\n')
  const tokenizer = new OutlineTokenizer()
  const tokens = marked.lexer(source, { ...marked.defaults, tokenizer })
  const sectionTokens = new Set(tokens)
  const renderer = new marked.Renderer()
  const renderHeading = renderer.heading
  renderer.heading = function (token) {
    const remaining = tokenizer.remainingLengths.get(token)
    if (!sectionTokens.has(token) || remaining === undefined) return renderHeading.call(this, token)

    const offset = source.length - remaining
    const level = token.depth
    const html = this.parser.parseInline(token.tokens)
    const title = DOMPurify.sanitize(html, { RETURN_DOM_FRAGMENT: true })
    title.querySelectorAll('img').forEach((image) => image.replaceWith(image.alt))
    title.querySelectorAll('br').forEach((lineBreak) => lineBreak.replaceWith(' '))
    headings.push({ offset, level, label: (title.textContent ?? '').replace(/\s+/g, ' ').trim() })
    return `<h${level} data-quick-note-heading="${offset}">${html}</h${level}>\n`
  }

  const html = marked.parser(tokens, { ...marked.defaults, renderer })
  return { html, headings }
}
