package com.luoboduner.moo.tool.util;

import cn.hutool.json.JSONUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.bean.FuncGroup;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.FuncTabCatalog.FuncTab;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 功能分组与最近使用持久化。
 */
public final class FuncGroupUtil {

    private static final String GROUP_SETTING = "func.group";

    private static final String CUSTOM_GROUPS_KEY = "customGroups";

    private static final String RECENT_FUNC_IDS_KEY = "recentFuncIds";

    private FuncGroupUtil() {
    }

    public static List<FuncGroup> getCustomGroups() {
        String json = App.config.getSettingStr(CUSTOM_GROUPS_KEY, GROUP_SETTING, "[]");
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        return JSONUtil.toList(json, FuncGroup.class);
    }

    public static void saveCustomGroups(List<FuncGroup> groups) {
        App.config.putSettingByGroup(CUSTOM_GROUPS_KEY, GROUP_SETTING, JSONUtil.toJsonStr(groups));
        App.config.save();
    }

    public static boolean isRecentVisible() {
        return App.config.isFuncRecentVisible();
    }

    public static List<FuncTab> getRecentTabs() {
        String raw = App.config.getSettingStr(RECENT_FUNC_IDS_KEY, GROUP_SETTING, "");
        if (StringUtils.isBlank(raw)) {
            return new ArrayList<>();
        }
        List<FuncTab> result = new ArrayList<>();
        for (String id : raw.split(",")) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            FuncTabCatalog.byId(id.trim()).ifPresent(tab -> {
                if (tab.index() > 0) {
                    result.add(tab);
                }
            });
            if (result.size() >= FuncTabCatalog.RECENT_LIMIT) {
                break;
            }
        }
        return result;
    }

    public static void recordRecent(int tabIndex) {
        if (tabIndex <= 0) {
            return;
        }
        Optional<FuncTab> tabOptional = FuncTabCatalog.byIndex(tabIndex);
        if (tabOptional.isEmpty()) {
            return;
        }
        String funcId = tabOptional.get().id();
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        orderedIds.add(funcId);
        for (FuncTab recentTab : getRecentTabs()) {
            orderedIds.add(recentTab.id());
            if (orderedIds.size() >= FuncTabCatalog.RECENT_LIMIT) {
                break;
            }
        }
        App.config.putSettingByGroup(RECENT_FUNC_IDS_KEY, GROUP_SETTING, String.join(",", orderedIds));
    }
}
