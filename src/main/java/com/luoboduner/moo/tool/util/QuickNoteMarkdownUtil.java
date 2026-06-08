package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.FlatLaf;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * 随手记 Markdown 转 HTML 工具
 */
public class QuickNoteMarkdownUtil {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        ));
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
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
        return "<html><head><style>" + buildStyles() + "</style></head><body>" + body + "</body></html>";
    }

    private static String buildStyles() {
        Color textColor = getUiColor("Label.foreground", Color.BLACK);
        Color background = getUiColor("Editor.background", Color.WHITE);
        Color borderColor = getUiColor("Component.borderColor", new Color(200, 200, 200));
        Color codeBackground = FlatLaf.isLafDark()
                ? new Color(45, 45, 45)
                : new Color(246, 248, 250);
        Color linkColor = getUiColor("Component.linkColor", new Color(9, 105, 218));
        Color quoteColor = FlatLaf.isLafDark()
                ? new Color(180, 180, 180)
                : new Color(106, 115, 125);
        String fontFamily = resolveFontFamily();

        return "body{font-family:" + fontFamily + ";font-size:14px;color:rgb(" + toRgb(textColor)
                + ");background:rgb(" + toRgb(background) + ");padding:12px 16px;line-height:1.6;margin:0;}"
                + "h1{font-size:2em;font-weight:600;border-bottom:1px solid rgb(" + toRgb(borderColor)
                + ");padding-bottom:0.3em;margin-top:0.8em;margin-bottom:0.6em;}"
                + "h2{font-size:1.5em;font-weight:600;border-bottom:1px solid rgb(" + toRgb(borderColor)
                + ");padding-bottom:0.3em;margin-top:0.8em;margin-bottom:0.6em;}"
                + "h3{font-size:1.25em;font-weight:600;margin-top:0.8em;margin-bottom:0.5em;}"
                + "h4,h5,h6{font-weight:600;margin-top:0.8em;margin-bottom:0.5em;}"
                + "p{margin:0.6em 0;}"
                + "a{color:rgb(" + toRgb(linkColor) + ");text-decoration:none;}"
                + "a:hover{text-decoration:underline;}"
                + "code{font-family:monospace;background:rgb(" + toRgb(codeBackground)
                + ");padding:2px 5px;border-radius:4px;font-size:0.9em;}"
                + "pre{background:rgb(" + toRgb(codeBackground) + ");padding:12px;border-radius:6px;"
                + "border:1px solid rgb(" + toRgb(borderColor) + ");overflow:auto;}"
                + "pre code{background:none;padding:0;border-radius:0;}"
                + "blockquote{border-left:4px solid rgb(" + toRgb(borderColor) + ");margin:0.6em 0;"
                + "padding:0 1em;color:rgb(" + toRgb(quoteColor) + ");}"
                + "ul,ol{padding-left:1.6em;margin:0.6em 0;}"
                + "li{margin:0.2em 0;}"
                + "table{border-collapse:collapse;margin:0.8em 0;width:100%;}"
                + "th,td{border:1px solid rgb(" + toRgb(borderColor) + ");padding:6px 12px;}"
                + "th{background:rgb(" + toRgb(codeBackground) + ");font-weight:600;}"
                + "hr{border:none;border-top:1px solid rgb(" + toRgb(borderColor) + ");margin:1em 0;}"
                + "img{max-width:100%;}"
                + "del{opacity:0.7;}";
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

    private static String toRgb(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
}
