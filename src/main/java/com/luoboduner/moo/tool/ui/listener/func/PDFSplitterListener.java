package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.dialog.CommonTipsDialog;
import com.luoboduner.moo.tool.ui.form.func.PdfForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class PDFSplitterListener {

    private JPanel jPanel;
    private AbstractTableModel model;
    private List<Object[]> components;

    private JButton newTask;
    private JLabel helpBtn;
    private JButton startTask;

    private static Pattern compile = Pattern.compile("^[0-9]+-[0-9]+$");
    private static Pattern ruleCompile = Pattern.compile("^\\d+([-;]\\d+?)*?$");

    public PDFSplitterListener(JPanel jPanel, AbstractTableModel model) {
        this.jPanel = jPanel;
        this.model = model;
    }

    public void selectFile(File file, int row) {
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            showErrorMessage("错误：文件类型错误，该功能仅支持PDF格式文件。");
        } else {
            JLabel label = get(row, 2, JLabel.class);
            label.setText(file.getName());
            getPdfReader(file, reader -> {
                int pages = reader.getNumberOfPages();
                JTextField jTextField = get(row, 3, JTextField.class);
                jTextField.setText("1-" + pages);

                JProgressBar progressBar = get(row, 6, JProgressBar.class);
                JLabel statusLabel = get(row, 7, JLabel.class);
                progressBar.setValue(0);
                progressBar.updateUI();
                statusLabel.setText("未开始");

                PdfParam param = getPdfParam(row);
                param.setFile(file);
                param.setPageRange("1-" + pages);
                param.setRule("");
                param.setMaxPage(pages);
                param.setSplitType(SplitType.ODD);
            });
            model.fireTableDataChanged();
        }
    }

    private PdfParam initParam() {
        PdfParam param = new PdfParam();
        param.setFile(null);
        param.setPageRange(null);
        param.setRule("");
        param.setMaxPage(0);
        param.setSplitType(SplitType.ODD);
        return param;
    }

    private PdfParam getPdfParam(int row) {
        PdfParam param = get(row, 8, PdfParam.class);
        if (null == param) {
            param = initParam();
            this.components.get(row)[8] = param;
        }
        return param;
    }

    public void changeSplitRange(String range, int row) {
        PdfParam param = this.getPdfParam(row);
        JTextField jTextField = get(row, 3, JTextField.class);
        if (null == param.getFile()) {
            return;
        }

        if (StringUtils.isBlank(range)) {
            showErrorMessage("错误：拆分范围不能为空！");
            return;
        }

        if (!compile.matcher(range).find()) {
            showErrorMessage("错误：拆分范围内容输入错误，例如（1-20或15-15）！");
            asyncRun(() -> jTextField.setText("1-" + param.getMaxPage()));
        } else {
            String[] split = range.split("-");
            Integer start = NumberUtils.createInteger(split[0]);
            Integer end = NumberUtils.createInteger(split[1]);
            if (start > end) {
                showErrorMessage("错误：拆分范围内容输入错误，例如（1-20或15-15）！");
                asyncRun(() -> jTextField.setText("1-" + param.getMaxPage()));
                return;
            }
            if (start < 1 || param.getMaxPage() < start) {
                showErrorMessage("错误：拆分范围内容输入错误，拆分起始页只能是1-" + param.getMaxPage() + "中任一一个数字。");
                asyncRun(() -> jTextField.setText("1-" + param.getMaxPage()));
                return;
            } else if (param.maxPage < end) {
                showErrorMessage("错误：拆分范围内容输入错误，拆分终止页只能是1-" + param.getMaxPage() + "中任一一个数字。");
                asyncRun(() -> jTextField.setText("1-" + param.getMaxPage()));
                return;
            }
            param.setPageRange(range);
            model.fireTableDataChanged();
        }
    }

    public void changeSplitRuleType(String type, int row) {
        SplitType splitType = SplitType.valueOfDesc(type);
        PdfParam param = this.getPdfParam(row);
        param.setSplitType(splitType);
        JTextField customRule = this.get(row, 5, JTextField.class);
        if (SplitType.CUSTOM == splitType) {
            customRule.setEnabled(true);
        } else {
            customRule.setEnabled(false);
            customRule.setText(null);
        }
    }

    public void changeSplitRule(String rule, int row) {
        if (StringUtils.isBlank(rule)) {
            showErrorMessage("错误：自定义规则不能为空！");
            return;
        }
        PdfParam param = this.getPdfParam(row);
        param.setRule(rule);
    }

    public void setSplit(int row, boolean split) {
        PdfParam param = this.getPdfParam(row);
        param.setSplit(split);
    }

    private <T> T get(int row, int column, Class<T> tClass) {
        Object o = this.components.get(row)[column];
        return tClass.cast(o);
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

    private String ruleChecker(String rule) {
        if (StringUtils.isBlank(rule)) {
            return "自定义规则不能为空！";
        }
        if (!ruleCompile.matcher(rule).find()) {
            return "自定义规则格式输入错误！";
        }
        return null;
    }

    public void startSplit() {
        List<String> errs = new ArrayList<>(1);
        forEachComponents((param, i) -> {
            if (SplitType.CUSTOM == param.getSplitType()) {
                String errorMsg = ruleChecker(param.getRule());
                if (StringUtils.isNotBlank(errorMsg)) {
                    errs.add(0, "错误：" + param.getFile().getName() + errorMsg);
                }
            }
            if (param.isSplit && null == param.getFile()) {
                errs.add(0, String.format("错误：第%d个任务未选择文件！" , i + 1));
            }
        });

        if (!errs.isEmpty()) {
            showErrorMessage(errs.get(0));
            return;
        }

        forEachComponents((param, i) -> {
            JProgressBar progressBar = get(i, 6, JProgressBar.class);
            progressBar.setValue(0);
            model.fireTableCellUpdated(i, 6);
            JLabel label = get(i, 7, JLabel.class);
            label.setText("未开始");
            model.fireTableCellUpdated(i, 7);
        });

        forEachComponents((param, n) -> {
            PdfReader reader = null;
            Document document = null;
            PdfCopy pdfCopy = null;
            try {
                JProgressBar progressBar = get(n, 6, JProgressBar.class);
                JLabel label = get(n, 7, JLabel.class);
                label.setText("拆分中");
                String filePath = param.getFile().getAbsolutePath();
                String parent = param.getFile().getParent();
                document = new Document();
                pdfCopy = new PdfCopy(document, new FileOutputStream(parent + File.separator + newFileName(param.getFile().getName())));
                reader = new PdfReader(filePath);
                int numberOfPages = reader.getNumberOfPages();
                document.open();
                String pageRange = param.getPageRange();
                String[] split = pageRange.split("-");
                for (int i = 1; i <= numberOfPages; i++) {
                    if (Integer.valueOf(split[0]) <= i && i <= Integer.valueOf(split[1])) {
                        if (param.getSplitType().canSplit(i, param.getRule())) {
                            document.newPage();
                            PdfImportedPage page = pdfCopy.getImportedPage(reader, i);
                            pdfCopy.addPage(page);
                        }
                    }
                    progressBar.setValue((int)((float) i / (float) numberOfPages * 100));
                    progressBar.updateUI();
                    model.fireTableDataChanged();
                }
                label.setText("已完成");
                model.fireTableDataChanged();
            } catch (Exception e) {
                log.error("拆分文件出错！", e);
            } finally {
                if (null != document) {
                    document.close();
                }
                if (null != pdfCopy) {
                    pdfCopy.close();
                }
                if (null != reader) {
                    reader.close();
                }
            }
        });
    }

    private void asyncRun(Runnable runnable) {
        ThreadUtil.execute(runnable);
    }

    private String newFileName(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        lowerCaseName = lowerCaseName.replace(".pdf", "");
        return lowerCaseName + "_split.pdf";
    }

    private void forEachComponents(BiConsumer<PdfParam, Integer> consumer) {
        for (int i = 0; i < this.components.size(); i++) {
            Object[] objects = this.components.get(i);
            Object obj = objects[8];
            if (null == obj) {
                continue;
            }
            PdfParam param = (PdfParam) obj;
            if (param.isSplit) {
                consumer.accept(param, i);
            }
        }
    }

    public void startListener() {
        startTask.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                startSplit();
            }
        });

        newTask.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ((PdfForm.PDFTableModel) model).add();
            }
        });

        helpBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>关于PDF拆分</h1>");
                tipsBuilder.append("<p>1. 该拆分功能默认可并行同时拆分10个PDF，可增加最大任务上线为20个。</p>");
                tipsBuilder.append("<p>2. 当勾选了待拆分的任务后，该任务才会生效。</p>");
                tipsBuilder.append("<p>3. PDF拆分范围默认1-最大页(PDF最大页)，不可设置1-最大页以外的页数。</p>");
                tipsBuilder.append("<p>4. 拆分规则为“奇数”、“偶数”、“自定义”三种规则，都依托于前面的拆分范围，当设定拆分范围后，拆分器按照“奇数”、“偶数”、“自定义”进行拆分。</p>");
                tipsBuilder.append("<p>5. 拆分规则为“自定义”规则时，需要设定自定义规则，该规则可设置“范围”、“指定页”。<br/>");
                tipsBuilder.append("<ul>");
                tipsBuilder.append("<li>范围：1-5;6-7</li>");
                tipsBuilder.append("<li>指定页：1;5;9;10</li>");
                tipsBuilder.append("<li>范围与指定页(指定页与指定页，范围与范围)之间，需要用英文分号“;”隔开</li>");
                tipsBuilder.append("</ul></p>");
                tipsBuilder.append("<p>6. 拆分后的文件在源文件的同级目录下，后缀为“_split.pdf”。<br/>");
                tipsBuilder.append("<p>7. 本功能仅支持未加密的PDF。<br/>");

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

    private void showMessageDialog(String message, String title, int type) {
        JOptionPane.showMessageDialog(jPanel, message, title, type);
    }

    private void showErrorMessage(String message) {
        showMessageDialog(message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static class PdfParam {
        /** 待拆分PDF页面范围 */
        private String pageRange;
        /** 拆分类型 */
        private SplitType splitType;
        /** 拆分规则 */
        private String rule;

        private int maxPage;

        private File file;

        private boolean isSplit;

        public String getPageRange() {
            return pageRange;
        }

        public void setPageRange(String pageRange) {
            this.pageRange = pageRange;
        }

        public SplitType getSplitType() {
            return splitType;
        }

        public void setSplitType(SplitType splitType) {
            this.splitType = splitType;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public int getMaxPage() {
            return maxPage;
        }

        public void setMaxPage(int maxPage) {
            this.maxPage = maxPage;
        }

        public boolean isSplit() {
            return isSplit;
        }

        public void setSplit(boolean split) {
            isSplit = split;
        }
    }

    public interface SplitRule {
        boolean canSplit(Integer num, String rule);
    }

    public enum SplitType implements SplitRule {
        EVEN("偶数") {
            @Override
            public boolean canSplit(Integer num, String rule) {
                return num % 2 == 0;
            }
        },
        ODD("奇数") {
            @Override
            public boolean canSplit(Integer num, String rule) {
                return num % 2 != 0;
            }
        },
        CUSTOM("自定义") {
            @Override
            public boolean canSplit(Integer num, String rule) {
                String[] args = rule.split(";");
                for (String elem : args) {
                    if (elem.contains("-")) {
                        String[] split = elem.split("-");
                        if (Integer.valueOf(split[0]) <= num && num <= Integer.valueOf(split[1])) {
                            return true;
                        }
                    } else {
                        if (Integer.valueOf(elem).compareTo(num) == 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

        private String desc;

        SplitType(String desc) {
            this.desc = desc;
        }

        public static SplitType valueOfDesc(String desc) {
            return Stream.of(SplitType.values())
                    .filter(item -> item.getDesc().equals(desc))
                    .findAny()
                    .orElse(null);
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    public void setComponents(List<Object[]> components) {
        this.components = components;
    }

    public void setNewTask(JButton newTask) {
        this.newTask = newTask;
    }

    public void setHelpBtn(JLabel helpBtn) {
        this.helpBtn = helpBtn;
    }

    public void setStartTask(JButton startTask) {
        this.startTask = startTask;
    }
}
