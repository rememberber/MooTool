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
     * 上次关闭前所在的tab
     */
    private int recentTabIndex;

    private String quickNoteFontName;

    private int quickNoteFontSize;

    private String jsonBeautyFontName;

    private int jsonBeautyFontSize;

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
        return setting.getStr("beforeVersion", "setting.common", "v1.0.0");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.put("setting.common", "beforeVersion", beforeVersion);
    }

    public String getTheme() {
        return setting.getStr("theme", "setting.appearance", "Darcula(推荐)");
    }

    public void setTheme(String theme) {
        setting.put("setting.appearance", "theme", theme);
    }

    public String getFont() {
        return setting.getStr("font", "setting.appearance", "微软雅黑");
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
        return setting.getStr("quickNoteFontName", "setting.quickNote", "等线");
    }

    public void setQuickNoteFontName(String quickNoteFontName) {
        setting.put("setting.quickNote", "quickNoteFontName", quickNoteFontName);
    }

    public int getQuickNoteFontSize() {
        return setting.getInt("quickNoteFontSize", "setting.quickNote", 0);
    }

    public void setQuickNoteFontSize(int quickNoteFontSize) {
        setting.put("setting.quickNote", "quickNoteFontSize", String.valueOf(quickNoteFontSize));
    }

    public String getJsonBeautyFontName() {
        return setting.getStr("jsonBeautyFontName", "setting.jsonBeauty", "等线");
    }

    public void setJsonBeautyFontName(String jsonBeautyFontName) {
        setting.put("setting.jsonBeauty", "jsonBeautyFontName", jsonBeautyFontName);
    }

    public int getJsonBeautyFontSize() {
        return setting.getInt("jsonBeautyFontSize", "setting.jsonBeauty", 0);
    }

    public void setJsonBeautyFontSize(int jsonBeautyFontSize) {
        setting.put("setting.jsonBeauty", "jsonBeautyFontSize", String.valueOf(jsonBeautyFontSize));
    }
}
