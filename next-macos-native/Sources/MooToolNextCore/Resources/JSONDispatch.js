// Native-product boundary: all caller text is data, never script source.
const messagesByLocale = {
  'zh-CN': {
    'json.error.empty': '请输入 JSON。', 'json.error.notString': '输入必须是 JSON 字符串。',
    'json.error.duplicateKeys': '存在重复键：{paths}', 'json.error.objectRequired': '此操作需要 JSON 对象。',
    'json.error.emptyJavaBean': '请输入 JavaBean 源码。', 'json.error.noJavaFields': '没有找到可转换的 Java 字段。',
    'json.error.emptyXml': '请输入 XML。', 'json.error.emptyPath': '请输入 JSONPath。',
    'json.error.nodeLimit': 'JSON 最多处理 10 万节点、128 层。',
    'json.error.pointerEscapeInvalid': 'JSON Pointer 转义无效。',
    'json.error.pointerIndexInvalid': 'JSON Pointer 数组下标无效。',
    'json.error.pointerPathMissing': 'JSON Pointer 路径不存在。',
    'json.error.unknownQuickReplace': '未知快速替换操作。',
    'json.error.resultTooLarge8MB': '转换结果超过 8 MB。',
    'json.error.xmlDtdEntity': '不支持含 DTD 或实体声明的 XML。',
    'json.error.xmlLine': 'XML 第 {line} 行：{msg}',
    'json.error.jsonPathMustStartWithDollar': 'JSONPath 需以 $ 开头。',
    'json.error.queryTooManyResults': '查询结果超过 2 万项，请缩小查询范围。',
    'json.error.regexInvalid': '正则表达式无效：{message}',
    'json.error.findTooManyResults': '查找结果超过 2 万项，请缩小查询范围。',
    'json.error.unknownAction': '未知 JSON 操作。',
    'json.valid.workerPrefix': '有效 JSON · ',
    'json.valid.idle': '输入 JSON 开始',
    'json.valid.ok': '有效 JSON · {type}',
    'json.valid.error': 'JSON 无效或无法解析。'
  },
  'en-US': {
    'json.error.empty': 'Enter JSON.', 'json.error.notString': 'Input must be a JSON string.',
    'json.error.duplicateKeys': 'Duplicate keys: {paths}', 'json.error.objectRequired': 'This action requires a JSON object.',
    'json.error.emptyJavaBean': 'Enter JavaBean source code.', 'json.error.noJavaFields': 'No convertible Java fields were found.',
    'json.error.emptyXml': 'Enter XML.', 'json.error.emptyPath': 'Enter a JSONPath.',
    'json.error.nodeLimit': 'JSON supports at most 100,000 nodes and 128 levels.',
    'json.error.pointerEscapeInvalid': 'Invalid JSON Pointer escape.',
    'json.error.pointerIndexInvalid': 'Invalid JSON Pointer array index.',
    'json.error.pointerPathMissing': 'JSON Pointer path does not exist.',
    'json.error.unknownQuickReplace': 'Unknown quick-replace action.',
    'json.error.resultTooLarge8MB': 'Conversion result exceeds 8 MB.',
    'json.error.xmlDtdEntity': 'XML with DTD or entity declarations is not supported.',
    'json.error.xmlLine': 'XML line {line}: {msg}',
    'json.error.jsonPathMustStartWithDollar': 'JSONPath must start with $.',
    'json.error.queryTooManyResults': 'Query returned more than 20,000 items. Narrow the query.',
    'json.error.regexInvalid': 'Invalid regular expression: {message}',
    'json.error.findTooManyResults': 'Find returned more than 20,000 matches. Narrow the query.',
    'json.error.unknownAction': 'Unknown JSON action.',
    'json.valid.workerPrefix': 'Valid JSON · ',
    'json.valid.idle': 'Enter JSON to begin',
    'json.valid.ok': 'Valid JSON · {type}',
    'json.valid.error': 'JSON is invalid or could not be parsed.'
  },
  'ja-JP': {
    'json.error.empty': 'JSON を入力してください。', 'json.error.notString': '入力は JSON 文字列である必要があります。',
    'json.error.duplicateKeys': '重複キー：{paths}', 'json.error.objectRequired': 'この操作には JSON オブジェクトが必要です。',
    'json.error.emptyJavaBean': 'JavaBean ソースを入力してください。', 'json.error.noJavaFields': '変換可能な Java フィールドがありません。',
    'json.error.emptyXml': 'XML を入力してください。', 'json.error.emptyPath': 'JSONPath を入力してください。',
    'json.error.nodeLimit': 'JSON は最大 10 万ノード・128 階層までです。',
    'json.error.pointerEscapeInvalid': 'JSON Pointer のエスケープが無効です。',
    'json.error.pointerIndexInvalid': 'JSON Pointer の配列インデックスが無効です。',
    'json.error.pointerPathMissing': 'JSON Pointer パスが存在しません。',
    'json.error.unknownQuickReplace': '不明なクイック置換操作です。',
    'json.error.resultTooLarge8MB': '変換結果が 8 MB を超えています。',
    'json.error.xmlDtdEntity': 'DTD またはエンティティ宣言を含む XML は非対応です。',
    'json.error.xmlLine': 'XML {line} 行目：{msg}',
    'json.error.jsonPathMustStartWithDollar': 'JSONPath は $ で始めてください。',
    'json.error.queryTooManyResults': 'クエリ結果が 2 万件を超えています。範囲を狭めてください。',
    'json.error.regexInvalid': '正規表現が無効です：{message}',
    'json.error.findTooManyResults': '検索結果が 2 万件を超えています。範囲を狭めてください。',
    'json.error.unknownAction': '不明な JSON 操作です。',
    'json.valid.workerPrefix': '有効な JSON · ',
    'json.valid.idle': 'JSON を入力してください',
    'json.valid.ok': '有効な JSON · {type}',
    'json.valid.error': 'JSON が無効か、解析できません。'
  }
};
function translate(key, params = {}, locale = 'zh-CN') {
  const table = messagesByLocale[locale] || messagesByLocale['zh-CN'];
  let value = table[key] || messagesByLocale['zh-CN'][key] || key;
  for (const [k, v] of Object.entries(params)) value = value.replaceAll('{' + k + '}', v);
  return value;
}
function checkShape(value, t) {
  const pending = [[value, 0]]; let nodes = 0;
  while (pending.length) {
    const [item, depth] = pending.pop();
    if (++nodes > 100000 || depth > 128) throw Error(t('json.error.nodeLimit'));
    if (item && typeof item === 'object') for (const key of Object.keys(item)) pending.push([item[key], depth + 1]);
  }
}
function pointerQuery(input, path, t) {
  let value = JSON.parse(input);
  for (const token of path.slice(1).split('/')) {
    if (/~(?![01])/u.test(token)) throw Error(t('json.error.pointerEscapeInvalid'));
    const key = token.replaceAll('~1', '/').replaceAll('~0', '~');
    if (Array.isArray(value) && !/^(0|[1-9]\d*)$/u.test(key)) throw Error(t('json.error.pointerIndexInvalid'));
    if (value === null || typeof value !== 'object' || !Object.hasOwn(value, key)) throw Error(t('json.error.pointerPathMissing'));
    value = value[key];
  }
  return JSON.stringify(value, null, 2);
}
// The path picker emits literal segments. Preserve arbitrary JSON keys that
// JSONPath-Plus's expression normalizer interprets as operators or separators.
function literalPathTokens(path) {
  if (path[0] !== '$') return null;
  const tokens = []; let cursor = 1;
  while (cursor < path.length) {
    const rest = path.slice(cursor), property = rest.match(/^\.([A-Za-z_$][A-Za-z0-9_$]*)/u), index = rest.match(/^\[(0|[1-9]\d*)\]/u);
    if (property) { tokens.push(property[1]); cursor += property[0].length; continue; }
    if (index) { tokens.push(index[1]); cursor += index[0].length; continue; }
    if (!rest.startsWith('["')) return null;
    let end = cursor + 2;
    for (; end < path.length; end++) {
      if (path[end] === '\\') { end++; continue; }
      if (path[end] === '"') break;
    }
    if (path[end + 1] !== ']') return null;
    try { tokens.push(JSON.parse(path.slice(cursor + 1, end + 1))); } catch { return null; }
    cursor = end + 2;
  }
  return tokens;
}
function runJSONRequest(payload) {
  try {
    const r = JSON.parse(payload);
    const locale = r.language || 'zh-CN';
    const t = (key, params = {}) => translate(key, params, locale);
    const findOptions = { matchCase: r.matchCase, wholeWord: r.wholeWord, regex: r.regex };
    const nonJSON = new Set(['escape', 'escapeText', 'unescapeText', 'xmlToJson', 'beanToJson', 'find', 'replace', 'replaceAll', 'quickReplace', 'noteBullet', 'noteNumbered']);
    if (!nonJSON.has(r.action)) checkShape(parseJson(r.input, t), t);
    switch (r.action) {
      case 'quickReplace': case 'noteBullet': case 'noteNumbered': {
        let start = Math.max(0, Math.min(r.input.length, r.selectionStart)), end = Math.max(start, Math.min(r.input.length, r.selectionEnd));
        if (r.action === 'quickReplace' && end === start) { start = 0; end = r.input.length; }
        if (r.action !== 'quickReplace') {
          start = r.input.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
          const nextLine = r.input.indexOf('\n', end); end = nextLine < 0 ? r.input.length : nextLine;
        }
        const source = r.input.slice(start, end);
        const transformed = r.action === 'quickReplace' ? runNativeQuickReplace(source, r.path) : source.split('\n').map((line, i) => (r.action === 'noteBullet' ? '- ' : `${i + 1}. `) + line).join('\n');
        if (typeof transformed !== 'string') throw Error(t('json.error.unknownQuickReplace'));
        if (transformed.length > 8 * 1024 * 1024) throw Error(t('json.error.resultTooLarge8MB'));
        return JSON.stringify({ value: r.input.slice(0, start) + transformed + r.input.slice(end), match: { start, end: start + transformed.length } });
      }
      case 'validate': {
        const v = JSON.parse(r.input);
        return JSON.stringify({ value: t('json.valid.workerPrefix') + (Array.isArray(v) ? 'Array' : v === null ? 'Null' : typeof v === 'object' ? 'Object' : typeof v) });
      }
      case 'format': return JSON.stringify({ value: formatJson(r.input, t, 2) });
      case 'compress': return JSON.stringify({ value: compressJson(r.input, t) });
      case 'advanced': return JSON.stringify({ value: formatJsonAdvanced(r.input, t, { spaces: r.indent, sortKeys: r.sortKeys, ignoreCase: r.ignoreCase, checkDuplicateKeys: r.checkDuplicateKeys }) });
      case 'duplicates': return JSON.stringify({ value: JSON.stringify(findDuplicateJsonKeys(r.input, r.ignoreCase)), count: findDuplicateJsonKeys(r.input, r.ignoreCase).length });
      case 'escape': return JSON.stringify({ value: escapeJsonString(r.input) });
      case 'unescape': return JSON.stringify({ value: unescapeJsonString(r.input, t) });
      case 'escapeText': return JSON.stringify({ value: escapeJavaString(r.input) });
      case 'unescapeText': return JSON.stringify({ value: unescapeJsonText(r.input) });
      case 'swap': return JSON.stringify({ value: swapJsonKeysAndValues(r.input, t) });
      case 'jsonToXml': return JSON.stringify({ value: jsonToXml(r.input, t) });
      case 'xmlToJson': {
        if (/<!\s*(DOCTYPE|ENTITY)\b/iu.test(r.input)) throw Error(t('json.error.xmlDtdEntity'));
        const validation = XMLValidator.validate(r.input);
        if (validation !== true) throw Error(t('json.error.xmlLine', { line: String(validation.err.line), msg: validation.err.msg }));
        const value = xmlToJson(r.input, t); checkShape(JSON.parse(value), t); return JSON.stringify({ value });
      }
      case 'beanToJson': return JSON.stringify({ value: javaBeanToJson(r.input, t) });
      case 'jsonToBean': return JSON.stringify({ value: jsonToJavaBean(r.input, t, r.className) });
      case 'query': {
        const path = r.path.trim(); if (!path) throw Error(t('json.error.emptyPath'));
        if (path.startsWith('/')) return JSON.stringify({ value: pointerQuery(r.input, path, t) });
        if (!path.startsWith('$')) throw Error(t('json.error.jsonPathMustStartWithDollar'));
        const tokens = literalPathTokens(path);
        if (tokens) {
          let value = JSON.parse(r.input);
          for (const key of tokens) {
            if (value === null || typeof value !== 'object' || !Object.hasOwn(value, key)) { value = undefined; break; }
            value = value[key];
          }
          return JSON.stringify({ value: formatJsonPathValue(value), count: value === undefined ? 0 : 1 });
        }
        let count = 0;
        const value = JSONPath({ path, json: JSON.parse(r.input), wrap: false, eval: 'safe', callback() { if (++count > 20000) throw Error(t('json.error.queryTooManyResults')); } });
        return JSON.stringify({ value: formatJsonPathValue(value), count });
      }
      case 'find': case 'replace': case 'replaceAll': {
        if (r.regex && r.query) { try { new RegExp(r.query, 'u'); } catch (e) { throw Error(t('json.error.regexInvalid', { message: e.message })); } }
        const matches = findAllMatches(r.input, r.query, findOptions);
        if (matches.length > 20000) throw Error(t('json.error.findTooManyResults'));
        if (r.action === 'find') return JSON.stringify({ matches, count: matches.length, match: findNextMatch(r.input, r.query, findOptions, r.forward ? r.selectionEnd : r.selectionStart, r.forward) });
        if (r.action === 'replaceAll') { const value = replaceAllMatches(r.input, r.query, r.replacement, findOptions); return JSON.stringify({ value: value.content, count: value.count }); }
        const value = replaceCurrentMatch(r.input, r.query, r.replacement, findOptions, { start: r.selectionStart, end: r.selectionEnd });
        return JSON.stringify({ value: value.content, count: value.replaced ? 1 : 0, match: value.match });
      }
      default: throw Error(t('json.error.unknownAction'));
    }
  } catch (error) { return JSON.stringify({ error: error.message || String(error) }); }
}
