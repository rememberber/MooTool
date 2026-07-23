package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;

import javax.swing.JComponent;

/**
 * JSON Beauty 懒加载：后台扫 Vault，EDT 创建 UI 并绑定。
 */
public final class JsonBeautyToolInitializer implements LazyToolInitializer<JsonBeautyLoadData> {

    @Override
    public JsonBeautyLoadData loadData() {
        return JsonBeautyLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return JsonBeautyForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, JsonBeautyLoadData data) {
        JsonBeautyForm.bindLoadedData(data);
    }

    @Override
    public void startServices() {
        JsonBeautyForm.startPageServices();
    }

    @Override
    public void dispose() {
        JsonBeautyForm.stopPageServices();
    }

    @Override
    public String loadingMessage() {
        return "正在加载 JSON 数据……";
    }
}
