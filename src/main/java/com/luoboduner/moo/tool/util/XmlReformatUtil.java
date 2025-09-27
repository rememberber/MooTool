package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.util.codeformatter.XmlFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class XmlReformatUtil {
    // log
    private static final Logger logger = LoggerFactory.getLogger(XmlReformatUtil.class);

    public static String reformat(File file, int spaceNum) throws Exception {
        return XmlFormatting.formatFile(file, spaceNum);
    }

    public static String format(String xmlStr) {
        try {
            return XmlFormatting.formatString(xmlStr, 4);
        } catch (Exception e) {
            logger.error("XML格式化失败", e);
            return xmlStr;
        }
    }
}
