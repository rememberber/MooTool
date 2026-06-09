package com.luoboduner.moo.tool.util;

import org.junit.Test;

import javax.swing.JTextArea;
import java.awt.event.KeyEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UndoUtilTest {

    @Test
    public void ctrlZUndoConsumesEvent() {
        JTextArea textArea = new JTextArea();
        TextAreaHolder holder = new TextAreaHolder(textArea);
        UndoUtil.register(holder);
        textArea.setText("before");
        textArea.append(" after");

        KeyEvent ctrlZ = new KeyEvent(textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_Z, KeyEvent.CHAR_UNDEFINED);
        textArea.getKeyListeners()[0].keyPressed(ctrlZ);

        assertEquals("before", textArea.getText());
        assertTrue(ctrlZ.isConsumed());
    }

    @Test
    public void directTextComponentCanBeRegistered() {
        JTextArea textArea = new JTextArea();
        UndoUtil.register(textArea);
        textArea.setText("before");
        textArea.append(" after");

        KeyEvent ctrlZ = new KeyEvent(textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_Z, KeyEvent.CHAR_UNDEFINED);
        textArea.getKeyListeners()[0].keyPressed(ctrlZ);

        assertEquals("before", textArea.getText());
        assertTrue(ctrlZ.isConsumed());
    }

    private static class TextAreaHolder {
        private final JTextArea textArea;

        private TextAreaHolder(JTextArea textArea) {
            this.textArea = textArea;
        }
    }
}
