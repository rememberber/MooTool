package com.rememberber.mootool.nextfx.platform;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

public final class DesktopLinks {

    public static final Map<String, String> PAGES = Map.ofEntries(
            Map.entry("home", "https://mootool.luoboduner.com"),
            Map.entry("github", "https://github.com/rememberber/MooTool"),
            Map.entry("gitee", "https://gitee.com/zhoubochina/MooTool"),
            Map.entry("issues", "https://github.com/rememberber/MooTool/issues"),
            Map.entry("wePush", "https://github.com/rememberber/WePush"),
            Map.entry("mooInfo", "https://github.com/rememberber/MooInfo"),
            Map.entry("contributorCassianFlorin", "https://github.com/CassianFlorin"),
            Map.entry("contributorFelixcn", "https://github.com/felixcn"),
            Map.entry("contributorFelixnan168", "https://gitee.com/felixnan168"),
            Map.entry("contributorLyp", "https://gitee.com/L1yp"),
            Map.entry("contributorSunsence", "https://github.com/sunsence"),
            Map.entry("contributorRememberber", "https://github.com/rememberber")
    );

    private DesktopLinks() {
    }

    public static void open(String pageId) {
        String url = PAGES.get(pageId);
        if (url == null) {
            throw new IllegalArgumentException("Unknown page: " + pageId);
        }
        openUrl(url);
    }

    public static void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to open " + url, exception);
        }
    }
}
