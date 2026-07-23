package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.TranslationForm;

import javax.swing.JComponent;

/**
 * 翻译页懒加载。
 */
public final class TranslationToolInitializer implements LazyToolInitializer<TranslationLoadData> {

    @Override
    public TranslationLoadData loadData() {
        return TranslationLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return TranslationForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, TranslationLoadData data) {
        TranslationForm.bindLoadedData(data);
    }

    @Override
    public String loadingMessage() {
        return "正在加载翻译数据……";
    }
}
