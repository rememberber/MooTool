package com.luoboduner.moo.tool.ui.component.textviewer;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JsonRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public JsonRSyntaxTextViewer() {

        addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI(e.getURL().toString()));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        setDoubleBuffered(true);
        
        updateTheme();
    }

    public void updateTheme() {
        super.updateTheme();

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        setCodeFoldingEnabled(true);

        String fontName = App.config.getJsonBeautyFontName();
        int fontSize = App.config.getJsonBeautyFontSize();
        if (fontSize == 0) {
            fontSize = MainWindow.getInstance().getMainPanel().getFont().getSize() + 2;
        }
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        setFont(font);
    }
}
