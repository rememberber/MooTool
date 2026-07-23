package com.luoboduner.moo.tool.ui.listener;


import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.NetForm;
import com.luoboduner.moo.tool.ui.listener.func.HardwareInfoListener;
import com.luoboduner.moo.tool.ui.listener.func.NetListener;
import com.luoboduner.moo.tool.ui.startup.LazyToolManager;
import com.luoboduner.moo.tool.ui.startup.StartupCoordinator;
import com.luoboduner.moo.tool.ui.startup.ToolLoadState;
import com.luoboduner.moo.tool.util.FuncGroupUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <pre>
 * tab事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/21.
 */
@Slf4j
public class TabListener {

    public static void addListeners() {
        MainWindow.getInstance().getTabbedPane().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
                FuncGroupUtil.recordRecent(index);
                FrameListener.persistRecentTab(index);

                FuncTabCatalog.byIndex(index).ifPresent(tab -> {
                    StartupCoordinator.getInstance().ensureToolWhenReady(tab.id());
                    if ("hardware".equals(tab.id())
                            && LazyToolManager.getInstance().stateOf(tab.id()) == ToolLoadState.READY) {
                        HardwareInfoListener.onTabSelected();
                    }
                    if ("net".equals(tab.id())
                            && LazyToolManager.getInstance().stateOf(tab.id()) == ToolLoadState.READY
                            && NetForm.getInstance().getIpConfigTextArea() != null
                            && NetForm.getInstance().getIpConfigTextArea().getText().isBlank()) {
                        NetListener.refreshIpConfigAsync();
                    }
                });
            }
        });
    }
}
