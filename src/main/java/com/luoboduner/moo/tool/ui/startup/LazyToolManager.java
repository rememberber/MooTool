package com.luoboduner.moo.tool.ui.startup;

import lombok.extern.slf4j.Slf4j;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 以稳定工具 ID 管理页面懒加载。
 */
@Slf4j
public final class LazyToolManager {

    private static final LazyToolManager INSTANCE = new LazyToolManager();

    private final Map<String, ToolSlot<?>> slots = new ConcurrentHashMap<>();

    private LazyToolManager() {
    }

    public static LazyToolManager getInstance() {
        return INSTANCE;
    }

    public <M> void register(String toolId, LazyToolInitializer<M> initializer, JPanel tabContainer) {
        EdtGuard.assertEdt();
        ToolContentHost host = new ToolContentHost();
        tabContainer.removeAll();
        tabContainer.setLayout(new BorderLayout());
        tabContainer.add(host.getRoot(), BorderLayout.CENTER);
        tabContainer.revalidate();
        ToolSlot<M> slot = new ToolSlot<>(toolId, initializer, host);
        slots.put(toolId, slot);
        log.debug("Registered lazy tool {}", toolId);
    }

    public void ensureInitialized(String toolId) {
        ToolSlot<?> slot = slots.get(toolId);
        if (slot == null) {
            log.warn("Unknown tool id for lazy init: {}", toolId);
            return;
        }
        slot.ensureInitialized();
    }

    public void ensureInitializedByIndex(int tabIndex) {
        com.luoboduner.moo.tool.ui.FuncTabCatalog.byIndex(tabIndex)
                .ifPresent(tab -> ensureInitialized(tab.id()));
    }

    public ToolLoadState stateOf(String toolId) {
        ToolSlot<?> slot = slots.get(toolId);
        return slot == null ? ToolLoadState.NEW : slot.state();
    }

    public boolean isReady(String toolId) {
        return stateOf(toolId) == ToolLoadState.READY;
    }

    public Optional<ToolContentHost> hostOf(String toolId) {
        ToolSlot<?> slot = slots.get(toolId);
        return slot == null ? Optional.empty() : Optional.of(slot.host());
    }

    public Collection<String> registeredToolIds() {
        return slots.keySet();
    }

    public void retry(String toolId) {
        ToolSlot<?> slot = slots.get(toolId);
        if (slot != null) {
            slot.retry();
        }
    }

    public void disposeAll() {
        slots.values().forEach(ToolSlot::dispose);
        slots.clear();
    }
}
