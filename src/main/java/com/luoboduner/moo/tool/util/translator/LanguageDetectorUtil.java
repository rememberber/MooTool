//package com.luoboduner.moo.tool.util;
//
//import org.apache.tika.language.detect.LanguageDetector;
//import org.apache.tika.language.detect.LanguageResult;
//
///**
// * 语言检测工具类
// * <p>
// * 依赖 tika-core
// * <p>
// * https://mvnrepository.com/artifact/org.apache.tika/tika-core
// * <p>
// * https://tika.apache.org/
// * <p>
// */
//public class LanguageDetectorUtil {
//
//    private static final LanguageDetector detector = LanguageDetector.getDefaultLanguageDetector();
//
//    static {
//        try {
//            detector.loadModels();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static String detectLanguage(String text) {
//        LanguageResult result = detector.detect(text);
//        return result.getLanguage();
//    }
//
//    public static void main(String[] args) {
//        String text = "This is a test sentence.";
//        String language = detectLanguage(text);
//        System.out.println("Detected language: " + language);
//
//        text = "这是一个测试句子。";
//        language = detectLanguage(text);
//        System.out.println("Detected language: " + language);
//    }
//}