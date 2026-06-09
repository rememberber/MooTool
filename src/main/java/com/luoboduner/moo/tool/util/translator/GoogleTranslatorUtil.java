package com.luoboduner.moo.tool.util.translator;

import cn.hutool.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 翻译工具类
 */
@Slf4j
public class GoogleTranslatorUtil implements Translator {

    private static final int MAX_CHUNK_SIZE = 1800;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public String translate(String word, String sourceLanguage, String targetLanguage) {
        try {
            if (StringUtils.isEmpty(word)) {
                return "";
            }
            if (StringUtils.isEmpty(sourceLanguage)) {
                sourceLanguage = "auto";
            }
            if (StringUtils.isEmpty(targetLanguage)) {
                targetLanguage = "zh-CN";
            }

            if (word.length() <= MAX_CHUNK_SIZE) {
                return translateChunk(word, sourceLanguage, targetLanguage);
            }

            StringBuilder result = new StringBuilder();
            for (String chunk : splitText(word)) {
                result.append(translateChunk(chunk, sourceLanguage, targetLanguage));
            }
            return result.toString();
        } catch (SSLHandshakeException e) {
            log.error("SSLHandshakeException", e);
            return "访问Google翻译接口网络异常：" + e.getMessage();
        } catch (SocketTimeoutException e) {
            log.error("SocketTimeoutException", e);
            return "访问Google翻译接口超时：" + e.getMessage();
        } catch (Exception e) {
            log.error("访问Google翻译异常", e);
            return "访问Google翻译接口异常：" + e.getMessage();
        }
    }

    static List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        String[] paragraphs = text.split("(\r?\n)", -1);
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > MAX_CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                for (int i = 0; i < paragraph.length(); i += MAX_CHUNK_SIZE) {
                    chunks.add(paragraph.substring(i, Math.min(i + MAX_CHUNK_SIZE, paragraph.length())));
                }
                continue;
            }

            int extra = current.length() == 0 ? 0 : 1;
            if (current.length() + extra + paragraph.length() > MAX_CHUNK_SIZE) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(paragraph);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private String translateChunk(String word, String sourceLanguage, String targetLanguage) throws Exception {
        String url = "https://translate.googleapis.com/translate_a/single?" +
                "client=gtx&" +
                "sl=" + sourceLanguage +
                "&tl=" + targetLanguage +
                "&dt=t&q=" + URLEncoder.encode(word, StandardCharsets.UTF_8);

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(10000);
        con.setRequestProperty("User-Agent", USER_AGENT);

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return parseResult(response.toString());
    }

    private static String parseResult(String inputJson) {
        JSONArray jsonArray2 = (JSONArray) new JSONArray(inputJson).get(0);
        StringBuilder result = new StringBuilder();
        for (Object o : jsonArray2) {
            result.append(((JSONArray) o).get(0).toString());
        }
        return result.toString();
    }
}
