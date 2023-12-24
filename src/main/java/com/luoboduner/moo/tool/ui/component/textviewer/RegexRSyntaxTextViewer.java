package com.luoboduner.moo.tool.ui.component.textviewer;

public class RegexRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public RegexRSyntaxTextViewer() {

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
    }
}
