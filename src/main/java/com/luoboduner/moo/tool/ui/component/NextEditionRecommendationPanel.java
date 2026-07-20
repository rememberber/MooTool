package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.util.I18n;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Promotes the recommended MooTool Next edition from the Java client.
 */
public class NextEditionRecommendationPanel extends JPanel {

    private final JLabel descriptionLabel = new JLabel();
    private final JLabel downloadLinkLabel = new JLabel();

    public NextEditionRecommendationPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(0, 8, 8, 8)
        ));

        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadLinkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(descriptionLabel);
        add(Box.createVerticalStrut(6));
        add(downloadLinkLabel);

        downloadLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openDownloadPage();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                downloadLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });

        applyI18n();
    }

    public void applyI18n() {
        if (getBorder() instanceof javax.swing.border.CompoundBorder compoundBorder
                && compoundBorder.getOutsideBorder() instanceof TitledBorder titledBorder) {
            titledBorder.setTitle(I18n.get("about.section.nextEdition"));
        }
        descriptionLabel.setText(I18n.get("about.nextEdition.desc"));
        downloadLinkLabel.setText("<html><a href=\"" + UiConsts.NEXT_EDITION_RELEASE_URL + "\">"
                + I18n.get("about.nextEdition.download") + "</a></html>");
        revalidate();
        repaint();
    }

    private void openDownloadPage() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(UiConsts.NEXT_EDITION_RELEASE_URL));
        } catch (IOException | URISyntaxException ignored) {
            // Keep the About/Home page usable when no browser is available.
        }
    }
}
