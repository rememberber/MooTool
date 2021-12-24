package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import com.luoboduner.moo.tool.util.TextAreaUtil;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class QuickNotePlainTextViewer extends PlainTextViewer {
    public QuickNotePlainTextViewer() {

        // 文本域键盘事件
        addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S) {
                    QuickNoteListener.quickSave(true);
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
                    JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                    quickNoteForm.getFindReplacePanel().setVisible(true);
                    quickNoteForm.getFindTextField().setText(view.getSelectedText());
                    quickNoteForm.getFindTextField().grabFocus();
                    quickNoteForm.getFindTextField().selectAll();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_R) {
                    JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                    quickNoteForm.getFindReplacePanel().setVisible(true);
                    quickNoteForm.getFindTextField().setText(view.getSelectedText());
                    quickNoteForm.getReplaceTextField().grabFocus();
                    quickNoteForm.getReplaceTextField().selectAll();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_N) {
                    QuickNoteListener.newNote();
                } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_D) {
                    JTextArea view = (JTextArea) quickNoteForm.getScrollPane().getViewport().getView();
                    TextAreaUtil.deleteSelectedLine(view);
                }
                QuickNoteListener.quickSave(true);
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        // 文本域鼠标点击事件，隐藏删除按钮
        addMouseListener(new MouseListener() {
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();

            @Override
            public void mouseClicked(MouseEvent e) {
                quickNoteForm.getDeletePanel().setVisible(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
    }
}
