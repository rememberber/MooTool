package com.rememberber.mootool.nextfx.app;

import com.rememberber.mootool.nextfx.domain.ToolDefinition;
import com.rememberber.mootool.nextfx.domain.ToolGroupId;
import com.rememberber.mootool.nextfx.domain.ToolId;
import com.rememberber.mootool.nextfx.domain.ToolStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Static registry of the 26 stable tool IDs. Factories stay lazy in {@link AppServices}.
 */
public final class ToolRegistry {

    public static final List<ToolGroupId> GROUP_ORDER = List.of(
            ToolGroupId.TEXT,
            ToolGroupId.DEV,
            ToolGroupId.NETWORK,
            ToolGroupId.ENCODE,
            ToolGroupId.DAILY,
            ToolGroupId.SYSTEM
    );

    private static final List<ToolDefinition> TOOLS = List.of(
            tool(ToolId.MOOTOOL, ToolGroupId.HOME, "app.nav.home", List.of("home", "about", "首页", "主页", "关于"), "⌂", ToolStatus.IN_PROGRESS),
            tool(ToolId.QUICK_NOTE, ToolGroupId.TEXT, "app.nav.quickNote", List.of("note", "memo", "markdown", "随手记", "笔记", "记事"), "✎", ToolStatus.PLACEHOLDER),
            tool(ToolId.TEXT_DIFF, ToolGroupId.TEXT, "app.nav.textDiff", List.of("diff", "compare", "text", "对比", "比较"), "⇆", ToolStatus.PLACEHOLDER),
            tool(ToolId.REFORMAT, ToolGroupId.TEXT, "app.nav.reformat", List.of("format", "java", "xml", "html", "nginx", "格式化"), "🖌", ToolStatus.PLACEHOLDER),
            tool(ToolId.JSON, ToolGroupId.DEV, "app.nav.json", List.of("json", "xml", "javabean", "jsonpath", "格式化"), "{ }", ToolStatus.IN_PROGRESS),
            tool(ToolId.JAVA, ToolGroupId.DEV, "app.nav.java", List.of("java", "groovy", "python", "node", "console", "运行"), "▶", ToolStatus.PLACEHOLDER),
            tool(ToolId.YML_PROPERTIES, ToolGroupId.DEV, "app.nav.ymlProperties", List.of("yaml", "yml", "properties", "config", "配置", "转换"), "⚙", ToolStatus.PLACEHOLDER),
            tool(ToolId.PROTOBUF, ToolGroupId.DEV, "app.nav.protobuf", List.of("protobuf", "proto", "wire", "序列化"), "⬡", ToolStatus.PLACEHOLDER),
            tool(ToolId.VARIABLES, ToolGroupId.DEV, "app.nav.variables", List.of("env", "environment", "variable", "环境变量"), "$", ToolStatus.PLACEHOLDER),
            tool(ToolId.HTTP, ToolGroupId.NETWORK, "app.nav.http", List.of("http", "curl", "api", "request", "请求"), "🌐", ToolStatus.PLACEHOLDER),
            tool(ToolId.HOST, ToolGroupId.NETWORK, "app.nav.host", List.of("host", "dns", "域名"), "🖥", ToolStatus.PLACEHOLDER),
            tool(ToolId.NET, ToolGroupId.NETWORK, "app.nav.net", List.of("network", "ip", "ping", "whois", "网络"), "⌁", ToolStatus.PLACEHOLDER),
            tool(ToolId.UA_PARSE, ToolGroupId.NETWORK, "app.nav.uaParse", List.of("ua", "user-agent", "browser", "浏览器", "分析"), "UA", ToolStatus.PLACEHOLDER),
            tool(ToolId.ENCODE, ToolGroupId.ENCODE, "app.nav.encode", List.of("encode", "decode", "base64", "url", "编码", "解码"), "⇄", ToolStatus.IN_PROGRESS),
            tool(ToolId.CRYPTO, ToolGroupId.ENCODE, "app.nav.crypto", List.of("crypto", "hash", "md5", "sha", "random", "加密", "随机"), "🔒", ToolStatus.PLACEHOLDER),
            tool(ToolId.REGEX, ToolGroupId.ENCODE, "app.nav.regex", List.of("regex", "regexp", "regular", "正则", "匹配"), ".*", ToolStatus.PLACEHOLDER),
            tool(ToolId.CRON, ToolGroupId.ENCODE, "app.nav.cron", List.of("cron", "schedule", "定时", "表达式"), "⏱", ToolStatus.PLACEHOLDER),
            tool(ToolId.QR_CODE, ToolGroupId.ENCODE, "app.nav.qrCode", List.of("qr", "qrcode", "二维码", "条码"), "▣", ToolStatus.PLACEHOLDER),
            tool(ToolId.TIME_CONVERT, ToolGroupId.DAILY, "app.nav.timeConvert", List.of("time", "timestamp", "clock", "时间", "时区", "时间戳"), "🕒", ToolStatus.PLACEHOLDER),
            tool(ToolId.MESSAGE_BOARD, ToolGroupId.DAILY, "app.nav.messageBoard", List.of("message", "sign", "notice", "board", "留言", "告示"), "✉", ToolStatus.PLACEHOLDER),
            tool(ToolId.TRANSLATION, ToolGroupId.DAILY, "app.nav.translation", List.of("translate", "translation", "word", "翻译", "单词"), "文", ToolStatus.PLACEHOLDER),
            tool(ToolId.CALCULATOR, ToolGroupId.DAILY, "app.nav.calculator", List.of("calculator", "calc", "math", "计算", "表达式"), "∑", ToolStatus.PLACEHOLDER),
            tool(ToolId.COLOR_BOARD, ToolGroupId.DAILY, "app.nav.colorBoard", List.of("color", "palette", "hex", "rgb", "调色", "颜色"), "◉", ToolStatus.PLACEHOLDER),
            tool(ToolId.IMAGE, ToolGroupId.DAILY, "app.nav.image", List.of("image", "watermark", "compress", "图片", "图像"), "🖼", ToolStatus.PLACEHOLDER),
            tool(ToolId.PDF, ToolGroupId.DAILY, "app.nav.pdf", List.of("pdf", "merge", "split", "合并", "拆分"), "PDF", ToolStatus.PLACEHOLDER),
            tool(ToolId.HARDWARE, ToolGroupId.SYSTEM, "app.nav.hardware", List.of("hardware", "system", "cpu", "memory", "系统", "硬件"), "⌘", ToolStatus.PLACEHOLDER)
    );

    private static final Map<ToolId, ToolDefinition> BY_ID = new EnumMap<>(ToolId.class);

    static {
        for (ToolDefinition definition : TOOLS) {
            BY_ID.put(definition.id(), definition);
        }
    }

    private ToolRegistry() {
    }

    public static List<ToolDefinition> all() {
        return TOOLS;
    }

    public static Optional<ToolDefinition> find(ToolId id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static ToolDefinition require(ToolId id) {
        return find(id).orElseThrow();
    }

    public static List<ToolDefinition> inGroup(ToolGroupId groupId) {
        List<ToolDefinition> matches = new ArrayList<>();
        for (ToolDefinition definition : TOOLS) {
            if (definition.groupId() == groupId) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    public static List<ToolDefinition> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        if (needle.isEmpty()) {
            return List.of();
        }
        List<ToolDefinition> matches = new ArrayList<>();
        for (ToolDefinition definition : TOOLS) {
            if (definition.id().id().toLowerCase().contains(needle)
                    || definition.titleKey().toLowerCase().contains(needle)
                    || definition.keywords().stream().anyMatch(keyword -> keyword.toLowerCase().contains(needle))) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    private static ToolDefinition tool(
            ToolId id,
            ToolGroupId groupId,
            String titleKey,
            List<String> keywords,
            String glyph,
            ToolStatus status
    ) {
        boolean history = id != ToolId.MOOTOOL && id != ToolId.HARDWARE && id != ToolId.MESSAGE_BOARD;
        boolean favorites = id == ToolId.REGEX || id == ToolId.CRON || id == ToolId.COLOR_BOARD;
        return new ToolDefinition(id, groupId, titleKey, keywords, glyph, status, history, favorites);
    }
}
