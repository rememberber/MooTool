package com.luoboduner.moo.tool.ui.component.textviewer;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class JavaRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public JavaRSyntaxTextViewer() {

        setDoubleBuffered(true);

        updateTheme();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        // Consolas is not available on Linux
//        if (SystemUtil.isLinuxOs()) {
//            setFont(new Font("Monospaced", Font.PLAIN, 14));
//        } else {
//            setFont(new Font("Consolas", Font.PLAIN, 14));
//        }

        // 字体字号+1
        setFont(getFont().deriveFont((float) getFont().getSize() + 1));
        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
    }
}
