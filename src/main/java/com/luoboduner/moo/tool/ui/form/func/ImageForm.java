package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.listener.func.ImageListener;
import com.luoboduner.moo.tool.util.JTableUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * <pre>
 * ImageForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/13.
 */
@Getter
public class ImageForm {
    private JPanel imagePanel;
    private JTable listTable;
    private JButton deleteButton;
    private JButton 截图Button;
    private JButton saveFromClipboardButton;
    private JButton openButton;
    private JSplitPane splitPane;
    private JPanel showImagePanel;
    private JLabel showImageLabel;
    private JScrollPane scrollPane;
    private JPanel menuPanel;
    private JButton copyToClipboardButton;
    private JButton saveButton;
    private JButton listItemButton;
    private JButton exportButton;
    private JPanel deletePanel;
    private JButton newButton;
    private JButton ocrButton;
    private JButton scaleImageButton;
    private JButton pressImageButton;

    private static ImageForm imageForm;

    private static final Log logger = LogFactory.get();

    private ImageForm() {
        UndoUtil.register(this);
    }

    public void getImageFromClipboard() {
        try {
            Image image = ClipboardUtil.getImage();
            ImageListener.selectedImage = image;
            if (image != null) {
                getInstance().getShowImageLabel().setIcon(new ImageIcon(image));
            } else {
                JOptionPane.showMessageDialog(App.mainFrame, "还没有复制图片到剪贴板吧？\n\n", "失败", JOptionPane.WARNING_MESSAGE);
            }
        } catch (HeadlessException ex) {
            ex.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(ex));
        }
    }

    public static ImageForm getInstance() {
        if (imageForm == null) {
            imageForm = new ImageForm();
        }
        return imageForm;
    }

    public static void init() {
        imageForm = getInstance();

        initUi();
        initListTable();
    }

    private static void initUi() {
        imageForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 5));
        imageForm.getListTable().setRowHeight(UiConsts.TABLE_ROW_HEIGHT);

        imageForm.getDeletePanel().setVisible(false);

        // 设置滚动条速度
        imageForm.getScrollPane().getVerticalScrollBar().setUnitIncrement(16);
        imageForm.getScrollPane().getVerticalScrollBar().setDoubleBuffered(true);
        imageForm.getScrollPane().getHorizontalScrollBar().setUnitIncrement(16);
        imageForm.getScrollPane().getHorizontalScrollBar().setDoubleBuffered(true);

        imageForm.getSaveFromClipboardButton().grabFocus();

        imageForm.getImagePanel().updateUI();
    }

    public static void initListTable() {
        String[] headerNames = {"id", "名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        imageForm.getListTable().setModel(model);
        // 隐藏表头
        JTableUtil.hideTableHeader(imageForm.getListTable());
        // 隐藏id列
        JTableUtil.hideColumn(imageForm.getListTable(), 0);

        Object[] data;

        List<String> fileNames = FileUtil.listFileNames(ImageListener.IMAGE_PATH_PRE_FIX);
        for (String fileName : fileNames) {
            data = new Object[2];
            data[0] = fileName;
            data[1] = fileName;
            model.addRow(data);
        }
        if (fileNames.size() > 0) {
            imageForm.getShowImageLabel().setIcon(new ImageIcon(ImageListener.IMAGE_PATH_PRE_FIX + fileNames.get(0)));
            imageForm.getShowImagePanel().updateUI();
            imageForm.getListTable().setRowSelectionInterval(0, 0);
            ImageListener.selectedName = fileNames.get(0).replace(".png", "");
            try {
                ImageListener.selectedImage = ImageIO.read(FileUtil.newFile(ImageListener.IMAGE_PATH_PRE_FIX + fileNames.get(0)));
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        imagePanel = new JPanel();
        imagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(204);
        splitPane.setDividerSize(2);
        imagePanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        deletePanel = new JPanel();
        deletePanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 5, 5, 5), -1, -1));
        panel1.add(deletePanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("");
        deleteButton.setToolTipText("删除");
        deletePanel.add(deleteButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        deletePanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setIcon(new ImageIcon(getClass().getResource("/icon/export_dark.png")));
        exportButton.setText("");
        exportButton.setToolTipText("导出");
        deletePanel.add(exportButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        listTable = new JTable();
        scrollPane1.setViewportView(listTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel3);
        menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayoutManager(1, 11, new Insets(5, 5, 5, 5), -1, -1));
        panel3.add(menuPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        截图Button = new JButton();
        截图Button.setText("截图");
        menuPanel.add(截图Button, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        menuPanel.add(spacer2, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        saveFromClipboardButton = new JButton();
        saveFromClipboardButton.setText("从剪贴板获取");
        saveFromClipboardButton.setToolTipText("Ctrl+V");
        menuPanel.add(saveFromClipboardButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openButton = new JButton();
        openButton.setText("从系统打开");
        menuPanel.add(openButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyToClipboardButton = new JButton();
        copyToClipboardButton.setText("复制到剪贴板");
        copyToClipboardButton.setToolTipText("Ctrl+C");
        menuPanel.add(copyToClipboardButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("保存");
        saveButton.setToolTipText("Ctrl+S");
        menuPanel.add(saveButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        listItemButton = new JButton();
        listItemButton.setIcon(new ImageIcon(getClass().getResource("/icon/listFiles_dark.png")));
        listItemButton.setText("");
        menuPanel.add(listItemButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        newButton = new JButton();
        newButton.setText("新建");
        newButton.setToolTipText("Ctrl+N");
        menuPanel.add(newButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ocrButton = new JButton();
        ocrButton.setText("OCR识别");
        menuPanel.add(ocrButton, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scaleImageButton = new JButton();
        scaleImageButton.setText("压缩");
        menuPanel.add(scaleImageButton, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pressImageButton = new JButton();
        pressImageButton.setText("加水印");
        menuPanel.add(pressImageButton, new GridConstraints(0, 10, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        panel3.add(scrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        showImagePanel = new JPanel();
        showImagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane.setViewportView(showImagePanel);
        showImageLabel = new JLabel();
        showImageLabel.setText("");
        showImagePanel.add(showImageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return imagePanel;
    }

}
