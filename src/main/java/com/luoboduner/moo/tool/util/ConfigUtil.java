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

    private boolean defaultMaxWindow;

    private boolean unifiedBackground;

    /**
     * 主题颜色跟随系统
     */
    private boolean themeColorFollowSystem;

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
     * 功能Tab位置
     */
    private String funcTabPosition;

    private boolean tabCompact;

    private boolean tabHideTitle;

    private boolean tabSeparator;

    private boolean tabCard;

    /**
     * sql dialect
     */
    private String sqlDialect;

    private String accentColor;

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

    private String regexText;

    public boolean isAutoCheckUpdate() {
        return setting.getBool("autoCheckUpdate", "setting.common", true);
    }

    public void setAutoCheckUpdate(boolean autoCheckUpdate) {
        setting.putByGroup("autoCheckUpdate", "setting.common", String.valueOf(autoCheckUpdate));
    }

    public boolean isDefaultMaxWindow() {
        return setting.getBool("defaultMaxWindow", "setting.normal", false);
    }

    public void setDefaultMaxWindow(boolean defaultMaxWindow) {
        setting.putByGroup("defaultMaxWindow", "setting.normal", String.valueOf(defaultMaxWindow));
    }

    public boolean isUnifiedBackground() {
        return setting.getBool("unifiedBackground", "setting.normal", true);
    }

    public void setUnifiedBackground(boolean unifiedBackground) {
        setting.putByGroup("unifiedBackground", "setting.normal", String.valueOf(unifiedBackground));
    }

    public boolean isThemeColorFollowSystem() {
        return setting.getBool("themeColorFollowSystem", "setting.normal", true);
    }

    public void setThemeColorFollowSystem(boolean themeColorFollowSystem) {
        setting.putByGroup("themeColorFollowSystem", "setting.normal", String.valueOf(themeColorFollowSystem));
    }

    public int getRecentTabIndex() {
        return setting.getInt("recentTabIndex", "setting.common", 0);
    }

    public void setRecentTabIndex(int recentTabIndex) {
        setting.putByGroup("recentTabIndex", "setting.common", String.valueOf(recentTabIndex));
    }

    public String getBeforeVersion() {
        return setting.getStr("beforeVersion", "setting.common", "v0.0.0");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.putByGroup("beforeVersion", "setting.common", beforeVersion);
    }

    public String getTheme() {
        if (SystemUtil.isMacOs()) {
            return setting.getStr("theme", "setting.appearance", "Flat macOS Dark");
        } else {
            return setting.getStr("theme", "setting.appearance", "Flat Darcula");
        }
    }

    public void setTheme(String theme) {
        setting.putByGroup("theme", "setting.appearance", theme);
    }

    public String getFont() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else if (SystemUtil.isMacOs()) {
            return setting.getStr("font", "setting.appearance", "PingFang SC");
        } else {
            return setting.getStr("font", "setting.appearance", "微软雅黑");
        }
    }

    public void setFont(String font) {
        setting.putByGroup("font", "setting.appearance", font);
    }

    public int getFontSize() {
        return setting.getInt("fontSize", "setting.appearance", 12);
    }

    public void setFontSize(int fontSize) {
        setting.putByGroup("fontSize", "setting.appearance", String.valueOf(fontSize));
    }

    public boolean isHttpUseProxy() {
        return setting.getBool("httpUseProxy", "setting.http", false);
    }

    public void setHttpUseProxy(boolean httpUseProxy) {
        setting.putByGroup("httpUseProxy", "setting.http", String.valueOf(httpUseProxy));
    }

    public String getHttpProxyHost() {
        return setting.getStr("httpProxyHost", "setting.http", "");
    }

    public void setHttpProxyHost(String httpProxyHost) {
        setting.putByGroup("httpProxyHost", "setting.http", httpProxyHost);
    }

    public String getHttpProxyPort() {
        return setting.getStr("httpProxyPort", "setting.http", "");
    }

    public void setHttpProxyPort(String httpProxyPort) {
        setting.putByGroup("httpProxyPort", "setting.http", httpProxyPort);
    }

    public String getHttpProxyUserName() {
        return setting.getStr("httpProxyUserName", "setting.http", "");
    }

    public void setHttpProxyUserName(String httpProxyUserName) {
        setting.putByGroup("httpProxyUserName", "setting.http", httpProxyUserName);
    }

    public String getHttpProxyPassword() {
        return setting.getStr("httpProxyPassword", "setting.http", "");
    }

    public void setHttpProxyPassword(String httpProxyPassword) {
        setting.putByGroup("httpProxyPassword", "setting.http", httpProxyPassword);
    }

    public String getQuickNoteFontName() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("quickNoteFontName", "func.quickNote", "等线");
        }
    }

    public void setQuickNoteFontName(String quickNoteFontName) {
        setting.putByGroup("quickNoteFontName", "func.quickNote", quickNoteFontName);
    }

    public int getQuickNoteFontSize() {
        return setting.getInt("quickNoteFontSize", "func.quickNote", 0);
    }

    public void setQuickNoteFontSize(int quickNoteFontSize) {
        setting.putByGroup("quickNoteFontSize", "func.quickNote", String.valueOf(quickNoteFontSize));
    }

    public String getJsonBeautyFontName() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("jsonBeautyFontName", "func.jsonBeauty", "等线");
        }
    }

    public void setJsonBeautyFontName(String jsonBeautyFontName) {
        setting.putByGroup("jsonBeautyFontName", "func.jsonBeauty", jsonBeautyFontName);
    }

    public int getJsonBeautyFontSize() {
        return setting.getInt("jsonBeautyFontSize", "func.jsonBeauty", 0);
    }

    public void setJsonBeautyFontSize(int jsonBeautyFontSize) {
        setting.putByGroup("jsonBeautyFontSize", "func.jsonBeauty", String.valueOf(jsonBeautyFontSize));
    }

    public int getQrCodeSize() {
        return setting.getInt("qrCodeSize", "func.qrCode", 320);
    }

    public void setQrCodeSize(int qrCodeSize) {
        setting.putByGroup("qrCodeSize", "func.qrCode", String.valueOf(qrCodeSize));
    }

    public String getQrCodeErrorCorrectionLevel() {
        return setting.getStr("qrCodeErrorCorrectionLevel", "func.qrCode", "中低");
    }

    public void setQrCodeErrorCorrectionLevel(String qrCodeErrorCorrectionLevel) {
        setting.putByGroup("qrCodeErrorCorrectionLevel", "func.qrCode", qrCodeErrorCorrectionLevel);
    }

    public String getQrCodeLogoPath() {
        return setting.getStr("qrCodeLogoPath", "func.qrCode", "");
    }

    public void setQrCodeLogoPath(String qrCodeLogoPath) {
        setting.putByGroup("qrCodeLogoPath", "func.qrCode", qrCodeLogoPath);
    }

    public String getQrCodeSaveAsPath() {
        return setting.getStr("qrCodeSaveAsPath", "func.qrCode", "");
    }

    public void setQrCodeSaveAsPath(String qrCodeSaveAsPath) {
        setting.putByGroup("qrCodeSaveAsPath", "func.qrCode", qrCodeSaveAsPath);
    }

    public String getQrCodeRecognitionImagePath() {
        return setting.getStr("qrCodeRecognitionImagePath", "func.qrCode", "");
    }

    public void setQrCodeRecognitionImagePath(String qrCodeRecognitionImagePath) {
        setting.putByGroup("qrCodeRecognitionImagePath", "func.qrCode", qrCodeRecognitionImagePath);
    }

    public String getCurrentHostName() {
        return setting.getStr("currentHostName", "func.host", "");
    }

    public void setCurrentHostName(String currentHostName) {
        setting.putByGroup("currentHostName", "func.host", currentHostName);
    }

    public String getDigestFilePath() {
        return setting.getStr("digestFilePath", "func.crypto", "");
    }

    public void setDigestFilePath(String digestFilePath) {
        setting.putByGroup("digestFilePath", "func.crypto", digestFilePath);
    }

    public int getRandomNumDigit() {
        return setting.getInt("randomNumDigit", "func.crypto", 16);
    }

    public void setRandomNumDigit(int randomNumDigit) {
        setting.putByGroup("randomNumDigit", "func.crypto", String.valueOf(randomNumDigit));
    }

    public int getRandomStringDigit() {
        return setting.getInt("randomStringDigit", "func.crypto", 16);
    }

    public void setRandomStringDigit(int randomStringDigit) {
        setting.putByGroup("randomStringDigit", "func.crypto", String.valueOf(randomStringDigit));
    }

    public int getRandomPasswordDigit() {
        return setting.getInt("randomPasswordDigit", "func.crypto", 16);
    }

    public void setRandomPasswordDigit(int randomPasswordDigit) {
        setting.putByGroup("randomPasswordDigit", "func.crypto", String.valueOf(randomPasswordDigit));
    }

    public String getMenuBarPosition() {
        return setting.getStr("menuBarPosition", "setting.custom", "上方");
    }

    public void setMenuBarPosition(String menuBarPosition) {
        setting.putByGroup("menuBarPosition", "setting.custom", menuBarPosition);
    }

    public String getFuncTabPosition() {
        return setting.getStr("funcTabPosition", "setting.custom", "左侧");
    }

    public void setFuncTabPosition(String funcTabPosition) {
        setting.putByGroup("funcTabPosition", "setting.custom", funcTabPosition);
    }

    public boolean isTabCompact() {
        return setting.getBool("tabCompact", "setting.custom", false);
    }

    public void setTabCompact(boolean tabCompact) {
        setting.putByGroup("tabCompact", "setting.custom", String.valueOf(tabCompact));
    }

    public boolean isTabHideTitle() {
        return setting.getBool("tabHideTitle", "setting.custom", true);
    }

    public void setTabHideTitle(boolean tabHideTitle) {
        setting.putByGroup("tabHideTitle", "setting.custom", String.valueOf(tabHideTitle));
    }

    public boolean isTabSeparator() {
        return setting.getBool("tabSeparator", "setting.custom", false);
    }

    public void setTabSeparator(boolean tabSeparator) {
        setting.putByGroup("tabSeparator", "setting.custom", String.valueOf(tabSeparator));
    }

    public boolean isTabCard() {
        return setting.getBool("tabCard", "setting.custom", false);
    }

    public void setTabCard(boolean tabCard) {
        setting.putByGroup("tabCard", "setting.custom", String.valueOf(tabCard));
    }

    public String getSqlDialect() {
        return setting.getStr("sqlDialect", "setting.quickNote", "Standard SQL");
    }

    public void setSqlDialect(String sqlDialect) {
        setting.putByGroup("sqlDialect", "setting.quickNote", sqlDialect);
    }

    public String getAccentColor() {
        return setting.getStr("accentColor", "setting.quickNote", "");
    }

    public void setAccentColor(String accentColor) {
        setting.putByGroup("accentColor", "setting.quickNote", accentColor);
    }

    public String getCalculatorInputExpress() {
        return setting.getStr("calculatorInputExpress", "func.calculator", "3*(8-1)");
    }

    public void setCalculatorInputExpress(String calculatorInputExpress) {
        setting.putByGroup("calculatorInputExpress", "func.calculator", calculatorInputExpress);
    }

    public String getDbFilePath() {
        return setting.getStr("dbFilePath", "func.advanced", "");
    }

    public void setDbFilePath(String dbFilePath) {
        setting.putByGroup("dbFilePath", "func.advanced", dbFilePath);
    }

    public String getDbFilePathBefore() {
        return setting.getStr("dbFilePathBefore", "func.advanced", "");
    }

    public void setDbFilePathBefore(String dbFilePathBefore) {
        setting.putByGroup("dbFilePathBefore", "func.advanced", dbFilePathBefore);
    }

    public String getQuickNoteExportPath() {
        return setting.getStr("quickNoteExportPath", "func.quickNote", "");
    }

    public void setQuickNoteExportPath(String quickNoteExportPath) {
        setting.putByGroup("quickNoteExportPath", "func.quickNote", quickNoteExportPath);
    }

    public String getJsonBeautyExportPath() {
        return setting.getStr("jsonBeautyExportPath", "func.jsonBeauty", "");
    }

    public void setJsonBeautyExportPath(String jsonBeautyExportPath) {
        setting.putByGroup("jsonBeautyExportPath", "func.jsonBeauty", jsonBeautyExportPath);
    }

    public String getImageExportPath() {
        return setting.getStr("imageExportPath", "func.image", "");
    }

    public void setImageExportPath(String imageExportPath) {
        setting.putByGroup("imageExportPath", "func.image", imageExportPath);
    }

    public String getHostExportPath() {
        return setting.getStr("hostExportPath", "func.host", "");
    }

    public void setHostExportPath(String hostExportPath) {
        setting.putByGroup("hostExportPath", "func.host", hostExportPath);
    }

    public String getLastSelectedColor() {
        return setting.getStr("lastSelectedColor", "func.colorBoard", "007AAE");
    }

    public void setLastSelectedColor(String lastSelectedColor) {
        setting.putByGroup("lastSelectedColor", "func.colorBoard", lastSelectedColor);
    }

    public String getColorTheme() {
        return setting.getStr("colorTheme", "func.colorBoard", "默认");
    }

    public void setColorTheme(String colorTheme) {
        setting.putByGroup("colorTheme", "func.colorBoard", colorTheme);
    }

    public String getColorCodeType() {
        return setting.getStr("colorCodeType", "func.colorBoard", "HTML");
    }

    public void setColorCodeType(String colorCodeType) {
        setting.putByGroup("colorCodeType", "func.colorBoard", colorCodeType);
    }

    public String getRegexText() {
        return setting.getStr("regexText", "func.regex", "");
    }

    public void setRegexText(String regexText) {
        setting.putByGroup("regexText", "func.regex", regexText);
    }
}
