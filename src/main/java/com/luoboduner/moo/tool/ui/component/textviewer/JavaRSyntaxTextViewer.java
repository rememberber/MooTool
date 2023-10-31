package com.luoboduner.moo.tool.ui.component.textviewer;

import com.luoboduner.moo.tool.util.SystemUtil;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;

public class JavaRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public JavaRSyntaxTextViewer() {

        setDoubleBuffered(true);

        updateTheme();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        // Consolas is not available on Linux
        if (SystemUtil.isLinuxOs()) {
            setFont(new Font("Monospaced", Font.PLAIN, 14));
        } else {
            setFont(new Font("Consolas", Font.PLAIN, 14));
        }
        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
    }
}
