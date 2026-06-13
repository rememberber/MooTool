package com.luoboduner.moo.tool.ui.listener;


import cn.hutool.core.util.RuntimeUtil;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.util.FuncGroupUtil;
import com.luoboduner.moo.tool.ui.FuncTabCatalog;
import com.luoboduner.moo.tool.ui.form.func.HardwareInfoForm;
import com.luoboduner.moo.tool.ui.form.func.NetForm;
import com.luoboduner.moo.tool.ui.listener.func.HardwareInfoListener;
import com.luoboduner.moo.tool.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

    private static boolean warnFlag = true;

    public static void addListeners() {
        MainWindow.getInstance().getTabbedPane().addChangeListener(new ChangeListener() {
            /**
             * Invoked when the target of the listener has changed its state.
             *
             * @param e a ChangeEvent object
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = MainWindow.getInstance().getTabbedPane().getSelectedIndex();
                FuncGroupUtil.recordRecent(index);
                String tabTitle = MainWindow.getInstance().getTabbedPane().getTitleAt(index);
                if (HardwareInfoForm.TAB_TITLE.equals(tabTitle)) {
                    HardwareInfoListener.onTabSelected();
                }
                if (FuncTabCatalog.byId("net").map(tab -> tab.index() == index).orElse(false)) {
                    try {
                        String ipConfigStr;
                        if (SystemUtil.isWindowsOs()) {
                            ipConfigStr = RuntimeUtil.execForStr("ipconfig");
                        } else {
                            ipConfigStr = RuntimeUtil.execForStr("ifconfig");
                        }
                        NetForm.getInstance().getIpConfigTextArea().setText(ipConfigStr);
                        NetForm.getInstance().getIpConfigTextArea().setCaretPosition(0);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.error(ExceptionUtils.getStackTrace(ex));
                    }
                }
            }
        });
    }
}
