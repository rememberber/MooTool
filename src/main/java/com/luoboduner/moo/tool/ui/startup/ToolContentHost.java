package com.luoboduner.moo.tool.ui.startup;

import lombok.Getter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;

/**
 * 单个工具内容区：LOADING / CONTENT / ERROR。
 */
public final class ToolContentHost {

    public static final String CARD_LOADING = "LOADING";
    public static final String CARD_CONTENT = "CONTENT";
    public static final String CARD_ERROR = "ERROR";

    @Getter
    private final JPanel root;
    private final CardLayout cardLayout;
    private final JPanel contentCard;
    private final JLabel loadingLabel;
    private JLabel errorSummaryLabel;
    private JTextArea errorDetailArea;
    private Consumer<Void> retryAction;

    public ToolContentHost() {
        cardLayout = new CardLayout();
        root = new JPanel(cardLayout);

        JPanel loadingCard = new JPanel(new BorderLayout());
        loadingLabel = new JLabel("Loading……", JLabel.CENTER);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN, 16f));
        loadingCard.add(loadingLabel, BorderLayout.CENTER);

        contentCard = new JPanel(new BorderLayout());

        JPanel errorCard = buildErrorCard();

        root.add(loadingCard, CARD_LOADING);
        root.add(contentCard, CARD_CONTENT);
        root.add(errorCard, CARD_ERROR);
        showLoading("Loading……");
    }

    private JPanel buildErrorCard() {
        JPanel errorCard = new JPanel();
        errorCard.setLayout(new BoxLayout(errorCard, BoxLayout.Y_AXIS));
        errorCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        errorSummaryLabel = new JLabel("加载失败");
        errorSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorSummaryLabel.setFont(errorSummaryLabel.getFont().deriveFont(Font.BOLD, 14f));

        JButton retryButton = new JButton("重试");
        retryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        retryButton.addActionListener(e -> {
            if (retryAction != null) {
                retryAction.accept(null);
            }
        });

        JButton detailButton = new JButton("查看详情");
        detailButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailButton.addActionListener(e -> {
            JTextArea area = new JTextArea(errorDetailArea.getText());
            area.setEditable(false);
            area.setCaretPosition(0);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(520, 320));
            JOptionPane.showMessageDialog(root, scroll, "错误详情", JOptionPane.ERROR_MESSAGE);
        });

        errorDetailArea = new JTextArea();
        errorDetailArea.setEditable(false);
        errorDetailArea.setVisible(false);

        errorCard.add(errorSummaryLabel);
        errorCard.add(Box.createVerticalStrut(12));
        errorCard.add(retryButton);
        errorCard.add(Box.createVerticalStrut(8));
        errorCard.add(detailButton);
        errorCard.add(errorDetailArea);
        return errorCard;
    }

    public void showLoading(String message) {
        EdtGuard.assertEdt();
        loadingLabel.setText(message == null || message.isBlank() ? "Loading……" : message);
        cardLayout.show(root, CARD_LOADING);
    }

    public void showContent(JComponent content) {
        EdtGuard.assertEdt();
        contentCard.removeAll();
        if (content != null) {
            contentCard.add(content, BorderLayout.CENTER);
        }
        contentCard.revalidate();
        contentCard.repaint();
        cardLayout.show(root, CARD_CONTENT);
    }

    public void showError(String summary, Throwable error, Consumer<Void> retry) {
        EdtGuard.assertEdt();
        this.retryAction = retry;
        errorSummaryLabel.setText(summary == null ? "加载失败" : summary);
        if (error != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(error).append('\n');
            for (StackTraceElement el : error.getStackTrace()) {
                sb.append("  at ").append(el).append('\n');
                if (sb.length() > 4000) {
                    break;
                }
            }
            errorDetailArea.setText(sb.toString());
        } else {
            errorDetailArea.setText("");
        }
        cardLayout.show(root, CARD_ERROR);
    }
}
