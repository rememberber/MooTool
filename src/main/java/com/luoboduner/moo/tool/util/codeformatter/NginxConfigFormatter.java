package com.luoboduner.moo.tool.util.codeformatter;

public class NginxConfigFormatter implements CodeFormatter {

    public String format(String input) {
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

    private void appendIndentedLine(StringBuilder builder, String line, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            builder.append("    "); // 4 spaces for each indent level
        }
        builder.append(line).append(System.lineSeparator());
    }

}