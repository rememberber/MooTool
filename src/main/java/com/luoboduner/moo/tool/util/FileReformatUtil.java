package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.util.codeformatter.XmlFormatting;
import de.hunsicker.jalopy.Jalopy;
import de.hunsicker.jalopy.storage.Convention;
import de.hunsicker.jalopy.storage.ConventionKeys;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.StringWriter;

public class FileReformatUtil {

    private static Jalopy jalopy = new Jalopy();

    public static String reformatXML(File file, int spaceNum) throws Exception {
        return XmlFormatting.formatFile(file, spaceNum);
    }

    public static String reformatHTML(File file, int spaceNum) throws Exception {
        return Jsoup.parse(file, "UTF-8")
                .outputSettings(new org.jsoup.nodes.Document.OutputSettings().indentAmount(spaceNum))
                .outerHtml();
    }

    public static String reformatJAVA(File file, int spaceNum) throws Exception {
        Convention instance = Convention.getInstance();
        instance.put(ConventionKeys.SPACE_BEFORE_BRACES, "true");
        instance.put(ConventionKeys.SPACE_BEFORE_BRACKETS, "true");
        instance.put(ConventionKeys.INDENT_SIZE, String.valueOf(spaceNum));

        try (StringWriter stringWriter = new StringWriter()) {
            jalopy.setInput(file);
            jalopy.setOutput(stringWriter);
            boolean result = jalopy.format();
            if (!result) {
                throw new Exception("格式化失败！");
            }

            return stringWriter.toString();
        } catch (Exception e) {
            throw e;
        }
    }
}
