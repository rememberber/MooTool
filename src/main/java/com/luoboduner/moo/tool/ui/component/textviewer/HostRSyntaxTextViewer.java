package com.luoboduner.moo.tool.ui.component.textviewer;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HostRSyntaxTextViewer extends CommonRSyntaxTextViewer {
    public HostRSyntaxTextViewer() {

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

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HOSTS);

        setHyperlinksEnabled(true);
    }
}
