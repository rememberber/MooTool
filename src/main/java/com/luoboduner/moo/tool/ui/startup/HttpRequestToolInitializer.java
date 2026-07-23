package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.HttpRequestForm;

import javax.swing.JComponent;

/**
 * HTTP 请求页懒加载。
 */
public final class HttpRequestToolInitializer implements LazyToolInitializer<HttpRequestLoadData> {

    @Override
    public HttpRequestLoadData loadData() {
        return HttpRequestLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return HttpRequestForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, HttpRequestLoadData data) {
        HttpRequestForm.bindLoadedData(data);
    }

    @Override
    public String loadingMessage() {
        return "正在加载 HTTP 请求……";
    }
}
