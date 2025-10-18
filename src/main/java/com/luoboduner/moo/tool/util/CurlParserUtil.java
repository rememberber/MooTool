package com.luoboduner.moo.tool.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * cURL命令解析工具
 * @author Cassian Flroin
 * @email flowercard591@gmail.com
 * @date 2025/10/09 14:43
 */
public class CurlParserUtil {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameValue {
        private String name;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurlResult {
        private String method;
        private String url;
        private List<NameValue> headers = new ArrayList<NameValue>();
        private List<NameValue> cookies = new ArrayList<NameValue>();
        private String body;
        private String contentType;
        private boolean useGetWithQuery;

    }

    /**
     * 解析curl命令
     * @param raw cURL
     * @return 解析结果
     */
    public static CurlResult parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("cURL is empty");
        }
        // 标准化多行：删除反斜杠换行符
        String s = raw.replace("\r\n", "\n");
        // 将反斜杠续行（\\ 或 \r\n / \n）折叠为空格
        s = s.replaceAll("\\\\\r?\n", " ");

        List<String> tokens = normalizeTokens(tokenize(s.trim()));
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("cURL is empty");
        }
        if (tokens.getFirst().equalsIgnoreCase("curl")) {
            tokens.removeFirst();
        }

        CurlResult result = new CurlResult();
        String method = null;
        String url = null;
        boolean useGet = false;
        List<String> dataList = new ArrayList<String>();
        List<NameValue> headers = new ArrayList<NameValue>();
        List<NameValue> cookieList = new ArrayList<NameValue>();
        String contentType = null;

        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if ("-X".equals(t) || "--request".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                method = v == null ? null : v.toUpperCase();
            } else if ("-H".equals(t) || "--header".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                contentType = handleHeader(v, headers, cookieList, contentType);
            } else if ("-A".equals(t) || "--user-agent".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                headers.add(new NameValue("User-Agent", v));
            } else if ("-u".equals(t) || "--user".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                headers.add(new NameValue("Authorization", encodeBasicAuth(v)));
            } else if ("-b".equals(t) || "--cookie".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                if (v.contains("=")) {
                    addCookiesFromString(cookieList, v);
                }
                // 否则 cookie-jar 文件路径 -> 忽略
            } else if ("-d".equals(t) || "--data".equals(t) || "--data-raw".equals(t)
                    || "--data-binary".equals(t) || "--data-urlencode".equals(t)) {
                String v = stripQuotes(next(tokens, ++i));
                dataList.add(v);
            } else if ("-G".equals(t) || "--get".equals(t)) {
                useGet = true;
            } else if ("--url".equals(t)) {
                String v = next(tokens, ++i);
                url = stripQuotes(v);
            } else if (!t.startsWith("-")) {
                // token可能是URL，如果不是选项且url未设置
                if (url == null) {
                    url = stripQuotes(t);
                }
            } else {
                // 忽略其他标志，如 -s, -k, --compressed 等
            }
        }

        // 确定方法和body
        String body = null;
        if (useGet) {
            method = (method == null) ? "GET" : method;
            if (!dataList.isEmpty()) {
                String q = joinWithAmp(dataList);
                url = appendQuery(url, q);
            }
            body = "";
        } else {
            if (method == null) {
                method = dataList.isEmpty() ? "GET" : "POST";
            }
            if (!dataList.isEmpty()) {
                body = joinWithAmp(dataList);
            }
        }

        result.method = method;
        result.url = url;
        result.headers = headers;
        result.cookies = cookieList;
        result.body = body == null ? "" : body;
        result.contentType = contentType;
        result.useGetWithQuery = useGet;
        return result;
    }

    /**
     * 将多个数据项连接为字符串，并使用 "&" 连接
     * @param dataList 数据项列表
     * @return 连接后的字符串
     */
    private static String joinWithAmp(List<String> dataList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            if (i > 0) sb.append('&');
            sb.append(dataList.get(i));
        }
        return sb.toString();
    }

    /**
     * 添加查询参数
     * @param url URL
     * @param q 查询参数
     * @return 添加查询参数后的URL
     */
    private static String appendQuery(String url, String q) {
        if (url == null || q == null || q.isEmpty()) return url;
        int hash = url.indexOf('#');
        String base = hash >= 0 ? url.substring(0, hash) : url;
        String frag = hash >= 0 ? url.substring(hash) : "";
        if (base.contains("?")) {
            if (base.endsWith("?") || base.endsWith("&")) {
                base = base + q;
            } else {
                base = base + "&" + q;
            }
        } else {
            base = base + "?" + q;
        }
        return base + frag;
    }

    /**
     * 从字符串中添加Cookie
     * @param cookies Cookie列表
     * @param v Cookie字符串
     */
    private static void addCookiesFromString(List<NameValue> cookies, String v) {
        if (v == null || v.trim().isEmpty()) return;
        String[] segs = v.split(";\\s*");
        for (String seg : segs) {
            if (seg.trim().isEmpty()) continue;
            int eq = seg.indexOf('=');
            if (eq > 0) {
                String name = seg.substring(0, eq).trim();
                String value = seg.substring(eq + 1).trim();
                cookies.add(new NameValue(name, value));
            }
        }
    }

    /**
     * 解析一个HTTP头
     * @param v 头字符串
     * @return 解析后的头
     */
    private static NameValue parseHeader(String v) {
        if (v == null) return null;
        int idx = v.indexOf(':');
        if (idx <= 0) return null;
        String name = v.substring(0, idx).trim();
        String value = v.substring(idx + 1).trim();
        return new NameValue(name, value);
    }

    /**
     * 去掉引号
     * @param s 字符串
     * @return 去掉引号后的字符串
     */
    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * 标记化一个类似shell的字符串，尊重引号和反斜杠转义
     */
    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inS = false;
        boolean inD = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inS) {
                if (c == '\'') {
                    inS = false;
                } else {
                    cur.append(c);
                }
            } else if (inD) {
                if (c == '"') {
                    inD = false;
                } else if (c == '\\') {
                    if (i + 1 < s.length()) {
                        i++;
                        cur.append(s.charAt(i));
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '\'') {
                    inS = true;
                } else if (c == '"') {
                    inD = true;
                } else if (Character.isWhitespace(c)) {
                    if (!cur.isEmpty()) {
                        out.add(cur.toString());
                        cur.setLength(0);
                    }
                } else if (c == '\\') {
                    if (i + 1 < s.length()) {
                        i++;
                        cur.append(s.charAt(i));
                    }
                } else {
                    cur.append(c);
                }
            }
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        return out;
    }

    private static String next(List<String> tokens, int idx) {
        if (idx >= tokens.size()) {
            return "";
        }
        return tokens.get(idx);
    }

    /**
     * 规格化 token：
     * - 将 "--opt=value" 拆分为 ["--opt", "value"]
     * - 将 "-XPOST" 拆分为 ["-X", "POST"]
     * - 支持 "-uuser:pass" -> ["-u", "user:pass"]
     */
    private static List<String> normalizeTokens(List<String> tokens) {
        List<String> normalized = new ArrayList<String>(tokens.size());
        for (String t : tokens) {
            if (t == null) continue;
            if (t.startsWith("--")) {
                int eq = t.indexOf('=');
                if (eq > 2) {
                    normalized.add(t.substring(0, eq));
                    normalized.add(t.substring(eq + 1));
                } else {
                    normalized.add(t);
                }
            } else if (t.startsWith("-X") && t.length() > 2) {
                normalized.add("-X");
                normalized.add(t.substring(2));
            } else if (t.startsWith("-u") && t.length() > 2) {
                normalized.add("-u");
                normalized.add(t.substring(2));
            } else {
                normalized.add(t);
            }
        }
        return normalized;
    }

    private static String handleHeader(String v, List<NameValue> headers, List<NameValue> cookieList, String currentContentType) {
        NameValue hv = parseHeader(v);
        if (hv != null) {
            String hn = hv.getName();
            if (hn != null && hn.equalsIgnoreCase("content-type")) {
                currentContentType = hv.getValue();
                headers.add(hv); // 同时保留在 headers
            } else if (hn != null && hn.equalsIgnoreCase("cookie")) {
                addCookiesFromString(cookieList, hv.getValue());
            } else {
                headers.add(hv);
            }
        }
        return currentContentType;
    }

    private static String encodeBasicAuth(String userAndPass) {
        if (userAndPass == null) return "Basic ";
        String encoded = Base64.getEncoder().encodeToString(userAndPass.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}

