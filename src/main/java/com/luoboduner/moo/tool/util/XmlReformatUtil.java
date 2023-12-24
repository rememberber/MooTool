package com.luoboduner.moo.tool.util;

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

public class XmlReformatUtil {

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
}
