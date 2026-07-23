package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;

import javax.swing.JComponent;

/**
 * Quick Note 懒加载：后台扫 Vault，EDT 创建 UI 并绑定。
 */
public final class QuickNoteToolInitializer implements LazyToolInitializer<QuickNoteLoadData> {

    @Override
    public QuickNoteLoadData loadData() throws Exception {
        return QuickNoteLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return QuickNoteForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, QuickNoteLoadData data) {
        QuickNoteForm.bindLoadedData(data);
    }

    @Override
    public void startServices() {
        QuickNoteForm.startPageServices();
    }

    @Override
    public void dispose() {
        QuickNoteForm.stopPageServices();
    }

    @Override
    public String loadingMessage() {
        return "正在加载便签数据……";
    }
}
