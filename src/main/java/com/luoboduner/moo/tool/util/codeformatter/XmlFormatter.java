package com.luoboduner.moo.tool.util.codeformatter;

public class XmlFormatter implements CodeFormatter {

    public String format(String input) {
        try {
            // Delegate to centralized safe XML formatter with default indent 4
            return XmlFormatting.formatString(input, 4);
        } catch (Exception e) {
            throw new RuntimeException("Error formatting XML", e);
        }
    }

}
