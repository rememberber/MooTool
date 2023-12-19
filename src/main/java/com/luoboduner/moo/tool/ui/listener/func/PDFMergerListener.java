package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.dialog.CommonTipsDialog;
import com.luoboduner.moo.tool.ui.form.func.PdfForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
public class PDFMergerListener {

    private static Pattern rangeCompile = Pattern.compile("^\\d+([-;]\\d+?)*?$");
    private JPanel jPanel;
    private AbstractTableModel model;
    private List<Object[]> components;
    private JButton startMerge;
    private JLabel helpMerge;
    private JButton addFile;

    private String outputDir;

    public PDFMergerListener(JPanel jPanel, AbstractTableModel model) {
        this.jPanel = jPanel;
        this.model = model;
    }

    public void setComponents(List<Object[]> components) {
        this.components = components;
    }

    public void startListener() {
        startMerge.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                startMerge();
            }
        });

        addFile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ((PdfForm.PDFTableModel) model).add();
            }
        });

        helpMerge.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>关于PDF合并</h1>");
                tipsBuilder.append("<p>1. 该合并功能默认可并行同时拆分10个PDF，可增加最大任务上线为20个。</p>");
                tipsBuilder.append("<p>2. 当勾选了待合并的任务后，该任务才会生效。</p>");
                tipsBuilder.append("<p>3. PDF合并范围默认1-最大页(PDF最大页)，不可设置1-最大页以外的页数。</p>");
                tipsBuilder.append("<p>4. 合并范围可设置“范围”、“指定页”。<br/>");
                tipsBuilder.append("<ul>");
                tipsBuilder.append("<li>范围：1-5;6-7</li>");
                tipsBuilder.append("<li>指定页：1;5;9;10</li>");
                tipsBuilder.append("<li>范围与指定页(指定页与指定页，范围与范围)之间，需要用英文分号“;”隔开</li>");
                tipsBuilder.append("</ul></p>");
                tipsBuilder.append("<p>5. 合并后的文件在桌面目录下，文件名为“merge.pdf”。<br/>");
                tipsBuilder.append("<p>6. 本功能仅支持未加密的PDF。<br/>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(UiConsts.HELP_FOCUSED_ICON);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(UiConsts.HELP_ICON);
                super.mouseExited(e);
            }
        });
    }

    public void selectFile(File file, int row) {
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            showErrorMessage("错误：文件类型错误，该功能仅支持PDF格式文件。");
        } else {
            getPdfReader(file, reader -> {
                JLabel label = get(row, 2, JLabel.class);
                label.setText(file.getName());

                int pages = reader.getNumberOfPages();
                JTextField jTextField = get(row, 3, JTextField.class);
                jTextField.setText("1-" + pages);

                JProgressBar progressBar = get(row, 4, JProgressBar.class);
                JLabel statusLabel = get(row, 5, JLabel.class);
                progressBar.setValue(0);
                progressBar.updateUI();
                statusLabel.setText("未开始");

                PdfParam param = getPdfParam(row);
                param.setFile(file);
                param.setPageRange("1-" + pages);
                param.setMaxPage(pages);
            });
            model.fireTableRowsUpdated(row, row);
        }
    }

    public void setStartMerge(JButton startMerge) {
        this.startMerge = startMerge;
    }

    public void setHelpMerge(JLabel helpMerge) {
        this.helpMerge = helpMerge;
    }

    public void setAddFile(JButton addFile) {
        this.addFile = addFile;
    }

    private void startMerge() {
        Document document= null;
        PdfCopy pdfCopy = null;
        int i = 0;
        try {
            checker();
            document = new Document();
            pdfCopy = new PdfCopy(document, new FileOutputStream(getOutputDir() + File.separator + "merge.pdf"));
            document.open();
            for (; i < this.components.size(); i++) {
                PdfParam pdfParam = getPdfParam(i);
                if (pdfParam.isMerge) {
                    copyPdf(pdfParam, document, pdfCopy);

                    JProgressBar progressBar = get(i, 4, JProgressBar.class);
                    progressBar.setValue(100);
                    JLabel label = get(i, 5, JLabel.class);
                    label.setText("已完成");
                    model.fireTableRowsUpdated(i, i);
                }
            }
        } catch (IllegalArgumentException e) {
            showErrorMessage("错误：" + e.getMessage());
            JTextField jTextField = get(i, 3, JTextField.class);
            PdfParam pdfParam = getPdfParam(i);
            ThreadUtil.execute(() -> jTextField.setText("1-" + pdfParam.getMaxPage()));
        } catch (Exception e) {
            log.error("合并文件错误！", e);
        } finally {
            if (null != document) {
                document.close();
            }
            if (null != pdfCopy) {
                pdfCopy.close();
            }
        }
    }

    private <T> T get(int row, int column, Class<T> tClass) {
        Object o = this.components.get(row)[column];
        return tClass.cast(o);
    }

    private String getOutputDir() {
        if (StringUtils.isBlank(this.outputDir)) {
            synchronized (this) {
                if (StringUtils.isBlank(this.outputDir)) {
                    this.outputDir = System.getProperty("user.home") + File.separator + "Desktop";
                }
            }
        }
        return this.outputDir;
    }

    private PdfParam getPdfParam(int row) {
        PdfParam param = get(row, 6, PdfParam.class);
        if (null == param) {
            param = initParam();
            this.components.get(row)[6] = param;
        }
        return param;
    }

    private PdfParam initParam() {
        PdfParam param = new PdfParam();
        param.setFile(null);
        param.setPageRange(null);
        param.setMaxPage(0);
        return param;
    }

    private void showMessageDialog(String message, String title, int type) {
        JOptionPane.showMessageDialog(jPanel, message, title, type);
    }

    private void showErrorMessage(String message) {
        showMessageDialog(message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void getPdfReader(File file, Consumer<PdfReader> consumer) {
        PdfReader reader = null;
        try {
            reader = new PdfReader(file.getAbsolutePath());
            consumer.accept(reader);
        } catch (Exception e) {
            log.error("获取PDFReader出错！", e);
        } finally {
            if (null != reader) {
                reader.close();
            }
        }
    }

    public void changeMergeRange(String text, int row) {
        PdfParam pdfParam = getPdfParam(row);
        pdfParam.setPageRange(text);
    }

    private void checker() throws IllegalArgumentException {
        int mergeCount = 0;
        for (int i = 0; i < this.components.size(); i++) {
            PdfParam pdfParam = getPdfParam(i);
            if (pdfParam.isMerge) {
                if (null == pdfParam.getFile()) {
                    throw new IllegalArgumentException(String.format("第%d个没有选择PDF文件！", i + 1));
                }
                if (!rangeCompile.matcher(pdfParam.getPageRange()).find()) {
                    throw new IllegalArgumentException("PDF合并范围格式输入错误！");
                }
                validRange(pdfParam.getPageRange());
                if (pdfParam.outOfRange()) {
                    throw new IllegalArgumentException(String.format("第%d个PDF文件合并范围有误，该PDF最大页数是%d！", i + 1, pdfParam.getMaxPage()));
                }
                mergeCount++;
            }
        }

        if (mergeCount < 2) {
            throw new IllegalArgumentException("待合并的PDF最少是2个！");
        }
    }

    public void setMerge(boolean isSelected, int row) {
        PdfParam pdfParam = getPdfParam(row);
        pdfParam.setMerge(isSelected);
    }

    public void copyPdf(PdfParam param, Document document, PdfCopy pdfCopy) throws Exception {
        getPdfReader(param.getFile(), reader -> {
            int pages = reader.getNumberOfPages();
            if (0 == pages) {
                return;
            }
            Predicate<Integer> predicate = num -> {
                String[] ranges = param.pageRange.split(";");
                for (String range : ranges) {
                    if (range.contains("-")) {
                        String[] split = range.split("-");
                        if (Integer.valueOf(split[0]) <= num && num <= Integer.valueOf(split[1])) {
                            return true;
                        }
                    }
                }
                return false;
            };

            for (int i = 1; i <= pages; i++) {
                if (predicate.test(i)) {
                    document.newPage();
                    try {
                        pdfCopy.addPage(pdfCopy.getImportedPage(reader, i));
                    } catch (Exception e) {
                        log.error("PDF拷贝出错！", e);
                    }
                }
            }
        });
    }

    private void validRange(String pageRange) {
        String[] ranges = pageRange.split(";");
        for (String range : ranges) {
            if (range.contains("-")) {
                String[] split = range.split("-");
                if (Integer.valueOf(split[0]) > Integer.valueOf(split[1])) {
                    throw new IllegalArgumentException("PDF合并范围格式输入错误！");
                }
            }
        }
    }

    public static class PdfParam {
        /**
         * 待合并PDF页面范围
         */
        private String pageRange;

        private int maxPage;

        private File file;

        private boolean isMerge;

        public String getPageRange() {
            return pageRange;
        }

        public void setPageRange(String pageRange) {
            this.pageRange = pageRange;
        }

        public int getMaxPage() {
            return maxPage;
        }

        public void setMaxPage(int maxPage) {
            this.maxPage = maxPage;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public boolean isMerge() {
            return isMerge;
        }

        public void setMerge(boolean merge) {
            isMerge = merge;
        }

        private boolean outOfRange() {
            String[] ranges = this.pageRange.split(";");
            for (String range : ranges) {
                if (range.contains("-")) {
                    String[] split = range.split("-");
                    for (int i = Integer.valueOf(split[0]); i <= Integer.valueOf(split[1]); i++) {
                        if (i < 1 || maxPage < i) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
