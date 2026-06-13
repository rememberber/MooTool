package com.luoboduner.moo.tool.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.util.I18n;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 主界面功能 Tab 目录：索引、标识、图标与搜索关键词。
 */
public final class FuncTabCatalog {

    public static final int RECENT_LIMIT = 5;

    private static final int TAB_ICON_SIZE = 16;

    public record FuncTab(int index, String id, String titleKey, String iconPath, String[] keywords) {
    }

    public record BuiltinGroup(String id, String titleKey, List<String> funcIds) {
    }

    private static final List<BuiltinGroup> BUILTIN_GROUPS = List.of(
            new BuiltinGroup("text", "funcGroup.builtin.text",
                    List.of("quickNote", "textDiff", "reformat")),
            new BuiltinGroup("dev", "funcGroup.builtin.dev",
                    List.of("json", "java", "ymlProperties", "protobuf", "variables")),
            new BuiltinGroup("network", "funcGroup.builtin.network",
                    List.of("http", "host", "net", "uaParse")),
            new BuiltinGroup("encode", "funcGroup.builtin.encode",
                    List.of("encode", "crypto", "regex", "cron", "qrCode")),
            new BuiltinGroup("daily", "funcGroup.builtin.daily",
                    List.of("timeConvert", "translation", "calculator", "colorBoard", "image", "pdf")),
            new BuiltinGroup("system", "funcGroup.builtin.system",
                    List.of("hardware"))
    );

    private static final List<FuncTab> TABS = List.of(
            new FuncTab(0, "mootool", "tab.mootool", "icon/smile.svg", new String[]{"about", "home", "mootool", "关于", "首页"}),
            new FuncTab(1, "quickNote", "tab.quickNote", "icon/edit.svg", new String[]{"note", "memo", "markdown", "随手记", "笔记", "记事"}),
            new FuncTab(2, "timeConvert", "tab.timeConvert", "icon/time.svg", new String[]{"time", "timestamp", "clock", "时间", "时区", "时间戳"}),
            new FuncTab(3, "json", "tab.json", "icon/json.svg", new String[]{"json", "xml", "javabean", "jsonpath", "格式化"}),
            new FuncTab(4, "translation", "tab.translation", "icon/translate.svg", new String[]{"translate", "translation", "word", "翻译", "单词"}),
            new FuncTab(5, "host", "tab.host", "icon/check.svg", new String[]{"host", "dns", "域名"}),
            new FuncTab(6, "http", "tab.http", "icon/global.svg", new String[]{"http", "curl", "api", "request", "请求"}),
            new FuncTab(7, "uaParse", "tab.uaParse", "icon/ua.svg", new String[]{"ua", "user-agent", "browser", "分析"}),
            new FuncTab(8, "encode", "tab.encode", "icon/exchange.svg", new String[]{"encode", "decode", "base64", "url", "编码", "解码"}),
            new FuncTab(9, "qrCode", "tab.qrCode", "icon/QRcode.svg", new String[]{"qr", "qrcode", "二维码", "条码"}),
            new FuncTab(10, "crypto", "tab.crypto", "icon/method.svg", new String[]{"crypto", "hash", "md5", "sha", "random", "加解密", "随机"}),
            new FuncTab(11, "calculator", "tab.calculator", "icon/calculate.svg", new String[]{"calculator", "calc", "math", "计算", "表达式"}),
            new FuncTab(12, "net", "tab.net", "icon/network.svg", new String[]{"net", "network", "ip", "ifconfig", "ipconfig", "网络", "ping"}),
            new FuncTab(13, "colorBoard", "tab.colorBoard", "icon/color.svg", new String[]{"color", "palette", "hex", "rgb", "调色", "颜色"}),
            new FuncTab(14, "image", "tab.image", "icon/image.svg", new String[]{"image", "ocr", "watermark", "compress", "图片", "图像"}),
            new FuncTab(15, "cron", "tab.cron", "icon/schedule.svg", new String[]{"cron", "schedule", "定时", "表达式"}),
            new FuncTab(16, "regex", "tab.regex", "icon/reg.svg", new String[]{"regex", "regexp", "regular", "正则", "匹配"}),
            new FuncTab(17, "java", "tab.java", "icon/java.svg", new String[]{"java", "groovy", "console", "脚本", "控制台"}),
            new FuncTab(18, "reformat", "tab.reformat", "icon/format_painter.svg", new String[]{"format", "reformat", "sql", "yaml", "格式化"}),
            new FuncTab(19, "pdf", "tab.pdf", "icon/pdf.svg", new String[]{"pdf", "merge", "split", "合并", "拆分"}),
            new FuncTab(20, "variables", "tab.variables", "icon/fx.svg", new String[]{"env", "environment", "variable", "环境变量"}),
            new FuncTab(21, "hardware", "tab.hardware", "icon/info.svg", new String[]{"hardware", "system", "cpu", "memory", "系统信息", "硬件"}),
            new FuncTab(22, "ymlProperties", "tab.ymlProperties", "icon/suffix-yml.svg", new String[]{"yml", "yaml", "properties", "config", "配置", "转换"}),
            new FuncTab(23, "textDiff", "tab.textDiff", "icon/diff.svg", new String[]{"diff", "compare", "text", "对比", "比较"}),
            new FuncTab(24, "protobuf", "tab.protobuf", "icon/protobuf.svg", new String[]{"protobuf", "proto", "pb", "序列化"})
    );

    private FuncTabCatalog() {
    }

    public static List<BuiltinGroup> builtinGroups() {
        return BUILTIN_GROUPS;
    }

    public static List<FuncTab> tabsInGroup(List<String> funcIds) {
        List<FuncTab> tabs = new ArrayList<>();
        for (String funcId : funcIds) {
            byId(funcId).ifPresent(tabs::add);
        }
        return tabs;
    }

    public static boolean matchesGroupSearch(BuiltinGroup group, String query) {
        if (StringUtils.isBlank(query)) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return I18n.get(group.titleKey()).toLowerCase(Locale.ROOT).contains(normalized)
                || group.id().toLowerCase(Locale.ROOT).contains(normalized);
    }

    public static List<FuncTab> allTabs() {
        return TABS;
    }

    public static List<FuncTab> toolTabs() {
        return TABS.stream().filter(tab -> tab.index() > 0).toList();
    }

    public static int tabCount() {
        return TABS.size();
    }

    public static String[] titleKeys() {
        return TABS.stream().map(FuncTab::titleKey).toArray(String[]::new);
    }

    public static String[] iconPaths() {
        return TABS.stream().map(FuncTab::iconPath).toArray(String[]::new);
    }

    public static Optional<FuncTab> byIndex(int index) {
        if (index < 0 || index >= TABS.size()) {
            return Optional.empty();
        }
        return Optional.of(TABS.get(index));
    }

    public static Optional<FuncTab> byId(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        return TABS.stream().filter(tab -> tab.id().equals(id)).findFirst();
    }

    public static String titleAt(int index) {
        return byIndex(index).map(tab -> I18n.get(tab.titleKey())).orElse("");
    }

    public static FlatSVGIcon iconAt(int index, boolean iconOnly) {
        return byIndex(index)
                .map(tab -> iconOnly ? new FlatSVGIcon(tab.iconPath(), 20, 20) : new FlatSVGIcon(tab.iconPath()))
                .orElse(null);
    }

    public static FlatSVGIcon smallIcon(FuncTab tab) {
        return new FlatSVGIcon(tab.iconPath(), TAB_ICON_SIZE, TAB_ICON_SIZE);
    }

    public static void switchTo(int index) {
        MainWindow.getInstance().getTabbedPane().setSelectedIndex(index);
    }

    public static void switchToById(String id) {
        byId(id).ifPresent(FuncTabCatalog::switchTo);
    }

    public static void switchTo(FuncTab tab) {
        switchTo(tab.index());
    }

    public static boolean matchesSearch(FuncTab tab, String query) {
        if (StringUtils.isBlank(query)) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (I18n.get(tab.titleKey()).toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        if (tab.id().toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        return Arrays.stream(tab.keywords())
                .anyMatch(keyword -> keyword.toLowerCase(Locale.ROOT).contains(normalized));
    }
}
