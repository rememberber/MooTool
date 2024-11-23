package com.luoboduner.moo.tool.util.codeformatter;

public class CodeFormatterFactory {
    public enum FormatterType {
        JAVA,
        XML,
        HTML,
        NGINX
    }

    public static CodeFormatter getFormatter(FormatterType type) {
        switch (type) {
            case JAVA:
                return new JavaCodeFormatter();
            case XML:
                return new XmlFormatter();
            case HTML:
                return new HtmlCodeFormatter();
            case NGINX:
                return new NginxConfigFormatter();

            default:
                throw new IllegalArgumentException("Unknown formatter type: " + type);
        }
    }
}
