package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.HostForm;

import javax.swing.JComponent;

/**
 * Host 懒加载：后台查库，EDT 创建 UI 并绑定。
 */
public final class HostToolInitializer implements LazyToolInitializer<HostLoadData> {

    @Override
    public HostLoadData loadData() {
        return HostLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return HostForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, HostLoadData data) {
        HostForm.bindLoadedData(data);
    }

    @Override
    public void startServices() {
        HostForm.startPageServices();
    }

    @Override
    public String loadingMessage() {
        return "正在加载 Host 数据……";
    }
}
