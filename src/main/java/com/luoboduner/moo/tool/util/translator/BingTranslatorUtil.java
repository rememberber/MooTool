package com.luoboduner.moo.tool.util.translator;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bing翻译工具类 (使用Bing翻译网页版API，在中国大陆可访问)
 */
@Slf4j
public class BingTranslatorUtil implements Translator {

    private static final String BING_HOST = "https://cn.bing.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0";
    private static final Pattern IG_PATTERN = Pattern.compile("IG:\"([A-F0-9]{32})\"");
    private static final Pattern ABUSE_PREVENTION_PATTERN = Pattern.compile(
            "params_AbusePreventionHelper\\s*=\\s*\\[(\\d+),\"([^\"]+)\",(\\d+)\\]");

    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final Object SESSION_LOCK = new Object();
    private static BingSession cachedSession;

    static {
        CookieHandler.setDefault(COOKIE_MANAGER);
    }

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

            sourceLanguage = convertToBingLanguageCode(sourceLanguage);
            targetLanguage = convertToBingLanguageCode(targetLanguage);

            BingSession session = getSession();
            String postData = "fromLang=" + sourceLanguage +
                    "&to=" + targetLanguage +
                    "&text=" + URLEncoder.encode(word, StandardCharsets.UTF_8) +
                    "&token=" + URLEncoder.encode(session.token, StandardCharsets.UTF_8) +
                    "&key=" + session.key +
                    "&tryFetchingGenderDebiasedTranslations=true";

            String url = BING_HOST + "/ttranslatev3?isVertical=1&IG=" + session.ig +
                    "&IID=translator.5026." + session.nextRequestId();

            log.debug("Bing translation request - from: {}, to: {}, text length: {}", sourceLanguage, targetLanguage, word.length());

            HttpURLConnection con = openConnection(url, "POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Referer", BING_HOST + "/translator");
            con.setRequestProperty("Origin", BING_HOST);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = con.getResponseCode();
            String responseStr = readResponseBody(con, responseCode);
            log.debug("Bing API response code: {}", responseCode);

            if (responseCode != 200) {
                log.warn("Bing API error response (code {}): {}", responseCode, responseStr);
                return "Bing翻译接口返回错误状态码: " + responseCode;
            }

            if (responseStr.contains("\"ShowCaptcha\":true")) {
                invalidateSession();
                return "Bing翻译触发验证码，请稍后重试或切换其他翻译源";
            }

            return parseResult(responseStr);
        } catch (SSLHandshakeException e) {
            log.error("SSLHandshakeException", e);
            return "访问Bing翻译接口网络异常：" + e.getMessage();
        } catch (SocketTimeoutException e) {
            log.error("SocketTimeoutException", e);
            return "访问Bing翻译接口超时：" + e.getMessage();
        } catch (Exception e) {
            log.error("访问Bing翻译异常", e);
            return "访问Bing翻译接口异常：" + e.getMessage();
        }
    }

    static BingSession parseSessionFromPage(String pageHtml) {
        Matcher igMatcher = IG_PATTERN.matcher(pageHtml);
        if (!igMatcher.find()) {
            throw new IllegalStateException("无法从Bing页面解析IG参数");
        }

        Matcher abuseMatcher = ABUSE_PREVENTION_PATTERN.matcher(pageHtml);
        if (!abuseMatcher.find()) {
            throw new IllegalStateException("无法从Bing页面解析token/key参数");
        }

        BingSession session = new BingSession();
        session.ig = igMatcher.group(1);
        session.key = Long.parseLong(abuseMatcher.group(1));
        session.token = abuseMatcher.group(2);
        long ttl = Long.parseLong(abuseMatcher.group(3));
        session.expireAt = System.currentTimeMillis() + ttl - 60_000;
        return session;
    }

    private BingSession getSession() throws IOException {
        synchronized (SESSION_LOCK) {
            if (cachedSession != null && System.currentTimeMillis() < cachedSession.expireAt) {
                return cachedSession;
            }
            cachedSession = refreshSession();
            return cachedSession;
        }
    }

    private void invalidateSession() {
        synchronized (SESSION_LOCK) {
            cachedSession = null;
        }
    }

    private BingSession refreshSession() throws IOException {
        HttpURLConnection con = openConnection(BING_HOST + "/translator", "GET");
        int responseCode = con.getResponseCode();
        String pageHtml = readResponseBody(con, responseCode);
        if (responseCode != 200) {
            throw new IOException("获取Bing翻译页面失败，状态码: " + responseCode);
        }
        return parseSessionFromPage(pageHtml);
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        URL obj = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(10_000);
        con.setReadTimeout(10_000);
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        return con;
    }

    private String readResponseBody(HttpURLConnection con, int responseCode) throws IOException {
        java.io.InputStream stream = responseCode >= 400 ? con.getErrorStream() : con.getInputStream();
        if (stream == null) {
            stream = con.getInputStream();
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    String convertToBingLanguageCode(String code) {
        if (StringUtils.isEmpty(code) || "auto".equals(code)) {
            return "auto-detect";
        }
        switch (code) {
            case "zh-CN":
                return "zh-Hans";
            case "cht":
                return "zh-Hant";
            case "jp":
                return "ja";
            case "kor":
                return "ko";
            case "fra":
                return "fr";
            case "spa":
                return "es";
            case "ara":
                return "ar";
            case "swe":
                return "sv";
            case "vie":
                return "vi";
            default:
                return code;
        }
    }

    private String parseResult(String inputJson) {
        try {
            if (StringUtils.isEmpty(inputJson)) {
                log.warn("Bing API returned empty response");
                return "翻译返回结果为空";
            }

            if (inputJson.contains("\"ShowCaptcha\"")) {
                return "Bing翻译触发验证码，请稍后重试或切换其他翻译源";
            }

            JSONArray jsonArray = new JSONArray(inputJson);
            if (!jsonArray.isEmpty()) {
                JSONObject firstItem = jsonArray.getJSONObject(0);
                if (firstItem.containsKey("translations")) {
                    JSONArray translations = firstItem.getJSONArray("translations");
                    if (!translations.isEmpty()) {
                        JSONObject translation = translations.getJSONObject(0);
                        String result = translation.getStr("text");
                        if (!StringUtils.isEmpty(result)) {
                            return result;
                        }
                    }
                }
            }
            log.warn("Bing API response format unexpected: {}", inputJson);
            return "解析翻译结果失败，返回格式不符合预期";
        } catch (Exception e) {
            log.error("解析翻译结果异常，原始响应: {}", inputJson, e);
            return "解析翻译结果异常：" + e.getMessage();
        }
    }

    static final class BingSession {
        String ig;
        String token;
        long key;
        long expireAt;
        int requestCount;

        int nextRequestId() {
            requestCount += 2;
            return requestCount;
        }
    }
}
