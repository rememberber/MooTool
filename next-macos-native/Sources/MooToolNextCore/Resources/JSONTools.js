// Independent native-product copy of MooTool JSON and find/replace algorithms.
// Maintained here; no runtime/build dependency on the Electron product.
const { XMLBuilder, XMLParser, XMLValidator } = globalThis.fxp;
const { JSONPath } = globalThis.JSONPath;
function formatJson(input, t, spaces = 2) {
    return JSON.stringify(parseJson(input, t), null, spaces);
}
function compressJson(input, t) {
    return JSON.stringify(parseJson(input, t));
}
function formatJsonAdvanced(input, t, options) {
    if (options.checkDuplicateKeys) {
        const duplicates = findDuplicateJsonKeys(input, options.ignoreCase);
        if (duplicates.length > 0) {
            throw new Error(t('json.error.duplicateKeys', {
                paths: duplicates.join(', ')
            }));
        }
    }
    const parsed = parseJson(input, t);
    const value = options.sortKeys ? sortJsonKeys(parsed, options.ignoreCase) : parsed;
    return JSON.stringify(value, null, options.spaces);
}
function validateJson(input, t) {
    if (!input.trim()) {
        return {
            kind: 'idle',
            message: t('json.valid.idle')
        };
    }
    try {
        const value = parseJson(input, t);
        const type = Array.isArray(value) ? 'Array' : typeof value === 'object' && value !== null ? 'Object' : typeof value;
        return {
            kind: 'valid',
            message: t('json.valid.ok', {
                type
            })
        };
    } catch (error) {
        return {
            kind: 'error',
            message: error instanceof Error ? error.message : t('json.valid.error')
        };
    }
}
function escapeJsonString(input) {
    return JSON.stringify(input);
}
function unescapeJsonString(input, t) {
    const parsed = parseJson(input, t);
    if (typeof parsed !== 'string') {
        throw new Error(t('json.error.notString'));
    }
    return parsed;
}
function escapeJavaString(input) {
    return input.replaceAll('\\', '\\\\').replaceAll('\b', '\\b').replaceAll('\f', '\\f').replaceAll('\n', '\\n').replaceAll('\r', '\\r').replaceAll('\t', '\\t').replaceAll('"', '\\"');
}
function unescapeJsonText(input) {
    return JSON.parse(`"${input.replaceAll('"', '\\"')}"`);
}
function jsonToXml(input, t) {
    const value = parseJson(input, t);
    const builder = new XMLBuilder({
        format: true,
        ignoreAttributes: false,
        suppressEmptyNode: true
    });
    return builder.build({
        root: value
    });
}
function xmlToJson(input, t) {
    if (!input.trim()) {
        throw new Error(t('json.error.emptyXml'));
    }
    const parser = new XMLParser({
        ignoreAttributes: false,
        parseTagValue: true,
        parseAttributeValue: true,
        trimValues: true
    });
    return JSON.stringify(parser.parse(input), null, 2);
}
function queryJsonPath(input, path, t) {
    if (!path.trim()) {
        throw new Error(t('json.error.emptyPath'));
    }
    const value = JSONPath({
        path: path.trim(),
        json: parseJson(input, t),
        wrap: false
    });
    return formatJsonPathValue(value);
}
function swapJsonKeysAndValues(input, t) {
    const value = parseJson(input, t);
    if (!isJsonObject(value)) {
        throw new Error(t('json.error.objectRequired'));
    }
    return JSON.stringify(swapObject(value), null, 2);
}
function javaBeanToJson(input, t) {
    if (!input.trim()) {
        throw new Error(t('json.error.emptyJavaBean'));
    }
    const result = Object.create(null);
    const fieldPattern = /^(?:(?:public|protected|private)\s+)?(?:(?:static|final|transient|volatile)\s+)*([\w$.<>?, \[\]]+?)\s+(\w+)\s*(?:=.*)?$/;
    for (const statement of input.split(';')){
        const boundary = Math.max(statement.lastIndexOf('{'), statement.lastIndexOf('}'));
        const match = fieldPattern.exec(statement.slice(boundary + 1).trim());
        if (!match) continue;
        const type = match[1].trim();
        const name = match[2];
        if (name === 'serialVersionUID') continue;
        result[name] = mockJavaValue(type);
    }
    if (Object.keys(result).length === 0) {
        throw new Error(t('json.error.noJavaFields'));
    }
    return JSON.stringify(result, null, 2);
}
function jsonToJavaBean(input, t, rootClassName = 'Root') {
    const value = parseJson(input, t);
    if (!isJsonObject(value)) throw new Error(t('json.error.objectRequired'));
    return 'import java.util.List;\n\n' + buildNativeJavaClass(toPascalCase(rootClassName), value);
}
const javaKeywords = new Set(('abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null record sealed permits var yield _').split(' '));
function buildNativeJavaClass(rootName, object) {
    const occupiedTypes = new Set(['String','Object','Boolean','Long','Double','List']);
    function unique(base, used) { let name=base, n=2; while(used.has(name)) name=base+n++; used.add(name); return name; }
    rootName = unique(rootName, occupiedTypes);
    function build(name, fields, level, root) {
        const childClasses=[], fieldNames=new Set(), indentation='    '.repeat(level);
        function infer(key, value) {
            if(value===null) return 'Object';
            if(typeof value==='string') return 'String';
            if(typeof value==='boolean') return 'Boolean';
            if(typeof value==='number') return Number.isInteger(value)?'Long':'Double';
            if(Array.isArray(value)) {
                const nonNull=value.filter(v=>v!==null);
                if(!nonNull.length) return 'List<Object>';
                const kinds=new Set(nonNull.map(v=>Array.isArray(v)?'array':typeof v));
                if(kinds.size>1) return 'List<Object>';
                if(kinds.has('number')) return 'List<'+(nonNull.every(Number.isInteger)?'Long':'Double')+'>';
                return 'List<'+infer(singularize(key),nonNull[0])+'>';
            }
            const childName=unique(toPascalCase(key),occupiedTypes);
            childClasses.push({name:childName,value}); return childName;
        }
        const members=Object.entries(fields).map(([key,value])=>{
            const field=unique(toJavaIdentifier(key),fieldNames);
            return indentation+'    private '+infer(key,value)+' '+field+';';
        });
        for(const child of childClasses) members.push(build(child.name,child.value,level+1,false));
        return indentation+(root?'public class ':'public static class ')+name+' {\n'+members.join('\n\n')+'\n'+indentation+'}';
    }
    return build(rootName,object,0,true);
}
function findDuplicateJsonKeys(input, ignoreCase = false) {
    JSON.parse(input);
    return new DuplicateKeyParser(input, ignoreCase).parse();
}
function listJsonPaths(input, t) {
    const entries = [];
    collectJsonPaths(parseJson(input, t), '$', '$', 0, entries);
    return entries;
}
function parseJson(input, t) {
    if (!input.trim()) {
        throw new Error(t('json.error.empty'));
    }
    return JSON.parse(input);
}
function sortJsonKeys(value, ignoreCase) {
    if (Array.isArray(value)) {
        return value.map((item)=>sortJsonKeys(item, ignoreCase));
    }
    if (!isJsonObject(value)) {
        return value;
    }
    const compare = ignoreCase ? (left, right)=>left.localeCompare(right, undefined, {
            sensitivity: 'base'
        }) : (left, right)=>left.localeCompare(right);
    return Object.fromEntries(Object.keys(value).sort(compare).map((key)=>[
            key,
            sortJsonKeys(value[key], ignoreCase)
        ]));
}
function collectJsonPaths(value, path, label, depth, entries) {
    entries.push({
        path,
        label,
        value,
        depth
    });
    if (Array.isArray(value)) {
        value.forEach((item, index)=>collectJsonPaths(item, `${path}[${index}]`, `[${index}]`, depth + 1, entries));
        return;
    }
    if (isJsonObject(value)) {
        for (const [key, item] of Object.entries(value)){
            const childPath = /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`;
            collectJsonPaths(item, childPath, key, depth + 1, entries);
        }
    }
}
function swapObject(value) {
    const result = Object.create(null);
    for (const [key, item] of Object.entries(value)){
        if (isJsonObject(item)) {
            result[key] = swapObject(item);
            continue;
        }
        const swappedKey = Array.isArray(item) ? JSON.stringify(item) : String(item);
        result[swappedKey] = key;
    }
    return result;
}
function formatJsonPathValue(value) {
    if (typeof value === 'string') return JSON.stringify(value);
    if (value === undefined) return 'undefined';
    return JSON.stringify(value, null, 2);
}
function mockJavaValue(type) {
    const normalized = type.replaceAll(' ', '');
    if (normalized.endsWith('[]') || /^(List|Set|Collection|Iterable)</.test(normalized)) return [];
    if (/^(Map|HashMap|LinkedHashMap)</.test(normalized)) return {};
    if (/^(boolean|Boolean)$/.test(normalized)) return false;
    if (/^(byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double|BigDecimal|BigInteger)$/.test(normalized)) return 0;
    if (/^(char|Character|String|CharSequence)$/.test(normalized)) return '';
    return null;
}
function buildJavaClass(className, value, depth, root) {
    const fields = [];
    const childClasses = [];
    const indent = '    '.repeat(depth);
    const bodyIndent = '    '.repeat(depth + 1);
    for (const [key, item] of Object.entries(value)){
        const fieldName = toJavaIdentifier(key);
        const type = inferJavaType(key, item, childClasses);
        fields.push(`${bodyIndent}private ${type} ${fieldName};`);
    }
    const declaration = root ? `public class ${className}` : `public static class ${className}`;
    const children = childClasses.map((child)=>buildJavaClass(child.name, child.value, depth + 1, false));
    const members = [
        ...fields,
        ...children
    ];
    return `${indent}${declaration} {\n${members.join('\n\n')}\n${indent}}`;
}
function inferJavaType(key, value, childClasses) {
    if (value === null) return 'Object';
    if (typeof value === 'string') return 'String';
    if (typeof value === 'boolean') return 'Boolean';
    if (typeof value === 'number') return Number.isInteger(value) ? 'Long' : 'Double';
    if (Array.isArray(value)) {
        const first = value.find((item)=>item !== null);
        if (first === undefined) return 'List<Object>';
        if (isJsonObject(first)) {
            const name = toPascalCase(singularize(key));
            childClasses.push({
                name,
                value: first
            });
            return `List<${name}>`;
        }
        return `List<${inferJavaType(key, first, childClasses)}>`;
    }
    if (isJsonObject(value)) {
        const name = toPascalCase(key);
        childClasses.push({
            name,
            value
        });
        return name;
    }
    return 'Object';
}
function isJsonObject(value) {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function toJavaIdentifier(value) {
    let name=String(value).replace(/[^a-zA-Z0-9_$]/g,'_')||'value';
    if(/^\d/.test(name)) name='_'+name;
    if(javaKeywords.has(name)) name+='Value';
    return name;
}
function toPascalCase(value) {
    const normalized=String(value).replace(/[^a-zA-Z0-9_$]+(.)/g,(_,letter)=>letter.toUpperCase());
    const name=toJavaIdentifier(normalized||'Root');
    return name.charAt(0).toUpperCase()+name.slice(1);
}
function singularize(value) {
    return value.endsWith('ies') ? `${value.slice(0, -3)}y` : value.endsWith('s') ? value.slice(0, -1) : value;
}
class DuplicateKeyParser {
    source;
    ignoreCase;
    index = 0;
    duplicates = [];
    constructor(source, ignoreCase){
        this.source = source;
        this.ignoreCase = ignoreCase;
    }
    parse() {
        this.parseValue('$');
        return this.duplicates;
    }
    parseValue(path) {
        this.skipWhitespace();
        const token = this.source[this.index];
        if (token === '{') this.parseObject(path);
        else if (token === '[') this.parseArray(path);
        else if (token === '"') this.parseString();
        else this.parsePrimitive();
    }
    parseObject(path) {
        this.index++;
        this.skipWhitespace();
        const keys = new Set();
        if (this.source[this.index] === '}') {
            this.index++;
            return;
        }
        while(this.index < this.source.length){
            this.skipWhitespace();
            const key = this.parseString();
            const normalized = this.ignoreCase ? key.toLocaleLowerCase() : key;
            const keyPath = /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`;
            if (keys.has(normalized)) this.duplicates.push(keyPath);
            keys.add(normalized);
            this.skipWhitespace();
            this.index++;
            this.parseValue(keyPath);
            this.skipWhitespace();
            const next = this.source[this.index++];
            if (next === '}') return;
        }
    }
    parseArray(path) {
        this.index++;
        this.skipWhitespace();
        if (this.source[this.index] === ']') {
            this.index++;
            return;
        }
        let itemIndex = 0;
        while(this.index < this.source.length){
            this.parseValue(`${path}[${itemIndex++}]`);
            this.skipWhitespace();
            const next = this.source[this.index++];
            if (next === ']') return;
        }
    }
    parseString() {
        const start = this.index++;
        let escaped = false;
        while(this.index < this.source.length){
            const character = this.source[this.index++];
            if (escaped) escaped = false;
            else if (character === '\\') escaped = true;
            else if (character === '"') break;
        }
        return JSON.parse(this.source.slice(start, this.index));
    }
    parsePrimitive() {
        while(this.index < this.source.length && !/[\s,}\]]/.test(this.source[this.index]))this.index++;
    }
    skipWhitespace() {
        while(/\s/.test(this.source[this.index] ?? ''))this.index++;
    }
}
const defaultFindReplaceOptions = {
    matchCase: false,
    wholeWord: false,
    regex: false
};
function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
function buildSearchRegExp(query, options) {
    if (!query) return null;
    try {
        const source = options.regex ? query : escapeRegExp(query);
        const wrapped = options.wholeWord ? `\\b(?:${source})\\b` : source;
        const flags = options.matchCase ? 'gu' : 'giu';
        return new RegExp(wrapped, flags);
    } catch  {
        return null;
    }
}
function findAllMatches(content, query, options) {
    const expression = buildSearchRegExp(query, options);
    if (!expression) return [];
    const matches = [];
    for (const match of content.matchAll(expression)){
        if (match.index === undefined) continue;
        const text = match[0];
        if (!text.length) {
            expression.lastIndex = match.index + 1;
            continue;
        }
        matches.push({
            start: match.index,
            end: match.index + text.length
        });
    }
    return matches;
}
function findNextMatch(content, query, options, fromIndex, forward) {
    const matches = findAllMatches(content, query, options);
    if (!matches.length) return null;
    if (forward) {
        return matches.find((match)=>match.start >= fromIndex) ?? matches[0];
    }
    for(let index = matches.length - 1; index >= 0; index -= 1){
        const match = matches[index];
        if (match.end <= fromIndex) return match;
    }
    return matches[matches.length - 1];
}
function expandRegexReplacement(replaceWith) {
    return replaceWith.replace(/\\([nrt\\])/g, (_match, token)=>{
        if (token === 'n') return '\n';
        if (token === 'r') return '\r';
        if (token === 't') return '\t';
        return '\\';
    });
}
function replacementTemplate(replaceWith, regex) {
    return regex ? expandRegexReplacement(replaceWith) : replaceWith;
}
function applyReplacement(matchedText, replaceWith, expression) {
    const single = new RegExp(expression.source, expression.flags.replace('g', ''));
    return matchedText.replace(single, replaceWith);
}
function replaceCurrentMatch(content, query, replaceWith, options, selection) {
    if (!query) return {
        content,
        nextFrom: selection?.end ?? 0,
        replaced: false,
        match: null
    };
    const expression = buildSearchRegExp(query, options);
    if (!expression) return {
        content,
        nextFrom: selection?.end ?? 0,
        replaced: false,
        match: null
    };
    const replacement = replacementTemplate(replaceWith, options.regex);
    if (selection && selection.end > selection.start) {
        const selected = content.slice(selection.start, selection.end);
        const selectedMatches = findAllMatches(selected, query, options);
        const exact = selectedMatches.length === 1 && selectedMatches[0].start === 0 && selectedMatches[0].end === selected.length;
        if (exact) {
            const replacedSlice = applyReplacement(selected, replacement, expression);
            const next = `${content.slice(0, selection.start)}${replacedSlice}${content.slice(selection.end)}`;
            return {
                content: next,
                nextFrom: selection.start + replacedSlice.length,
                replaced: true,
                match: {
                    start: selection.start,
                    end: selection.start + replacedSlice.length
                }
            };
        }
    }
    const from = selection?.end ?? 0;
    const match = findNextMatch(content, query, options, from, true);
    if (!match) return {
        content,
        nextFrom: from,
        replaced: false,
        match: null
    };
    const slice = content.slice(match.start, match.end);
    const replacedSlice = applyReplacement(slice, replacement, expression);
    const next = `${content.slice(0, match.start)}${replacedSlice}${content.slice(match.end)}`;
    return {
        content: next,
        nextFrom: match.start + replacedSlice.length,
        replaced: true,
        match: {
            start: match.start,
            end: match.start + replacedSlice.length
        }
    };
}
function replaceAllMatches(content, query, replaceWith, options) {
    const expression = buildSearchRegExp(query, options);
    if (!expression) return {
        content,
        count: 0
    };
    const replacement = replacementTemplate(replaceWith, options.regex);
    let count = 0;
    const next = content.replace(expression, (match)=>{
        if (!match.length) return match;
        count += 1;
        return applyReplacement(match, replacement, expression);
    });
    return {
        content: next,
        count
    };
}
