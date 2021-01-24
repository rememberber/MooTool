package com.luoboduner.moo.tool.util;

/**
 * <pre>
 * 配置管理
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/14.
 */
public class ConfigUtil extends ConfigBaseUtil {

    private static ConfigUtil configUtil = new ConfigUtil();

    public static ConfigUtil getInstance() {
        return configUtil;
    }

    private ConfigUtil() {
        super();
    }

    private boolean autoCheckUpdate;

    private String beforeVersion;

    private String theme;

    private String font;

    private int fontSize;

    private boolean httpUseProxy;

    private String httpProxyHost;

    private String httpProxyPort;

    private String httpProxyUserName;

    private String httpProxyPassword;

    /**
     * 菜单栏位置
     */
    private String menuBarPosition;

    /**
     * 上次关闭前所在的tab
     */
    private int recentTabIndex;

    private String quickNoteFontName;

    private int quickNoteFontSize;

    private String jsonBeautyFontName;

    private int jsonBeautyFontSize;

    private String currentHostName;

    private int qrCodeSize;

    private String qrCodeErrorCorrectionLevel;

    private String qrCodeLogoPath;

    private String qrCodeSaveAsPath;

    private String qrCodeRecognitionImagePath;

    private String digestFilePath;

    private int randomNumDigit;

    private int randomStringDigit;

    private int randomPasswordDigit;

    private String calculatorInputExpress;

    private String dbFilePath;

    private String dbFilePathBefore;

    private String quickNoteExportPath;

    private String jsonBeautyExportPath;

    private String imageExportPath;

    private String hostExportPath;

    private String lastSelectedColor;

    private String colorTheme;

    private String colorCodeType;

    public boolean isAutoCheckUpdate() {
        return setting.getBool("autoCheckUpdate", "setting.common", true);
    }

    public void setAutoCheckUpdate(boolean autoCheckUpdate) {
        setting.put("setting.common", "autoCheckUpdate", String.valueOf(autoCheckUpdate));
    }

    public int getRecentTabIndex() {
        return setting.getInt("recentTabIndex", "setting.common", 0);
    }

    public void setRecentTabIndex(int recentTabIndex) {
        setting.put("setting.common", "recentTabIndex", String.valueOf(recentTabIndex));
    }

    public String getBeforeVersion() {
        return setting.getStr("beforeVersion", "setting.common", "v0.0.0");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.put("setting.common", "beforeVersion", beforeVersion);
    }

    public String getTheme() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("theme", "setting.appearance", "系统默认");
        } else {
            return setting.getStr("theme", "setting.appearance", "Flat Darcula");
        }
    }

    public void setTheme(String theme) {
        setting.put("setting.appearance", "theme", theme);
    }

    public String getFont() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("font", "setting.appearance", "微软雅黑");
        }
    }

    public void setFont(String font) {
        setting.put("setting.appearance", "font", font);
    }

    public int getFontSize() {
        return setting.getInt("fontSize", "setting.appearance", 13);
    }

    public void setFontSize(int fontSize) {
        setting.put("setting.appearance", "fontSize", String.valueOf(fontSize));
    }

    public boolean isHttpUseProxy() {
        return setting.getBool("httpUseProxy", "setting.http", false);
    }

    public void setHttpUseProxy(boolean httpUseProxy) {
        setting.put("setting.http", "httpUseProxy", String.valueOf(httpUseProxy));
    }

    public String getHttpProxyHost() {
        return setting.getStr("httpProxyHost", "setting.http", "");
    }

    public void setHttpProxyHost(String httpProxyHost) {
        setting.put("setting.http", "httpProxyHost", httpProxyHost);
    }

    public String getHttpProxyPort() {
        return setting.getStr("httpProxyPort", "setting.http", "");
    }

    public void setHttpProxyPort(String httpProxyPort) {
        setting.put("setting.http", "httpProxyPort", httpProxyPort);
    }

    public String getHttpProxyUserName() {
        return setting.getStr("httpProxyUserName", "setting.http", "");
    }

    public void setHttpProxyUserName(String httpProxyUserName) {
        setting.put("setting.http", "httpProxyUserName", httpProxyUserName);
    }

    public String getHttpProxyPassword() {
        return setting.getStr("httpProxyPassword", "setting.http", "");
    }

    public void setHttpProxyPassword(String httpProxyPassword) {
        setting.put("setting.http", "httpProxyPassword", httpProxyPassword);
    }

    public String getQuickNoteFontName() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("quickNoteFontName", "func.quickNote", "等线");
        }
    }

    public void setQuickNoteFontName(String quickNoteFontName) {
        setting.put("func.quickNote", "quickNoteFontName", quickNoteFontName);
    }

    public int getQuickNoteFontSize() {
        return setting.getInt("quickNoteFontSize", "func.quickNote", 0);
    }

    public void setQuickNoteFontSize(int quickNoteFontSize) {
        setting.put("func.quickNote", "quickNoteFontSize", String.valueOf(quickNoteFontSize));
    }

    public String getJsonBeautyFontName() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("jsonBeautyFontName", "func.jsonBeauty", "等线");
        }
    }

    public void setJsonBeautyFontName(String jsonBeautyFontName) {
        setting.put("func.jsonBeauty", "jsonBeautyFontName", jsonBeautyFontName);
    }

    public int getJsonBeautyFontSize() {
        return setting.getInt("jsonBeautyFontSize", "func.jsonBeauty", 0);
    }

    public void setJsonBeautyFontSize(int jsonBeautyFontSize) {
        setting.put("func.jsonBeauty", "jsonBeautyFontSize", String.valueOf(jsonBeautyFontSize));
    }

    public int getQrCodeSize() {
        return setting.getInt("qrCodeSize", "func.qrCode", 320);
    }

    public void setQrCodeSize(int qrCodeSize) {
        setting.put("func.qrCode", "qrCodeSize", String.valueOf(qrCodeSize));
    }

    public String getQrCodeErrorCorrectionLevel() {
        return setting.getStr("qrCodeErrorCorrectionLevel", "func.qrCode", "中低");
    }

    public void setQrCodeErrorCorrectionLevel(String qrCodeErrorCorrectionLevel) {
        setting.put("func.qrCode", "qrCodeErrorCorrectionLevel", qrCodeErrorCorrectionLevel);
    }

    public String getQrCodeLogoPath() {
        return setting.getStr("qrCodeLogoPath", "func.qrCode", "");
    }

    public void setQrCodeLogoPath(String qrCodeLogoPath) {
        setting.put("func.qrCode", "qrCodeLogoPath", qrCodeLogoPath);
    }

    public String getQrCodeSaveAsPath() {
        return setting.getStr("qrCodeSaveAsPath", "func.qrCode", "");
    }

    public void setQrCodeSaveAsPath(String qrCodeSaveAsPath) {
        setting.put("func.qrCode", "qrCodeSaveAsPath", qrCodeSaveAsPath);
    }

    public String getQrCodeRecognitionImagePath() {
        return setting.getStr("qrCodeRecognitionImagePath", "func.qrCode", "");
    }

    public void setQrCodeRecognitionImagePath(String qrCodeRecognitionImagePath) {
        setting.put("func.qrCode", "qrCodeRecognitionImagePath", qrCodeRecognitionImagePath);
    }

    public String getCurrentHostName() {
        return setting.getStr("currentHostName", "func.host", "");
    }

    public void setCurrentHostName(String currentHostName) {
        setting.put("func.host", "currentHostName", currentHostName);
    }

    public String getDigestFilePath() {
        return setting.getStr("digestFilePath", "func.crypto", "");
    }

    public void setDigestFilePath(String digestFilePath) {
        setting.put("func.crypto", "digestFilePath", digestFilePath);
    }

    public int getRandomNumDigit() {
        return setting.getInt("randomNumDigit", "func.crypto", 16);
    }

    public void setRandomNumDigit(int randomNumDigit) {
        setting.put("func.crypto", "randomNumDigit", String.valueOf(randomNumDigit));
    }

    public int getRandomStringDigit() {
        return setting.getInt("randomStringDigit", "func.crypto", 16);
    }

    public void setRandomStringDigit(int randomStringDigit) {
        setting.put("func.crypto", "randomStringDigit", String.valueOf(randomStringDigit));
    }

    public int getRandomPasswordDigit() {
        return setting.getInt("randomPasswordDigit", "func.crypto", 16);
    }

    public void setRandomPasswordDigit(int randomPasswordDigit) {
        setting.put("func.crypto", "randomPasswordDigit", String.valueOf(randomPasswordDigit));
    }

    public String getMenuBarPosition() {
        return setting.getStr("menuBarPosition", "setting.custom", "上方");
    }

    public void setMenuBarPosition(String menuBarPosition) {
        setting.put("setting.custom", "menuBarPosition", menuBarPosition);
    }

    public String getCalculatorInputExpress() {
        return setting.getStr("calculatorInputExpress", "func.calculator", "3*(8-1)");
    }

    public void setCalculatorInputExpress(String calculatorInputExpress) {
        setting.put("func.calculator", "calculatorInputExpress", calculatorInputExpress);
    }

    public String getDbFilePath() {
        return setting.getStr("dbFilePath", "func.advanced", "");
    }

    public void setDbFilePath(String dbFilePath) {
        setting.put("func.advanced", "dbFilePath", dbFilePath);
    }

    public String getDbFilePathBefore() {
        return setting.getStr("dbFilePathBefore", "func.advanced", "");
    }

    public void setDbFilePathBefore(String dbFilePathBefore) {
        setting.put("func.advanced", "dbFilePathBefore", dbFilePathBefore);
    }

    public String getQuickNoteExportPath() {
        return setting.getStr("quickNoteExportPath", "func.quickNote", "");
    }

    public void setQuickNoteExportPath(String quickNoteExportPath) {
        setting.put("func.quickNote", "quickNoteExportPath", quickNoteExportPath);
    }

    public String getJsonBeautyExportPath() {
        return setting.getStr("jsonBeautyExportPath", "func.jsonBeauty", "");
    }

    public void setJsonBeautyExportPath(String jsonBeautyExportPath) {
        setting.put("func.jsonBeauty", "jsonBeautyExportPath", jsonBeautyExportPath);
    }

    public String getImageExportPath() {
        return setting.getStr("imageExportPath", "func.image", "");
    }

    public void setImageExportPath(String imageExportPath) {
        setting.put("func.image", "imageExportPath", imageExportPath);
    }

    public String getHostExportPath() {
        return setting.getStr("hostExportPath", "func.host", "");
    }

    public void setHostExportPath(String hostExportPath) {
        setting.put("func.host", "hostExportPath", hostExportPath);
    }

    public String getLastSelectedColor() {
        return setting.getStr("lastSelectedColor", "func.colorBoard", "007AAE");
    }

    public void setLastSelectedColor(String lastSelectedColor) {
        setting.put("func.colorBoard", "lastSelectedColor", lastSelectedColor);
    }

    public String getColorTheme() {
        return setting.getStr("colorTheme", "func.colorBoard", "默认");
    }

    public void setColorTheme(String colorTheme) {
        setting.put("func.colorBoard", "colorTheme", colorTheme);
    }

    public String getColorCodeType() {
        return setting.getStr("colorCodeType", "func.colorBoard", "HTML");
    }

    public void setColorCodeType(String colorCodeType) {
        setting.put("func.colorBoard", "colorCodeType", colorCodeType);
    }
}
