package com.luoboduner.moo.tool.util.codeformatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
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

/**
 * XML格式化工具
 *
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 22:15
 */
public final class XmlFormatting {

    private XmlFormatting() {
    }

    public static String formatString(String xml, int indent) throws Exception {
        try {
            return formatWithJsoup(xml, indent);
        } catch (Exception ignore) {
            // fallback to JAXP secure DOM formatting
            org.w3c.dom.Document document = parseString(xml);
            return transform(document, indent);
        }
    }

    public static String formatFile(File file, int indent) throws Exception {
        try {
            String content = readFileUtf8(file);
            return formatWithJsoup(content, indent);
        } catch (Exception ignore) {
            org.w3c.dom.Document document = parseFile(file);
            return transform(document, indent);
        }
    }

    private static String formatWithJsoup(String xml, int indent) throws Exception {
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        doc.outputSettings(new Document.OutputSettings()
                .prettyPrint(true)
                .indentAmount(indent)
                .syntax(Document.OutputSettings.Syntax.xml));
        return doc.outerHtml();
    }

    private static org.w3c.dom.Document parseString(String xml) throws Exception {
        DocumentBuilderFactory factory = secureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource source = new InputSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        org.w3c.dom.Document document = builder.parse(source);
        normalize(document);
        return document;
    }

    private static org.w3c.dom.Document parseFile(File file) throws Exception {
        DocumentBuilderFactory factory = secureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document document = builder.parse(file);
        normalize(document);
        return document;
    }

    private static void normalize(org.w3c.dom.Document document) {
        Element rootElement = document.getDocumentElement();
        document.setXmlStandalone(true);
        document.setXmlVersion("1.0");
        if (rootElement != null) {
            rootElement.normalize();
        }
    }

    private static String transform(org.w3c.dom.Document document, int indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Throwable ignore) {
            // Some TransformerFactory implementations may not support this feature; ignore to keep compatibility
        }
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));

        StringWriter res = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(res));
        return res.toString();
    }

    private static String readFileUtf8(File file) throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        return new String(bytes, "UTF-8");
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        try {
            // Enable secure processing
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Throwable ignore) {
            // keep going for max compatibility with various parsers
        }
        try {
            // Disallow loading external DTDs and external entities to avoid blocking/XXE
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Throwable ignore) {
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Throwable ignore) {
        }
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Throwable ignore) {
        }
        // Do not expand entity references automatically
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
