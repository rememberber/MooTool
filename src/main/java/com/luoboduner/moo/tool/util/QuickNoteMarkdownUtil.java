package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.FontUtils;
import com.luoboduner.moo.tool.App;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.fife.ui.rsyntaxtextarea.HtmlUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.util.Arrays;

/**
 * 随手记 Markdown 转 HTML 工具
 */
public class QuickNoteMarkdownUtil {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;
    private static final Theme DARK_SYNTAX_THEME;
    private static final Theme LIGHT_SYNTAX_THEME;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        ));
        // 随手记常用 Tab 做层级缩进，避免被 Markdown 当成缩进代码块
        options.set(Parser.INDENTED_CODE_BLOCK_PARSER, false);
        // 单换行即预览为换行，无需行尾两个空格
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options)
                .escapeHtml(true)
                .build();

        try {
            DARK_SYNTAX_THEME = Theme.load(App.class.getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
            LIGHT_SYNTAX_THEME = Theme.load(App.class.getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private QuickNoteMarkdownUtil() {
    }

    public static boolean isMarkdownSyntax(String syntax) {
        return SyntaxConstants.SYNTAX_STYLE_MARKDOWN.equals(syntax);
    }

    public static String toHtmlDocument(String markdown) {
        if (markdown == null) {
            markdown = "";
        }
        Node document = PARSER.parse(markdown);
        String body = RENDERER.render(document);
        MarkdownTheme theme = MarkdownTheme.current();
        String styledBody = applyInlineStyles(body, theme);
        return "<html><head><style>" + theme.baseCss() + "</style></head><body>"
                + "<div class=\"md-root\">" + styledBody + "</div></body></html>";
    }

    private static String applyInlineStyles(String html, MarkdownTheme theme) {
        Document doc = Jsoup.parseBodyFragment(html);
        Element body = doc.body();

        styleHeadings(body, theme);
        styleParagraphs(body, theme);
        styleLinks(body, theme);
        styleInlineCode(body, theme);
        highlightCodeBlocks(body);
        styleCodeBlocks(body, theme);
        styleBlockquotes(body, theme);
        styleLists(body, theme);
        styleTables(body, theme);
        styleHorizontalRules(body, theme);
        resolveImageSources(body);
        styleImages(body, theme);
        styleStrongAndEm(body, theme);
        styleDeleted(body, theme);

        return body.html();
    }

    private static void styleHeadings(Element body, MarkdownTheme theme) {
        applyStyle(body.select("h1"), theme.h1Style());
        applyStyle(body.select("h2"), theme.h2Style());
        applyStyle(body.select("h3"), theme.h3Style());
        applyStyle(body.select("h4"), theme.h4Style());
        applyStyle(body.select("h5"), theme.h5Style());
        applyStyle(body.select("h6"), theme.h6Style());
    }

    private static void styleParagraphs(Element body, MarkdownTheme theme) {
        applyStyle(body.select("p"), theme.paragraphStyle());
    }

    private static void styleLinks(Element body, MarkdownTheme theme) {
        applyStyle(body.select("a"), theme.linkStyle());
    }

    private static void styleInlineCode(Element body, MarkdownTheme theme) {
        for (Element code : body.select("code")) {
            if (code.parent() != null && "pre".equalsIgnoreCase(code.parent().tagName())) {
                continue;
            }
            code.attr("style", theme.inlineCodeStyle());
        }
    }

    private static void highlightCodeBlocks(Element body) {
        RSyntaxTextArea helper = createSyntaxHelper();

        for (Element pre : body.select("pre")) {
            Element codeEl = pre.selectFirst("code");
            if (codeEl == null) {
                continue;
            }

            String language = extractLanguage(codeEl);
            if (language == null || language.isBlank()) {
                continue;
            }

            String syntaxStyle = resolveSyntaxEditingStyle(language);
            if (SyntaxConstants.SYNTAX_STYLE_NONE.equals(syntaxStyle)) {
                continue;
            }

            String codeText = codeEl.text();
            if (codeText == null) {
                codeText = "";
            }
            int len = codeText.length();
            if (len == 0) {
                continue;
            }

            helper.setSyntaxEditingStyle(syntaxStyle);
            helper.setText(codeText);
            String highlighted = HtmlUtil.getTextAsHtml(helper, 0, len - 1);

            Document frag = Jsoup.parseBodyFragment(highlighted);
            Element newPre = frag.selectFirst("pre");
            if (newPre != null) {
                pre.replaceWith(newPre);
            }
        }
    }

    private static RSyntaxTextArea createSyntaxHelper() {
        RSyntaxTextArea helper = new RSyntaxTextArea();
        helper.setEditable(false);
        Theme theme = FlatLaf.isLafDark() ? DARK_SYNTAX_THEME : LIGHT_SYNTAX_THEME;
        theme.apply(helper);
        Font codeFont = FontUtils.getCompositeFont(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, 13);
        helper.setFont(codeFont);
        return helper;
    }

    private static String extractLanguage(Element codeEl) {
        String classAttr = codeEl.className();
        if (classAttr == null || classAttr.isBlank()) {
            return null;
        }
        // flexmark-fenced-code-block 的默认输出一般是: language-java / language-javascript ...
        for (String cls : classAttr.split("\\s+")) {
            if (cls.startsWith("language-")) {
                return cls.substring("language-".length());
            }
            if (cls.startsWith("lang-")) {
                return cls.substring("lang-".length());
            }
        }
        return null;
    }

    private static String resolveSyntaxEditingStyle(String language) {
        String lang = language.trim().toLowerCase();

        // 兼容一些常见写法
        if ("c++".equals(lang)) {
            lang = "cpp";
        }
        if ("c#".equals(lang)) {
            lang = "csharp";
        }

        return switch (lang) {
            case "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA;
            case "javascript", "js" -> SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            case "typescript", "ts" -> SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT;
            case "python", "py" -> SyntaxConstants.SYNTAX_STYLE_PYTHON;
            case "c" -> SyntaxConstants.SYNTAX_STYLE_C;
            case "cpp" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
            case "csharp" -> SyntaxConstants.SYNTAX_STYLE_CSHARP;
            case "html" -> SyntaxConstants.SYNTAX_STYLE_HTML;
            case "xml" -> SyntaxConstants.SYNTAX_STYLE_XML;
            case "css" -> SyntaxConstants.SYNTAX_STYLE_CSS;
            case "json" -> SyntaxConstants.SYNTAX_STYLE_JSON;
            case "sql" -> SyntaxConstants.SYNTAX_STYLE_SQL;
            case "yaml", "yml" -> SyntaxConstants.SYNTAX_STYLE_YAML;
            case "bash", "shell", "sh", "zsh" -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL;
            case "dockerfile" -> SyntaxConstants.SYNTAX_STYLE_DOCKERFILE;
            case "markdown", "md" -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN;
            default -> SyntaxConstants.SYNTAX_STYLE_NONE;
        };
    }

    private static void styleCodeBlocks(Element body, MarkdownTheme theme) {
        for (Element pre : body.select("pre")) {
            Element codeEl = pre.selectFirst("code");
            String language = codeEl != null ? extractLanguage(codeEl) : null;
            boolean syntaxHighlighted = language != null && !language.isBlank();
            pre.attr("style", syntaxHighlighted ? theme.preStyle() : theme.prePlainStyle());
            if (codeEl != null) {
                codeEl.attr("style", syntaxHighlighted ? theme.preCodeStyle() : theme.prePlainCodeStyle());
            }
        }
    }

    private static void styleBlockquotes(Element body, MarkdownTheme theme) {
        applyStyle(body.select("blockquote"), theme.blockquoteStyle());
        applyStyle(body.select("blockquote p"), theme.blockquoteParagraphStyle());
    }

    private static void styleLists(Element body, MarkdownTheme theme) {
        applyStyle(body.select("ul"), theme.listStyle());
        applyStyle(body.select("ol"), theme.listStyle());
        applyStyle(body.select("li"), theme.listItemStyle());
    }

    private static void styleTables(Element body, MarkdownTheme theme) {
        applyStyle(body.select("table"), theme.tableStyle());
        applyStyle(body.select("thead"), theme.tableHeadStyle());
        applyStyle(body.select("th"), theme.thStyle());
        applyStyle(body.select("td"), theme.tdStyle());
        applyStyle(body.select("tr"), theme.trStyle());
    }

    private static void styleHorizontalRules(Element body, MarkdownTheme theme) {
        applyStyle(body.select("hr"), theme.hrStyle());
    }

    private static void resolveImageSources(Element body) {
        for (Element img : body.select("img")) {
            String resolved = QuickNoteAttachmentUtil.resolveImageSrc(img.attr("src"));
            img.attr("src", resolved);
        }
    }

    private static void styleImages(Element body, MarkdownTheme theme) {
        applyStyle(body.select("img"), theme.imageStyle());
    }

    private static void styleStrongAndEm(Element body, MarkdownTheme theme) {
        applyStyle(body.select("strong"), theme.strongStyle());
        applyStyle(body.select("em"), theme.emStyle());
    }

    private static void styleDeleted(Element body, MarkdownTheme theme) {
        applyStyle(body.select("del"), theme.delStyle());
    }

    private static void applyStyle(Elements elements, String style) {
        for (Element element : elements) {
            element.attr("style", style);
        }
    }

    private static final class MarkdownTheme {

        private final String text;
        private final String muted;
        private final String background;
        private final String surface;
        private final String border;
        private final String accent;
        private final String link;
        private final String codeText;
        private final String codeBg;
        private final String quoteBorder;
        private final String fontFamily;
        private final String monoFamily;

        private MarkdownTheme(boolean dark) {
            Color editorBg = getUiColor("Editor.background", dark ? new Color(30, 30, 30) : Color.WHITE);
            Color editorFg = getUiColor("Label.foreground", dark ? new Color(220, 220, 220) : new Color(36, 41, 47));
            Color linkColor = getUiColor("Component.linkColor", dark ? new Color(88, 166, 255) : new Color(9, 105, 218));
            Color accentColor = getUiColor("Component.accentColor", linkColor);

            if (dark) {
                text = toHex(editorFg);
                muted = "#9ea7b3";
                background = toHex(editorBg);
                surface = blend(editorBg, Color.WHITE, 0.06);
                border = "#3d444d";
                accent = toHex(accentColor);
                link = toHex(linkColor);
                codeText = "#e6edf3";
                codeBg = blend(editorBg, Color.WHITE, 0.08);
                quoteBorder = "#484f58";
            } else {
                text = toHex(editorFg);
                muted = "#656d76";
                background = toHex(editorBg);
                surface = "#f6f8fa";
                border = "#d0d7de";
                accent = toHex(accentColor);
                link = toHex(linkColor);
                codeText = "#24292f";
                codeBg = "#f6f8fa";
                quoteBorder = "#d0d7de";
            }
            String uiFont = resolveFontFamily();
            fontFamily = toCssFontFamily(uiFont);
            // JEditorPane 对 CompositeFont 支持有限，需显式提供中文回退字体
            monoFamily = toCssFontFamily(FlatJetBrainsMonoFont.FAMILY) + ", " + fontFamily + ", monospace";
        }

        static MarkdownTheme current() {
            return new MarkdownTheme(FlatLaf.isLafDark());
        }

        String baseCss() {
            return "body{margin:0;padding:0;background-color:" + background + ";}"
                    + ".md-root{font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";line-height:1.7;padding:20px 24px 28px 24px;background-color:" + background + ";}";
        }

        String h1Style() {
            return "font-family:" + fontFamily + ";font-size:28px;font-weight:bold;color:" + text
                    + ";margin:0 0 16px 0;padding:0 0 10px 0;border-bottom:2px solid " + border + ";line-height:1.3;";
        }

        String h2Style() {
            return "font-family:" + fontFamily + ";font-size:22px;font-weight:bold;color:" + text
                    + ";margin:28px 0 12px 0;padding:0 0 8px 0;border-bottom:1px solid " + border + ";line-height:1.35;";
        }

        String h3Style() {
            return "font-family:" + fontFamily + ";font-size:18px;font-weight:bold;color:" + text
                    + ";margin:24px 0 10px 0;line-height:1.4;";
        }

        String h4Style() {
            return "font-family:" + fontFamily + ";font-size:16px;font-weight:bold;color:" + text
                    + ";margin:20px 0 8px 0;line-height:1.45;";
        }

        String h5Style() {
            return "font-family:" + fontFamily + ";font-size:15px;font-weight:bold;color:" + muted
                    + ";margin:18px 0 8px 0;line-height:1.45;";
        }

        String h6Style() {
            return "font-family:" + fontFamily + ";font-size:14px;font-weight:bold;color:" + muted
                    + ";margin:16px 0 8px 0;line-height:1.45;";
        }

        String paragraphStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";margin:0 0 14px 0;line-height:1.75;";
        }

        String linkStyle() {
            return "font-family:" + fontFamily + ";color:" + link + ";text-decoration:underline;";
        }

        String inlineCodeStyle() {
            // 行内代码常含中文，使用界面字体避免缺字方框
            return "font-family:" + fontFamily + ";font-size:13px;color:" + codeText
                    + ";background-color:" + codeBg + ";padding:2px 6px;border:1px solid " + border + ";";
        }

        String preStyle() {
            return "font-family:" + monoFamily + ";font-size:13px;color:" + codeText
                    + ";background-color:" + codeBg + ";border:1px solid " + border
                    + ";padding:14px 16px;margin:0 0 16px 0;line-height:1.55;white-space:pre-wrap;";
        }

        String preCodeStyle() {
            return "font-family:" + monoFamily + ";font-size:13px;color:" + codeText
                    + ";background-color:" + codeBg + ";padding:0;margin:0;border:0;";
        }

        String prePlainStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";background-color:" + codeBg + ";border:1px solid " + border
                    + ";padding:14px 16px;margin:0 0 16px 0;line-height:1.7;white-space:pre-wrap;";
        }

        String prePlainCodeStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";background-color:" + codeBg + ";padding:0;margin:0;border:0;";
        }

        String blockquoteStyle() {
            return "font-family:" + fontFamily + ";color:" + muted + ";background-color:" + surface
                    + ";border-left:4px solid " + quoteBorder + ";margin:0 0 16px 0;padding:12px 16px;";
        }

        String blockquoteParagraphStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + muted
                    + ";margin:0 0 8px 0;line-height:1.7;";
        }

        String listStyle() {
            return "font-family:" + fontFamily + ";color:" + text + ";margin:0 0 14px 0;padding-left:28px;";
        }

        String listItemStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";margin:0 0 6px 0;line-height:1.7;";
        }

        String tableStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";border-collapse:collapse;width:100%;margin:0 0 16px 0;border:1px solid " + border + ";";
        }

        String tableHeadStyle() {
            return "background-color:" + surface + ";";
        }

        String thStyle() {
            return "font-family:" + fontFamily + ";font-size:13px;font-weight:bold;color:" + text
                    + ";background-color:" + surface + ";border:1px solid " + border
                    + ";padding:10px 14px;text-align:left;";
        }

        String tdStyle() {
            return "font-family:" + fontFamily + ";font-size:14px;color:" + text
                    + ";border:1px solid " + border + ";padding:10px 14px;";
        }

        String trStyle() {
            return "background-color:" + background + ";";
        }

        String hrStyle() {
            return "border:0;height:1px;background-color:" + border + ";margin:24px 0;";
        }

        String imageStyle() {
            return "max-width:100%;margin:8px 0 16px 0;border:1px solid " + border + ";";
        }

        String strongStyle() {
            return "font-weight:bold;color:" + text + ";";
        }

        String emStyle() {
            return "font-style:italic;color:" + text + ";";
        }

        String delStyle() {
            return "color:" + muted + ";text-decoration:line-through;";
        }

        private static Color getUiColor(String key, Color fallback) {
            Color color = UIManager.getColor(key);
            return color != null ? color : fallback;
        }

        private static String resolveFontFamily() {
            Font font = UIManager.getFont("Label.font");
            if (font == null) {
                return "sans-serif";
            }
            return font.getFamily();
        }

        private static String toCssFontFamily(String family) {
            if (family == null || family.isBlank()
                    || "monospace".equals(family) || "sans-serif".equals(family)) {
                return family;
            }
            return "'" + family + "'";
        }

        private static String toHex(Color color) {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }

        private static String blend(Color base, Color overlay, double ratio) {
            int r = (int) (base.getRed() * (1 - ratio) + overlay.getRed() * ratio);
            int g = (int) (base.getGreen() * (1 - ratio) + overlay.getGreen() * ratio);
            int b = (int) (base.getBlue() * (1 - ratio) + overlay.getBlue() * ratio);
            return toHex(new Color(r, g, b));
        }
    }
}
