package com.luoboduner.moo.tool.util;

import de.hunsicker.jalopy.Jalopy;
import de.hunsicker.jalopy.storage.Convention;
import de.hunsicker.jalopy.storage.ConventionKeys;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;

public class FileReformatUtil {

    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static Jalopy jalopy = new Jalopy();

    public static String reformatXML(File file, int spaceNum) throws Exception {
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
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
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
