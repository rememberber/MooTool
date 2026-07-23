package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.ui.form.func.ImageForm;

import javax.swing.JComponent;

/**
 * 图片助手懒加载：后台扫描并预解码首图。
 */
public final class ImageToolInitializer implements LazyToolInitializer<ImageLoadData> {

    @Override
    public ImageLoadData loadData() {
        return ImageLoadData.loadInitial();
    }

    @Override
    public JComponent createView() {
        return ImageForm.createViewShell();
    }

    @Override
    public void bindData(JComponent view, ImageLoadData data) {
        ImageForm.bindLoadedData(data);
    }

    @Override
    public String loadingMessage() {
        return "正在加载图片列表……";
    }
}
