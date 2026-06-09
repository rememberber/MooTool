package com.luoboduner.moo.tool.ui.frame;

import com.formdev.flatlaf.util.SystemInfo;
import com.luoboduner.moo.tool.ui.UiConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * 屏幕区域截图
 */
@Slf4j
public class ScreenCaptureFrame extends JWindow {

    private static ScreenCaptureFrame screenCaptureFrame;

    private final BufferedImage screenImage;
    private final Rectangle virtualBounds;
    private final Consumer<BufferedImage> captureCallback;
    private final Runnable cancelCallback;

    private Point startPoint;
    private Rectangle selectionRect;
    private boolean capturing;

    private ScreenCaptureFrame(BufferedImage screenImage, Rectangle virtualBounds,
                               Consumer<BufferedImage> captureCallback, Runnable cancelCallback) {
        super((Window) null);
        this.screenImage = screenImage;
        this.virtualBounds = virtualBounds;
        this.captureCallback = captureCallback;
        this.cancelCallback = cancelCallback;
        init();
    }

    public static void start(Consumer<BufferedImage> captureCallback, Runnable cancelCallback) {
        try {
            Rectangle virtualBounds = getVirtualScreenBounds();
            BufferedImage screenImage = captureScreens(virtualBounds);
            screenCaptureFrame = new ScreenCaptureFrame(screenImage, virtualBounds, captureCallback, cancelCallback);
            screenCaptureFrame.setVisible(true);
            screenCaptureFrame.requestFocus();
        } catch (AWTException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            JOptionPane.showMessageDialog(null, "截图失败：\n\n" + e.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            if (cancelCallback != null) {
                cancelCallback.run();
            }
        }
    }

    public static void exit() {
        if (screenCaptureFrame != null) {
            screenCaptureFrame.setVisible(false);
            screenCaptureFrame.dispose();
            screenCaptureFrame = null;
        }
    }

    private void init() {
        setName(UiConsts.APP_NAME);
        setAlwaysOnTop(true);
        setBounds(virtualBounds);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setFocusableWindowState(true);
        setFocusable(true);

        JPanel capturePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (screenImage == null) {
                    return;
                }
                g.drawImage(screenImage, 0, 0, null);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (selectionRect != null && selectionRect.width > 0 && selectionRect.height > 0) {
                    g2.drawImage(screenImage,
                            selectionRect.x, selectionRect.y,
                            selectionRect.x + selectionRect.width, selectionRect.y + selectionRect.height,
                            selectionRect.x, selectionRect.y,
                            selectionRect.x + selectionRect.width, selectionRect.y + selectionRect.height,
                            null);

                    g2.setColor(new Color(64, 158, 255));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);

                    String sizeText = selectionRect.width + " × " + selectionRect.height;
                    FontMetrics metrics = g2.getFontMetrics();
                    int textWidth = metrics.stringWidth(sizeText);
                    int textHeight = metrics.getHeight();
                    int textX = selectionRect.x;
                    int textY = selectionRect.y - 6;
                    if (textY < textHeight) {
                        textY = selectionRect.y + selectionRect.height + textHeight + 4;
                    }
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRoundRect(textX - 4, textY - textHeight, textWidth + 8, textHeight + 4, 6, 6);
                    g2.setColor(Color.WHITE);
                    g2.drawString(sizeText, textX, textY - 4);
                }

                g2.dispose();
            }
        };
        capturePanel.setLayout(null);

        JLabel hintLabel = new JLabel("拖动鼠标选择截图区域，Esc 取消");
        hintLabel.setForeground(Color.WHITE);
        hintLabel.setOpaque(true);
        hintLabel.setBackground(new Color(0, 0, 0, 160));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        hintLabel.setBounds(12, 12, 260, 28);
        capturePanel.add(hintLabel);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                startPoint = e.getPoint();
                selectionRect = new Rectangle(startPoint);
                capturePanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint == null) {
                    return;
                }
                selectionRect = createSelectionRect(startPoint, e.getPoint());
                capturePanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || startPoint == null || capturing) {
                    return;
                }
                selectionRect = createSelectionRect(startPoint, e.getPoint());
                startPoint = null;
                if (selectionRect.width < 5 || selectionRect.height < 5) {
                    selectionRect = null;
                    capturePanel.repaint();
                    return;
                }
                finishCapture();
            }
        };

        capturePanel.addMouseListener(mouseAdapter);
        capturePanel.addMouseMotionListener(mouseAdapter);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
            }
        });

        setContentPane(capturePanel);
    }

    private void finishCapture() {
        if (capturing || selectionRect == null) {
            return;
        }
        capturing = true;

        int x = Math.max(0, selectionRect.x);
        int y = Math.max(0, selectionRect.y);
        int width = Math.min(selectionRect.width, screenImage.getWidth() - x);
        int height = Math.min(selectionRect.height, screenImage.getHeight() - y);
        if (width <= 0 || height <= 0) {
            cancel();
            return;
        }

        BufferedImage captured = screenImage.getSubimage(x, y, width, height);
        exit();
        if (captureCallback != null) {
            captureCallback.accept(captured);
        }
    }

    private void cancel() {
        exit();
        if (cancelCallback != null) {
            cancelCallback.run();
        }
    }

    private static Rectangle createSelectionRect(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle(x, y, width, height);
    }

    private static Rectangle getVirtualScreenBounds() {
        Rectangle bounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : ge.getScreenDevices()) {
            bounds = bounds.union(device.getDefaultConfiguration().getBounds());
        }
        return bounds;
    }

    private static BufferedImage captureScreens(Rectangle virtualBounds) throws AWTException {
        Robot robot = new Robot();
        if (SystemInfo.isMacOS) {
            robot.delay(50);
        }

        BufferedImage image = new BufferedImage(virtualBounds.width, virtualBounds.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice device : ge.getScreenDevices()) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                BufferedImage screen = robot.createScreenCapture(bounds);
                g.drawImage(screen, bounds.x - virtualBounds.x, bounds.y - virtualBounds.y, null);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
