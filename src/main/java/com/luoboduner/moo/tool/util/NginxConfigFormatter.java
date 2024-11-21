package com.luoboduner.moo.tool.util;

public class NginxConfigFormatter {

    public static String format(String input) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = input.split("\n");
        int indentLevel = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("}")) {
                indentLevel--;
            }
            appendIndentedLine(formatted, line, indentLevel);
            if (line.endsWith("{")) {
                indentLevel++;
            }
        }

        return formatted.toString();
    }

    private static void appendIndentedLine(StringBuilder builder, String line, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            builder.append("    "); // 4 spaces for each indent level
        }
        builder.append(line).append(System.lineSeparator());
    }

    public static void main(String[] args) {
        String input = "server {\n    listen 80;\n    server_name example.com;\n    location / {\n        proxy_pass http://localhost:8080;\n    }\n}";
        String output = format(input);
        System.out.println(output);
    }
}