package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.util.JsonBeautyVaultUtil;
import lombok.Getter;

import java.util.List;

/**
 * JSON Beauty 后台加载快照。
 */
@Getter
public final class JsonBeautyLoadData {

    private final List<TJsonBeauty> items;
    private final List<String> folders;

    public JsonBeautyLoadData(List<TJsonBeauty> items, List<String> folders) {
        this.items = items == null ? List.of() : List.copyOf(items);
        this.folders = folders == null ? List.of() : List.copyOf(folders);
    }

    public static JsonBeautyLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        JsonBeautyVaultUtil.ensureVaultReady();
        List<TJsonBeauty> items = JsonBeautyVaultUtil.listByFilter("");
        List<String> folders = JsonBeautyVaultUtil.listFolders();
        return new JsonBeautyLoadData(items, folders);
    }
}
