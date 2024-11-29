package com.luoboduner.moo.tool.util.codeformatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities.EscapeMode;

public class HtmlCodeFormatter implements CodeFormatter {

    public String format(String input) {
        try {
            Document document = Jsoup.parse(input);
            document.outputSettings(new OutputSettings().prettyPrint(true).indentAmount(4).escapeMode(EscapeMode.xhtml));
            return document.html();
        } catch (Exception e) {
            throw new RuntimeException("Error formatting HTML code", e);
        }
    }

}