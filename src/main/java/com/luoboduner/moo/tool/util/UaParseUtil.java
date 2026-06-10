package com.luoboduner.moo.tool.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-Agent 解析工具
 */
public class UaParseUtil {

    private static final String BROWSER_WECHAT = "WeChat";

    private static final Pattern BOT_PATTERN = Pattern.compile(
            "bot|spider|crawl|slurp|archiver|wget|curl|python-requests|java/|okhttp|go-http|libwww|httpclient",
            Pattern.CASE_INSENSITIVE);

    private UaParseUtil() {
    }

    public static Map<String, String> parse(String userAgent) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(userAgent)) {
            result.put(I18n.get("ua.field.error"), I18n.get("ua.error.empty"));
            return result;
        }

        String ua = userAgent.trim();
        boolean bot = isBot(ua);
        boolean mobile = isMobile(ua);

        String os = parseOs(ua);
        String osVersion = parseOsVersion(ua, os);
        String browser = parseBrowser(ua);
        String browserVersion = parseBrowserVersion(ua, browser);
        String engine = parseEngine(ua);
        String engineVersion = parseEngineVersion(ua, engine);
        String deviceType = parseDeviceType(ua, mobile, bot);
        String deviceBrand = parseDeviceBrand(ua);
        String deviceModel = parseDeviceModel(ua);

        result.put(I18n.get("ua.field.browser"), localizeBrowserName(browser));
        result.put(I18n.get("ua.field.browserVersion"), browserVersion);
        result.put(I18n.get("ua.field.engine"), engine);
        result.put(I18n.get("ua.field.engineVersion"), engineVersion);
        result.put(I18n.get("ua.field.os"), os);
        result.put(I18n.get("ua.field.osVersion"), osVersion);
        result.put(I18n.get("ua.field.deviceType"), deviceType);
        result.put(I18n.get("ua.field.deviceBrand"), deviceBrand);
        result.put(I18n.get("ua.field.deviceModel"), deviceModel);
        result.put(I18n.get("ua.field.mobile"), mobile ? I18n.get("common.yes") : I18n.get("common.no"));
        result.put(I18n.get("ua.field.bot"), bot ? I18n.get("common.yes") : I18n.get("common.no"));
        return result;
    }

    public static List<String[]> presetUserAgents() {
        List<String[]> presets = new ArrayList<>();
        presets.add(new String[]{"Chrome (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"});
        presets.add(new String[]{"Chrome (macOS)", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"});
        presets.add(new String[]{"Firefox (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0"});
        presets.add(new String[]{"Safari (macOS)", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Safari/605.1.15"});
        presets.add(new String[]{"Edge (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"});
        presets.add(new String[]{"Chrome (Android)", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"});
        presets.add(new String[]{"Safari (iPhone)", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1"});
        presets.add(new String[]{"Safari (iPad)", "Mozilla/5.0 (iPad; CPU OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1"});
        presets.add(new String[]{I18n.get("ua.preset.wechat"), "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.56(0x1800383b) NetType/WIFI Language/zh_CN"});
        presets.add(new String[]{"curl", "curl/8.7.1"});
        return presets;
    }

    private static String localizeBrowserName(String browser) {
        if (BROWSER_WECHAT.equals(browser)) {
            return I18n.get("ua.preset.wechat");
        }
        return browser;
    }

    private static boolean isBot(String ua) {
        return BOT_PATTERN.matcher(ua).find();
    }

    private static boolean isMobile(String ua) {
        return ua.contains("Mobile")
                || ua.contains("Android")
                || ua.contains("iPhone")
                || ua.contains("iPad")
                || ua.contains("Windows Phone");
    }

    private static String parseOs(String ua) {
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            return "iOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        if (ua.contains("CrOS")) {
            return "Chrome OS";
        }
        return "未知";
    }

    private static String parseOsVersion(String ua, String os) {
        Matcher matcher;
        switch (os) {
            case "Windows":
                matcher = Pattern.compile("Windows NT ([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return mapWindowsVersion(matcher.group(1));
                }
                matcher = Pattern.compile("Windows ([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "macOS":
                matcher = Pattern.compile("Mac OS X ([\\d_]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1).replace('_', '.');
                }
                break;
            case "iOS":
                matcher = Pattern.compile("(?:iPhone OS|CPU OS) ([\\d_]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1).replace('_', '.');
                }
                break;
            case "Android":
                matcher = Pattern.compile("Android ([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Linux":
                matcher = Pattern.compile("Linux ([^;)]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
                break;
            default:
                break;
        }
        return "未知";
    }

    private static String mapWindowsVersion(String ntVersion) {
        switch (ntVersion) {
            case "10.0":
                return "10/11";
            case "6.3":
                return "8.1";
            case "6.2":
                return "8";
            case "6.1":
                return "7";
            case "6.0":
                return "Vista";
            case "5.1":
                return "XP";
            default:
                return ntVersion;
        }
    }

    private static String parseBrowser(String ua) {
        if (ua.contains("MicroMessenger")) {
            return BROWSER_WECHAT;
        }
        if (ua.contains("Edg/") || ua.contains("Edge/")) {
            return "Microsoft Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/") && !ua.contains("Chromium")) {
            return "Chrome";
        }
        if (ua.contains("Safari/") && !ua.contains("Chrome/") && !ua.contains("Chromium")) {
            return "Safari";
        }
        if (ua.contains("MSIE") || ua.contains("Trident/")) {
            return "Internet Explorer";
        }
        if (ua.startsWith("curl/")) {
            return "curl";
        }
        if (ua.contains("PostmanRuntime")) {
            return "Postman";
        }
        return "未知";
    }

    private static String parseBrowserVersion(String ua, String browser) {
        Matcher matcher;
        switch (browser) {
            case "Microsoft Edge":
                matcher = Pattern.compile("(?:Edg|Edge)/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Opera":
                matcher = Pattern.compile("OPR/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Firefox":
                matcher = Pattern.compile("Firefox/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Chrome":
                matcher = Pattern.compile("Chrome/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Safari":
                matcher = Pattern.compile("Version/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                matcher = Pattern.compile("Safari/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Internet Explorer":
                matcher = Pattern.compile("MSIE ([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                matcher = Pattern.compile("rv:([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case BROWSER_WECHAT:
                matcher = Pattern.compile("MicroMessenger/([\\d.()]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "curl":
                matcher = Pattern.compile("curl/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            default:
                break;
        }
        return "未知";
    }

    private static String parseEngine(String ua) {
        if (ua.contains("Gecko/")) {
            return "Gecko";
        }
        if (ua.contains("AppleWebKit/")) {
            return "WebKit";
        }
        if (ua.contains("Trident/")) {
            return "Trident";
        }
        return "未知";
    }

    private static String parseEngineVersion(String ua, String engine) {
        Matcher matcher;
        switch (engine) {
            case "Gecko":
                matcher = Pattern.compile("Gecko/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "WebKit":
                matcher = Pattern.compile("AppleWebKit/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            case "Trident":
                matcher = Pattern.compile("Trident/([\\d.]+)").matcher(ua);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                break;
            default:
                break;
        }
        return "未知";
    }

    private static String parseDeviceType(String ua, boolean mobile, boolean bot) {
        if (bot) {
            return "Bot/爬虫";
        }
        if (ua.contains("iPad") || ua.contains("Tablet")) {
            return "平板";
        }
        if (mobile) {
            return "手机";
        }
        return "桌面";
    }

    private static String parseDeviceBrand(String ua) {
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            return "Apple";
        }
        Matcher matcher = Pattern.compile("Android [\\d.]+; ([^;)]+)").matcher(ua);
        if (matcher.find()) {
            String device = matcher.group(1).trim();
            if (device.contains("Build/")) {
                device = device.substring(0, device.indexOf("Build/")).trim();
            }
            String[] parts = device.split("\\s+");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "未知";
    }

    private static String parseDeviceModel(String ua) {
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        Matcher matcher = Pattern.compile("Android [\\d.]+; ([^;)]+)").matcher(ua);
        if (matcher.find()) {
            String device = matcher.group(1).trim();
            if (device.contains("Build/")) {
                device = device.substring(0, device.indexOf("Build/")).trim();
            }
            return device;
        }
        return "未知";
    }
}
