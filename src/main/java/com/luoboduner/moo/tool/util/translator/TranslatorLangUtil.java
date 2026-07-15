package com.luoboduner.moo.tool.util.translator;

import com.luoboduner.moo.tool.util.I18n;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translation language codes and localized display names.
 */
public final class TranslatorLangUtil {

    public static final String AUTO_DETECT_CODE = "auto";
    public static final String DEFAULT_TARGET_CODE = "zh-CN";

    private static final String[] SOURCE_CODES = {
            AUTO_DETECT_CODE, "zh-CN", "en", "yue", "wyw", "jp", "kor", "fra", "spa", "th", "ara", "ru", "pt",
            "de", "it", "el", "nl", "pl", "bul", "est", "dan", "fin", "cs", "rom", "slo", "swe", "hu", "cht", "vie"
    };

    private static final Map<String, String> LEGACY_ZH_NAME_TO_CODE = legacyZhNameToCode();

    private TranslatorLangUtil() {
    }

    private static Map<String, String> legacyZhNameToCode() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("自动检测", AUTO_DETECT_CODE);
        map.put("中文（简体）", "zh-CN");
        map.put("英语", "en");
        map.put("粤语", "yue");
        map.put("文言文", "wyw");
        map.put("日语", "jp");
        map.put("韩语", "kor");
        map.put("法语", "fra");
        map.put("西班牙语", "spa");
        map.put("泰语", "th");
        map.put("阿拉伯语", "ara");
        map.put("俄语", "ru");
        map.put("葡萄牙语", "pt");
        map.put("德语", "de");
        map.put("意大利语", "it");
        map.put("希腊语", "el");
        map.put("荷兰语", "nl");
        map.put("波兰语", "pl");
        map.put("保加利亚语", "bul");
        map.put("爱沙尼亚语", "est");
        map.put("丹麦语", "dan");
        map.put("芬兰语", "fin");
        map.put("捷克语", "cs");
        map.put("罗马尼亚语", "rom");
        map.put("斯洛文尼亚语", "slo");
        map.put("瑞典语", "swe");
        map.put("匈牙利语", "hu");
        map.put("繁体中文", "cht");
        map.put("越南语", "vie");
        return map;
    }

    public static String getAutoDetectLabel() {
        return getDisplayName(AUTO_DETECT_CODE);
    }

    public static String getDisplayName(String code) {
        if (StringUtils.isBlank(code)) {
            return getAutoDetectLabel();
        }
        return I18n.get(propertyKey(code));
    }

    public static String toDisplayName(String codeOrLegacyName) {
        if (StringUtils.isBlank(codeOrLegacyName)) {
            return getAutoDetectLabel();
        }
        String code = resolveCode(codeOrLegacyName, null);
        if (code != null) {
            return getDisplayName(code);
        }
        return codeOrLegacyName;
    }

    public static String[] getSourceLanguageNames() {
        String[] names = new String[SOURCE_CODES.length];
        for (int i = 0; i < SOURCE_CODES.length; i++) {
            names[i] = getDisplayName(SOURCE_CODES[i]);
        }
        return names;
    }

    public static String[] getTargetLanguageNames() {
        String[] names = new String[SOURCE_CODES.length - 1];
        int index = 0;
        for (String code : SOURCE_CODES) {
            if (!AUTO_DETECT_CODE.equals(code)) {
                names[index++] = getDisplayName(code);
            }
        }
        return names;
    }

    public static String resolveCode(String languageNameOrCode, String defaultCode) {
        if (StringUtils.isBlank(languageNameOrCode)) {
            return defaultCode;
        }
        if (isKnownCode(languageNameOrCode)) {
            return languageNameOrCode;
        }
        for (String code : SOURCE_CODES) {
            if (languageNameOrCode.equals(getDisplayName(code))) {
                return code;
            }
        }
        String legacyCode = LEGACY_ZH_NAME_TO_CODE.get(languageNameOrCode);
        if (legacyCode != null) {
            return legacyCode;
        }
        return defaultCode;
    }

    public static boolean isAutoDetectLabel(String label) {
        return AUTO_DETECT_CODE.equals(resolveCode(label, null));
    }

    public static void selectComboByCode(JComboBox<String> comboBox, String code) {
        if (comboBox == null || StringUtils.isBlank(code)) {
            return;
        }
        String label = getDisplayName(code);
        comboBox.setSelectedItem(label);
    }

    private static boolean isKnownCode(String value) {
        for (String code : SOURCE_CODES) {
            if (code.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String propertyKey(String code) {
        return "translation.lang." + code.replace('-', '_');
    }
}
