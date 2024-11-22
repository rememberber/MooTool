package com.luoboduner.moo.tool.util.codeformatter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

public class JavaCodeFormatter implements CodeFormatter {

    @Override
    public String format(String input) {
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

}