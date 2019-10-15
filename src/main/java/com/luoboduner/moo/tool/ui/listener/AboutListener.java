package com.luoboduner.moo.tool.ui.listener;

import com.luoboduner.moo.tool.ui.form.AboutForm;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <pre>
 * AboutListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/15.
 */
public class AboutListener {

    public static void addListeners() {
        AboutForm aboutForm = AboutForm.getInstance();

        aboutForm.getCodeGitHubLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/MooTool"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getCodeGiteeLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/zhoubochina/MooTool"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getIssueLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/MooTool/issues"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getDaculaLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/bulenkov/Darcula"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getBeautyEyeLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/jackjiang/beautyeye"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getHutoolLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://hutool.cn/"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getWePushPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/WePush"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
        aboutForm.getVsCodeIconsLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/microsoft/vscode-icons"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }
}
