package com.luoboduner.moo.tool.ui.dialog;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.ImageSvgUtil;

import javax.swing.*;
import java.awt.*;

/** Options for offline bitmap-to-vector conversion. */
public class ImageSvgDialog extends JDialog {

    private final JComboBox<String> presetCombo;
    private final JSpinner colorCountSpinner;
    private final JComboBox<String> detailCombo;
    private final JSpinner filterSpeckleSpinner;
    private boolean confirmed;

    public ImageSvgDialog(int imageCount) {
        super(App.mainFrame, I18n.get("imageSvg.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        contentPane.add(new JLabel(I18n.format("imageCompress.selectedCount", imageCount)), BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        presetCombo = new JComboBox<>(new String[]{
                I18n.get("imageSvg.preset.poster"),
                I18n.get("imageSvg.preset.photo"),
                I18n.get("imageSvg.preset.bw")
        });
        addRow(optionsPanel, gbc, 0, I18n.get("imageSvg.preset"), presetCombo);

        colorCountSpinner = new JSpinner(new SpinnerNumberModel(16, 2, 64, 1));
        addRow(optionsPanel, gbc, 1, I18n.get("imageSvg.colors"), colorCountSpinner);

        detailCombo = new JComboBox<>(new String[]{
                I18n.get("imageSvg.detail.low"),
                I18n.get("imageSvg.detail.medium"),
                I18n.get("imageSvg.detail.high")
        });
        detailCombo.setSelectedIndex(1);
        addRow(optionsPanel, gbc, 2, I18n.get("imageSvg.detail"), detailCombo);

        filterSpeckleSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 128, 1));
        addRow(optionsPanel, gbc, 3, I18n.get("imageSvg.speckle"), filterSpeckleSpinner);

        JLabel hint = new JLabel("<html><body style='width:360px'>" + I18n.get("imageSvg.hint") + "</body></html>");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(9, 4, 2, 4);
        optionsPanel.add(hint, gbc);
        contentPane.add(optionsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelButton = new JButton(I18n.get("common.cancel"));
        JButton startButton = new JButton(I18n.get("imageSvg.start"));
        buttonPanel.add(cancelButton);
        buttonPanel.add(startButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        presetCombo.addActionListener(event -> colorCountSpinner.setEnabled(presetCombo.getSelectedIndex() != 2));
        cancelButton.addActionListener(event -> dispose());
        startButton.addActionListener(event -> {
            confirmed = true;
            dispose();
        });

        setContentPane(contentPane);
        pack();
        setMinimumSize(new Dimension(460, getPreferredSize().height));
        setLocationRelativeTo(App.mainFrame);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ImageSvgUtil.SvgOptions getOptions() {
        ImageSvgUtil.Preset preset = switch (presetCombo.getSelectedIndex()) {
            case 1 -> ImageSvgUtil.Preset.PHOTO;
            case 2 -> ImageSvgUtil.Preset.BLACK_AND_WHITE;
            default -> ImageSvgUtil.Preset.POSTER;
        };
        ImageSvgUtil.Detail detail = switch (detailCombo.getSelectedIndex()) {
            case 0 -> ImageSvgUtil.Detail.LOW;
            case 2 -> ImageSvgUtil.Detail.HIGH;
            default -> ImageSvgUtil.Detail.MEDIUM;
        };
        return new ImageSvgUtil.SvgOptions(preset, (Integer) colorCountSpinner.getValue(), detail,
                (Integer) filterSpeckleSpinner.getValue());
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }
}
