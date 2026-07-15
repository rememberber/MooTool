package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.EditorFontUtil;
import com.luoboduner.moo.tool.domain.TJsonBeauty;
import com.luoboduner.moo.tool.ui.component.FindReplaceBar;
import com.luoboduner.moo.tool.ui.component.JsonBeautyTreeDragDrop;
import com.luoboduner.moo.tool.ui.component.SplitPaneUtil;
import com.luoboduner.moo.tool.ui.dialog.JsonPathPickerDialog;
import com.luoboduner.moo.tool.ui.dialog.JsonResultDialog;
import com.luoboduner.moo.tool.ui.dialog.JsonBeautyGitDialog;
import com.luoboduner.moo.tool.ui.dialog.JsonBeautySettingsUi;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.JsonBeautyForm;
import com.luoboduner.moo.tool.util.*;
import com.luoboduner.moo.tool.util.MsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Json格式化事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/6.
 */
@Slf4j
public class JsonBeautyListener {

    public static String selectedNameJson;

    public static String selectedPathJson;

    public static boolean ignoreQuickSave;

    /** 忽略 JOptionPane 关闭后回传到列表的 Enter 键，避免重命名弹框重复弹出 */
    private static boolean suppressListEnterRename;

    public static void addListeners() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();

        // 格式化按钮事件
        jsonBeautyForm.getBeautifyButton().addActionListener(e -> {
            String jsonText = jsonBeautyForm.getTextArea().getText();
            jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
            jsonBeautyForm.getTextArea().setCaretPosition(0);
        });

        // 保存按钮事件
        jsonBeautyForm.getSaveButton().addActionListener(e -> {
            if (StringUtils.isEmpty(selectedNameJson)) {
                selectedNameJson = NamingUtil.defaultUntitledName();
            }
            String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), selectedNameJson);
            if (StringUtils.isNotBlank(name)) {
                TJsonBeauty tJsonBeauty = JsonBeautyVaultUtil.loadByName(name);
                if (tJsonBeauty == null) {
                    tJsonBeauty = JsonBeautyVaultUtil.createJson(name, JsonBeautyForm.getSelectedFolderPath());
                }
                String now = SqliteUtil.nowDateForSqlite();
                tJsonBeauty.setName(name);
                tJsonBeauty.setContent(JsonBeautyForm.getInstance().getTextArea().getText());
                tJsonBeauty.setCreateTime(now);
                tJsonBeauty.setModifiedTime(now);
                JsonBeautyVaultUtil.saveJson(tJsonBeauty, tJsonBeauty.getContent());
                JsonBeautyVaultRefreshCoordinator.markInternalWrite();
                JsonBeautyForm.refreshList();
                JsonBeautyForm.updateGitButtonStatus();
                selectedNameJson = name;
                selectedPathJson = tJsonBeauty.getRelativePath();

            }
        });

        // 左侧树切换：鼠标按下时直接按坐标解析节点，树选择监听统一覆盖
        // 键盘导航和程序化选择，避免读取 Swing 尚未更新完成的 selection。
        JTree noteTree = jsonBeautyForm.getNoteTree();
        if (noteTree != null) {
            noteTree.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showTreeJson(noteTree.getPathForLocation(e.getX(), e.getY()));
                }
            });
            noteTree.addTreeSelectionListener(e -> showTreeJson(e.getNewLeadSelectionPath()));
        }

        // 左侧树按键事件（重命名、删除、导航）
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
                        renameSelectedItem(jsonBeautyForm);
                    } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
                        deleteFiles(jsonBeautyForm);
                    }
                }
            });
        }

        // 文本域按键事件
        jsonBeautyForm.getTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_S) {
                    quickSave(true);
                } else if (((evt.isControlDown() || evt.isMetaDown()) && evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F) ||
                        evt.isMetaDown() && evt.isAltDown() && evt.getKeyCode() == KeyEvent.VK_L) {
                    String jsonText = jsonBeautyForm.getTextArea().getText();
                    jsonBeautyForm.getTextArea().setText(formatJson(jsonText));
                    jsonBeautyForm.getTextArea().setCaretPosition(0);
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_F) {
                    showFindPanel();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_R) {
                    showFindPanel();
                } else if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_N) {
                    newJson();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });

        jsonBeautyForm.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                JsonBeautyAutoGitScheduler.recordActivity();
                quickSave(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                JsonBeautyAutoGitScheduler.recordActivity();
                quickSave(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (ignoreQuickSave) {
                    return;
                }
                JsonBeautyAutoGitScheduler.recordActivity();
                quickSave(true);
            }
        });

        // 删除按钮事件
        jsonBeautyForm.getDeleteButton().addActionListener(e -> {
            deleteFiles(jsonBeautyForm);
        });

        // 字体名称下拉框事件
        jsonBeautyForm.getFontNameComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String fontName = e.getItem().toString();
                int fontSize = Integer.parseInt(jsonBeautyForm.getFontSizeComboBox().getSelectedItem().toString());
                Font font = EditorFontUtil.getEditorFont(fontName, Font.PLAIN, fontSize);
                jsonBeautyForm.getTextArea().setFont(font);

                App.config.setJsonBeautyFontName(fontName);
                App.config.setJsonBeautyFontSize(fontSize);
                App.config.save();
            }
        });

        // 字体大小下拉框事件
        jsonBeautyForm.getFontSizeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int fontSize = Integer.parseInt(e.getItem().toString());
                String fontName = jsonBeautyForm.getFontNameComboBox().getSelectedItem().toString();
                Font font = EditorFontUtil.getEditorFont(fontName, Font.PLAIN, fontSize);
                jsonBeautyForm.getTextArea().setFont(font);

                App.config.setJsonBeautyFontName(fontName);
                App.config.setJsonBeautyFontSize(fontSize);
                App.config.save();
            }
        });

        // 自动换行按钮事件
        jsonBeautyForm.getWrapButton().addActionListener(e -> {
            jsonBeautyForm.getTextArea().setLineWrap(!jsonBeautyForm.getTextArea().getLineWrap());
        });

        // 添加按钮事件
        jsonBeautyForm.getAddButton().addActionListener(e -> {
            newJson();
        });

        if (jsonBeautyForm.getListSortComboBox() != null) {
            jsonBeautyForm.getListSortComboBox().addActionListener(e -> {
                JsonBeautyListSortMode mode = JsonBeautyForm.getListSortMode();
                App.config.setJsonBeautyListSortMode(mode.name());
                App.config.save();
                JsonBeautyForm.refreshJsonTree();
            });
        }

        jsonBeautyForm.getFindButton().addActionListener(e -> {
            showFindPanel();
        });

        jsonBeautyForm.getListItemButton().addActionListener(e -> {
            int currentDividerLocation = jsonBeautyForm.getSplitPane().getDividerLocation();
            if (currentDividerLocation < 5) {
                SplitPaneUtil.configureListEditorSplit(jsonBeautyForm.getSplitPane());
            } else {
                jsonBeautyForm.getSplitPane().setDividerLocation(0);
            }
        });

        jsonBeautyForm.getExportButton().addActionListener(e -> exportSelectedJsons(jsonBeautyForm));

        // 搜索框变更事件
        jsonBeautyForm.getSearchTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                JsonBeautyForm.refreshList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                JsonBeautyForm.refreshList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                JsonBeautyForm.refreshList();
            }
        });

        jsonBeautyForm.getCompressButton().addActionListener(e -> {
            String jsonText = jsonBeautyForm.getTextArea().getText();
            jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonStr(JSONUtil.isTypeJSONArray(jsonText) ? JSONUtil.parseArray(jsonText) : JSONUtil.parseObj(jsonText), 0));
            jsonBeautyForm.getTextArea().setCaretPosition(0);
        });

        jsonBeautyForm.getCustomFormatButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                JSONConfig jsonConfig = new JSONConfig();
                jsonConfig.setIgnoreCase(jsonBeautyForm.getIgnoreCaseCheckBox().isSelected());
                jsonConfig.setCheckDuplicate(jsonBeautyForm.getCheckDuplicateCheckBox().isSelected());
                if (jsonBeautyForm.getKeySortCheckBox().isSelected()) {
                    jsonConfig.setKeyComparator((o1, o2) -> {
                        if (jsonBeautyForm.getIgnoreCaseCheckBox().isSelected()) {
                            return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
                        } else {
                            return o1.toString().compareTo(o2.toString());
                        }
                    });
                }
                jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonPrettyStr(JSONUtil.parse(jsonText, jsonConfig)));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getJsonToXmlButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                JsonResultDialog jsonResultDialog = new JsonResultDialog("XML", I18n.get("jsonBeauty.jsonToXml"), "Display");
                String xmlStr = JSONUtil.toXmlStr(JSONUtil.isTypeJSONArray(jsonText) ? JSONUtil.parseArray(jsonText) : JSONUtil.parseObj(jsonText));
                xmlStr = "<root>" + xmlStr + "</root>";
                jsonResultDialog.setToTextArea(XmlReformatUtil.format(xmlStr));
                jsonResultDialog.setVisible(true);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getXmlToJsonButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("XML", I18n.get("jsonBeauty.xmlInput"), "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(JSONUtil.toJsonPrettyStr(JSONUtil.xmlToJson(inputValue)));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getEscapeJsonButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(JSONUtil.escape(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.escapeFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getEscapeStringButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(StringEscapeUtils.escapeJava(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.escapeFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getUnescapeButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                jsonBeautyForm.getTextArea().setText(StringEscapeUtils.unescapeJson(jsonText));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.unescapeFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }

        });

        jsonBeautyForm.getGetByJsonPathButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                String jsonPath = jsonBeautyForm.getJsonPathTextField().getText();
                if (StringUtils.isBlank(jsonText)) {
                    MsgUtil.info(App.mainFrame, "msg.emptyJson");
                    return;
                }
                if (StringUtils.isBlank(jsonPath)) {
                    MsgUtil.info(App.mainFrame, "msg.jsonPathEmpty");
                    return;
                }
                JsonResultDialog jsonResultDialog = new JsonResultDialog("JSON", I18n.get("jsonBeauty.jsonPathResult"), "Display");
                jsonResultDialog.setToTextArea(JsonPathUtil.getFormattedValue(jsonText, jsonPath));
                jsonResultDialog.setVisible(true);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.getFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getGetJsonPathButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();
                if (StringUtils.isBlank(jsonText)) {
                    MsgUtil.info(App.mainFrame, "msg.emptyJson");
                    return;
                }
                JsonPathPickerDialog jsonPathPickerDialog = new JsonPathPickerDialog(jsonText);
                jsonPathPickerDialog.setVisible(true);
                String selectedJsonPath = jsonPathPickerDialog.getSelectedJsonPath();
                if (StringUtils.isNotBlank(selectedJsonPath)) {
                    jsonBeautyForm.getJsonPathTextField().setText(selectedJsonPath);
                }
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.getFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // 快捷替换按钮
        jsonBeautyForm.getMoreButton().addActionListener(e -> {
            int totalWidth = jsonBeautyForm.getContentSplitPane().getWidth();
            int currentDividerLocation = jsonBeautyForm.getContentSplitPane().getDividerLocation();

            if (totalWidth - currentDividerLocation < 10) {
                jsonBeautyForm.getMoreScrollPane().setVisible(true);
                SplitPaneUtil.showSecondary(jsonBeautyForm.getContentSplitPane(), SplitPaneUtil.SECONDARY_PANEL_RATIO);
            } else {
                SplitPaneUtil.hideSecondary(jsonBeautyForm.getContentSplitPane());
                jsonBeautyForm.getMoreScrollPane().setVisible(false);
            }
        });

        jsonBeautyForm.getMoreCloseButton().addActionListener(e -> {
            SplitPaneUtil.hideSecondary(jsonBeautyForm.getContentSplitPane());
            jsonBeautyForm.getMoreScrollPane().setVisible(false);
        });

        jsonBeautyForm.getBeanToJsonButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", I18n.get("jsonBeauty.javaBeanInput"), "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(MockDataGenerator.classCodeToJson(inputValue));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getJavaBeanToJSONButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", I18n.get("jsonBeauty.javaBeanInput"), "Input");
                jsonResultDialog.setVisible(true);
                String inputValue = JsonResultDialog.textInputValue;
                if (StringUtils.isBlank(inputValue)) {
                    return;
                }
                jsonBeautyForm.getTextArea().setText(MockDataGenerator.classCodeToJson(inputValue));
                jsonBeautyForm.getTextArea().setCaretPosition(0);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getJsonToJavaBeanButton().addActionListener(e -> {
            try {
                JsonResultDialog jsonResultDialog = new JsonResultDialog("Java", I18n.get("jsonBeauty.javaBeanResult"), "Display");
                jsonResultDialog.setToTextArea(MockDataGenerator.jsonToClassCode(jsonBeautyForm.getTextArea().getText()));
                jsonResultDialog.setVisible(true);
            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        jsonBeautyForm.getKeyValueSwapButton().addActionListener(e -> {
            try {
                String jsonText = jsonBeautyForm.getTextArea().getText();

                JsonResultDialog jsonResultDialog = new JsonResultDialog("JSON", I18n.get("jsonBeauty.kvSwapResult"), "Display");
                jsonResultDialog.setToTextArea(JSONUtil.toJsonPrettyStr(JsonKeyValueSwapper.swapKeysAndValues(jsonText)));
                jsonResultDialog.setVisible(true);

            } catch (Exception e1) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.convertFailed", e1.getMessage());
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });

        // 左侧列表增加右键菜单
        JPopupMenu noteListPopupMenu = new JPopupMenu();
        JMenuItem newFileMenuItem = new JMenuItem(I18n.get("quickNote.newFile"));
        JMenuItem newFolderMenuItem = new JMenuItem(I18n.get("quickNote.newFolder"));
        JMenuItem renameMenuItem = new JMenuItem(I18n.get("common.rename"));
        JMenuItem deleteMenuItem = new JMenuItem(I18n.get("common.delete"));
        JMenuItem exportMenuItem = new JMenuItem(I18n.get("common.export"));
        JMenuItem duplicateMenuItem = new JMenuItem(I18n.get("quickNote.menu.duplicate"));
        JMenuItem moveToFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.moveToFolder"));
        JMenuItem deleteFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.deleteFolder"));
        JMenuItem revealInFolderMenuItem = new JMenuItem(I18n.get("quickNote.menu.revealInFolder"));
        noteListPopupMenu.add(newFileMenuItem);
        noteListPopupMenu.add(newFolderMenuItem);
        noteListPopupMenu.add(renameMenuItem);
        noteListPopupMenu.add(moveToFolderMenuItem);
        noteListPopupMenu.add(deleteMenuItem);
        noteListPopupMenu.add(deleteFolderMenuItem);
        noteListPopupMenu.add(exportMenuItem);
        noteListPopupMenu.add(duplicateMenuItem);
        noteListPopupMenu.addSeparator();
        noteListPopupMenu.add(revealInFolderMenuItem);

        JMenuItem refreshMenuItem = new JMenuItem(I18n.get("jsonBeauty.menu.refresh"));
        JMenuItem openVaultMenuItem = new JMenuItem(I18n.get("jsonBeauty.menu.openVault"));
        JMenuItem vaultSettingsMenuItem = new JMenuItem(I18n.get("jsonBeauty.menu.vaultSettings"));
        JMenuItem gitMenuItem = new JMenuItem(I18n.get("jsonBeauty.menu.git"));
        noteListPopupMenu.addSeparator();
        noteListPopupMenu.add(refreshMenuItem);
        noteListPopupMenu.add(openVaultMenuItem);
        noteListPopupMenu.add(vaultSettingsMenuItem);
        noteListPopupMenu.add(gitMenuItem);
        if (noteTree != null) {
            noteTree.setComponentPopupMenu(noteListPopupMenu);
            noteListPopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    boolean hasItems = !JsonBeautyForm.getSelectedJsons().isEmpty();
                    String folderPath = JsonBeautyForm.getSelectedFolderPath();
                    boolean folderNode = isFolderNodeSelected(noteTree);
                    moveToFolderMenuItem.setEnabled(hasItems);
                    deleteFolderMenuItem.setEnabled(folderNode && StringUtils.isNotBlank(folderPath)
                            && JsonBeautyVaultUtil.isFolderEmpty(folderPath));
                    renameMenuItem.setEnabled(hasItems || (folderNode && StringUtils.isNotBlank(folderPath)));
                    deleteMenuItem.setEnabled(hasItems);
                    exportMenuItem.setEnabled(hasItems);
                    duplicateMenuItem.setEnabled(hasItems);
                    revealInFolderMenuItem.setEnabled(hasItems || folderNode);
                }

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                }
            });
        }

        newFileMenuItem.addActionListener(e -> newJson());
        jsonBeautyForm.getNewFolderButton().addActionListener(e -> createFolder(jsonBeautyForm));
        newFolderMenuItem.addActionListener(e -> createFolder(jsonBeautyForm));
        renameMenuItem.addActionListener(e -> renameSelectedItem(jsonBeautyForm));
        deleteMenuItem.addActionListener(e -> deleteFiles(jsonBeautyForm));
        moveToFolderMenuItem.addActionListener(e -> moveSelectedJsonsToFolder(jsonBeautyForm));
        deleteFolderMenuItem.addActionListener(e -> deleteSelectedFolder(jsonBeautyForm));
        exportMenuItem.addActionListener(e -> exportSelectedJsons(jsonBeautyForm));
        duplicateMenuItem.addActionListener(e -> duplicateSelectedJsons(jsonBeautyForm));
        revealInFolderMenuItem.addActionListener(e -> revealSelectedInFileManager(jsonBeautyForm));

        refreshMenuItem.addActionListener(e -> JsonBeautyVaultRefreshCoordinator.refreshAfterExternalChange());
        openVaultMenuItem.addActionListener(e -> JsonBeautyVaultUtil.openVaultDir());
        vaultSettingsMenuItem.addActionListener(e -> JsonBeautySettingsUi.showDialog());
        gitMenuItem.addActionListener(e -> JsonBeautyGitDialog.showDialog());

        jsonBeautyForm.getGitButton().addActionListener(e ->
                JsonBeautyCommitEntryAction.trigger(jsonBeautyForm.getGitButton()));
        jsonBeautyForm.getVaultSettingsButton().addActionListener(e -> JsonBeautySettingsUi.showDialog());

    }

    public static void quickSaveSync(boolean refreshModifiedTime) {
        quickSave(refreshModifiedTime);
    }

    public static void flushSelectedJsonBeforePathChange() {
        if (StringUtils.isNotBlank(selectedPathJson)) {
            quickSaveSync(true);
        }
    }

    public static void runJsonPathMutation(Runnable mutation, String... pathsToSkip) {
        ignoreQuickSave = true;
        try {
            JsonBeautyVaultRefreshCoordinator.addSkipSavePaths(List.of(pathsToSkip));
            mutation.run();
        } finally {
            JsonBeautyVaultRefreshCoordinator.clearSkipSavePaths();
            ignoreQuickSave = false;
        }
    }

    private static void createFolder(JsonBeautyForm jsonBeautyForm) {
        String parentFolder = JsonBeautyForm.getSelectedFolderPath();
        String folderName = promptNameSuppressingTreeEnter(I18n.get("quickNote.newFolderDefault"));
        if (StringUtils.isBlank(folderName)) {
            return;
        }
        folderName = JsonBeautyVaultUtil.sanitizeFileName(folderName);
        String folderPath = StringUtils.isBlank(parentFolder)
                ? folderName
                : parentFolder + "/" + folderName;
        JsonBeautyVaultUtil.createFolder(folderPath);
        JsonBeautyForm.initList();
    }

    private static void showTreeJson(TreePath treePath) {
        if (treePath == null) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        Object value = node.getUserObject();
        if (!(value instanceof TJsonBeauty item)
                || StringUtils.equals(selectedPathJson, item.getRelativePath())) {
            return;
        }
        flushSelectedJsonBeforePathChange();
        ignoreQuickSave = true;
        try {
            JsonBeautyForm.showJson(item);
        } catch (Exception ex) {
            log.error("Switch JSON file failed: {}", item.getRelativePath(), ex);
        } finally {
            ignoreQuickSave = false;
        }
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

    private static void moveSelectedJsonsToFolder(JsonBeautyForm jsonBeautyForm) {
        List<TJsonBeauty> selectedItems = JsonBeautyForm.getSelectedJsons();
        if (selectedItems.isEmpty()) {
            MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            return;
        }
        List<String> folders = new ArrayList<>();
        folders.add("");
        folders.addAll(JsonBeautyVaultUtil.listFolders());
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
        String currentFolder = JsonBeautyVaultUtil.parentFolder(selectedItems.get(0).getRelativePath());
        folderComboBox.setSelectedItem(currentFolder);

        int confirm = JOptionPane.showConfirmDialog(
                jsonBeautyForm.getJsonBeautyPanel(),
                folderComboBox,
                I18n.get("quickNote.menu.moveToFolder"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        Object selected = folderComboBox.getSelectedItem();
        String targetFolder = selected == null ? "" : selected.toString();
        JsonBeautyTreeDragDrop.moveJsonFiles(
                selectedItems.stream().map(TJsonBeauty::getRelativePath).toList(),
                targetFolder,
                jsonBeautyForm.getNoteTree());
    }

    private static void deleteSelectedFolder(JsonBeautyForm jsonBeautyForm) {
        String folderPath = JsonBeautyForm.getSelectedFolderPath();
        if (StringUtils.isBlank(folderPath)) {
            return;
        }
        if (!JsonBeautyVaultUtil.isFolderEmpty(folderPath)) {
            MsgUtil.info(App.mainFrame, "quickNote.folderNotEmpty");
            return;
        }
        int confirm = MsgUtil.confirm(App.mainFrame, "quickNote.confirmDeleteFolder");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (JsonBeautyVaultUtil.deleteFolderIfEmpty(folderPath)) {
            JsonBeautyForm.initList();
            JsonBeautyForm.updateGitButtonStatus();
        }
    }

    private static void duplicateSelectedJsons(JsonBeautyForm jsonBeautyForm) {
        List<TJsonBeauty> selectedItems = JsonBeautyForm.getSelectedJsons();
        if (selectedItems.isEmpty()) {
            MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
            return;
        }
        String lastPath = null;
        for (TJsonBeauty item : selectedItems) {
            TJsonBeauty copy = JsonBeautyVaultUtil.duplicateJson(item.getRelativePath());
            if (copy != null) {
                lastPath = copy.getRelativePath();
            }
        }
        if (StringUtils.isNotBlank(lastPath)) {
            selectedPathJson = lastPath;
            TJsonBeauty copied = JsonBeautyVaultUtil.loadByPath(lastPath);
            if (copied != null) {
                selectedNameJson = copied.getName();
            }
            JsonBeautyForm.initList();
            JsonBeautyForm.updateGitButtonStatus();
        }
    }

    private static void exportSelectedJsons(JsonBeautyForm jsonBeautyForm) {
        try {
            List<TJsonBeauty> selectedItems = JsonBeautyForm.getSelectedJsons();
            if (selectedItems.isEmpty()) {
                MsgUtil.info(jsonBeautyForm.getJsonBeautyPanel(), "msg.selectAtLeastOne");
                return;
            }
            SystemFileChooser fileChooser = new SystemFileChooser(App.config.getJsonBeautyExportPath());
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            int approve = fileChooser.showOpenDialog(jsonBeautyForm.getJsonBeautyPanel());
            String exportPath;
            if (approve == SystemFileChooser.APPROVE_OPTION) {
                exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                App.config.setJsonBeautyExportPath(exportPath);
                App.config.save();
            } else {
                return;
            }

            Set<String> usedExportNames = new HashSet<>();
            for (TJsonBeauty item : selectedItems) {
                TJsonBeauty fullItem = JsonBeautyVaultUtil.loadByPath(item.getRelativePath());
                if (fullItem == null) {
                    continue;
                }
                String targetName = fullItem.getName() + ".json";
                int suffix = 1;
                String uniqueName = targetName;
                while (usedExportNames.contains(uniqueName)) {
                    String base = fullItem.getName() + "-" + suffix;
                    uniqueName = base + ".json";
                    suffix++;
                }
                usedExportNames.add(uniqueName);
                File exportFile = FileUtil.touch(exportPath + File.separator + uniqueName);
                FileUtil.writeUtf8String(fullItem.getContent(), exportFile);
            }
            MsgUtil.success(jsonBeautyForm.getJsonBeautyPanel(), "msg.exportSuccess");
            try {
                Desktop.getDesktop().open(new File(exportPath));
            } catch (Exception e2) {
                log.error(ExceptionUtils.getStackTrace(e2));
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(jsonBeautyForm.getJsonBeautyPanel(), "msg.exportFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
        }
    }

    private static void revealSelectedInFileManager(JsonBeautyForm jsonBeautyForm) {
        List<TJsonBeauty> selectedItems = JsonBeautyForm.getSelectedJsons();
        if (!selectedItems.isEmpty()) {
            JsonBeautyVaultUtil.revealInFileManager(selectedItems.get(0).getRelativePath());
            return;
        }
        if (isFolderNodeSelected(jsonBeautyForm.getNoteTree())) {
            JsonBeautyVaultUtil.revealInFileManager(JsonBeautyForm.getSelectedFolderPath());
        }
    }

    private static String promptNameSuppressingTreeEnter(String defaultName) {
        suppressListEnterRename = true;
        return MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), defaultName);
    }

    private static void renameSelectedItem(JsonBeautyForm jsonBeautyForm) {
        TJsonBeauty item = JsonBeautyForm.getSelectedTreeJson();
        if (item != null) {
            renameSelectedJson(jsonBeautyForm, item);
            return;
        }
        String folderPath = JsonBeautyForm.getSelectedFolderPath();
        if (StringUtils.isBlank(folderPath) || !isFolderNodeSelected(jsonBeautyForm.getNoteTree())) {
            return;
        }
        String beforeName = JsonBeautyVaultUtil.folderLeafName(folderPath);
        String afterName = promptNameSuppressingTreeEnter(beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            suppressListEnterRename = false;
            return;
        }
        try {
            List<String> affectedPaths = JsonBeautyVaultUtil.listJsonPathsUnderFolder(folderPath);
            if (StringUtils.isNotBlank(selectedPathJson) && affectedPaths.contains(selectedPathJson)) {
                flushSelectedJsonBeforePathChange();
            }
            runJsonPathMutation(() -> {
                String newFolderPath = JsonBeautyVaultUtil.renameFolder(folderPath, afterName);
                if (StringUtils.isNotBlank(selectedPathJson)) {
                    selectedPathJson = JsonBeautyVaultUtil.remapPathAfterFolderRename(
                            selectedPathJson, folderPath, newFolderPath);
                }
                JsonBeautyForm.initList();
                JsonBeautyForm.updateGitButtonStatus();
            }, affectedPaths.toArray(new String[0]));
        } catch (Exception e) {
            MsgUtil.info(App.mainFrame, "msg.renameFolderFailed");
            JsonBeautyForm.initList();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void renameSelectedJson(JsonBeautyForm jsonBeautyForm, TJsonBeauty item) {
        String beforeName = item.getName();
        String beforePath = item.getRelativePath();
        if (StringUtils.isBlank(beforeName) || StringUtils.isBlank(beforePath)) {
            return;
        }
        String afterName = promptNameSuppressingTreeEnter(beforeName);
        if (StringUtils.isBlank(afterName) || afterName.equals(beforeName)) {
            return;
        }
        try {
            flushSelectedJsonBeforePathChange();
            runJsonPathMutation(() -> {
                String newPath = JsonBeautyVaultUtil.renameJson(beforePath, afterName);
                selectedNameJson = afterName;
                selectedPathJson = newPath;
                JsonBeautyForm.initList();
                JsonBeautyForm.updateGitButtonStatus();
            }, beforePath);
        } catch (Exception e) {
            MsgUtil.info(App.mainFrame, "msg.renameFailed");
            JsonBeautyForm.initList();
            log.error(e.toString());
        }
    }

    private static void deleteFiles(JsonBeautyForm jsonBeautyForm) {
        try {
            List<TJsonBeauty> selectedItems = JsonBeautyForm.getSelectedJsons();
            if (selectedItems.isEmpty()) {
                MsgUtil.info(App.mainFrame, "msg.selectAtLeastOne");
                return;
            }
            int isDelete = MsgUtil.confirm(App.mainFrame, "msg.confirmDelete");
            if (isDelete != JOptionPane.YES_OPTION) {
                return;
            }
            List<String> deletedPaths = selectedItems.stream()
                    .map(TJsonBeauty::getRelativePath)
                    .filter(StringUtils::isNotBlank)
                    .toList();
            ignoreQuickSave = true;
            try {
                JsonBeautyVaultRefreshCoordinator.addSkipSavePaths(deletedPaths);
                if (StringUtils.isNotBlank(selectedPathJson) && deletedPaths.contains(selectedPathJson)) {
                    selectedPathJson = null;
                    selectedNameJson = null;
                    jsonBeautyForm.getTextArea().setText("");
                }
                for (String path : deletedPaths) {
                    JsonBeautyVaultUtil.deleteByPath(path);
                }
                JsonBeautyForm.initList();
                JsonBeautyForm.updateGitButtonStatus();
            } finally {
                JsonBeautyVaultRefreshCoordinator.clearSkipSavePaths();
                ignoreQuickSave = false;
            }
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.deleteFailed", e1.getMessage());
            log.error(e1.toString());
        }
    }

    /**
     * save for quick key and item change
     *
     * @param refreshModifiedTime
     */
    private static void quickSave(boolean refreshModifiedTime) {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        String now = SqliteUtil.nowDateForSqlite();
        String path = selectedPathJson;
        if (path != null) {
            if (JsonBeautyVaultRefreshCoordinator.shouldSkipSaveForPath(path)) {
                return;
            }
            TJsonBeauty tJsonBeauty = JsonBeautyVaultUtil.loadByPath(path);
            if (tJsonBeauty == null) {
                return;
            }
            tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
            tJsonBeauty.setModifiedTime(refreshModifiedTime ? now : tJsonBeauty.getModifiedTime());
            JsonBeautyVaultUtil.saveJson(tJsonBeauty, tJsonBeauty.getContent());
            JsonBeautyVaultRefreshCoordinator.markInternalWrite();
            JsonBeautyForm.updateGitButtonStatus();
        } else {
            String tempName = NamingUtil.defaultUntitledName();
            String name = MsgUtil.inputName(MainWindow.getInstance().getMainPanel(), tempName);
            if (StringUtils.isNotBlank(name)) {
                TJsonBeauty tJsonBeauty = JsonBeautyVaultUtil.createJson(name, JsonBeautyForm.getSelectedFolderPath());
                tJsonBeauty.setContent(jsonBeautyForm.getTextArea().getText());
                tJsonBeauty.setCreateTime(now);
                tJsonBeauty.setModifiedTime(now);

                JsonBeautyVaultUtil.saveJson(tJsonBeauty, tJsonBeauty.getContent());
                JsonBeautyVaultRefreshCoordinator.markInternalWrite();
                JsonBeautyForm.refreshList();
                JsonBeautyForm.updateGitButtonStatus();
                selectedNameJson = name;
                selectedPathJson = tJsonBeauty.getRelativePath();
            }
        }
    }

    private static void newJson() {
        String name = promptNameSuppressingTreeEnter(getDefaultFileName());
        if (StringUtils.isNotBlank(name)) {
            String folderPath = JsonBeautyForm.getSelectedFolderPath();
            if (JsonBeautyVaultUtil.jsonTitleExistsInFolder(name, folderPath)) {
                MsgUtil.info(App.mainFrame, "msg.duplicateNoteName");
                return;
            }
            TJsonBeauty tJsonBeauty = JsonBeautyVaultUtil.createJson(name, folderPath);
            selectedNameJson = tJsonBeauty.getName();
            selectedPathJson = tJsonBeauty.getRelativePath();
            JsonBeautyForm.initList();
            JsonBeautyForm.updateGitButtonStatus();
        }
    }

    static String formatJson(String jsonText) {
        try {
            jsonText = JSONUtil.toJsonPrettyStr(jsonText);
        } catch (Exception e1) {
            MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", e1.getMessage());
            log.error(ExceptionUtils.getStackTrace(e1));
            try {
                jsonText = JSONUtil.formatJsonStr(jsonText);
            } catch (Exception e) {
                MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", e.getMessage());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return jsonText;
    }

    public static void showFindPanel() {
        JsonBeautyForm jsonBeautyForm = JsonBeautyForm.getInstance();
        jsonBeautyForm.getFindReplacePanel().removeAll();
        jsonBeautyForm.getFindReplacePanel().setDoubleBuffered(true);
        FindReplaceBar findReplaceBar = new FindReplaceBar(jsonBeautyForm.getTextArea());
        jsonBeautyForm.getFindReplacePanel().add(findReplaceBar.getFindOptionPanel());
        jsonBeautyForm.getFindReplacePanel().setVisible(true);
        jsonBeautyForm.getFindReplacePanel().updateUI();
        findReplaceBar.getFindField().setText(jsonBeautyForm.getTextArea().getSelectedText());
        findReplaceBar.getFindField().grabFocus();
        findReplaceBar.getFindField().selectAll();
    }

    /**
     * Default File Name
     *
     * @return
     */
    private static String getDefaultFileName() {
        return NamingUtil.defaultUntitledName();
    }

}
