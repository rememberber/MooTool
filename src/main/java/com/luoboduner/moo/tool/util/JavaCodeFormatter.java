package com.luoboduner.moo.tool.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

public class JavaCodeFormatter {

    public static String format(String input) {
        try {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(input).getResult().orElseThrow(() -> new RuntimeException("Parsing failed"));
            DefaultPrinterConfiguration config = new DefaultPrinterConfiguration();
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter(config);
            return prettyPrinter.print(cu);
        } catch (Exception e) {
            throw new RuntimeException("Error formatting Java code", e);
        }
    }

    public static void main(String[] args) {
        String input = "public class Test {public static void main(String[] args) {System.out.println(\"Hello, world!\");}}";
        String output = format(input);
        System.out.println(output);
    }
}