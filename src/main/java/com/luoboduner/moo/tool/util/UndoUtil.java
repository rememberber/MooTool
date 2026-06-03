package com.luoboduner.moo.tool.util;

import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.lang.reflect.Field;

/**
 * <pre>
 * 撤销/重做工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/9.
 */
@Slf4j
public class UndoUtil {

    /**
     * 给对象中的文本框和文本域添加撤销/重做事件
     *
     * @param object
     */
    public static void register(Object object) {
        if (object instanceof JTextComponent) {
            registerTextComponent((JTextComponent) object);
            return;
        }
        Class strClass = object.getClass();
        Field[] declaredFields = strClass.getDeclaredFields();
        for (Field field : declaredFields) {
            if (JTextComponent.class.isAssignableFrom(field.getType())) {
                if (RSyntaxTextArea.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    registerTextComponent((JTextComponent) field.get(object));
                } catch (IllegalAccessException e) {
                    log.error(e.toString());
                }
            }
        }
    }

    private static void registerTextComponent(JTextComponent textComponent) {
        if (textComponent == null) {
            return;
        }
        UndoManager undoManager = new UndoManager();
        textComponent.getDocument().addUndoableEditListener(undoManager);
        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_Z) {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                    evt.consume();
                }
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_Y) {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                    evt.consume();
                }
            }
        });
    }
}
