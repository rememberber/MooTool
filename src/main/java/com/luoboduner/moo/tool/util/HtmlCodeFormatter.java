package com.luoboduner.moo.tool.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities.EscapeMode;

public class HtmlCodeFormatter {

    public static String format(String input) {
        try {
            Document document = Jsoup.parse(input);
            document.outputSettings(new OutputSettings().prettyPrint(true).indentAmount(4).escapeMode(EscapeMode.xhtml));
            return document.html();
        } catch (Exception e) {
            throw new RuntimeException("Error formatting HTML code", e);
        }
    }

    public static void main(String[] args) {
        String input = "<html><body><h1>Hello, world!</h1></body></html>";
        String output = format(input);
        System.out.println(output);
    }
}