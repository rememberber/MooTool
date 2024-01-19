package com.luoboduner.moo.tool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;

public class XmlReformatUtil {
    // log
    private static final Logger logger = LoggerFactory.getLogger(XmlReformatUtil.class);

    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    public static String reformat(File file, int spaceNum) throws Exception {
        StringWriter res = new StringWriter();
        try {
            // 创建DocumentBuilder对象
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析XML文件并返回Document对象
            Document document = builder.parse(file);
            // 获取文档的根元素
            Element rootElement = document.getDocumentElement();

            // 设置格式化属性，这里使用缩进和换行
            document.setXmlStandalone(true);
            document.setXmlVersion("1.0");
            rootElement.normalize();

            // 创建Transformer对象
            Transformer transformer = transformerFactory.newTransformer();

            // 设置输出格式化属性，这里使用缩进和换行
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(spaceNum));

            // 创建DOMSource对象
            DOMSource source = new DOMSource(document);

            // 创建StreamResult对象，指定输出位置
            StreamResult result = new StreamResult(res);

            // 格式化XML并输出结果
            transformer.transform(source, result);

            return res.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            res.close();
        }
    }

    public static String format(String xmlStr) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new ByteArrayInputStream(xmlStr.getBytes("utf-8"))));
            Element rootElement = document.getDocumentElement();
            document.setXmlStandalone(true);
            document.setXmlVersion("1.0");
            rootElement.normalize();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(document);
            StringWriter res = new StringWriter();
            StreamResult result = new StreamResult(res);
            transformer.transform(source, result);
            return res.toString();
        } catch (Exception e) {
            logger.error("XML格式化失败", e);
            return xmlStr;
        }
    }
}
