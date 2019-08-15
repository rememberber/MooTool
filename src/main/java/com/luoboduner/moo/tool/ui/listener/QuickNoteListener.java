package com.luoboduner.moo.tool.ui.listener;

import com.luoboduner.moo.tool.ui.form.fun.QuickNoteForm;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <pre>
 * 随手记事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/15.
 */
public class QuickNoteListener {

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        quickNoteForm.getSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showInputDialog("请命名", "");
            }
        });
    }
}
