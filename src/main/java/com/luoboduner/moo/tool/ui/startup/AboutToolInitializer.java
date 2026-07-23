package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.AboutForm;

import javax.swing.JComponent;

/**
 * About 页懒加载：后台拉贡献者与头像，EDT 组装。
 */
public final class AboutToolInitializer implements LazyToolInitializer<AboutLoadData> {

    @Override
    public AboutLoadData loadData() {
        return AboutLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return AboutForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, AboutLoadData data) {
        AboutForm.bindLoadedData(data);
    }

    @Override
    public void startServices() {
        AboutForm.startPageServices();
    }

    @Override
    public void dispose() {
        AboutForm.stopPageServices();
    }

    @Override
    public String loadingMessage() {
        return "正在加载关于页……";
    }
}
