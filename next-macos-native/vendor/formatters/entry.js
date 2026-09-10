import { format } from 'prettier/standalone';
import html from 'prettier/plugins/html';
import xml from '@prettier/plugin-xml';
import java from 'prettier-plugin-java';

// Match the Electron formatter's token boundaries, including quoted directives and comments.
function nginx(input, indent) {
  const tokens = []; let current = '', quote = '', escaped = false, comment = false;
  const flush = () => { const value = current.trim(); if (value) tokens.push(value); current = ''; };
  for (const char of input) {
    if (comment) { current += char; if (char === '\n') { flush(); comment = false; } continue; }
    if (escaped) { current += char; escaped = false; continue; }
    if (char === '\\') { current += char; escaped = true; continue; }
    if (quote) { current += char; if (char === quote) quote = ''; continue; }
    if (char === '"' || char === "'") { quote = char; current += char; continue; }
    if (char === '#') { comment = true; current += char; continue; }
    if (char === '{') { current = current.trimEnd() + ' {'; flush(); }
    else if (char === '}') { flush(); tokens.push('}'); }
    else if (char === ';') { current = current.trimEnd() + ';'; flush(); }
    else if (char === '\n' || char === '\r') flush();
    else current += char;
  }
  flush(); let level = 0;
  return tokens.map(token => {
    if (token === '}') level = Math.max(0, level - 1);
    if (level > 256) throw new Error('Nginx 嵌套超过 256 层。');
    const line = ' '.repeat(level * indent) + token;
    if (token.endsWith('{')) level++;
    return line;
  }).join('\n');
}

globalThis.startNativeReformat = async (payload) => {
  try {
    const { input, path: type, indent } = JSON.parse(payload);
    if (!['nginx', 'java', 'xml', 'html', 'json'].includes(type)) throw new Error('不支持的格式化类型。');
    const tabWidth = Math.min(8, Math.max(1, Math.round(indent)));
    let value = '';
    if (input.trim()) {
      if (type === 'nginx') value = nginx(input, tabWidth);
      else if (type === 'json') value = JSON.stringify(JSON.parse(input), null, tabWidth);
      else value = (await format(input, {
        parser: type, plugins: [type === 'java' ? java : type === 'xml' ? xml : html],
        tabWidth, useTabs: false, printWidth: 120, endOfLine: 'lf',
        ...(type === 'xml' ? { xmlWhitespaceSensitivity: 'preserve' } : {})
      })).trimEnd();
    }
    globalThis.nativeReformatReply = JSON.stringify({ value });
  } catch (error) { globalThis.nativeReformatReply = JSON.stringify({ error: String(error?.message || error).slice(0, 4096) }); }
};
