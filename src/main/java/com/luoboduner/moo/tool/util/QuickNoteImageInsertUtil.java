package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.ui.listener.func.ImageListener;
import com.luoboduner.moo.tool.ui.listener.func.QuickNoteListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 随手记图片插入工具
 */
@Slf4j
public class QuickNoteImageInsertUtil {

    private QuickNoteImageInsertUtil() {
    }

    public static void insertImageFromChooser() {
        RSyntaxTextArea textArea = getCurrentTextArea();
        if (textArea == null) {
            return;
        }

        SystemFileChooser fileChooser = new SystemFileChooser(QuickNoteAttachmentUtil.getAttachmentsDir());
        SystemFileChooser.FileNameExtensionFilter filter = new SystemFileChooser.FileNameExtensionFilter(
                "图片文件 (*.png, *.jpg, *.jpeg, *.gif, *.bmp, *.webp)",
                "png", "jpg", "jpeg", "gif", "bmp", "webp");
        fileChooser.setFileFilter(filter);
        int approve = fileChooser.showOpenDialog(MainWindow.getInstance().getMainPanel());
        if (approve != SystemFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || !QuickNoteAttachmentUtil.isImageFile(selectedFile)) {
            MsgUtil.info(MainWindow.getInstance().getMainPanel(), "msg.selectValidImage");
            return;
        }

        try {
            insertImageFromFile(textArea, selectedFile);
        } catch (IOException e) {
            MsgUtil.errorWithDetail(MainWindow.getInstance().getMainPanel(), "msg.insertImageFailed", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public static boolean tryPasteImage(RSyntaxTextArea textArea) {
        if (textArea == null) {
            return false;
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);
        if (transferable == null) {
            return false;
        }

        try {
            if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.imageFlavor);
                if (data instanceof Image image) {
                    insertBufferedImage(textArea, ImageListener.toBufferedImage(image));
                    return true;
                }
            }

            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files != null && files.size() == 1 && QuickNoteAttachmentUtil.isImageFile(files.get(0))) {
                    insertImageFromFile(textArea, files.get(0));
                    return true;
                }
            }
        } catch (IOException | UnsupportedFlavorException e) {
            log.error("粘贴图片失败: {}", ExceptionUtils.getStackTrace(e));
        }

        Image hutoolImage = ClipboardUtil.getImage();
        if (hutoolImage != null) {
            try {
                insertBufferedImage(textArea, ImageListener.toBufferedImage(hutoolImage));
                return true;
            } catch (IOException e) {
                log.error("粘贴图片失败: {}", ExceptionUtils.getStackTrace(e));
            }
        }

        return false;
    }

    public static void insertImageFromFile(RSyntaxTextArea textArea, File sourceFile) throws IOException {
        BufferedImage image = ImageIO.read(sourceFile);
        if (image == null) {
            throw new IOException("无法读取图片: " + sourceFile.getName());
        }
        String relativePath = QuickNoteAttachmentUtil.saveImage(sourceFile);
        String alt = FileUtil.mainName(sourceFile.getName());
        insertMarkdownImage(textArea, relativePath, alt);
    }

    private static void insertBufferedImage(RSyntaxTextArea textArea, BufferedImage image) throws IOException {
        String relativePath = QuickNoteAttachmentUtil.saveImage(image);
        insertMarkdownImage(textArea, relativePath, "image");
    }

    private static void insertMarkdownImage(RSyntaxTextArea textArea, String relativePath, String alt) {
        String markdown = QuickNoteAttachmentUtil.toMarkdownImage(relativePath, alt);
        int caret = textArea.getCaretPosition();
        String text = textArea.getText();
        String toInsert = markdown;

        if (caret > 0 && text.charAt(caret - 1) != '\n') {
            toInsert = "\n" + toInsert;
        }
        if (caret < text.length() && text.charAt(caret) != '\n') {
            toInsert = toInsert + "\n";
        }

        textArea.insert(toInsert, caret);
        textArea.setCaretPosition(caret + toInsert.length());
        QuickNoteListener.quickSave(true, false);
    }

    private static RSyntaxTextArea getCurrentTextArea() {
        if (QuickNoteForm.quickNoteRSyntaxTextViewerManager == null) {
            return null;
        }
        if (StringUtils.isBlank(QuickNoteListener.selectedName)) {
            MsgUtil.info(MainWindow.getInstance().getMainPanel(), "msg.selectOrCreateNote");
            return null;
        }
        return QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
    }
}
