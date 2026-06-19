package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.component.QuickNoteTreeDragDrop;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteEditorPanel;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewer;
import com.luoboduner.moo.tool.ui.component.textviewer.QuickNoteRSyntaxTextViewerManager;
import com.luoboduner.moo.tool.ui.dialog.DocInfoDialog;
import com.luoboduner.moo.tool.ui.dialog.QuickNoteGitDialog;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.component.SplitPaneUtil;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.NamingUtil;
import com.luoboduner.moo.tool.util.ListUtils;
import com.luoboduner.moo.tool.util.QuickNoteAttachmentUtil;
import com.luoboduner.moo.tool.util.QuickNoteImageInsertUtil;
import com.luoboduner.moo.tool.util.QuickNoteIndicatorTools;
import com.luoboduner.moo.tool.util.QuickNoteVaultRefreshCoordinator;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.codeformatter.CodeFormatterFactory;
import com.luoboduner.moo.tool.util.MsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * 随手记事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/15.
 */
@Slf4j
public class QuickNoteListener {

    public static String selectedName;
    public static String selectedPath;

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

    // 创建一个单线程的ExecutorService
    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;

        // 保存按钮
        quickNoteForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedPath)) {
                selectedName = getDefaultFileName();
            } else {
                TQuickNote current = QuickNoteVaultUtil.loadByPath(selectedPath);
                if (current != null) {
                    selectedName = current.getName();
                }
            }

            String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), selectedName);
            if (StringUtils.isNotBlank(name)) {
                if (StringUtils.isBlank(selectedPath)) {
                    TQuickNote created = QuickNoteVaultUtil.createNote(name, QuickNoteForm.getSelectedFolderPath());
                    selectedPath = created.getRelativePath();
                    selectedName = created.getName();
                    quickSaveSync(true, false, true);
                    QuickNoteForm.initNoteList();
                    return;
                }
                TQuickNote current = QuickNoteVaultUtil.loadByPath(selectedPath);
                if (current != null && !name.equals(current.getName())) {
                    String newPath = QuickNoteVaultUtil.renameNote(selectedPath, name);
                    QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(selectedPath);
                    selectedPath = newPath;
                    selectedName = name;
                }
                quickSaveSync(true, false, true);
                QuickNoteForm.initNoteList();
            }
        });

        // 点击左侧树事件
        JTree noteTree = quickNoteForm.getNoteTree();
        if (noteTree != null) {
            noteTree.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = noteTree.getRowForLocation(e.getX(), e.getY());
                    if (row >= 0) {
                        noteTree.setSelectionRow(row);
                    }
                    TQuickNote note = QuickNoteForm.getSelectedTreeNote();
                    if (note == null) {
                        return;
                    }
                    QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
                    try {
                        QuickNoteForm.showNote(note);
                    } catch (Exception e1) {
                        log.error(e1.toString());
                    } finally {
                        QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
                    }

                    super.mousePressed(e);
                }
            });
        }


        // 删除按钮事件
        quickNoteForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(quickNoteForm);
        });

        // 语法高亮下拉框事件
        quickNoteForm.getSyntaxComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String syntaxName = e.getItem().toString();
                QuickNoteForm.updateInsertImageButtonVisibility();

                if (StringUtils.isNotEmpty(syntaxName)) {
                    if (selectedPath != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                    updateCurrentMetadata(note -> note.setSyntax("text/" + syntaxName));
                    quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(selectedPath);
                    QuickNoteEditorPanel editorPanel = quickNoteRSyntaxTextViewerManager.getEditorPanel(selectedPath);
                        quickNoteForm.getContentSplitPane().setLeftComponent(editorPanel);
                        editorPanel.updateUI();
                    }

                }
            }
        });

        // 字体名称下拉框事件
        quickNoteForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();

                if (selectedPath != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                    updateCurrentMetadata(note -> note.setFontName(fontName));
                    App.config.setQuickNoteFontName(fontName);
                    App.config.save();

                    quickNoteRSyntaxTextViewerManager.updateFont(selectedPath);
                }
            }
        });

        // 字体大小下拉框事件
        quickNoteForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());

                if (selectedPath != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                    updateCurrentMetadata(note -> note.setFontSize(String.valueOf(fontSize)));

                    App.config.setQuickNoteFontSize(fontSize);
                    App.config.save();

                    quickNoteRSyntaxTextViewerManager.updateFont(selectedPath);
                }
            }
        });

        // 插入图片按钮事件
        quickNoteForm.getInsertImageButton().addActionListener(e -> QuickNoteImageInsertUtil.insertImageFromChooser());

        // 自动换行按钮事件
        quickNoteForm.getWrapButton().addActionListener(e -> {
            RSyntaxTextArea view = quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
            view.setLineWrap(!view.getLineWrap());
            if (selectedPath != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                updateCurrentMetadata(note -> note.setLineWrap(view.getLineWrap() ? "1" : "0"));
            }
        });

        // 添加按钮事件
        quickNoteForm.getAddButton().addActionListener(e -> newNote());


        // 左侧树按键事件（重命名）
        if (noteTree != null) {
            noteTree.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suppressListEnterRename) {
                        suppressListEnterRename = false;
                        return;
                    }
                    renameSelectedItem(quickNoteForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteFiles(quickNoteForm);
                } else if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
                    TQuickNote note = QuickNoteForm.getSelectedTreeNote();
                    if (note == null) {
                        return;
                    }
                    QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
                    try {
                        QuickNoteForm.showNote(note);
                    } catch (Exception e1) {
                        log.error(e1.toString());
                    } finally {
                        QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
                    }
                }
            }
        });
        }

        // 查找按钮
        quickNoteForm.getFindButton().addActionListener(e -> {
            showFindPanel();

        });

        // 快捷替换按钮
        quickNoteForm.getQuickReplaceButton().addActionListener(e -> {
            int totalWidth = quickNoteForm.getContentSplitPane().getWidth();
            int currentDividerLocation = quickNoteForm.getContentSplitPane().getDividerLocation();

            if (totalWidth - currentDividerLocation < 10) {
                quickNoteForm.getQuickReplacePanel().setVisible(true);
                SplitPaneUtil.showSecondary(quickNoteForm.getContentSplitPane(), SplitPaneUtil.SECONDARY_PANEL_RATIO);
            } else {
                SplitPaneUtil.hideSecondary(quickNoteForm.getContentSplitPane());
                quickNoteForm.getQuickReplacePanel().setVisible(false);
            }
        });

        // 列表按钮
        quickNoteForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = quickNoteForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                SplitPaneUtil.configureListEditorSplit(quickNoteForm.getSplitPane());
            } else {
                quickNoteForm.getSplitPane().setDividerLocation(0);
            }
        });

        // 导出按钮
        quickNoteForm.getExportButton().addActionListener(e -> exportSelectedNotes(quickNoteForm));

        // 格式化按钮
        quickNoteForm.getFormatButton().addActionListener(e -> format());

        // 执行快捷替换
        quickNoteForm.getStartQuickReplaceButton().addActionListener(e -> quickReplace());

        // 关闭快捷操作面板
        quickNoteForm.getQuickReplaceCloseButton().addActionListener(e -> {
            SplitPaneUtil.hideSecondary(quickNoteForm.getContentSplitPane());
            quickNoteForm.getQuickReplacePanel().setVisible(false);
        });

        quickNoteForm.getColorButton().addActionListener(e -> quickNoteForm.getColorSettingPanel().setVisible(!quickNoteForm.getColorSettingPanel().isVisible()));

        // 颜色按钮事件
        String[] colorKeys = QuickNoteForm.COLOR_KEYS;
        JToggleButton[] colorButtons = QuickNoteForm.COLOR_BUTTONS;
        for (int i = 0; i < colorButtons.length; i++) {
            colorButtons[i].addActionListener(e -> {
                String colorKey = colorKeys[0];
                for (int i1 = 0; i1 < colorButtons.length; i1++) {
                    if (colorButtons[i1].isSelected()) {
                        colorKey = colorKeys[i1];
                        break;
                    }
                }
                final String selectedColorKey = colorKey;
                if (StringUtils.isNotEmpty(selectedColorKey)) {
                    if (selectedPath != null && !QuickNoteRSyntaxTextViewer.ignoreQuickSave) {
                        updateCurrentMetadata(note -> note.setColor(selectedColorKey));

                        quickNoteForm.getColorButton().setIcon(new QuickNoteForm.ListColorIcon(selectedColorKey, 18, 18));
                        QuickNoteForm.applyCurrentEditorOutline(selectedColorKey);
                        if (quickNoteForm.getNoteTree() != null) {
                            quickNoteForm.getNoteTree().repaint();
                        }
                    }
                }
            });
        }

        // 搜索框变更事件
        quickNoteForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                QuickNoteForm.refreshNoteTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                QuickNoteForm.refreshNoteTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        // 搜索框包含内容checkbox变更事件
        quickNoteForm.getSearchContentCheckBox().addActionListener(e -> {
            QuickNoteForm.refreshNoteTree();
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem newFolderMenuItem = new JMenuItem(I18n.get("quickNote.newFolder"));
        JMenuItem renameMenuItem = new JMenuItem(I18n.get("common.rename"));
        JMenuItem deleteMenuItem = new JMenuItem(I18n.get("common.delete"));
        JMenuItem exportMenuItem = new JMenuItem(I18n.get("common.export"));
        JMenuItem duplicateMenuItem = new JMenuItem(I18n.get("quickNote.menu.duplicate"));
        JMenuItem moveToFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.moveToFolder"));
        JMenuItem deleteFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.deleteFolder"));
        JMenuItem revealInFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.revealInFolder"));
        noteListPopupMenu.add(newFolderMenuItem);
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(moveToFolderMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(deleteFolderMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        noteListPopupMenu.add(duplicateMenuItem);
        noteListPopupMenu.addSeparator();
        noteListPopupMenu.add(revealInFolderMenuItem);
        JMenuItem refreshMenuItem = new JMenuItem(I18n.get("quickNote.menu.refresh"));
        JMenuItem openVaultMenuItem = new JMenuItem(I18n.get("quickNote.menu.openVault"));
        JMenuItem gitMenuItem = new JMenuItem(I18n.get("quickNote.menu.git"));
        noteListPopupMenu.add(refreshMenuItem);
        noteListPopupMenu.add(openVaultMenuItem);
        noteListPopupMenu.add(gitMenuItem);
        if (noteTree != null) {
            noteTree.setComponentPopupMenu(noteListPopupMenu);
            noteListPopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    boolean hasNotes = !QuickNoteForm.getSelectedNotes().isEmpty();
                    String folderPath = QuickNoteForm.getSelectedFolderPath();
                    boolean folderNode = isFolderNodeSelected(noteTree);
                    moveToFolderMenuItem.setEnabled(hasNotes);
                    deleteFolderMenuItem.setEnabled(folderNode && StringUtils.isNotBlank(folderPath)
                            && QuickNoteVaultUtil.isFolderEmpty(folderPath));
                    renameMenuItem.setEnabled(hasNotes || (folderNode && StringUtils.isNotBlank(folderPath)));
                    deleteMenuItem.setEnabled(hasNotes);
                    exportMenuItem.setEnabled(hasNotes);
                    duplicateMenuItem.setEnabled(hasNotes);
                    revealInFolderMenuItem.setEnabled(hasNotes || folderNode);
                }

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                }
            });
        }

        newFolderMenuItem.addActionListener(e -> createFolder(quickNoteForm));

        // 重命名菜单项事件
        renameMenuItem.addActionListener(e -> renameSelectedItem(quickNoteForm));

        // 删除菜单项事件
        deleteMenuItem.addActionListener(e -> deleteFiles(quickNoteForm));

        moveToFolderMenuItem.addActionListener(e -> moveSelectedNotesToFolder(quickNoteForm));
        deleteFolderMenuItem.addActionListener(e -> deleteSelectedFolder(quickNoteForm));
        revealInFolderMenuItem.addActionListener(e -> revealSelectedInFileManager(quickNoteForm));

        // 导出菜单项事件
        exportMenuItem.addActionListener(e -> exportSelectedNotes(quickNoteForm));
        duplicateMenuItem.addActionListener(e -> duplicateSelectedNotes(quickNoteForm));

        refreshMenuItem.addActionListener(e -> QuickNoteVaultRefreshCoordinator.refreshAfterExternalChange());
        openVaultMenuItem.addActionListener(e -> QuickNoteVaultUtil.openVaultDir());
        gitMenuItem.addActionListener(e -> QuickNoteGitDialog.showDialog());

        quickNoteForm.getUnOrderListButton().addActionListener(e -> toggleUnorderedList(
                QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea()));

        quickNoteForm.getOrderListButton().addActionListener(e -> toggleOrderedList(
                QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea()));

        quickNoteForm.getGitButton().addActionListener(e -> QuickNoteGitDialog.showDialog());

        quickNoteForm.getInfoButton().addActionListener(e -> {
            try {
                DocInfoDialog docInfoDialog = new DocInfoDialog();
                docInfoDialog.pack();

                String createTime = "";
                String modifiedTime = "";
                if (StringUtils.isNotEmpty(selectedPath)) {
                    TQuickNote tQuickNote = QuickNoteVaultUtil.loadByPath(selectedPath);
                    createTime = tQuickNote.getCreateTime();
                    modifiedTime = tQuickNote.getModifiedTime();
                }
                RSyntaxTextArea view = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();

                String content = view.getText();

                // 去掉空格和tab和换行后的字符数
                String wordCnt = String.valueOf(content.replace(" ", "").replace("\t", "").replace("\n", "").length());
                // 去掉空格后的字符数
                String charCntNoBlank = String.valueOf(content.replace(" ", "").length());
                String charCntWithBlank = String.valueOf(content.length());

                docInfoDialog.setInfo(createTime, modifiedTime, wordCnt, charCntNoBlank, charCntWithBlank);

                docInfoDialog.setVisible(true);
            } catch (Exception e1) {
                log.error("InfoButton error", e1);
            }
        });

    }

    public static void showFindPanel() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        QuickNoteRSyntaxTextViewerManager quickNoteRSyntaxTextViewerManager = QuickNoteForm.quickNoteRSyntaxTextViewerManager;
        quickNoteForm.getFindReplacePanel().removeAll();
        quickNoteForm.getFindReplacePanel().setDoubleBuffered(true);
        RSyntaxTextArea view = quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();
        FindReplaceBar findReplaceBar = new FindReplaceBar(view);
        quickNoteForm.getFindReplacePanel().add(findReplaceBar.getFindOptionPanel());
        quickNoteForm.getFindReplacePanel().setVisible(true);
        quickNoteForm.getFindReplacePanel().updateUI();
        findReplaceBar.getFindField().setText(view.getSelectedText());
        findReplaceBar.getFindField().grabFocus();
        findReplaceBar.getFindField().selectAll();
    }

    private static void updateCurrentMetadata(Consumer<TQuickNote> updater) {
        if (StringUtils.isBlank(selectedPath)) {
            return;
        }
        TQuickNote note = QuickNoteVaultUtil.loadByPath(selectedPath);
        if (note == null) {
            return;
        }
        updater.accept(note);
        note.setModifiedTime(SqliteUtil.nowDateForSqlite());
        QuickNoteVaultUtil.saveMetadata(note);
    }

    private static void createFolder(QuickNoteForm quickNoteForm) {
        String parentFolder = QuickNoteForm.getSelectedFolderPath();
        String folderName = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), I18n.get("quickNote.newFolderDefault"));
        if (StringUtils.isBlank(folderName)) {
            return;
        }
        String folderPath = StringUtils.isBlank(parentFolder)
                ? folderName
                : parentFolder + "/" + folderName;
        QuickNoteVaultUtil.createFolder(folderPath);
        QuickNoteForm.initNoteList();
    }

    private static boolean isFolderNodeSelected(JTree noteTree) {
        if (noteTree == null) {
            return false;
        }
        TreePath selectionPath = noteTree.getSelectionPath();
        if (selectionPath == null) {
            return false;
        }
        Object userObject = ((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getUserObject();
        return userObject instanceof String;
    }

    private static void moveSelectedNotesToFolder(QuickNoteForm quickNoteForm) {
        List<TQuickNote> selectedNotes = QuickNoteForm.getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            return;
        }
        List<String> folders = new ArrayList<>();
        folders.add("");
        folders.addAll(QuickNoteVaultUtil.listFolders());
        JComboBox<String> folderComboBox = new JComboBox<>(folders.toArray(new String[0]));
        folderComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String folder = value == null ? "" : value.toString();
                String label = StringUtils.isBlank(folder)
                        ? I18n.get("quickNote.folderRoot")
                        : folder;
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        String currentFolder = QuickNoteVaultUtil.parentFolder(selectedNotes.get(0).getRelativePath());
        folderComboBox.setSelectedItem(currentFolder);

        int confirm = JOptionPane.showConfirmDialog(
                quickNoteForm.getQuickNotePanel(),
                folderComboBox,
                I18n.get("quickNote.menu.moveToFolder"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        Object selected = folderComboBox.getSelectedItem();
        String targetFolder = selected == null ? "" : selected.toString();
        QuickNoteTreeDragDrop.moveNotes(
                selectedNotes.stream().map(TQuickNote::getRelativePath).toList(),
                targetFolder,
                quickNoteForm.getNoteTree());
    }

    private static void deleteSelectedFolder(QuickNoteForm quickNoteForm) {
        String folderPath = QuickNoteForm.getSelectedFolderPath();
        if (StringUtils.isBlank(folderPath)) {
            return;
        }
        if (!QuickNoteVaultUtil.isFolderEmpty(folderPath)) {
            MsgUtil.info(App.mainFrame, "quickNote.folderNotEmpty");
            return;
        }
        int confirm = MsgUtil.confirm(App.mainFrame, "quickNote.confirmDeleteFolder");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (QuickNoteVaultUtil.deleteFolderIfEmpty(folderPath)) {
            QuickNoteForm.initNoteList();
            QuickNoteForm.updateGitButtonStatus();
        }
    }

    private static void duplicateSelectedNotes(QuickNoteForm quickNoteForm) {
        List<TQuickNote> selectedNotes = QuickNoteForm.getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            return;
        }
        String lastPath = null;
        for (TQuickNote note : selectedNotes) {
            TQuickNote copy = QuickNoteVaultUtil.duplicateNote(note.getRelativePath());
            if (copy != null) {
                lastPath = copy.getRelativePath();
            }
        }
        if (StringUtils.isNotBlank(lastPath)) {
            selectedPath = lastPath;
            TQuickNote copied = QuickNoteVaultUtil.loadByPath(lastPath);
            if (copied != null) {
                selectedName = copied.getName();
            }
            QuickNoteForm.initNoteList();
            QuickNoteForm.updateGitButtonStatus();
        }
    }

    private static void exportSelectedNotes(QuickNoteForm quickNoteForm) {
        try {
            List<TQuickNote> selectedNotes = QuickNoteForm.getSelectedNotes();
            if (selectedNotes.isEmpty()) {
                MsgUtil.info(quickNoteForm.getQuickNotePanel(), "msg.selectAtLeastOne");
                return;
            }
            SystemFileChooser fileChooser = new SystemFileChooser(App.config.getQuickNoteExportPath());
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            int approve = fileChooser.showOpenDialog(quickNoteForm.getQuickNotePanel());
            String exportPath;
            if (approve == SystemFileChooser.APPROVE_OPTION) {
                exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                App.config.setQuickNoteExportPath(exportPath);
                App.config.save();
            } else {
                return;
            }

            for (TQuickNote note : selectedNotes) {
                TQuickNote fullNote = QuickNoteVaultUtil.loadByPath(note.getRelativePath());
                if (fullNote == null) {
                    continue;
                }
                String targetName = QuickNoteVaultUtil.sanitizeFileName(fullNote.getName()) + QuickNoteVaultUtil.TXT_EXTENSION;
                File exportFile = FileUtil.touch(exportPath + File.separator + targetName);
                FileUtil.writeUtf8String(fullNote.getContent(), exportFile);
            }
            MsgUtil.success(quickNoteForm.getQuickNotePanel(), "msg.exportSuccess");
            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(new File(exportPath));
            } catch (Exception e2) {
                log.error(ExceptionUtils.getStackTrace(e2));
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(quickNoteForm.getQuickNotePanel(), "msg.exportFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    /**
     * Default File Name
     *
     * @return
     */
    private static String getDefaultFileName() {
        return NamingUtil.defaultUntitledName();
    }

    /**
     * 快捷替换
     */
    private static void quickReplace() {
        try {
            QuickNoteRSyntaxTextViewer.ignoreQuickSave = true;
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            RSyntaxTextArea view = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea();

            String content = view.getText();

            // 如果有选中的行，则只替换选中的行
            int start = view.getSelectionStart();
            int end = view.getSelectionEnd();
            String selectedText = view.getSelectedText();
            if (StringUtils.isNotEmpty(selectedText)) {
                content = selectedText;
            }

            String[] splits = content.split("\n");

            List<String> target = Lists.newArrayList();
            for (String split : splits) {

                if (quickNoteForm.getTrimBlankCheckBox().isSelected()) {
                    split = split.replace(" ", "");
                }

                if (quickNoteForm.getTrimBlankRowCheckBox().isSelected() && StringUtils.isBlank(split)) {
                    continue;
                }

                if (quickNoteForm.getClearTabTCheckBox().isSelected()) {
                    split = split.replace("\t", "");
                }

                // ------------

                if (quickNoteForm.getScientificToNormalCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            BigDecimal bigDecimal = NumberUtil.toBigDecimal(str.replace("e", "E"));
                            str = bigDecimal.toString();
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getNormalToScientificCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            BigDecimal bigDecimal = NumberUtil.toBigDecimal(str);
                            DecimalFormat decimalFormat = new DecimalFormat("0." + StringUtils.repeat("#", str.split("\\.")[0].length() - 1) + "E0");
                            str = decimalFormat.format(bigDecimal);
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getToThousandthCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        if (NumberUtil.isNumber(str)) {
                            str = toThousandth(str);
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getToNormalNumCheckBox().isSelected()) {
                    String[] strs = split.split(" ");
                    List<String> tmp = Lists.newArrayList();
                    for (String str : strs) {
                        // 如果str只包含数字和小数点和逗号，就去掉逗号
                        if (str.matches("^[0-9,\\.]+$")) {
                            str = str.replace(",", "");
                        }
                        tmp.add(str);
                    }
                    split = StringUtils.join(tmp, " ");
                }

                if (quickNoteForm.getUnderlineToHumpCheckBox().isSelected()) {
                    split = underlineToHump(split);
                }

                if (quickNoteForm.getHumpToUnderlineCheckBox().isSelected()) {
                    split = humpToUnderline(split);
                }

                if (quickNoteForm.getUperToLowerCheckBox().isSelected()) {
                    split = split.toLowerCase();
                }

                if (quickNoteForm.getLowerToUperCheckBox().isSelected()) {
                    split = split.toUpperCase();
                }

                // ------------
                if (quickNoteForm.getCommaToEnterCheckBox().isSelected()) {
                    split = split.replace(",", "\n");
                }
                if (quickNoteForm.getCommaSingleQuotesToEnterCheckBox().isSelected()) {
                    split = split.replace("','", "\n").replace("'", "");
                }
                if (quickNoteForm.getCommaDoubleQuotesToEnterCheckBox().isSelected()) {
                    split = split.replace("\",\"", "\n").replace("\"", "");
                }
                if (quickNoteForm.getTabToEnterCheckBox().isSelected()) {
                    split = split.replace("\t", "\n");
                }

                target.add(split);
            }

            if (quickNoteForm.getDeduplicationByLineCheckBox().isSelected()) {
                target = Lists.newArrayList(ArrayUtil.distinct(target.toArray(new String[0])));
            }

            if (quickNoteForm.getDeduplicationByLineCntCheckBox().isSelected()) {
                // 按行去重并统计出现次数，结果示例："abc"出现了2次\n"def"出现了3次
                target = Lists.newArrayList(ArrayUtil.distinct(target.toArray(new String[0])));
                List<String> targetWithCnt = Lists.newArrayList();
                for (String str : target) {
                    long cnt = ListUtils.matchCount(Arrays.asList(splits), s -> s.equals(str));
                    targetWithCnt.add(I18n.format("quickNote.lineOccurrence", str, cnt));
                }
                target = targetWithCnt;
            }

            if (quickNoteForm.getReverseByRowCheckBox().isSelected()) {
                target = ListUtil.reverse(target);
            }
            if (quickNoteForm.getSortFromAToZByRowCheckBox().isSelected()) {
                Comparator<String> comparator = Comparator.naturalOrder();
                target = ListUtil.sort(target, comparator);
            }
            if (quickNoteForm.getSortFromZToAByRowCheckBox().isSelected()) {
                Comparator<String> comparator = Comparator.reverseOrder();
                target = ListUtil.sort(target, comparator);
            }
            if (quickNoteForm.getSortByPinyinCheckBox().isSelected()) {
                target = ListUtil.sortByPinyin(target);
            }

            if (quickNoteForm.getClearEnterCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection(StringUtils.join(target, ""));
                } else {
                    view.setText(StringUtils.join(target, ""));
                }
            } else if (quickNoteForm.getEnterToCommaCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection(StringUtils.join(target, ","));
                } else {
                    view.setText(StringUtils.join(target, ","));
                }
            } else if (quickNoteForm.getEnterToCommaSingleQuotesCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection("'" + StringUtils.join(target, "','") + "'");
                } else {
                    view.setText("'" + StringUtils.join(target, "','") + "'");
                }
            } else if (quickNoteForm.getEnterToCommaDoubleQuotesCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection("\"" + StringUtils.join(target, "\",\"") + "\"");
                } else {
                    view.setText("\"" + StringUtils.join(target, "\",\"") + "\"");
                }
            } else {
                if (!quickNoteForm.getEscapeCheckBox().isSelected() && !quickNoteForm.getUnescapeCheckBox().isSelected()) {
                    // 如果有选中的行，则只替换选中的行
                    if (StringUtils.isNotEmpty(selectedText)) {
                        view.replaceSelection(StringUtils.join(target, "\n"));
                    } else {
                        view.setText(StringUtils.join(target, "\n"));
                    }
                }
            }

            if (quickNoteForm.getEscapeCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection(StringEscapeUtils.escapeJava(view.getSelectedText()));
                } else {
                    view.setText(StringEscapeUtils.escapeJava(view.getText()));
                }
            }
            if (quickNoteForm.getUnescapeCheckBox().isSelected()) {
                // 如果有选中的行，则只替换选中的行
                if (StringUtils.isNotEmpty(selectedText)) {
                    view.replaceSelection(StringEscapeUtils.unescapeJava(view.getSelectedText()));
                } else {
                    view.setText(StringEscapeUtils.unescapeJava(view.getText()));
                }
            }

        } catch (Exception e) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        } finally {
            QuickNoteRSyntaxTextViewer.ignoreQuickSave = false;
        }

    }

    private static void renameSelectedItem(QuickNoteForm quickNoteForm) {
        TQuickNote note = QuickNoteForm.getSelectedTreeNote();
        if (note != null) {
            renameSelectedNote(quickNoteForm, note);
            return;
        }
        String folderPath = QuickNoteForm.getSelectedFolderPath();
        if (StringUtils.isBlank(folderPath) || !isFolderNodeSelected(quickNoteForm.getNoteTree())) {
            return;
        }
        String beforeName = QuickNoteVaultUtil.folderLeafName(folderPath);
        String afterName = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            List<String> affectedPaths = QuickNoteVaultUtil.listNotePathsUnderFolder(folderPath);
            String newFolderPath = QuickNoteVaultUtil.renameFolder(folderPath, afterName);
            for (String oldPath : affectedPaths) {
                QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(oldPath);
            }
            if (StringUtils.isNotBlank(selectedPath)) {
                selectedPath = QuickNoteVaultUtil.remapPathAfterFolderRename(selectedPath, folderPath, newFolderPath);
            }
            QuickNoteForm.initNoteList();
            QuickNoteForm.updateGitButtonStatus();
        } catch (Exception e) {
            MsgUtil.info(App.mainFrame, "msg.renameFolderFailed");
            QuickNoteForm.initNoteList();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void revealSelectedInFileManager(QuickNoteForm quickNoteForm) {
        List<TQuickNote> selectedNotes = QuickNoteForm.getSelectedNotes();
        if (!selectedNotes.isEmpty()) {
            QuickNoteVaultUtil.revealInFileManager(selectedNotes.get(0).getRelativePath());
            return;
        }
        if (isFolderNodeSelected(quickNoteForm.getNoteTree())) {
            QuickNoteVaultUtil.revealInFileManager(QuickNoteForm.getSelectedFolderPath());
        }
    }

    private static void renameSelectedNote(QuickNoteForm quickNoteForm, TQuickNote note) {
        String beforeName = note.getName();
        String beforePath = note.getRelativePath();
        if (StringUtils.isBlank(beforeName) || StringUtils.isBlank(beforePath)) {
            return;
        }
        suppressListEnterRename = true;
        String afterName = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            String newPath = QuickNoteVaultUtil.renameNote(beforePath, afterName);
            selectedPath = newPath;
            selectedName = afterName;
            QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(beforePath);
            QuickNoteForm.initNoteList();
        } catch (Exception e) {
            MsgUtil.info(App.mainFrame, "msg.renameNoteFailed");
            QuickNoteForm.initNoteList();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 删除文件
     *
     * @param quickNoteForm
     */
    private static void deleteFiles(QuickNoteForm quickNoteForm) {
        try {
            List<TQuickNote> selectedNotes = QuickNoteForm.getSelectedNotes();
            if (selectedNotes.isEmpty()) {
                MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            } else {
                int isDelete = MsgUtil.confirm(App.mainFrame, "msg.confirmDelete");
                if (isDelete == JOptionPane.YES_OPTION) {
                    List<String> deletedNoteContents = new ArrayList<>();

                    for (TQuickNote listNote : selectedNotes) {
                        String path = listNote.getRelativePath();
                        TQuickNote fileNote = QuickNoteVaultUtil.loadByPath(path);
                        if (fileNote != null && StringUtils.isNotBlank(fileNote.getContent())) {
                            deletedNoteContents.add(fileNote.getContent());
                        }
                        QuickNoteVaultUtil.deleteByPath(path);
                        QuickNoteForm.quickNoteRSyntaxTextViewerManager.removeRTextScrollPane(path);
                    }

                    List<String> remainingNoteContents = QuickNoteVaultUtil.listAllBodies();
                    QuickNoteAttachmentUtil.cleanupAttachmentsForDeletedNotes(
                            deletedNoteContents, remainingNoteContents);

                    selectedName = null;
                    selectedPath = null;
                    QuickNoteForm.initNoteList();
                }
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.deleteFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    /**
     * save for quick key and item change
     *
     * @param refreshModifiedTime
     */
    public static Future<?> quickSave(boolean refreshModifiedTime, boolean writeLog) {
        return quickSave(refreshModifiedTime, writeLog, writeLog);
    }

    public static Future<?> quickSave(boolean refreshModifiedTime, boolean writeLog, boolean notifyUser) {
        if (!SwingUtilities.isEventDispatchThread()) {
            java.util.concurrent.CompletableFuture<Void> bridge = new java.util.concurrent.CompletableFuture<>();
            SwingUtilities.invokeLater(() -> {
                try {
                    quickSave(refreshModifiedTime, writeLog, notifyUser).get();
                    bridge.complete(null);
                } catch (Exception ex) {
                    bridge.completeExceptionally(ex);
                }
            });
            return bridge;
        }

        final String path = selectedPath;
        final String name = selectedName;
        String editorText = null;
        if (path != null && QuickNoteForm.quickNoteRSyntaxTextViewerManager != null) {
            editorText = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getTextByPath(path);
        }
        final String capturedText = editorText;

        QuickNoteVaultRefreshCoordinator.onSaveTaskStarted();
        return executorService.submit(() -> {
            try {
                String now = SqliteUtil.nowDateForSqlite();
                if (path != null) {
                    TQuickNote existingNote = QuickNoteVaultUtil.loadByPath(path);
                    String oldContent = existingNote != null ? existingNote.getContent() : "";

                    TQuickNote tQuickNote = existingNote != null ? existingNote : new TQuickNote();
                    tQuickNote.setRelativePath(path);
                    tQuickNote.setName(name);

                    String text = capturedText;
                    if (writeLog) {
                        log.info("save note: " + path + ", content: " + text);
                    }
                    tQuickNote.setContent(text);
                    if (refreshModifiedTime) {
                        tQuickNote.setModifiedTime(now);
                    }

                    QuickNoteVaultUtil.saveNote(tQuickNote, text);

                    List<String> otherNotesContents = QuickNoteVaultUtil.listOtherBodies(path);
                    QuickNoteAttachmentUtil.cleanupRemovedAttachments(oldContent, text, otherNotesContents);
                }

                if (notifyUser) {
                    final String savedName = name;
                    SwingUtilities.invokeLater(() -> QuickNoteIndicatorTools.showTips(
                            I18n.format("quickNote.saved", savedName),
                            QuickNoteIndicatorTools.TipsLevel.SUCCESS));
                }
                SwingUtilities.invokeLater(QuickNoteForm::updateGitButtonStatus);
            } finally {
                QuickNoteVaultRefreshCoordinator.onSaveTaskFinished();
            }
        });
    }

    public static void quickSaveSync(boolean refreshModifiedTime, boolean writeLog) {
        quickSaveSync(refreshModifiedTime, writeLog, writeLog);
    }

    public static void quickSaveSync(boolean refreshModifiedTime, boolean writeLog, boolean notifyUser) {
        try {
            quickSave(refreshModifiedTime, writeLog, notifyUser).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Quick note save failed: {}", ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * create new Note
     */
    public static void newNote() {
        String name = getDefaultFileName();
        name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), name);
        if (StringUtils.isNotBlank(name)) {
            if (QuickNoteVaultUtil.loadByName(name) != null) {
                MsgUtil.info(App.mainFrame, "msg.duplicateNoteName");
                return;
            }
            TQuickNote created = QuickNoteVaultUtil.createNote(name, QuickNoteForm.getSelectedFolderPath());
            selectedPath = created.getRelativePath();
            selectedName = created.getName();
            QuickNoteForm.initNoteList();
        }
    }

    /**
     * 将字符串数字转成千分位显示。
     */
    public static String toThousandth(String value) {
        DecimalFormat decimalFormat;
        if (value.indexOf(".") > 0) {
            int afterPointLength = value.length() - value.indexOf(".") - 1;

            StringBuilder formatBuilder = new StringBuilder("###,##0.");
            for (int i = 0; i < afterPointLength; i++) {
                formatBuilder.append("0");
            }
            decimalFormat = new DecimalFormat(formatBuilder.toString());
        } else {
            decimalFormat = new DecimalFormat("###,##0");
        }
        double number;
        try {
            number = Double.parseDouble(value);
        } catch (Exception e) {
            number = 0.0;
        }
        return decimalFormat.format(number);
    }


    /**
     * 下划线转驼峰
     *
     * @param split
     * @return
     */
    private static String underlineToHump(String split) {
        StringBuilder result = new StringBuilder();
        String[] strings = split.split("_");
        for (String str : strings) {
            if (result.length() == 0) {
                result.append(str.toLowerCase());
            } else {
                result.append(str.substring(0, 1).toUpperCase());
                result.append(str.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /**
     * 驼峰转下划线
     * 来源于网络，地址找不到了，如有侵权，请联系作者删除。
     *
     * @param split
     * @return
     */
    private static String humpToUnderline(String split) {
        StringBuilder underLineBuilder = new StringBuilder();
        // 连续大写字母单词开关
        boolean switchTag = true;
        for (int i = 0; i < split.length(); i++) {
            // 转为ASCII
            int asciiNum = split.charAt(i);
            if (asciiNum > 64 && asciiNum < 91) {
                // 首字母不加下划线
                boolean temp1 = i != 0;
                // 下一位为小写字母时
                boolean temp2 = i < split.length() - 1 && split.charAt(i + 1) > 95 && split.charAt(i + 1) < 123;
                // 添加下划线
                if (temp1 && (switchTag || temp2))
                    underLineBuilder.append("_");
                // 大写字母转为小写
                asciiNum += 32;
                switchTag = false;
            } else {
                switchTag = true;
            }
            underLineBuilder.append((char) (asciiNum));
        }
        return underLineBuilder.toString();
    }

    /**
     * 文本格式化
     */
    public static void format() {
        try {
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            final String text = QuickNoteForm.quickNoteRSyntaxTextViewerManager.getTextByPath(selectedPath);
            final String selectedSyntax = (String) quickNoteForm.getSyntaxComboBox().getSelectedItem();
            if (StringUtils.isBlank(text) || StringUtils.isEmpty(selectedSyntax)) {
                return;
            }

            executorService.submit(() -> {
                try {
                    String formatted;
                    switch ("text/" + selectedSyntax) {
                        case SyntaxConstants.SYNTAX_STYLE_SQL:
                            switch (App.config.getSqlDialect()) {
                                case "MariaDB":
                                    formatted = SqlFormatter.of(Dialect.MariaDb).format(text, "    ");
                                    break;
                                case "MySQL":
                                    formatted = SqlFormatter.of(Dialect.MySql).format(text, "    ");
                                    break;
                                case "PostgreSQL":
                                    formatted = SqlFormatter.of(Dialect.PostgreSql).format(text, "    ");
                                    break;
                                case "IBM DB2":
                                    formatted = SqlFormatter.of(Dialect.Db2).format(text, "    ");
                                    break;
                                case "Oracle PL/SQL":
                                    formatted = SqlFormatter.of(Dialect.PlSql).format(text, "    ");
                                    break;
                                case "Couchbase N1QL":
                                    formatted = SqlFormatter.of(Dialect.N1ql).format(text, "    ");
                                    break;
                                case "Amazon Redshift":
                                    formatted = SqlFormatter.of(Dialect.Redshift).format(text, "    ");
                                    break;
                                case "Spark":
                                    formatted = SqlFormatter.of(Dialect.SparkSql).format(text, "    ");
                                    break;
                                case "SQL Server Transact-SQL":
                                    formatted = SqlFormatter.of(Dialect.TSql).format(text, "    ");
                                    break;
                                default:
                                    formatted = SqlFormatter.of(Dialect.StandardSql).format(text, "    ");
                            }
                            break;
                        case SyntaxConstants.SYNTAX_STYLE_JSON:
                        case SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS:
                            try {
                                formatted = JSONUtil.toJsonPrettyStr(text);
                            } catch (Exception e1) {
                                log.error(ExceptionUtils.getStackTrace(e1));
                                formatted = JSONUtil.formatJsonStr(text);
                            }
                            break;

                        case SyntaxConstants.SYNTAX_STYLE_XML:
                            formatted = CodeFormatterFactory.getFormatter(CodeFormatterFactory.FormatterType.XML).format(text);
                            break;

                        case SyntaxConstants.SYNTAX_STYLE_JAVA:
                            formatted = CodeFormatterFactory.getFormatter(CodeFormatterFactory.FormatterType.JAVA).format(text);
                            break;
                        default:
                            SwingUtilities.invokeLater(() -> MsgUtil.info(App.mainFrame, "msg.unsupportedLanguage"));
                            return;
                    }

                    final String result = formatted;
                    SwingUtilities.invokeLater(() -> {
                        try {
                            QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setText(result);
                            QuickNoteForm.quickNoteRSyntaxTextViewerManager.getCurrentRSyntaxTextArea().setCaretPosition(0);
                            QuickNoteIndicatorTools.showTips(I18n.format("quickNote.formatted", selectedName),
                                    QuickNoteIndicatorTools.TipsLevel.SUCCESS);
                        } catch (Exception ex) {
                            MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", ex.getMessage());
                            log.error(ExceptionUtils.getStackTrace(ex));
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", e.getMessage()));
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            });
        } catch (Exception e) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 行首缩进捕获（空格或制表符）。
     */
    private static final Pattern LINE_INDENT = Pattern.compile("^([\\s\\t]*)(.*)$");

    /**
     * 已有无序列表标记的行：缩进 + （-、*、+、•、●） + 至少一个空白 + 内容。
     * group(1)=缩进, group(2)=marker, group(3)=内容（不含前导空白）。
     */
    private static final Pattern UNORDERED_LINE = Pattern.compile("^([\\s\\t]*)([-*+•●])\\s+(.*)$");

    /**
     * 已有有序列表标记的行：缩进 + 数字 + . + 至少一个空白 + 内容。
     * group(1)=缩进, group(2)=数字, group(3)=内容。
     */
    private static final Pattern ORDERED_LINE = Pattern.compile("^([\\s\\t]*)(\\d+)\\.\\s+(.*)$");

    /**
     * 无序列表按钮处理：
     * <ul>
     *     <li>markdown 语法下使用 {@code - } 标记，其他语法沿用 {@code ● }，更贴近笔记场景</li>
     *     <li>能识别 {@code -}、{@code *}、{@code +}、{@code •}、{@code ●} 等已有标记并替换/移除</li>
     *     <li>保留行首缩进；跳过纯空行</li>
     *     <li>整批 toggle：选中区域所有非空行都是当前 marker 时批量移除，否则批量添加/替换</li>
     *     <li>有选区时仅处理选区行，无选区时处理全文</li>
     * </ul>
     */
    static void toggleUnorderedList(RSyntaxTextArea view) {
        if (view == null) {
            return;
        }
        try {
            int start = view.getSelectionStart();
            int end = view.getSelectionEnd();
            String selectedText = view.getSelectedText();

            boolean isMarkdown = isCurrentSyntaxMarkdown();
            String marker = isMarkdown ? "-" : "●";
            String prefix = marker + " ";

            List<String> lines = getTargetLines(view, start, end, selectedText);

            // 整批判断：如果所有非空行都以当前 marker 开头，则本次为"批量移除"操作
            boolean allMarkedWithCurrent = hasNonEmptyLine(lines)
                    && lines.stream()
                    .filter(l -> !StringUtils.isBlank(l))
                    .allMatch(l -> l.startsWith(prefix));

            List<String> result = Lists.newArrayListWithCapacity(lines.size());
            for (String line : lines) {
                result.add(toggleUnorderedOnLine(line, marker, prefix, allMarkedWithCurrent));
            }

            applyLines(view, start, end, selectedText, result);
            view.setCaretPosition(0);
        } catch (Exception e1) {
            log.error("UnOrderListButton error", e1);
        }
    }

    /**
     * 处理单行无序列表 toggle。
     */
    private static String toggleUnorderedOnLine(String line, String marker, String prefix,
                                                boolean allMarkedWithCurrent) {
        if (StringUtils.isBlank(line)) {
            return line;
        }
        Matcher m = UNORDERED_LINE.matcher(line);
        if (m.matches()) {
            String indent = m.group(1);
            String content = m.group(3);
            if (allMarkedWithCurrent) {
                // 整批移除
                return indent + content;
            }
            // 替换为当前 marker（不管原本是 -/*/+/•/●）
            return indent + prefix + content;
        }
        // 普通行：仅做缩进 + 添加 marker
        Matcher indentMatch = LINE_INDENT.matcher(line);
        indentMatch.matches();
        return indentMatch.group(1) + prefix + indentMatch.group(2);
    }

    /**
     * 有序列表按钮处理：
     * <ul>
     *     <li>重新按行顺序从 1 开始编号，不再依赖行索引匹配</li>
     *     <li>整批 toggle：所有非空行都已经形成 1..N 连续编号时批量移除，否则全部重排/添加</li>
     *     <li>保留行首缩进；跳过纯空行（编号计数器不递增）</li>
     *     <li>能识别任意 {@code 数字. } 前缀并按当前序号重写</li>
     *     <li>有选区时仅处理选区行，无选区时处理全文</li>
     * </ul>
     */
    static void toggleOrderedList(RSyntaxTextArea view) {
        if (view == null) {
            return;
        }
        try {
            int start = view.getSelectionStart();
            int end = view.getSelectionEnd();
            String selectedText = view.getSelectedText();

            List<String> lines = getTargetLines(view, start, end, selectedText);

            // 收集每个非空行的现有编号，用于判断整批是否已经"1..N 连续"
            int[] nonEmptyNumbers = collectOrderedNumbers(lines);
            boolean allSequentialFromOne = nonEmptyNumbers.length > 0;
            for (int i = 0; i < nonEmptyNumbers.length; i++) {
                if (nonEmptyNumbers[i] != i + 1) {
                    allSequentialFromOne = false;
                    break;
                }
            }

            List<String> result = Lists.newArrayListWithCapacity(lines.size());
            int counter = 1;
            for (String line : lines) {
                if (StringUtils.isBlank(line)) {
                    // 空行不编号，但参与占位以保持行数
                    result.add(line);
                    continue;
                }
                result.add(toggleOrderedOnLine(line, counter, allSequentialFromOne));
                counter++;
            }

            applyLines(view, start, end, selectedText, result);
            view.setCaretPosition(0);
        } catch (Exception e1) {
            log.error("OrderListButton error", e1);
        }
    }

    /**
     * 处理单行有序列表 toggle。{@code number} 是该行在选中区域中的目标序号（从 1 开始）。
     */
    private static String toggleOrderedOnLine(String line, int number, boolean allSequentialFromOne) {
        Matcher m = ORDERED_LINE.matcher(line);
        if (m.matches()) {
            String indent = m.group(1);
            String content = m.group(3);
            if (allSequentialFromOne) {
                // 整批移除编号
                return indent + content;
            }
            // 已带编号或编号错误，重排为目标序号
            return indent + number + ". " + content;
        }
        // 普通行：加缩进 + 编号
        Matcher indentMatch = LINE_INDENT.matcher(line);
        indentMatch.matches();
        return indentMatch.group(1) + number + ". " + indentMatch.group(2);
    }

    /**
     * 获取"目标处理行"：有选区时取选区文本按行切分，否则取全文。
     * 使用 {@code split("\n", -1)} 保留末尾空行，避免丢行。
     */
    private static List<String> getTargetLines(RSyntaxTextArea view, int start, int end, String selectedText) {
        String content = StringUtils.isNotEmpty(selectedText) ? selectedText : view.getText();
        return Lists.newArrayList(content.split("\n", -1));
    }

    /**
     * 收集非空行的现有有序编号（解析失败的行记为 {@code -1}，表示"无编号"）。
     */
    private static int[] collectOrderedNumbers(List<String> lines) {
        List<Integer> numbers = Lists.newArrayList();
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            Matcher m = ORDERED_LINE.matcher(line);
            if (m.matches()) {
                try {
                    numbers.add(Integer.parseInt(m.group(2)));
                } catch (NumberFormatException ignore) {
                    numbers.add(-1);
                }
            } else {
                numbers.add(-1);
            }
        }
        int[] result = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            result[i] = numbers.get(i);
        }
        return result;
    }

    private static boolean hasNonEmptyLine(List<String> lines) {
        for (String line : lines) {
            if (StringUtils.isNotBlank(line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把处理后的行写回编辑器：有选区时替换选区，无选区时整文覆盖。
     */
    private static void applyLines(RSyntaxTextArea view, int start, int end, String selectedText,
                                   List<String> result) {
        String joined = StringUtils.join(result, "\n");
        if (StringUtils.isNotEmpty(selectedText)) {
            view.replaceRange(joined, start, end);
        } else {
            view.setText(joined);
        }
    }

    /**
     * 判断当前笔记的语法下拉是否为 markdown，用于切换列表标记风格。
     */
    private static boolean isCurrentSyntaxMarkdown() {
        try {
            QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
            if (quickNoteForm == null || quickNoteForm.getSyntaxComboBox() == null) {
                return false;
            }
            Object selectedSyntax = quickNoteForm.getSyntaxComboBox().getSelectedItem();
            return selectedSyntax != null
                    && SyntaxConstants.SYNTAX_STYLE_MARKDOWN.substring(5).equals(selectedSyntax.toString());
        } catch (Exception e1) {
            log.warn("isCurrentSyntaxMarkdown check failed", e1);
            return false;
        }
    }
}
