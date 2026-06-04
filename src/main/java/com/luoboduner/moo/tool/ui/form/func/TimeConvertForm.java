package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.dao.TFuncContentMapper;
import com.luoboduner.moo.tool.domain.TFuncContent;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.listener.func.TimeConvertListener;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.ScrollUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <pre>
 * 时间转换
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/7.
 */
@Getter
public class TimeConvertForm {
    private JTextField timestampTextField;
    private JComboBox unitComboBox;
    private JButton toLocalTimeButton;
    private JTextField gmtTextField;
    private JButton toTimestampButton;
    private JPanel timeConvertPanel;
    private JLabel currentTimestampLabel;
    private JLabel currentGmtLabel;
    private JButton copyGeneratedTimestampButton;
    private JButton copyGeneratedLocalTimeButton;
    private JButton copyCurrentGmtButton;
    private JButton copyCurrentTimestampButton;
    private JPanel leftPanel;
    private JTextArea timeHisTextArea;
    private JTextField timeFormatTextField;
    private JSplitPane splitPane;
    private JScrollPane leftScrollPane;
    private JButton clockButton;
    private JComboBox<String> timezoneComboBox;
    private JLabel gmtLabel;
    private JPanel timezoneQuickPanel;

    private static final Log logger = LogFactory.get();

    private static TimeConvertForm timeConvertForm;

    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Common timezone IDs for the combo box
     */
    private static final String[] COMMON_TIMEZONE_IDS = {
            "UTC",
            "Asia/Shanghai",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Asia/Singapore",
            "Asia/Hong_Kong",
            "Asia/Kolkata",
            "Asia/Dubai",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Moscow",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "Australia/Sydney",
            "Pacific/Auckland"
    };

    /**
     * Quick timezone buttons: display label -> timezone ID
     */
    private static final String[][] QUICK_TIMEZONE_BUTTONS = {
            {"UTC", "UTC"},
            {"+8", "Asia/Shanghai"},
            {"+9", "Asia/Tokyo"},
            {"-5", "America/New_York"},
            {"-8", "America/Los_Angeles"},
            {"+1", "Europe/Paris"},
            {"+3", "Europe/Moscow"},
    };

    private static TFuncContentMapper funcContentMapper = MybatisUtil.getSqlSession().getMapper(TFuncContentMapper.class);

    private TimeConvertForm() {
        UndoUtil.register(this);
    }

    public static TimeConvertForm getInstance() {
        if (timeConvertForm == null) {
            timeConvertForm = new TimeConvertForm();
        }
        return timeConvertForm;
    }

    /**
     * Get the currently selected TimeZone from the combo box.
     */
    public TimeZone getSelectedTimeZone() {
        if (timezoneComboBox == null || timezoneComboBox.getSelectedItem() == null) {
            return TimeZone.getDefault();
        }
        String selected = (String) timezoneComboBox.getSelectedItem();
        // Extract timezone ID from display string like "Asia/Shanghai (GMT+08:00)"
        int parenIndex = selected.indexOf(" (");
        String tzId = parenIndex > 0 ? selected.substring(0, parenIndex) : selected;
        return TimeZone.getTimeZone(tzId);
    }

    /**
     * Format a timezone ID for display: "Asia/Shanghai (GMT+08:00)"
     */
    private static String formatTimezoneDisplay(String tzId) {
        TimeZone tz = TimeZone.getTimeZone(tzId);
        int rawOffset = tz.getRawOffset();
        int hours = rawOffset / 3600000;
        int minutes = Math.abs((rawOffset % 3600000) / 60000);
        return String.format("%s (GMT%+03d:%02d)", tzId, hours, minutes);
    }

    /**
     * Update the gmtLabel to show the selected timezone.
     */
    private void updateGmtLabel() {
        TimeZone tz = getSelectedTimeZone();
        int rawOffset = tz.getRawOffset();
        int hours = rawOffset / 3600000;
        int minutes = Math.abs((rawOffset % 3600000) / 60000);
        String offsetStr = String.format("GMT%+03d:%02d", hours, minutes);
        if (gmtLabel != null) {
            gmtLabel.setText("时间(" + offsetStr + ")");
        }
    }

    public static void init() {
        timeConvertForm = getInstance();

        // Initialize timezone combo box
        initTimezoneComponents();

        ThreadUtil.execute(() -> {
            while (true) {
                timeConvertForm.getCurrentTimestampLabel().setText(String.valueOf(System.currentTimeMillis() / 1000));
                timeConvertForm.getCurrentGmtLabel().setText(DateFormatUtils.format(new Date(), TIME_FORMAT));
                ThreadUtil.safeSleep(1000);
            }
        });

        if ("".equals(timeConvertForm.getTimestampTextField().getText())) {
            timeConvertForm.getTimestampTextField().setText(String.valueOf(System.currentTimeMillis()));
        }
        if ("".equals(timeConvertForm.getGmtTextField().getText())) {
            TimeZone tz = timeConvertForm.getSelectedTimeZone();
            timeConvertForm.getGmtTextField().setText(DateFormatUtils.format(new Date(), TIME_FORMAT, tz));
        }

        Style.emphaticIndicatorFont(timeConvertForm.getCurrentGmtLabel());
        Style.emphaticIndicatorFont(timeConvertForm.getCurrentTimestampLabel());
        Style.emphaticIndicatorFont(timeConvertForm.getTimestampTextField());
        Style.emphaticIndicatorFont(timeConvertForm.getGmtTextField());
        Style.emphaticIndicatorFont(timeConvertForm.getGmtTextField());
        Style.emphaticIndicatorFont(timeConvertForm.getTimeFormatTextField());
        timeConvertForm.getCurrentGmtLabel().setForeground(Style.YELLOW);
        timeConvertForm.getCurrentTimestampLabel().setForeground(Style.YELLOW);

        Style.blackTextArea(timeConvertForm.getTimeHisTextArea());

        timeConvertForm.getToTimestampButton().setIcon(new FlatSVGIcon("icon/up.svg"));
        timeConvertForm.getToLocalTimeButton().setIcon(new FlatSVGIcon("icon/down.svg"));
        timeConvertForm.getClockButton().setIcon(new FlatSVGIcon("icon/full_screen.svg"));

        timeConvertForm.getSplitPane().setDividerLocation((int) (timeConvertForm.getSplitPane().getWidth() * 0.62));

        ScrollUtil.smoothPane(timeConvertForm.getLeftScrollPane());

        TFuncContent tFuncContent = funcContentMapper.selectByFunc(FuncConsts.TIME_CONVERT);
        if (tFuncContent != null) {
            timeConvertForm.getTimeHisTextArea().setText(tFuncContent.getContent());
        }
        timeConvertForm.getTimeConvertPanel().updateUI();

        TimeConvertListener.addListeners();
    }

    /**
     * Initialize timezone selection components and add them to the UI.
     */
    private static void initTimezoneComponents() {
        // Create timezone combo box with common timezones
        timeConvertForm.timezoneComboBox = new JComboBox<>();
        for (String tzId : COMMON_TIMEZONE_IDS) {
            timeConvertForm.timezoneComboBox.addItem(formatTimezoneDisplay(tzId));
        }

        // Set default selection to system default timezone
        String systemTzId = TimeZone.getDefault().getID();
        String systemDisplay = formatTimezoneDisplay(systemTzId);
        boolean found = false;
        for (int i = 0; i < timeConvertForm.timezoneComboBox.getItemCount(); i++) {
            if (timeConvertForm.timezoneComboBox.getItemAt(i).equals(systemDisplay)) {
                timeConvertForm.timezoneComboBox.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            // Add system timezone if not in the common list
            timeConvertForm.timezoneComboBox.insertItemAt(systemDisplay, 0);
            timeConvertForm.timezoneComboBox.setSelectedIndex(0);
        }

        // Create quick timezone buttons panel
        timeConvertForm.timezoneQuickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel tzLabel = new JLabel("时区:");
        timeConvertForm.timezoneQuickPanel.add(tzLabel);
        timeConvertForm.timezoneQuickPanel.add(timeConvertForm.timezoneComboBox);

        for (String[] btnDef : QUICK_TIMEZONE_BUTTONS) {
            JButton btn = new JButton(btnDef[0]);
            btn.setMargin(new Insets(2, 6, 2, 6));
            String tzId = btnDef[1];
            btn.setToolTipText(formatTimezoneDisplay(tzId));
            btn.addActionListener(e -> {
                String display = formatTimezoneDisplay(tzId);
                for (int i = 0; i < timeConvertForm.timezoneComboBox.getItemCount(); i++) {
                    if (timeConvertForm.timezoneComboBox.getItemAt(i).equals(display)) {
                        timeConvertForm.timezoneComboBox.setSelectedIndex(i);
                        return;
                    }
                }
                // If not found, add and select
                timeConvertForm.timezoneComboBox.addItem(display);
                timeConvertForm.timezoneComboBox.setSelectedItem(display);
            });
            timeConvertForm.timezoneQuickPanel.add(btn);
        }

        // Listen for timezone changes to update label
        timeConvertForm.timezoneComboBox.addActionListener(e -> {
            timeConvertForm.updateGmtLabel();
        });

        // Add timezone panel to panel5 (the conversion panel, parent of gmtTextField)
        Container gmtParent = timeConvertForm.getGmtTextField().getParent();
        if (gmtParent instanceof JPanel) {
            JPanel panel5 = (JPanel) gmtParent;
            LayoutManager lm = panel5.getLayout();
            if (lm instanceof GridLayoutManager) {
                // Store all existing components and their constraints
                Component[] components = panel5.getComponents();
                GridLayoutManager glm = (GridLayoutManager) lm;
                GridConstraints[] storedConstraints = new GridConstraints[components.length];
                for (int i = 0; i < components.length; i++) {
                    storedConstraints[i] = glm.getConstraintsForComponent(components[i]);
                }

                // Rebuild panel5 with 4 rows (was 3 rows x 3 cols)
                panel5.removeAll();
                panel5.setLayout(new GridLayoutManager(4, 3, new Insets(10, 10, 10, 10), -1, -1));

                // Re-add existing components with their original constraints
                for (int i = 0; i < components.length; i++) {
                    panel5.add(components[i], storedConstraints[i]);
                }

                // Add timezone panel at row 3, spanning all 3 columns
                panel5.add(timeConvertForm.timezoneQuickPanel,
                        new GridConstraints(3, 0, 1, 3,
                                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                GridConstraints.SIZEPOLICY_FIXED,
                                null, null, null, 0, false));
            }

            // Find and store reference to the "本地时间" label for dynamic updates
            for (Component comp : panel5.getComponents()) {
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    if (label.getText() != null && label.getText().contains("本地时间")) {
                        timeConvertForm.gmtLabel = label;
                        break;
                    }
                }
            }
        }

        // Update the label to show current timezone
        timeConvertForm.updateGmtLabel();
    }

    public static int saveContent() {
        timeConvertForm = getInstance();
        String timeHisText = timeConvertForm.getTimeHisTextArea().getText();
        String now = SqliteUtil.nowDateForSqlite();

        TFuncContent tFuncContent = funcContentMapper.selectByFunc(FuncConsts.TIME_CONVERT);
        if (tFuncContent == null) {
            tFuncContent = new TFuncContent();
            tFuncContent.setFunc(FuncConsts.TIME_CONVERT);
            tFuncContent.setContent(timeHisText);
            tFuncContent.setCreateTime(now);
            tFuncContent.setModifiedTime(now);

            return funcContentMapper.insert(tFuncContent);
        } else {
            tFuncContent.setContent(timeHisText);
            tFuncContent.setModifiedTime(now);
            return funcContentMapper.updateByPrimaryKeySelective(tFuncContent);
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
        timeConvertPanel = new JPanel();
        timeConvertPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        timeConvertPanel.setMinimumSize(new Dimension(400, 300));
        timeConvertPanel.setPreferredSize(new Dimension(400, 300));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(550);
        timeConvertPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(leftPanel);
        leftScrollPane = new JScrollPane();
        leftPanel.add(leftScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        leftScrollPane.setViewportView(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 5, new Insets(10, 10, 10, 10), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        currentGmtLabel = new JLabel();
        Font currentGmtLabelFont = this.$$$getFont$$$(null, -1, 36, currentGmtLabel.getFont());
        if (currentGmtLabelFont != null) currentGmtLabel.setFont(currentGmtLabelFont);
        currentGmtLabel.setForeground(new Color(-14739));
        currentGmtLabel.setText("--");
        panel3.add(currentGmtLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        copyCurrentGmtButton = new JButton();
        copyCurrentGmtButton.setText("复制");
        panel3.add(copyCurrentGmtButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        separator1.setOrientation(1);
        panel3.add(separator1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        clockButton = new JButton();
        clockButton.setText("");
        clockButton.setToolTipText("大屏时钟");
        panel3.add(clockButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        panel2.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        currentTimestampLabel = new JLabel();
        Font currentTimestampLabelFont = this.$$$getFont$$$(null, -1, 36, currentTimestampLabel.getFont());
        if (currentTimestampLabelFont != null) currentTimestampLabel.setFont(currentTimestampLabelFont);
        currentTimestampLabel.setForeground(new Color(-14739));
        currentTimestampLabel.setText("--");
        panel4.add(currentTimestampLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        copyCurrentTimestampButton = new JButton();
        copyCurrentTimestampButton.setText("复制");
        panel4.add(copyCurrentTimestampButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(3, 3, new Insets(10, 10, 10, 10), -1, -1));
        panel1.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        label1.setHorizontalTextPosition(11);
        label1.setText("时间戳(Unix timestamp)");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        timestampTextField = new JTextField();
        Font timestampTextFieldFont = this.$$$getFont$$$(null, -1, 26, timestampTextField.getFont());
        if (timestampTextFieldFont != null) timestampTextField.setFont(timestampTextFieldFont);
        panel5.add(timestampTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyGeneratedTimestampButton = new JButton();
        copyGeneratedTimestampButton.setText("复制");
        panel5.add(copyGeneratedTimestampButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(10);
        label2.setHorizontalTextPosition(11);
        label2.setText("本地时间(GMT +08)");
        panel5.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gmtTextField = new JTextField();
        Font gmtTextFieldFont = this.$$$getFont$$$(null, -1, 26, gmtTextField.getFont());
        if (gmtTextFieldFont != null) gmtTextField.setFont(gmtTextFieldFont);
        panel5.add(gmtTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        copyGeneratedLocalTimeButton = new JButton();
        copyGeneratedLocalTimeButton.setText("复制");
        panel5.add(copyGeneratedLocalTimeButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 4, new Insets(10, 0, 10, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        toLocalTimeButton = new JButton();
        toLocalTimeButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        toLocalTimeButton.setText("转换");
        toLocalTimeButton.setToolTipText("时间戳转换为本地时间");
        panel6.add(toLocalTimeButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        toTimestampButton = new JButton();
        toTimestampButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        toTimestampButton.setText("转换");
        toTimestampButton.setToolTipText("本地时间转换为时间戳");
        panel6.add(toTimestampButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unitComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("秒(s)");
        defaultComboBoxModel1.addElement("毫秒(ms)");
        unitComboBox.setModel(defaultComboBoxModel1);
        panel6.add(unitComboBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel1.add(spacer4, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel1.add(separator2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel1.add(separator3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel1.add(panel7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        timeFormatTextField = new JTextField();
        timeFormatTextField.setEditable(false);
        timeFormatTextField.setText("yyyy-MM-dd HH:mm:ss.SSS");
        panel7.add(timeFormatTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel8);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel8.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        timeHisTextArea = new JTextArea();
        scrollPane1.setViewportView(timeHisTextArea);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return timeConvertPanel;
    }

}
