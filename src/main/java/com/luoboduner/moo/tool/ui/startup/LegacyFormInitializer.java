package com.luoboduner.moo.tool.ui.startup;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 过渡期包装：将现有 Form::init 放到 EDT 执行，后台 loadData 默认空。
 */
public final class LegacyFormInitializer implements LazyToolInitializer<Void> {

    private final String loadingMessage;
    private final Runnable initOnEdt;
    private final Supplier<JComponent> viewSupplier;
    private final Runnable startServices;
    private final Runnable disposeServices;

    public LegacyFormInitializer(String loadingMessage,
                                 Runnable initOnEdt,
                                 Supplier<JComponent> viewSupplier) {
        this(loadingMessage, initOnEdt, viewSupplier, null, null);
    }

    public LegacyFormInitializer(String loadingMessage,
                                 Runnable initOnEdt,
                                 Supplier<JComponent> viewSupplier,
                                 Runnable startServices,
                                 Runnable disposeServices) {
        this.loadingMessage = loadingMessage == null ? "Loading……" : loadingMessage;
        this.initOnEdt = Objects.requireNonNull(initOnEdt, "initOnEdt");
        this.viewSupplier = Objects.requireNonNull(viewSupplier, "viewSupplier");
        this.startServices = startServices;
        this.disposeServices = disposeServices;
    }

    @Override
    public Void loadData() {
        return null;
    }

    @Override
    public JComponent createView() {
        EdtGuard.assertEdt();
        initOnEdt.run();
        return viewSupplier.get();
    }

    @Override
    public void bindData(JComponent view, Void data) {
        // legacy init 已完成绑定
    }

    @Override
    public void startServices() {
        if (startServices != null) {
            startServices.run();
        }
    }

    @Override
    public void dispose() {
        if (disposeServices != null) {
            disposeServices.run();
        }
    }

    @Override
    public String loadingMessage() {
        return loadingMessage;
    }
}
