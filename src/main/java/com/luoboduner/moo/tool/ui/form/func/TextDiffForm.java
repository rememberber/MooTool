package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.bean.textdiff.UIDiff;
import com.luoboduner.moo.tool.bean.textdiff.UnifiedView;
import com.luoboduner.moo.tool.dao.TFuncContentMapper;
import com.luoboduner.moo.tool.domain.TFuncContent;
import com.luoboduner.moo.tool.service.DiffService;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.component.textviewer.RegexRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.listener.func.TextDiffListener;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
/**
 * <pre>
 * TextDiffForm - 文本对比功能界面
 * </pre>
 *
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 10:51
 */
@Getter
public class TextDiffForm {
    private JPanel textDiffPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel toolBarPanel;
    private JButton compareButton;
    private JButton clearButton;
    private JButton swapButton;
    private JButton copyDiffButton;
    private JButton prevDiffButton;
    private JButton nextDiffButton;
    private JComboBox<String> highlightModeComboBox;
    private JLabel statusLabel;
    private JCheckBox realTimeCheckBox;
    private JCheckBox ignoreWhitespaceCheckBox;
    private JComboBox<String> displayModeComboBox;
    private JSplitPane mainSplitPane;
    private JScrollPane leftScrollPane;
    private JScrollPane rightScrollPane;
    private JScrollPane unifiedScrollPane;
    private JPanel unifiedPanel;

    private RegexRSyntaxTextViewer leftTextArea;
    private RegexRSyntaxTextViewer rightTextArea;
    private RegexRSyntaxTextViewer unifiedTextArea;

    // Diff highlight painters
    private final DefaultHighlighter.DefaultHighlightPainter delPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 204, 204)); // light red for deletions
    private final DefaultHighlighter.DefaultHighlightPainter insPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(204, 255, 204)); // light green for insertions
    private final DefaultHighlighter.DefaultHighlightPainter changePainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 249, 196)); // light yellow for modifications

    // 滚动同步状态标记，避免循环触发
    private boolean syncingScroll = false;

    // 行级高亮（带透明度）
    private final DefaultHighlighter.DefaultHighlightPainter delLinePainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 102, 102, 60));
    private final DefaultHighlighter.DefaultHighlightPainter insLinePainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0, 153, 0, 60));
    private final DefaultHighlighter.DefaultHighlightPainter changeLinePainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 193, 7, 60));

    // 统一视图：hunk 或标题行高亮
    private final DefaultHighlighter.DefaultHighlightPainter headerLinePainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(230, 230, 230));

    // 差异导航锚点与索引（按左/右侧段的中点行）
    private final List<Integer> leftNavOffsets = new ArrayList<>();
    private final List<Integer> rightNavOffsets = new ArrayList<>();
    private int navIndex = -1;

    private enum HighlightMode { CHAR_ONLY, LINE_ONLY, BOTH }
    private HighlightMode highlightMode = HighlightMode.BOTH;

    private static TextDiffForm textDiffForm;

    private static boolean i18nRegistered;

    private static final Log logger = LogFactory.get();

    private static final TFuncContentMapper funcContentMapper = MybatisUtil.getSqlSession().getMapper(TFuncContentMapper.class);

    private TextDiffForm() {
        UndoUtil.register(this);
        initTextAreas();
    }

    public static TextDiffForm getInstance() {
        if (textDiffForm == null) {
            textDiffForm = new TextDiffForm();
        }
        return textDiffForm;
    }

    public static void init() {
        textDiffForm = getInstance();
        textDiffForm.initForm();
        textDiffForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(TextDiffForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setText(compareButton, "textDiff.compare");
        I18nUiUtil.setText(clearButton, "textDiff.clear");
        I18nUiUtil.setText(swapButton, "textDiff.swap");
        I18nUiUtil.setText(copyDiffButton, "textDiff.copyDiff");
        I18nUiUtil.setText(prevDiffButton, "textDiff.prevDiff");
        I18nUiUtil.setText(nextDiffButton, "textDiff.nextDiff");
        I18nUiUtil.setText(realTimeCheckBox, "textDiff.realTime");
        I18nUiUtil.setText(ignoreWhitespaceCheckBox, "textDiff.ignoreWhitespace");
        I18nUiUtil.setTitledBorder(leftPanel, "textDiff.leftPanel");
        I18nUiUtil.setTitledBorder(rightPanel, "textDiff.rightPanel");
        I18nUiUtil.setTitledBorder(unifiedPanel, "textDiff.unifiedPanel");

        refreshDisplayModeComboBox();
        refreshHighlightModeComboBox();
        refreshIdleStatus();
    }

    private static void applyI18nStatic() {
        if (textDiffForm != null) {
            textDiffForm.applyI18n();
        }
    }

    private void refreshDisplayModeComboBox() {
        int index = displayModeComboBox.getSelectedIndex();
        displayModeComboBox.removeAllItems();
        displayModeComboBox.addItem(I18n.get("textDiff.display.sideBySide"));
        displayModeComboBox.addItem(I18n.get("textDiff.display.unified"));
        if (index >= 0 && index < displayModeComboBox.getItemCount()) {
            displayModeComboBox.setSelectedIndex(index);
        } else {
            displayModeComboBox.setSelectedIndex(0);
        }
    }

    private void refreshHighlightModeComboBox() {
        int index = highlightModeComboBox.getSelectedIndex();
        highlightModeComboBox.removeAllItems();
        highlightModeComboBox.addItem(I18n.get("textDiff.highlight.both"));
        highlightModeComboBox.addItem(I18n.get("textDiff.highlight.charOnly"));
        highlightModeComboBox.addItem(I18n.get("textDiff.highlight.lineOnly"));
        if (index >= 0 && index < highlightModeComboBox.getItemCount()) {
            highlightModeComboBox.setSelectedIndex(index);
        } else {
            highlightModeComboBox.setSelectedIndex(1);
        }
        syncHighlightModeFromCombo();
    }

    private void syncHighlightModeFromCombo() {
        switch (highlightModeComboBox.getSelectedIndex()) {
            case 1 -> highlightMode = HighlightMode.CHAR_ONLY;
            case 2 -> highlightMode = HighlightMode.LINE_ONLY;
            default -> highlightMode = HighlightMode.BOTH;
        }
    }

    private void refreshIdleStatus() {
        if (leftTextArea.getText().isEmpty() && rightTextArea.getText().isEmpty()) {
            statusLabel.setText(I18n.get("textDiff.status.ready"));
        } else if (realTimeCheckBox.isSelected()) {
            performDiff();
        }
    }

    /**
     * 初始化文本区域
     */
    private void initTextAreas() {
        // 左侧文本区域
        leftTextArea = new RegexRSyntaxTextViewer();
        leftTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        leftTextArea.setCodeFoldingEnabled(true);
        leftTextArea.setAntiAliasingEnabled(true);
        leftTextArea.setAutoIndentEnabled(true);
        leftTextArea.setMarkOccurrences(true);
        leftTextArea.setPaintMarkOccurrencesBorder(true);
        leftTextArea.setTabSize(4);
        leftTextArea.setCaretPosition(0);

        // 右侧文本区域
        rightTextArea = new RegexRSyntaxTextViewer();
        rightTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        rightTextArea.setCodeFoldingEnabled(true);
        rightTextArea.setAntiAliasingEnabled(true);
        rightTextArea.setAutoIndentEnabled(true);
        rightTextArea.setMarkOccurrences(true);
        rightTextArea.setPaintMarkOccurrencesBorder(true);
        rightTextArea.setTabSize(4);
        rightTextArea.setCaretPosition(0);

        // 统一diff显示区域
        unifiedTextArea = new RegexRSyntaxTextViewer();
        unifiedTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        unifiedTextArea.setCodeFoldingEnabled(true);
        unifiedTextArea.setAntiAliasingEnabled(true);
        unifiedTextArea.setEditable(false);
        unifiedTextArea.setTabSize(4);
        unifiedTextArea.setCaretPosition(0);

        // 创建滚动面板
        leftScrollPane = new RTextScrollPane(leftTextArea);
        rightScrollPane = new RTextScrollPane(rightTextArea);
        unifiedScrollPane = new RTextScrollPane(unifiedTextArea);

        ScrollUtil.smoothPane(leftScrollPane);
        ScrollUtil.smoothPane(rightScrollPane);
        ScrollUtil.smoothPane(unifiedScrollPane);

        // 同步左右滚动（便于并排对比时直观比照）
        setupScrollSync();

        // 将滚动面板添加到相应的面板中
        if (leftPanel != null) {
            leftPanel.add(
                    leftScrollPane,
                    new GridConstraints(
                            0,
                            0,
                            1,
                            1,
                            GridConstraints.ANCHOR_CENTER,
                            GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null,
                            null,
                            null,
                            0,
                            false
                    )
            );
        }
        if (rightPanel != null) {
            rightPanel.add(
                    rightScrollPane,
                    new GridConstraints(
                            0,
                            0,
                            1,
                            1,
                            GridConstraints.ANCHOR_CENTER,
                            GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null,
                            null,
                            null,
                            0,
                            false
                    )
            );
        }
        if (unifiedPanel != null) {
            unifiedPanel.add(unifiedScrollPane,
                    new GridConstraints(0,
                            0,
                            1,
                            1,
                            GridConstraints.ANCHOR_CENTER,
                            GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null,
                            null,
                            null,
                            0,
                            false
                    )
            );
        }
    }

    /**
     * 左右窗口竖向滚动同步
     */
    private void setupScrollSync() {
        if (!(leftScrollPane instanceof RTextScrollPane) || !(rightScrollPane instanceof RTextScrollPane)) {
            return;
        }
        // 获取滚动条
        JScrollBar leftBar = leftScrollPane.getVerticalScrollBar();
        JScrollBar rightBar = rightScrollPane.getVerticalScrollBar();
        if (leftBar == null || rightBar == null) {
            return;
        }

        // 滚动条滚动时，同步滚动
        leftBar.addAdjustmentListener(e -> {
            if (syncingScroll) return;
            syncingScroll = true;
            rightBar.setValue(e.getValue());
            syncingScroll = false;
        });
        rightBar.addAdjustmentListener(e -> {
            if (syncingScroll) return;
            syncingScroll = true;
            leftBar.setValue(e.getValue());
            syncingScroll = false;
        });
    }

    /**
     * 初始化
     */
    public void initForm() {
        TextDiffListener.addListeners();

        highlightModeComboBox.addActionListener(e -> {
            syncHighlightModeFromCombo();
            if (realTimeCheckBox.isSelected()) {
                performDiff();
            }
        });

        // 默认开启实时对比
        realTimeCheckBox.setSelected(true);

        // 初始化布局
        updateDisplayMode();

        // 恢复上次的内容
        initContent();
    }

    /**
     * 初始化内容
     */
    private void initContent() {
        try {
            TFuncContent tFuncContent = funcContentMapper.selectByFunc(FuncConsts.TEXT_DIFF + "_left");
            if (tFuncContent != null) {
                leftTextArea.setText(tFuncContent.getContent());
            }

            tFuncContent = funcContentMapper.selectByFunc(FuncConsts.TEXT_DIFF + "_right");
            if (tFuncContent != null) {
                rightTextArea.setText(tFuncContent.getContent());
            }
        } catch (Exception e) {
            logger.error("初始化文本对比内容失败", e);
        }
    }

    /**
     * 保存内容
     */
    public void save() {
        try {
            saveLeftContent();
            saveRightContent();
        } catch (Exception e) {
            logger.error("保存文本对比内容失败", e);
        }
    }

    /**
     * 保存左侧内容
     */
    private void saveLeftContent() {
        String text = leftTextArea.getText();
        String now = SqliteUtil.nowDateForSqlite();
        String funcKey = FuncConsts.TEXT_DIFF + "_left";

        saveContent(text, now, funcKey);
    }

    /**
     * 保存内容
     * @param text 文本
     * @param now  时间
     * @param funcKey 函数
     */
    private void saveContent(String text, String now, String funcKey) {
        TFuncContent tFuncContent = funcContentMapper.selectByFunc(funcKey);
        if (tFuncContent == null) {
            tFuncContent = new TFuncContent();
            tFuncContent.setFunc(funcKey);
            tFuncContent.setContent(text);
            tFuncContent.setCreateTime(now);
            tFuncContent.setModifiedTime(now);
            funcContentMapper.insert(tFuncContent);
        } else {
            tFuncContent.setContent(text);
            tFuncContent.setModifiedTime(now);
            funcContentMapper.updateByPrimaryKeySelective(tFuncContent);
        }
    }

    /**
     * 保存右侧内容
     */
    private void saveRightContent() {
        String text = rightTextArea.getText();
        String now = SqliteUtil.nowDateForSqlite();
        String funcKey = FuncConsts.TEXT_DIFF + "_right";

        saveContent(text, now, funcKey);
    }

    /**
     * 更新显示模式
     */
    public void updateDisplayMode() {
        if (displayModeComboBox.getSelectedIndex() == 0) {
            // 显示并排对比模式
            mainSplitPane.setLeftComponent(leftScrollPane);
            mainSplitPane.setRightComponent(rightScrollPane);
            mainSplitPane.setDividerLocation(0.5);
            unifiedPanel.setVisible(false);
        } else {
            // 显示统一差异模式
            mainSplitPane.setLeftComponent(leftScrollPane);
            mainSplitPane.setRightComponent(rightScrollPane);
            mainSplitPane.setDividerLocation(0.5);
            unifiedPanel.setVisible(true);
        }

        textDiffPanel.revalidate();
        textDiffPanel.repaint();
    }

    /**
     * 执行文本对比
     */
    public void performDiff() {
        String leftText = leftTextArea.getText();
        String rightText = rightTextArea.getText();

        if (leftText.isEmpty() && rightText.isEmpty()) {
            statusLabel.setText(I18n.get("textDiff.status.enterText"));
            return;
        }

        try {
            // 统一差异视图（文本 + 高亮区间）
            boolean ignoreWs = ignoreWhitespaceCheckBox != null && ignoreWhitespaceCheckBox.isSelected();
            var unifiedView = DiffService.buildUnifiedView(leftText, rightText, 3, ignoreWs);
            unifiedTextArea.setText(unifiedView.text());

            // 获取用于UI高亮的差异信息（基于字符区间）
            boolean charOnlyNav = (highlightMode == HighlightMode.CHAR_ONLY);
            var diffResult = DiffService.getSegmentsForUI(leftText, rightText, ignoreWs, charOnlyNav);

            // 构建导航锚点：每个差异段恰好追加一项（左/右没有则为 -1）
            leftNavOffsets.clear();
            rightNavOffsets.clear();
            navIndex = -1;
            if (diffResult.segments() != null) {
                for (var seg : diffResult.segments()) {
                    int lMid = (seg.leftStart() >= 0 && seg.leftEnd() >= 0)
                            ? (seg.leftStart() + seg.leftEnd()) / 2 : -1;
                    int rMid = (seg.rightStart() >= 0 && seg.rightEnd() >= 0)
                            ? (seg.rightStart() + seg.rightEnd()) / 2 : -1;
                    leftNavOffsets.add(lMid);
                    rightNavOffsets.add(rMid);
                }
            }

            // 左右并排的字符/行高亮
            applyDiffHighlight(diffResult);
            // 统一差异视图的行级/字符级高亮
            applyUnifiedSpans(unifiedView);

            // 更新状态：与可跳转点一致
            int changes = (diffResult.segments() != null) ? diffResult.segments().size() : 0;
            if (highlightMode == HighlightMode.CHAR_ONLY) {
                statusLabel.setText(I18n.format("textDiff.status.charDiff", changes));
            } else {
                statusLabel.setText(I18n.format("textDiff.status.complete", changes));
            }

        } catch (Exception e) {
            logger.error("执行文本对比失败", e);
            statusLabel.setText(I18n.format("textDiff.status.failed", e.getMessage()));
        }
    }

    /**
     * 清除两侧与统一视图上的所有高亮
     */
    private void clearHighlights() {
        if (leftTextArea != null && leftTextArea.getHighlighter() != null) {
            leftTextArea.getHighlighter().removeAllHighlights();
        }
        if (rightTextArea != null && rightTextArea.getHighlighter() != null) {
            rightTextArea.getHighlighter().removeAllHighlights();
        }
        if (unifiedTextArea != null && unifiedTextArea.getHighlighter() != null) {
            unifiedTextArea.getHighlighter().removeAllHighlights();
        }
    }

    /**
     * 应用差异高亮显示
     */
    private void applyDiffHighlight(UIDiff diffResult) {
        clearHighlights();
        if (diffResult == null || diffResult.segments() == null) {
            return;
        }

        // 删除、插入、修改
        for (var seg : diffResult.segments()) {
            switch (seg.type()) {
                case DELETE: {
                    if (highlightMode != HighlightMode.LINE_ONLY && seg.leftStart() >= 0 && seg.leftEnd() >= 0)
                        safeAddHighlight(leftTextArea, seg.leftStart(), seg.leftEnd(), delPainter);
                    if (highlightMode != HighlightMode.CHAR_ONLY && seg.leftStart() >= 0 && seg.leftEnd() >= 0)
                        safeAddLineHighlights(leftTextArea, seg.leftStart(), seg.leftEnd(), delLinePainter);
                    break;
                }
                case INSERT: {
                    if (highlightMode != HighlightMode.LINE_ONLY && seg.rightStart() >= 0 && seg.rightEnd() >= 0)
                        safeAddHighlight(rightTextArea, seg.rightStart(), seg.rightEnd(), insPainter);
                    if (highlightMode != HighlightMode.CHAR_ONLY && seg.rightStart() >= 0 && seg.rightEnd() >= 0)
                        safeAddLineHighlights(rightTextArea, seg.rightStart(), seg.rightEnd(), insLinePainter);
                    break;
                }
                case CHANGE: {
                    if (highlightMode != HighlightMode.LINE_ONLY) {
                        if (seg.leftStart() >= 0 && seg.leftEnd() >= 0)
                            safeAddHighlight(leftTextArea, seg.leftStart(), seg.leftEnd(), changePainter);
                        if (seg.rightStart() >= 0 && seg.rightEnd() >= 0)
                            safeAddHighlight(rightTextArea, seg.rightStart(), seg.rightEnd(), changePainter);
                    }
                    if (highlightMode != HighlightMode.CHAR_ONLY) {
                        if (seg.leftStart() >= 0 && seg.leftEnd() >= 0)
                            safeAddLineHighlights(leftTextArea, seg.leftStart(), seg.leftEnd(), changeLinePainter);
                        if (seg.rightStart() >= 0 && seg.rightEnd() >= 0)
                            safeAddLineHighlights(rightTextArea, seg.rightStart(), seg.rightEnd(), changeLinePainter);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * 在给定 TextArea 上安全添加高亮（自动裁剪区间并捕获异常）
     */
    private void safeAddHighlight(RSyntaxTextArea ta, int start, int end, Highlighter.HighlightPainter painter) {
        if (ta == null || painter == null) {
            return;
        }
        try {
            int docLen = ta.getDocument().getLength();
            int s = Math.max(0, Math.min(start, docLen));
            int e = Math.max(0, Math.min(end, docLen));
            if (e > s) {
                ta.getHighlighter().addHighlight(s, e, painter);
            }
        } catch (BadLocationException ex) {
            logger.warn("添加高亮失败: {}", ex.getMessage());
        }
    }

    /**
     * 以行为单位添加高亮：把 [start, end) 覆盖到完整的行区间并逐行高亮
     */
    private void safeAddLineHighlights(RSyntaxTextArea ta, int start, int end, Highlighter.HighlightPainter painter) {
        if (ta == null || painter == null) return;
        try {
            int docLen = ta.getDocument().getLength();
            int s = Math.max(0, Math.min(start, docLen));
            int e = Math.max(0, Math.min(end, docLen));
            if (e <= s) return;

            int startLine = ta.getLineOfOffset(s);
            int endLine = ta.getLineOfOffset(Math.max(s, e - 1));
            for (int line = startLine; line <= endLine; line++) {
                int ls = ta.getLineStartOffset(line);
                int le = ta.getLineEndOffset(line);
                // 为整行添加一层半透明底色，便于两侧比照
                ta.getHighlighter().addHighlight(ls, le, painter);
            }
        } catch (BadLocationException ex) {
            logger.warn("添加行级高亮失败: {}", ex.getMessage());
        }
    }

    private void gotoDiff(int index) {
        if (leftNavOffsets.isEmpty() && rightNavOffsets.isEmpty()) {
            return;
        }
        int max = Math.max(leftNavOffsets.size(), rightNavOffsets.size());
        navIndex = ((index % max) + max) % max; // 环绕
        int lOff = (navIndex < leftNavOffsets.size()) ? leftNavOffsets.get(navIndex) : -1;
        int rOff = (navIndex < rightNavOffsets.size()) ? rightNavOffsets.get(navIndex) : -1;
        if (lOff >= 0) {
            centerOnOffset(leftTextArea, lOff);
        }
        if (rOff >= 0) {
            centerOnOffset(rightTextArea, rOff);
        }
        statusLabel.setText(I18n.format("textDiff.status.gotoDiff", navIndex + 1, max));
    }

    private void centerOnOffset(RSyntaxTextArea ta, int offset) {
        try {
            int line = ta.getLineOfOffset(Math.max(0, Math.min(offset, ta.getDocument().getLength())));
            int start = ta.getLineStartOffset(line);
            ta.setCaretPosition(start);
            ta.requestFocusInWindow();
        } catch (BadLocationException ignore) { }
    }

    private void gotoNextDiff() { gotoDiff(navIndex + 1); }
    private void gotoPrevDiff() { gotoDiff(navIndex - 1); }

    /**
     * 统一差异视图高亮：
     * - 行级：+ 绿色, - 红色, @@ hunk 行黄色, 文件头灰色
     * - 字符级：对同一 hunk 中的 +/- 行按序配对做字符级 diff，仅高亮变更范围
     * 渲染统一差异视图的高亮（使用服务层返回的区间）
     */
    private void applyUnifiedSpans(UnifiedView view) {
        if (view == null) return;
        // 行级底色
        if (view.lineSpans() != null) {
            for (var sp : view.lineSpans()) {
                switch (sp.type()) {
                    case ADD_LINE -> safeAddLineHighlights(unifiedTextArea, sp.start(), sp.end(), insLinePainter);
                    case DEL_LINE -> safeAddLineHighlights(unifiedTextArea, sp.start(), sp.end(), delLinePainter);
                    case HUNK_LINE -> safeAddLineHighlights(unifiedTextArea, sp.start(), sp.end(), changeLinePainter);
                    case HEADER_LINE -> safeAddLineHighlights(unifiedTextArea, sp.start(), sp.end(), headerLinePainter);
                    default -> {}
                }
            }
        }
        // 字符级高亮
        if (view.charSpans() != null && highlightMode != HighlightMode.LINE_ONLY) {
            for (var sp : view.charSpans()) {
                switch (sp.type()) {
                    case ADD_CHAR -> safeAddHighlight(unifiedTextArea, sp.start(), sp.end(), insPainter);
                    case DEL_CHAR -> safeAddHighlight(unifiedTextArea, sp.start(), sp.end(), delPainter);
                    case CHANGE_CHAR -> safeAddHighlight(unifiedTextArea, sp.start(), sp.end(), changePainter);
                    default -> {}
                }
            }
        }
    }

    /**
     * 清空所有文本
     */
    public void clearAll() {
        clearHighlights();
        leftTextArea.setText("");
        rightTextArea.setText("");
        unifiedTextArea.setText("");
        statusLabel.setText(I18n.get("textDiff.status.cleared"));
    }

    /**
     * 交换左右文本
     */
    public void swapTexts() {
        String leftText = leftTextArea.getText();
        String rightText = rightTextArea.getText();

        leftTextArea.setText(rightText);
        rightTextArea.setText(leftText);

        // 交换后也做一次对比，确保高亮与内容同步
        performDiff();

        statusLabel.setText(I18n.get("textDiff.status.swapped"));
    }

    /**
     * 复制差异结果
     */
    public void copyDiffResult() {
        String diffText = unifiedTextArea.getText();
        if (!diffText.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(diffText), null);
            statusLabel.setText(I18n.get("textDiff.status.copied"));
        } else {
            statusLabel.setText(I18n.get("textDiff.status.noCopy"));
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
        prevDiffButton.addActionListener(e -> gotoPrevDiff());
        nextDiffButton.addActionListener(e -> gotoNextDiff());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        textDiffPanel = new JPanel();
        textDiffPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));

        // 工具栏面板
        toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayoutManager(1, 12, new Insets(0, 0, 10, 0), -1, -1));
        textDiffPanel.add(toolBarPanel, new GridConstraints(0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        compareButton = new JButton();
        compareButton.setText("对比");
        compareButton.setIcon(new FlatSVGIcon("icon/find.svg"));
        toolBarPanel.add(compareButton, new GridConstraints(0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        clearButton = new JButton();
        clearButton.setText("清空");
        clearButton.setIcon(new FlatSVGIcon("icon/remove.svg"));
        toolBarPanel.add(clearButton, new GridConstraints(0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        swapButton = new JButton();
        swapButton.setText("交换");
        swapButton.setIcon(new FlatSVGIcon("icon/exchange.svg"));
        toolBarPanel.add(swapButton, new GridConstraints(0,
                2,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        copyDiffButton = new JButton();
        copyDiffButton.setText("复制差异");
        copyDiffButton.setIcon(new FlatSVGIcon("icon/copy.svg"));
        toolBarPanel.add(copyDiffButton, new GridConstraints(0,
                3,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        prevDiffButton = new JButton();
        prevDiffButton.setText("上一处");
        toolBarPanel.add(prevDiffButton, new GridConstraints(0,
                4,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        nextDiffButton = new JButton();
        nextDiffButton.setText("下一处");
        toolBarPanel.add(nextDiffButton,
                new GridConstraints(0,
                        5,
                        1,
                        1,
                        GridConstraints.ANCHOR_CENTER,
                        GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED,
                        null,
                        null,
                        null,
                        0,
                        false));

        realTimeCheckBox = new JCheckBox();
        realTimeCheckBox.setText("实时对比");
        toolBarPanel.add(realTimeCheckBox, new GridConstraints(0,
                6,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        ignoreWhitespaceCheckBox = new JCheckBox();
        ignoreWhitespaceCheckBox.setText("忽略空白");
        toolBarPanel.add(ignoreWhitespaceCheckBox, new GridConstraints(0,
                7,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        highlightModeComboBox = new JComboBox<>();
        toolBarPanel.add(highlightModeComboBox, new GridConstraints(0,
                8,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        displayModeComboBox = new JComboBox<>();
        toolBarPanel.add(displayModeComboBox, new GridConstraints(0,
                9,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        final Spacer spacer1 = new Spacer();
        toolBarPanel.add(spacer1, new GridConstraints(0,
                10,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false));

        statusLabel = new JLabel();
        statusLabel.setText(I18n.get("textDiff.status.ready"));
        toolBarPanel.add(statusLabel, new GridConstraints(0,
                11,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false));

        // 主分割面板
        mainSplitPane = new JSplitPane();
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setResizeWeight(0.5);
        textDiffPanel.add(mainSplitPane, new GridConstraints(1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                new Dimension(200, 200),
                null,
                0,
                false));

        // 左侧面板
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 5), -1, -1));
        leftPanel.setBorder(BorderFactory.createTitledBorder(null, "原文本", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));

        // 右侧面板
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 5, 0, 0), -1, -1));
        rightPanel.setBorder(BorderFactory.createTitledBorder(null, "新文本", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));

        // 设置分割面板的左右组件
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);

        // 统一差异面板
        unifiedPanel = new JPanel();
        unifiedPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        unifiedPanel.setBorder(BorderFactory.createTitledBorder(null, "统一差异格式", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        unifiedPanel.setVisible(false);
        textDiffPanel.add(unifiedPanel, new GridConstraints(2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                new Dimension(-1, 200),
                null,
                0,
                false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return textDiffPanel;
    }
}
