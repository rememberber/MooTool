package com.luoboduner.moo.tool.ui.form.func;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonBeautyFormTest {

    @Test
    public void exposesNewFolderToolbarButton() throws Exception {
        Field field = JsonBeautyForm.class.getDeclaredField("newFolderButton");

        assertEquals(JButton.class, field.getType());
        assertEquals(JButton.class, JsonBeautyForm.class.getMethod("getNewFolderButton").getReturnType());
    }
}
