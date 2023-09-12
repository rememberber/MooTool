package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.CustomizeIcon;
import com.luoboduner.moo.tool.ui.listener.func.CronListener;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * <pre>
 * CronForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/12/13.
 */
@Getter
public class CronForm {
    private JPanel cronPanel;
    private JTextArea nextExecutionTimeTextArea;
    private JTextField cronExpressionTextField;
    private JTextField humanReadableTextField;
    private JButton cronToHumanReadableButton;
    private JButton button2;
    private JTabbedPane tabbedPane1;
    private JButton resolveToUIButton;
    private JTextField cronSecExpressionTextField;
    private JSplitPane splitPane;
    private JButton commonCronButton;
    private JButton favoriteBookButton;
    private JButton addToFavoriteButton;
    private JComboBox localComboBox;
    private JRadioButton secPerRadioButton;
    private JRadioButton secCycle1RadioButton;
    private JSpinner secCycle1Spinner1;
    private JSpinner secCycle1Spinner2;
    private JRadioButton secAssignRadioButton;
    private JCheckBox secCheckBox0;
    private JCheckBox secCheckBox1;
    private JCheckBox secCheckBox2;
    private JCheckBox secCheckBox3;
    private JCheckBox secCheckBox4;
    private JCheckBox secCheckBox5;
    private JCheckBox secCheckBox6;
    private JCheckBox secCheckBox7;
    private JCheckBox secCheckBox8;
    private JCheckBox secCheckBox9;
    private JCheckBox secCheckBox10;
    private JCheckBox secCheckBox20;
    private JCheckBox secCheckBox30;
    private JCheckBox secCheckBox11;
    private JCheckBox secCheckBox21;
    private JCheckBox secCheckBox31;
    private JCheckBox secCheckBox12;
    private JCheckBox secCheckBox22;
    private JCheckBox secCheckBox32;
    private JCheckBox secCheckBox13;
    private JCheckBox secCheckBox23;
    private JCheckBox secCheckBox33;
    private JCheckBox secCheckBox14;
    private JCheckBox secCheckBox24;
    private JCheckBox secCheckBox34;
    private JCheckBox secCheckBox15;
    private JCheckBox secCheckBox25;
    private JCheckBox secCheckBox35;
    private JCheckBox secCheckBox16;
    private JCheckBox secCheckBox26;
    private JCheckBox secCheckBox36;
    private JCheckBox secCheckBox17;
    private JCheckBox secCheckBox27;
    private JCheckBox secCheckBox37;
    private JCheckBox secCheckBox18;
    private JCheckBox secCheckBox28;
    private JCheckBox secCheckBox38;
    private JCheckBox secCheckBox19;
    private JCheckBox secCheckBox29;
    private JCheckBox secCheckBox39;
    private JRadioButton minuPerRadioButton;
    private JRadioButton minuCycle1RadioButton;
    private JSpinner minuCycle1Spinner1;
    private JSpinner minuCycle1Spinner2;
    private JCheckBox secCheckBox40;
    private JCheckBox secCheckBox41;
    private JCheckBox secCheckBox42;
    private JCheckBox secCheckBox43;
    private JCheckBox secCheckBox44;
    private JCheckBox secCheckBox45;
    private JCheckBox secCheckBox46;
    private JCheckBox secCheckBox47;
    private JCheckBox secCheckBox48;
    private JCheckBox secCheckBox49;
    private JCheckBox secCheckBox50;
    private JCheckBox secCheckBox51;
    private JCheckBox secCheckBox52;
    private JCheckBox secCheckBox53;
    private JCheckBox secCheckBox54;
    private JCheckBox secCheckBox55;
    private JCheckBox secCheckBox56;
    private JCheckBox secCheckBox57;
    private JCheckBox secCheckBox58;
    private JCheckBox secCheckBox59;
    private JRadioButton minuAssignRadioButton;
    private JCheckBox minuCheckBox0;
    private JCheckBox minuCheckBox1;
    private JCheckBox minuCheckBox2;
    private JCheckBox minuCheckBox3;
    private JCheckBox minuCheckBox4;
    private JCheckBox minuCheckBox5;
    private JCheckBox minuCheckBox6;
    private JCheckBox minuCheckBox7;
    private JCheckBox minuCheckBox8;
    private JCheckBox minuCheckBox9;
    private JRadioButton hourPerRadioButton;
    private JRadioButton hourAssignRadioButton;
    private JCheckBox hourCheckBox0;
    private JCheckBox hourCheckBox1;
    private JCheckBox hourCheckBox2;
    private JCheckBox hourCheckBox3;
    private JCheckBox hourCheckBox4;
    private JCheckBox hourCheckBox5;
    private JRadioButton dayPerRadioButton;
    private JRadioButton dayAssignRadioButton;
    private JCheckBox dayCheckBox1;
    private JCheckBox dayCheckBox2;
    private JCheckBox dayCheckBox3;
    private JCheckBox dayCheckBox4;
    private JCheckBox dayCheckBox5;
    private JCheckBox dayCheckBox6;
    private JCheckBox dayCheckBox7;
    private JCheckBox dayCheckBox8;
    private JCheckBox dayCheckBox9;
    private JCheckBox dayCheckBox10;
    private JRadioButton monthPerRadioButton;
    private JRadioButton monthAssignRadioButton;
    private JCheckBox monthCheckBox1;
    private JCheckBox monthCheckBox2;
    private JCheckBox monthCheckBox3;
    private JCheckBox monthCheckBox4;
    private JCheckBox monthCheckBox5;
    private JCheckBox monthCheckBox6;
    private JRadioButton weekNotAssignRadioButton;
    private JRadioButton weekPerRadioButton;
    private JRadioButton weekAssignRadioButton;
    private JCheckBox weekCheckBox1;
    private JCheckBox weekCheckBox2;
    private JCheckBox weekCheckBox3;
    private JCheckBox weekCheckBox4;
    private JCheckBox weekCheckBox5;
    private JCheckBox weekCheckBox6;
    private JCheckBox weekCheckBox7;
    private JRadioButton yearNotAssignRadioButton;
    private JRadioButton yearPerRadioButton;
    private JRadioButton yearCycleRadioButton;
    private JSpinner yearCycleSpinner1;
    private JSpinner yearCycleSpinner2;
    private JRadioButton secCycle2RadioButton;
    private JSpinner secCycle2Spinner1;
    private JSpinner secCycle2Spinner2;
    private JRadioButton minuCycle2RadioButton;
    private JSpinner minuCycle2Spinner1;
    private JSpinner minuCycle2Spinner2;
    private JRadioButton hourCycle1RadioButton;
    private JSpinner hourCycle1Spinner1;
    private JSpinner hourCycle1Spinner2;
    private JRadioButton hourCycle2RadioButton;
    private JSpinner hourCycle2Spinner1;
    private JSpinner hourCycle2Spinner2;
    private JRadioButton dayCycle1RadioButton;
    private JSpinner dayCycle1Spinner1;
    private JSpinner dayCycle1Spinner2;
    private JRadioButton dayCycle2RadioButton;
    private JSpinner dayCycle2Spinner1;
    private JSpinner dayCycle2Spinner2;
    private JRadioButton dayNotAssignRadioButton;
    private JRadioButton dayPerMonthRadioButton;
    private JSpinner dayPerMonthSpinner;
    private JRadioButton dayMonthLastRadioButton;
    private JRadioButton monthNotAssignRadioButton;
    private JRadioButton monthCycle1RadioButton;
    private JRadioButton monthCycle2RadioButton;
    private JRadioButton weekCycle1RadioButton;
    private JComboBox weekCycle1ComboBox1;
    private JComboBox weekCycle1ComboBox2;
    private JRadioButton weekInMonthRadioButton;
    private JComboBox weekInMonthComboBox1;
    private JComboBox weekInMonthComboBox2;
    private JRadioButton weekLastMonthRadioButton;
    private JComboBox weekLastMonthComboBox1;
    private JComboBox monthCycle1ComboBox1;
    private JComboBox monthCycle1ComboBox2;
    private JComboBox monthCycle2ComboBox1;
    private JComboBox monthCycle2ComboBox2;
    private JCheckBox minuCheckBox10;
    private JCheckBox minuCheckBox20;
    private JCheckBox minuCheckBox30;
    private JCheckBox minuCheckBox40;
    private JCheckBox minuCheckBox50;
    private JCheckBox minuCheckBox11;
    private JCheckBox minuCheckBox21;
    private JCheckBox minuCheckBox31;
    private JCheckBox minuCheckBox41;
    private JCheckBox minuCheckBox51;
    private JCheckBox minuCheckBox12;
    private JCheckBox minuCheckBox22;
    private JCheckBox minuCheckBox32;
    private JCheckBox minuCheckBox42;
    private JCheckBox minuCheckBox52;
    private JCheckBox minuCheckBox13;
    private JCheckBox minuCheckBox23;
    private JCheckBox minuCheckBox33;
    private JCheckBox minuCheckBox43;
    private JCheckBox minuCheckBox53;
    private JCheckBox minuCheckBox14;
    private JCheckBox minuCheckBox24;
    private JCheckBox minuCheckBox34;
    private JCheckBox minuCheckBox44;
    private JCheckBox minuCheckBox54;
    private JCheckBox minuCheckBox15;
    private JCheckBox minuCheckBox25;
    private JCheckBox minuCheckBox35;
    private JCheckBox minuCheckBox45;
    private JCheckBox minuCheckBox55;
    private JCheckBox minuCheckBox16;
    private JCheckBox minuCheckBox26;
    private JCheckBox minuCheckBox36;
    private JCheckBox minuCheckBox46;
    private JCheckBox minuCheckBox56;
    private JCheckBox minuCheckBox17;
    private JCheckBox minuCheckBox27;
    private JCheckBox minuCheckBox37;
    private JCheckBox minuCheckBox47;
    private JCheckBox minuCheckBox57;
    private JCheckBox minuCheckBox18;
    private JCheckBox minuCheckBox28;
    private JCheckBox minuCheckBox38;
    private JCheckBox minuCheckBox48;
    private JCheckBox minuCheckBox58;
    private JCheckBox minuCheckBox19;
    private JCheckBox minuCheckBox29;
    private JCheckBox minuCheckBox39;
    private JCheckBox minuCheckBox49;
    private JCheckBox minuCheckBox59;
    private JCheckBox hourCheckBox6;
    private JCheckBox hourCheckBox12;
    private JCheckBox hourCheckBox18;
    private JCheckBox hourCheckBox7;
    private JCheckBox hourCheckBox13;
    private JCheckBox hourCheckBox19;
    private JCheckBox hourCheckBox8;
    private JCheckBox hourCheckBox14;
    private JCheckBox hourCheckBox20;
    private JCheckBox hourCheckBox9;
    private JCheckBox hourCheckBox15;
    private JCheckBox hourCheckBox21;
    private JCheckBox hourCheckBox10;
    private JCheckBox hourCheckBox16;
    private JCheckBox hourCheckBox22;
    private JCheckBox hourCheckBox11;
    private JCheckBox hourCheckBox17;
    private JCheckBox hourCheckBox23;
    private JCheckBox dayCheckBox11;
    private JCheckBox dayCheckBox21;
    private JCheckBox dayCheckBox12;
    private JCheckBox dayCheckBox22;
    private JCheckBox dayCheckBox13;
    private JCheckBox dayCheckBox23;
    private JCheckBox dayCheckBox14;
    private JCheckBox dayCheckBox24;
    private JCheckBox dayCheckBox15;
    private JCheckBox dayCheckBox25;
    private JCheckBox dayCheckBox16;
    private JCheckBox dayCheckBox26;
    private JCheckBox dayCheckBox17;
    private JCheckBox dayCheckBox27;
    private JCheckBox dayCheckBox18;
    private JCheckBox dayCheckBox28;
    private JCheckBox dayCheckBox19;
    private JCheckBox dayCheckBox29;
    private JCheckBox dayCheckBox20;
    private JCheckBox dayCheckBox30;
    private JCheckBox dayCheckBox31;
    private JCheckBox monthCheckBox7;
    private JCheckBox monthCheckBox8;
    private JCheckBox monthCheckBox9;
    private JCheckBox monthCheckBox10;
    private JCheckBox monthCheckBox11;
    private JCheckBox monthCheckBox12;
    private JTextField cronMinuExpressionTextField;
    private JTextField cronHourExpressionTextField;
    private JTextField cronDayExpressionTextField;
    private JTextField cronMonthExpressionTextField;
    private JTextField cronWeekExpressionTextField;
    private JTextField cronYearExpressionTextField;

    private static CronForm cronForm;

    private static final Log logger = LogFactory.get();

    public static final Map<Integer, JCheckBox> SEC_CHECK_BOX_MAP = new TreeMap<>();
    public static final Map<Integer, JCheckBox> MINU_CHECK_BOX_MAP = new TreeMap<>();
    public static final Map<Integer, JCheckBox> HOUR_CHECK_BOX_MAP = new TreeMap<>();
    public static final Map<Integer, JCheckBox> DAY_CHECK_BOX_MAP = new TreeMap<>();
    public static final Map<Integer, JCheckBox> MONTH_CHECK_BOX_MAP = new TreeMap<>();
    public static final Map<Integer, JCheckBox> WEEK_CHECK_BOX_MAP = new TreeMap<>();

    private CronForm() {
        UndoUtil.register(this);
    }

    public static CronForm getInstance() {
        if (cronForm == null) {
            cronForm = new CronForm();

            // 秒
            SEC_CHECK_BOX_MAP.put(0, cronForm.getSecCheckBox0());
            SEC_CHECK_BOX_MAP.put(1, cronForm.getSecCheckBox1());
            SEC_CHECK_BOX_MAP.put(2, cronForm.getSecCheckBox2());
            SEC_CHECK_BOX_MAP.put(3, cronForm.getSecCheckBox3());
            SEC_CHECK_BOX_MAP.put(4, cronForm.getSecCheckBox4());
            SEC_CHECK_BOX_MAP.put(5, cronForm.getSecCheckBox5());
            SEC_CHECK_BOX_MAP.put(6, cronForm.getSecCheckBox6());
            SEC_CHECK_BOX_MAP.put(7, cronForm.getSecCheckBox7());
            SEC_CHECK_BOX_MAP.put(8, cronForm.getSecCheckBox8());
            SEC_CHECK_BOX_MAP.put(9, cronForm.getSecCheckBox9());
            SEC_CHECK_BOX_MAP.put(10, cronForm.getSecCheckBox10());
            SEC_CHECK_BOX_MAP.put(11, cronForm.getSecCheckBox11());
            SEC_CHECK_BOX_MAP.put(12, cronForm.getSecCheckBox12());
            SEC_CHECK_BOX_MAP.put(13, cronForm.getSecCheckBox13());
            SEC_CHECK_BOX_MAP.put(14, cronForm.getSecCheckBox14());
            SEC_CHECK_BOX_MAP.put(15, cronForm.getSecCheckBox15());
            SEC_CHECK_BOX_MAP.put(16, cronForm.getSecCheckBox16());
            SEC_CHECK_BOX_MAP.put(17, cronForm.getSecCheckBox17());
            SEC_CHECK_BOX_MAP.put(18, cronForm.getSecCheckBox18());
            SEC_CHECK_BOX_MAP.put(19, cronForm.getSecCheckBox19());
            SEC_CHECK_BOX_MAP.put(20, cronForm.getSecCheckBox20());
            SEC_CHECK_BOX_MAP.put(21, cronForm.getSecCheckBox21());
            SEC_CHECK_BOX_MAP.put(22, cronForm.getSecCheckBox22());
            SEC_CHECK_BOX_MAP.put(23, cronForm.getSecCheckBox23());
            SEC_CHECK_BOX_MAP.put(24, cronForm.getSecCheckBox24());
            SEC_CHECK_BOX_MAP.put(25, cronForm.getSecCheckBox25());
            SEC_CHECK_BOX_MAP.put(26, cronForm.getSecCheckBox26());
            SEC_CHECK_BOX_MAP.put(27, cronForm.getSecCheckBox27());
            SEC_CHECK_BOX_MAP.put(28, cronForm.getSecCheckBox28());
            SEC_CHECK_BOX_MAP.put(29, cronForm.getSecCheckBox29());
            SEC_CHECK_BOX_MAP.put(30, cronForm.getSecCheckBox30());
            SEC_CHECK_BOX_MAP.put(31, cronForm.getSecCheckBox31());
            SEC_CHECK_BOX_MAP.put(32, cronForm.getSecCheckBox32());
            SEC_CHECK_BOX_MAP.put(33, cronForm.getSecCheckBox33());
            SEC_CHECK_BOX_MAP.put(34, cronForm.getSecCheckBox34());
            SEC_CHECK_BOX_MAP.put(35, cronForm.getSecCheckBox35());
            SEC_CHECK_BOX_MAP.put(36, cronForm.getSecCheckBox36());
            SEC_CHECK_BOX_MAP.put(37, cronForm.getSecCheckBox37());
            SEC_CHECK_BOX_MAP.put(38, cronForm.getSecCheckBox38());
            SEC_CHECK_BOX_MAP.put(39, cronForm.getSecCheckBox39());
            SEC_CHECK_BOX_MAP.put(40, cronForm.getSecCheckBox40());
            SEC_CHECK_BOX_MAP.put(41, cronForm.getSecCheckBox41());
            SEC_CHECK_BOX_MAP.put(42, cronForm.getSecCheckBox42());
            SEC_CHECK_BOX_MAP.put(43, cronForm.getSecCheckBox43());
            SEC_CHECK_BOX_MAP.put(44, cronForm.getSecCheckBox44());
            SEC_CHECK_BOX_MAP.put(45, cronForm.getSecCheckBox45());
            SEC_CHECK_BOX_MAP.put(46, cronForm.getSecCheckBox46());
            SEC_CHECK_BOX_MAP.put(47, cronForm.getSecCheckBox47());
            SEC_CHECK_BOX_MAP.put(48, cronForm.getSecCheckBox48());
            SEC_CHECK_BOX_MAP.put(49, cronForm.getSecCheckBox49());
            SEC_CHECK_BOX_MAP.put(50, cronForm.getSecCheckBox50());
            SEC_CHECK_BOX_MAP.put(51, cronForm.getSecCheckBox51());
            SEC_CHECK_BOX_MAP.put(52, cronForm.getSecCheckBox52());
            SEC_CHECK_BOX_MAP.put(53, cronForm.getSecCheckBox53());
            SEC_CHECK_BOX_MAP.put(54, cronForm.getSecCheckBox54());
            SEC_CHECK_BOX_MAP.put(55, cronForm.getSecCheckBox55());
            SEC_CHECK_BOX_MAP.put(56, cronForm.getSecCheckBox56());
            SEC_CHECK_BOX_MAP.put(57, cronForm.getSecCheckBox57());
            SEC_CHECK_BOX_MAP.put(58, cronForm.getSecCheckBox58());
            SEC_CHECK_BOX_MAP.put(59, cronForm.getSecCheckBox59());

            // 分
            MINU_CHECK_BOX_MAP.put(0, cronForm.getMinuCheckBox0());
            MINU_CHECK_BOX_MAP.put(1, cronForm.getMinuCheckBox1());
            MINU_CHECK_BOX_MAP.put(2, cronForm.getMinuCheckBox2());
            MINU_CHECK_BOX_MAP.put(3, cronForm.getMinuCheckBox3());
            MINU_CHECK_BOX_MAP.put(4, cronForm.getMinuCheckBox4());
            MINU_CHECK_BOX_MAP.put(5, cronForm.getMinuCheckBox5());
            MINU_CHECK_BOX_MAP.put(6, cronForm.getMinuCheckBox6());
            MINU_CHECK_BOX_MAP.put(7, cronForm.getMinuCheckBox7());
            MINU_CHECK_BOX_MAP.put(8, cronForm.getMinuCheckBox8());
            MINU_CHECK_BOX_MAP.put(9, cronForm.getMinuCheckBox9());
            MINU_CHECK_BOX_MAP.put(10, cronForm.getMinuCheckBox10());
            MINU_CHECK_BOX_MAP.put(11, cronForm.getMinuCheckBox11());
            MINU_CHECK_BOX_MAP.put(12, cronForm.getMinuCheckBox12());
            MINU_CHECK_BOX_MAP.put(13, cronForm.getMinuCheckBox13());
            MINU_CHECK_BOX_MAP.put(14, cronForm.getMinuCheckBox14());
            MINU_CHECK_BOX_MAP.put(15, cronForm.getMinuCheckBox15());
            MINU_CHECK_BOX_MAP.put(16, cronForm.getMinuCheckBox16());
            MINU_CHECK_BOX_MAP.put(17, cronForm.getMinuCheckBox17());
            MINU_CHECK_BOX_MAP.put(18, cronForm.getMinuCheckBox18());
            MINU_CHECK_BOX_MAP.put(19, cronForm.getMinuCheckBox19());
            MINU_CHECK_BOX_MAP.put(20, cronForm.getMinuCheckBox20());
            MINU_CHECK_BOX_MAP.put(21, cronForm.getMinuCheckBox21());
            MINU_CHECK_BOX_MAP.put(22, cronForm.getMinuCheckBox22());
            MINU_CHECK_BOX_MAP.put(23, cronForm.getMinuCheckBox23());
            MINU_CHECK_BOX_MAP.put(24, cronForm.getMinuCheckBox24());
            MINU_CHECK_BOX_MAP.put(25, cronForm.getMinuCheckBox25());
            MINU_CHECK_BOX_MAP.put(26, cronForm.getMinuCheckBox26());
            MINU_CHECK_BOX_MAP.put(27, cronForm.getMinuCheckBox27());
            MINU_CHECK_BOX_MAP.put(28, cronForm.getMinuCheckBox28());
            MINU_CHECK_BOX_MAP.put(29, cronForm.getMinuCheckBox29());
            MINU_CHECK_BOX_MAP.put(30, cronForm.getMinuCheckBox30());
            MINU_CHECK_BOX_MAP.put(31, cronForm.getMinuCheckBox31());
            MINU_CHECK_BOX_MAP.put(32, cronForm.getMinuCheckBox32());
            MINU_CHECK_BOX_MAP.put(33, cronForm.getMinuCheckBox33());
            MINU_CHECK_BOX_MAP.put(34, cronForm.getMinuCheckBox34());
            MINU_CHECK_BOX_MAP.put(35, cronForm.getMinuCheckBox35());
            MINU_CHECK_BOX_MAP.put(36, cronForm.getMinuCheckBox36());
            MINU_CHECK_BOX_MAP.put(37, cronForm.getMinuCheckBox37());
            MINU_CHECK_BOX_MAP.put(38, cronForm.getMinuCheckBox38());
            MINU_CHECK_BOX_MAP.put(39, cronForm.getMinuCheckBox39());
            MINU_CHECK_BOX_MAP.put(40, cronForm.getMinuCheckBox40());
            MINU_CHECK_BOX_MAP.put(41, cronForm.getMinuCheckBox41());
            MINU_CHECK_BOX_MAP.put(42, cronForm.getMinuCheckBox42());
            MINU_CHECK_BOX_MAP.put(43, cronForm.getMinuCheckBox43());
            MINU_CHECK_BOX_MAP.put(44, cronForm.getMinuCheckBox44());
            MINU_CHECK_BOX_MAP.put(45, cronForm.getMinuCheckBox45());
            MINU_CHECK_BOX_MAP.put(46, cronForm.getMinuCheckBox46());
            MINU_CHECK_BOX_MAP.put(47, cronForm.getMinuCheckBox47());
            MINU_CHECK_BOX_MAP.put(48, cronForm.getMinuCheckBox48());
            MINU_CHECK_BOX_MAP.put(49, cronForm.getMinuCheckBox49());
            MINU_CHECK_BOX_MAP.put(50, cronForm.getMinuCheckBox50());
            MINU_CHECK_BOX_MAP.put(51, cronForm.getMinuCheckBox51());
            MINU_CHECK_BOX_MAP.put(52, cronForm.getMinuCheckBox52());
            MINU_CHECK_BOX_MAP.put(53, cronForm.getMinuCheckBox53());
            MINU_CHECK_BOX_MAP.put(54, cronForm.getMinuCheckBox54());
            MINU_CHECK_BOX_MAP.put(55, cronForm.getMinuCheckBox55());
            MINU_CHECK_BOX_MAP.put(56, cronForm.getMinuCheckBox56());
            MINU_CHECK_BOX_MAP.put(57, cronForm.getMinuCheckBox57());
            MINU_CHECK_BOX_MAP.put(58, cronForm.getMinuCheckBox58());
            MINU_CHECK_BOX_MAP.put(59, cronForm.getMinuCheckBox59());

            // 时
            HOUR_CHECK_BOX_MAP.put(0, cronForm.getHourCheckBox0());
            HOUR_CHECK_BOX_MAP.put(1, cronForm.getHourCheckBox1());
            HOUR_CHECK_BOX_MAP.put(2, cronForm.getHourCheckBox2());
            HOUR_CHECK_BOX_MAP.put(3, cronForm.getHourCheckBox3());
            HOUR_CHECK_BOX_MAP.put(4, cronForm.getHourCheckBox4());
            HOUR_CHECK_BOX_MAP.put(5, cronForm.getHourCheckBox5());
            HOUR_CHECK_BOX_MAP.put(6, cronForm.getHourCheckBox6());
            HOUR_CHECK_BOX_MAP.put(7, cronForm.getHourCheckBox7());
            HOUR_CHECK_BOX_MAP.put(8, cronForm.getHourCheckBox8());
            HOUR_CHECK_BOX_MAP.put(9, cronForm.getHourCheckBox9());
            HOUR_CHECK_BOX_MAP.put(10, cronForm.getHourCheckBox10());
            HOUR_CHECK_BOX_MAP.put(11, cronForm.getHourCheckBox11());
            HOUR_CHECK_BOX_MAP.put(12, cronForm.getHourCheckBox12());
            HOUR_CHECK_BOX_MAP.put(13, cronForm.getHourCheckBox13());
            HOUR_CHECK_BOX_MAP.put(14, cronForm.getHourCheckBox14());
            HOUR_CHECK_BOX_MAP.put(15, cronForm.getHourCheckBox15());
            HOUR_CHECK_BOX_MAP.put(16, cronForm.getHourCheckBox16());
            HOUR_CHECK_BOX_MAP.put(17, cronForm.getHourCheckBox17());
            HOUR_CHECK_BOX_MAP.put(18, cronForm.getHourCheckBox18());
            HOUR_CHECK_BOX_MAP.put(19, cronForm.getHourCheckBox19());
            HOUR_CHECK_BOX_MAP.put(20, cronForm.getHourCheckBox20());
            HOUR_CHECK_BOX_MAP.put(21, cronForm.getHourCheckBox21());
            HOUR_CHECK_BOX_MAP.put(22, cronForm.getHourCheckBox22());
            HOUR_CHECK_BOX_MAP.put(23, cronForm.getHourCheckBox23());

            // 日
            DAY_CHECK_BOX_MAP.put(1, cronForm.getDayCheckBox1());
            DAY_CHECK_BOX_MAP.put(2, cronForm.getDayCheckBox2());
            DAY_CHECK_BOX_MAP.put(3, cronForm.getDayCheckBox3());
            DAY_CHECK_BOX_MAP.put(4, cronForm.getDayCheckBox4());
            DAY_CHECK_BOX_MAP.put(5, cronForm.getDayCheckBox5());
            DAY_CHECK_BOX_MAP.put(6, cronForm.getDayCheckBox6());
            DAY_CHECK_BOX_MAP.put(7, cronForm.getDayCheckBox7());
            DAY_CHECK_BOX_MAP.put(8, cronForm.getDayCheckBox8());
            DAY_CHECK_BOX_MAP.put(9, cronForm.getDayCheckBox9());
            DAY_CHECK_BOX_MAP.put(10, cronForm.getDayCheckBox10());
            DAY_CHECK_BOX_MAP.put(11, cronForm.getDayCheckBox11());
            DAY_CHECK_BOX_MAP.put(12, cronForm.getDayCheckBox12());
            DAY_CHECK_BOX_MAP.put(13, cronForm.getDayCheckBox13());
            DAY_CHECK_BOX_MAP.put(14, cronForm.getDayCheckBox14());
            DAY_CHECK_BOX_MAP.put(15, cronForm.getDayCheckBox15());
            DAY_CHECK_BOX_MAP.put(16, cronForm.getDayCheckBox16());
            DAY_CHECK_BOX_MAP.put(17, cronForm.getDayCheckBox17());
            DAY_CHECK_BOX_MAP.put(18, cronForm.getDayCheckBox18());
            DAY_CHECK_BOX_MAP.put(19, cronForm.getDayCheckBox19());
            DAY_CHECK_BOX_MAP.put(20, cronForm.getDayCheckBox20());
            DAY_CHECK_BOX_MAP.put(21, cronForm.getDayCheckBox21());
            DAY_CHECK_BOX_MAP.put(22, cronForm.getDayCheckBox22());
            DAY_CHECK_BOX_MAP.put(23, cronForm.getDayCheckBox23());
            DAY_CHECK_BOX_MAP.put(24, cronForm.getDayCheckBox24());
            DAY_CHECK_BOX_MAP.put(25, cronForm.getDayCheckBox25());
            DAY_CHECK_BOX_MAP.put(26, cronForm.getDayCheckBox26());
            DAY_CHECK_BOX_MAP.put(27, cronForm.getDayCheckBox27());
            DAY_CHECK_BOX_MAP.put(28, cronForm.getDayCheckBox28());
            DAY_CHECK_BOX_MAP.put(29, cronForm.getDayCheckBox29());
            DAY_CHECK_BOX_MAP.put(30, cronForm.getDayCheckBox30());
            DAY_CHECK_BOX_MAP.put(31, cronForm.getDayCheckBox31());

            // 月
            MONTH_CHECK_BOX_MAP.put(1, cronForm.getMonthCheckBox1());
            MONTH_CHECK_BOX_MAP.put(2, cronForm.getMonthCheckBox2());
            MONTH_CHECK_BOX_MAP.put(3, cronForm.getMonthCheckBox3());
            MONTH_CHECK_BOX_MAP.put(4, cronForm.getMonthCheckBox4());
            MONTH_CHECK_BOX_MAP.put(5, cronForm.getMonthCheckBox5());
            MONTH_CHECK_BOX_MAP.put(6, cronForm.getMonthCheckBox6());
            MONTH_CHECK_BOX_MAP.put(7, cronForm.getMonthCheckBox7());
            MONTH_CHECK_BOX_MAP.put(8, cronForm.getMonthCheckBox8());
            MONTH_CHECK_BOX_MAP.put(9, cronForm.getMonthCheckBox9());
            MONTH_CHECK_BOX_MAP.put(10, cronForm.getMonthCheckBox10());
            MONTH_CHECK_BOX_MAP.put(11, cronForm.getMonthCheckBox11());
            MONTH_CHECK_BOX_MAP.put(12, cronForm.getMonthCheckBox12());

            // 周
            WEEK_CHECK_BOX_MAP.put(1, cronForm.getWeekCheckBox1());
            WEEK_CHECK_BOX_MAP.put(2, cronForm.getWeekCheckBox2());
            WEEK_CHECK_BOX_MAP.put(3, cronForm.getWeekCheckBox3());
            WEEK_CHECK_BOX_MAP.put(4, cronForm.getWeekCheckBox4());
            WEEK_CHECK_BOX_MAP.put(5, cronForm.getWeekCheckBox5());
            WEEK_CHECK_BOX_MAP.put(6, cronForm.getWeekCheckBox6());
            WEEK_CHECK_BOX_MAP.put(7, cronForm.getWeekCheckBox7());
        }
        return cronForm;
    }

    public static void init() {
        cronForm = getInstance();

        initUi();

        CronListener.addListeners();
    }

    private static void initUi() {
        cronForm.getAddToFavoriteButton().setIcon(CustomizeIcon.favoriteBtnIcon);
        cronForm.getFavoriteBookButton().setIcon(CustomizeIcon.favoriteBookBtnIcon);
        cronForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 2));
        cronForm.getNextExecutionTimeTextArea().setText("最近10次运行时间：");

        Style.blackTextArea(cronForm.getNextExecutionTimeTextArea());

        resetCronPanel();

        cronForm.getCronPanel().updateUI();
    }

    private static void resetCronPanel() {
        // 秒
        cronForm.getSecPerRadioButton().setSelected(true);
        cronForm.getSecCycle1RadioButton().setSelected(false);
        cronForm.getSecCycle1Spinner1().setValue(0);
        cronForm.getSecCycle1Spinner2().setValue(1);
        cronForm.getSecCycle2RadioButton().setSelected(false);
        cronForm.getSecCycle2Spinner1().setValue(0);
        cronForm.getSecCycle2Spinner2().setValue(1);
        cronForm.getSecAssignRadioButton().setSelected(false);
        SEC_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 分
        cronForm.getMinuPerRadioButton().setSelected(true);
        cronForm.getMinuCycle1RadioButton().setSelected(false);
        cronForm.getMinuCycle1Spinner1().setValue(0);
        cronForm.getMinuCycle1Spinner2().setValue(1);
        cronForm.getMinuCycle2RadioButton().setSelected(false);
        cronForm.getMinuCycle2Spinner1().setValue(0);
        cronForm.getMinuCycle2Spinner2().setValue(1);
        cronForm.getMinuAssignRadioButton().setSelected(false);
        MINU_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 时
        cronForm.getHourPerRadioButton().setSelected(true);
        cronForm.getHourCycle1RadioButton().setSelected(false);
        cronForm.getHourCycle1Spinner1().setValue(0);
        cronForm.getHourCycle1Spinner2().setValue(1);
        cronForm.getHourCycle2RadioButton().setSelected(false);
        cronForm.getHourCycle2Spinner1().setValue(0);
        cronForm.getHourCycle2Spinner2().setValue(1);
        cronForm.getHourAssignRadioButton().setSelected(false);
        HOUR_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 日
        cronForm.getDayPerRadioButton().setSelected(true);
        cronForm.getDayNotAssignRadioButton().setSelected(false);
        cronForm.getDayCycle1RadioButton().setSelected(false);
        cronForm.getDayCycle1Spinner1().setValue(1);
        cronForm.getDayCycle1Spinner2().setValue(1);
        cronForm.getDayCycle2RadioButton().setSelected(false);
        cronForm.getDayCycle2Spinner1().setValue(1);
        cronForm.getDayCycle2Spinner2().setValue(1);
        cronForm.getDayPerMonthRadioButton().setSelected(false);
        cronForm.getDayPerMonthSpinner().setValue(1);
        cronForm.getDayMonthLastRadioButton().setSelected(false);
        cronForm.getDayAssignRadioButton().setSelected(false);
        DAY_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 月
        cronForm.getMonthPerRadioButton().setSelected(true);
        cronForm.getMonthNotAssignRadioButton().setSelected(false);
        cronForm.getMonthCycle1RadioButton().setSelected(false);
        cronForm.getMonthCycle1ComboBox1().setSelectedIndex(0);
        cronForm.getMonthCycle1ComboBox2().setSelectedIndex(0);
        cronForm.getMonthCycle2RadioButton().setSelected(false);
        cronForm.getMonthCycle2ComboBox1().setSelectedIndex(0);
        cronForm.getMonthCycle2ComboBox2().setSelectedIndex(0);
        cronForm.getMonthAssignRadioButton().setSelected(false);
        MONTH_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 周
        cronForm.getWeekNotAssignRadioButton().setSelected(true);
        cronForm.getWeekPerRadioButton().setSelected(false);
        cronForm.getWeekCycle1RadioButton().setSelected(false);
        cronForm.getWeekCycle1ComboBox1().setSelectedIndex(0);
        cronForm.getWeekCycle1ComboBox2().setSelectedIndex(0);
        cronForm.getWeekInMonthRadioButton().setSelected(false);
        cronForm.getWeekInMonthComboBox1().setSelectedIndex(0);
        cronForm.getWeekInMonthComboBox2().setSelectedIndex(0);
        cronForm.getWeekLastMonthRadioButton().setSelected(false);
        cronForm.getWeekLastMonthComboBox1().setSelectedIndex(0);
        cronForm.getWeekAssignRadioButton().setSelected(false);
        WEEK_CHECK_BOX_MAP.forEach((k, v) -> v.setSelected(false));

        // 年
        cronForm.getYearNotAssignRadioButton().setSelected(true);
        cronForm.getYearPerRadioButton().setSelected(false);
        cronForm.getYearCycleRadioButton().setSelected(false);
        // 系统当前时间对应的年份
        cronForm.getYearCycleSpinner1().setValue(DateUtil.thisYear());
        cronForm.getYearCycleSpinner2().setValue(DateUtil.thisYear() + 1);

    }

    public static void clearSecRadioButtons() {
        cronForm.getSecPerRadioButton().setSelected(false);
        cronForm.getSecCycle1RadioButton().setSelected(false);
        cronForm.getSecCycle2RadioButton().setSelected(false);
        cronForm.getSecAssignRadioButton().setSelected(false);
    }

    public static void clearMinuRadioButtons() {
        cronForm.getMinuPerRadioButton().setSelected(false);
        cronForm.getMinuCycle1RadioButton().setSelected(false);
        cronForm.getMinuCycle2RadioButton().setSelected(false);
        cronForm.getMinuAssignRadioButton().setSelected(false);
    }

    public static void clearHourRadioButtons() {
        cronForm.getHourPerRadioButton().setSelected(false);
        cronForm.getHourCycle1RadioButton().setSelected(false);
        cronForm.getHourCycle2RadioButton().setSelected(false);
        cronForm.getHourAssignRadioButton().setSelected(false);
    }

    public static void clearDayRadioButtons() {
        cronForm.getDayPerRadioButton().setSelected(false);
        cronForm.getDayNotAssignRadioButton().setSelected(false);
        cronForm.getDayCycle1RadioButton().setSelected(false);
        cronForm.getDayCycle2RadioButton().setSelected(false);
        cronForm.getDayPerMonthRadioButton().setSelected(false);
        cronForm.getDayMonthLastRadioButton().setSelected(false);
        cronForm.getDayAssignRadioButton().setSelected(false);
    }

    public static void clearMonthRadioButtons() {
        cronForm.getMonthPerRadioButton().setSelected(false);
        cronForm.getMonthNotAssignRadioButton().setSelected(false);
        cronForm.getMonthCycle1RadioButton().setSelected(false);
        cronForm.getMonthCycle2RadioButton().setSelected(false);
        cronForm.getMonthAssignRadioButton().setSelected(false);
    }

    public static void clearWeekRadioButtons() {
        cronForm.getWeekNotAssignRadioButton().setSelected(false);
        cronForm.getWeekPerRadioButton().setSelected(false);
        cronForm.getWeekCycle1RadioButton().setSelected(false);
        cronForm.getWeekInMonthRadioButton().setSelected(false);
        cronForm.getWeekLastMonthRadioButton().setSelected(false);
        cronForm.getWeekAssignRadioButton().setSelected(false);
    }

    public static void clearYearRadioButtons() {
        cronForm.getYearNotAssignRadioButton().setSelected(false);
        cronForm.getYearPerRadioButton().setSelected(false);
        cronForm.getYearCycleRadioButton().setSelected(false);
    }

    public static String generateCronExpression() {
        String cronExpression = "";

        // 秒
        String cronSecExpression = "";
        if (cronForm.getSecPerRadioButton().isSelected()) {
            cronSecExpression = "*";
        } else if (cronForm.getSecCycle1RadioButton().isSelected()) {
            cronSecExpression = cronForm.getSecCycle1Spinner1().getValue() + "/" + cronForm.getSecCycle1Spinner2().getValue();
        } else if (cronForm.getSecCycle2RadioButton().isSelected()) {
            cronSecExpression = cronForm.getSecCycle2Spinner1().getValue() + "-" + cronForm.getSecCycle2Spinner2().getValue();
        } else if (cronForm.getSecAssignRadioButton().isSelected()) {
            String tempSecExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : SEC_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempSecExpression += entry.getKey() + ",";
                }
            }
            if (tempSecExpression.endsWith(",")) {
                tempSecExpression = tempSecExpression.substring(0, tempSecExpression.length() - 1);
            }
            cronSecExpression = tempSecExpression;
        }
        cronForm.getCronSecExpressionTextField().setText(cronSecExpression);

        // 分
        String cronMinuExpression = "";
        if (cronForm.getMinuPerRadioButton().isSelected()) {
            cronMinuExpression = "*";
        } else if (cronForm.getMinuCycle1RadioButton().isSelected()) {
            cronMinuExpression = cronForm.getMinuCycle1Spinner1().getValue() + "/" + cronForm.getMinuCycle1Spinner2().getValue();
        } else if (cronForm.getMinuCycle2RadioButton().isSelected()) {
            cronMinuExpression = cronForm.getMinuCycle2Spinner1().getValue() + "-" + cronForm.getMinuCycle2Spinner2().getValue();
        } else if (cronForm.getMinuAssignRadioButton().isSelected()) {
            String tempMinuExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : MINU_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempMinuExpression += entry.getKey() + ",";
                }
            }
            if (tempMinuExpression.endsWith(",")) {
                tempMinuExpression = tempMinuExpression.substring(0, tempMinuExpression.length() - 1);
            }
            cronMinuExpression = tempMinuExpression;
        }
        cronForm.getCronMinuExpressionTextField().setText(cronMinuExpression);

        // 时
        String cronHourExpression = "";
        if (cronForm.getHourPerRadioButton().isSelected()) {
            cronHourExpression = "*";
        } else if (cronForm.getHourCycle1RadioButton().isSelected()) {
            cronHourExpression = cronForm.getHourCycle1Spinner1().getValue() + "/" + cronForm.getHourCycle1Spinner2().getValue();
        } else if (cronForm.getHourCycle2RadioButton().isSelected()) {
            cronHourExpression = cronForm.getHourCycle2Spinner1().getValue() + "-" + cronForm.getHourCycle2Spinner2().getValue();
        } else if (cronForm.getHourAssignRadioButton().isSelected()) {
            String tempHourExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : HOUR_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempHourExpression += entry.getKey() + ",";
                }
            }
            if (tempHourExpression.endsWith(",")) {
                tempHourExpression = tempHourExpression.substring(0, tempHourExpression.length() - 1);
            }
            cronHourExpression = tempHourExpression;
        }
        cronForm.getCronHourExpressionTextField().setText(cronHourExpression);

        // 日
        String cronDayExpression = "";
        if (cronForm.getDayPerRadioButton().isSelected()) {
            cronDayExpression = "*";
        } else if (cronForm.getDayNotAssignRadioButton().isSelected()) {
            cronDayExpression = "?";
        } else if (cronForm.getDayCycle1RadioButton().isSelected()) {
            cronDayExpression = cronForm.getDayCycle1Spinner1().getValue() + "/" + cronForm.getDayCycle1Spinner2().getValue();
        } else if (cronForm.getDayCycle2RadioButton().isSelected()) {
            cronDayExpression = cronForm.getDayCycle2Spinner1().getValue() + "-" + cronForm.getDayCycle2Spinner2().getValue();
        } else if (cronForm.getDayPerMonthRadioButton().isSelected()) {
            cronDayExpression = cronForm.getDayPerMonthSpinner().getValue() + "W";
        } else if (cronForm.getDayMonthLastRadioButton().isSelected()) {
            cronDayExpression = "L";
        } else if (cronForm.getDayAssignRadioButton().isSelected()) {
            String tempDayExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : DAY_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempDayExpression += entry.getKey() + ",";
                }
            }
            if (tempDayExpression.endsWith(",")) {
                tempDayExpression = tempDayExpression.substring(0, tempDayExpression.length() - 1);
            }
            cronDayExpression = tempDayExpression;
        }
        cronForm.getCronDayExpressionTextField().setText(cronDayExpression);

        // 月
        String cronMonthExpression = "";
        if (cronForm.getMonthPerRadioButton().isSelected()) {
            cronMonthExpression = "*";
        } else if (cronForm.getMonthNotAssignRadioButton().isSelected()) {
            cronMonthExpression = "?";
        } else if (cronForm.getMonthCycle1RadioButton().isSelected()) {
            cronMonthExpression = cronForm.getMonthCycle1ComboBox1().getSelectedItem() + "/" + cronForm.getMonthCycle1ComboBox2().getSelectedItem();
        } else if (cronForm.getMonthCycle2RadioButton().isSelected()) {
            cronMonthExpression = cronForm.getMonthCycle2ComboBox1().getSelectedItem() + "-" + cronForm.getMonthCycle2ComboBox2().getSelectedItem();
        } else if (cronForm.getMonthAssignRadioButton().isSelected()) {
            String tempMonthExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : MONTH_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempMonthExpression += entry.getKey() + ",";
                }
            }
            if (tempMonthExpression.endsWith(",")) {
                tempMonthExpression = tempMonthExpression.substring(0, tempMonthExpression.length() - 1);
            }
            cronMonthExpression = tempMonthExpression;
        }
        cronForm.getCronMonthExpressionTextField().setText(cronMonthExpression);

        // 周
        String cronWeekExpression = "";
        if (cronForm.getWeekNotAssignRadioButton().isSelected()) {
            cronWeekExpression = "?";
        } else if (cronForm.getWeekPerRadioButton().isSelected()) {
            cronWeekExpression = "*";
        } else if (cronForm.getWeekCycle1RadioButton().isSelected()) {
            cronWeekExpression = (cronForm.getWeekCycle1ComboBox1().getSelectedIndex() + 1) + "-" + (cronForm.getWeekCycle1ComboBox2().getSelectedIndex() + 1);
        } else if (cronForm.getWeekInMonthRadioButton().isSelected()) {
            cronWeekExpression = (cronForm.getWeekInMonthComboBox2().getSelectedIndex() + 1) + "#" + (cronForm.getWeekInMonthComboBox1().getSelectedIndex() + 1);
        } else if (cronForm.getWeekLastMonthRadioButton().isSelected()) {
            cronWeekExpression = (cronForm.getWeekLastMonthComboBox1().getSelectedIndex() + 1) + "L";
        } else if (cronForm.getWeekAssignRadioButton().isSelected()) {
            String tempWeekExpression = "";
            for (Map.Entry<Integer, JCheckBox> entry : WEEK_CHECK_BOX_MAP.entrySet()) {
                if (entry.getValue().isSelected()) {
                    tempWeekExpression += entry.getKey() + ",";
                }
            }
            if (tempWeekExpression.endsWith(",")) {
                tempWeekExpression = tempWeekExpression.substring(0, tempWeekExpression.length() - 1);
            }
            cronWeekExpression = tempWeekExpression;
        }
        cronForm.getCronWeekExpressionTextField().setText(cronWeekExpression);

        // 年
        String cronYearExpression = "";
        if (cronForm.getYearNotAssignRadioButton().isSelected()) {
            cronYearExpression = "";
        } else if (cronForm.getYearPerRadioButton().isSelected()) {
            cronYearExpression = "*";
        } else if (cronForm.getYearCycleRadioButton().isSelected()) {
            cronYearExpression = cronForm.getYearCycleSpinner1().getValue() + "-" + cronForm.getYearCycleSpinner2().getValue();
        }
        cronForm.getCronYearExpressionTextField().setText(cronYearExpression);

        cronExpression = cronSecExpression + " " + cronMinuExpression + " " + cronHourExpression + " " + cronDayExpression + " " + cronMonthExpression + " " + cronWeekExpression + " " + cronYearExpression;
        cronForm.getCronExpressionTextField().setText(cronExpression);

        return cronExpression;
    }

    /**
     * 将cron表达式解析到UI
     */
    public static void resolveToUI() {
        String cronExpression = cronForm.getCronExpressionTextField().getText();

        if (StringUtils.isBlank(cronExpression)) {
            return;
        }

        // 替换MON，WED，THU，FRI，SAT，SUN等
        cronExpression = cronExpression.replace("SUN", "1");
        cronExpression = cronExpression.replace("MON", "2");
        cronExpression = cronExpression.replace("TUE", "3");
        cronExpression = cronExpression.replace("WED", "4");
        cronExpression = cronExpression.replace("THU", "5");
        cronExpression = cronExpression.replace("FRI", "6");
        cronExpression = cronExpression.replace("SAT", "7");

        String[] cronExpressionArray = cronExpression.split(" ");
        if (cronExpressionArray.length < 6) {
            return;
        }

        // 秒
        clearSecRadioButtons();
        String cronSecExpression = cronExpressionArray[0];
        if ("*".equals(cronSecExpression)) {
            cronForm.getSecPerRadioButton().setSelected(true);
        } else if (cronSecExpression.contains("/")) {
            cronForm.getSecCycle1RadioButton().setSelected(true);
            String[] cronSecExpressionArray = cronSecExpression.split("/");
            cronForm.getSecCycle1Spinner1().setValue(Integer.parseInt(cronSecExpressionArray[0]));
            cronForm.getSecCycle1Spinner2().setValue(Integer.parseInt(cronSecExpressionArray[1]));
        } else if (cronSecExpression.contains("-")) {
            cronForm.getSecCycle2RadioButton().setSelected(true);
            String[] cronSecExpressionArray = cronSecExpression.split("-");
            cronForm.getSecCycle2Spinner1().setValue(Integer.parseInt(cronSecExpressionArray[0]));
            cronForm.getSecCycle2Spinner2().setValue(Integer.parseInt(cronSecExpressionArray[1]));
        } else {
            cronForm.getSecAssignRadioButton().setSelected(true);
            String[] cronSecExpressionArray = cronSecExpression.split(",");
            for (String sec : cronSecExpressionArray) {
                SEC_CHECK_BOX_MAP.get(Integer.parseInt(sec)).setSelected(true);
            }
        }

        // 分
        clearMinuRadioButtons();
        String cronMinuExpression = cronExpressionArray[1];
        if ("*".equals(cronMinuExpression)) {
            cronForm.getMinuPerRadioButton().setSelected(true);
        } else if (cronMinuExpression.contains("/")) {
            cronForm.getMinuCycle1RadioButton().setSelected(true);
            String[] cronMinuExpressionArray = cronMinuExpression.split("/");
            cronForm.getMinuCycle1Spinner1().setValue(Integer.parseInt(cronMinuExpressionArray[0]));
            cronForm.getMinuCycle1Spinner2().setValue(Integer.parseInt(cronMinuExpressionArray[1]));
        } else if (cronMinuExpression.contains("-")) {
            cronForm.getMinuCycle2RadioButton().setSelected(true);
            String[] cronMinuExpressionArray = cronMinuExpression.split("-");
            cronForm.getMinuCycle2Spinner1().setValue(Integer.parseInt(cronMinuExpressionArray[0]));
            cronForm.getMinuCycle2Spinner2().setValue(Integer.parseInt(cronMinuExpressionArray[1]));
        } else {
            cronForm.getMinuAssignRadioButton().setSelected(true);
            String[] cronMinuExpressionArray = cronMinuExpression.split(",");
            for (String minu : cronMinuExpressionArray) {
                MINU_CHECK_BOX_MAP.get(Integer.parseInt(minu)).setSelected(true);
            }
        }

        // 时
        clearHourRadioButtons();
        String cronHourExpression = cronExpressionArray[2];
        if ("*".equals(cronHourExpression)) {
            cronForm.getHourPerRadioButton().setSelected(true);
        } else if (cronHourExpression.contains("/")) {
            cronForm.getHourCycle1RadioButton().setSelected(true);
            String[] cronHourExpressionArray = cronHourExpression.split("/");
            cronForm.getHourCycle1Spinner1().setValue(Integer.parseInt(cronHourExpressionArray[0]));
            cronForm.getHourCycle1Spinner2().setValue(Integer.parseInt(cronHourExpressionArray[1]));
        } else if (cronHourExpression.contains("-")) {
            cronForm.getHourCycle2RadioButton().setSelected(true);
            String[] cronHourExpressionArray = cronHourExpression.split("-");
            cronForm.getHourCycle2Spinner1().setValue(Integer.parseInt(cronHourExpressionArray[0]));
            cronForm.getHourCycle2Spinner2().setValue(Integer.parseInt(cronHourExpressionArray[1]));
        } else {
            cronForm.getHourAssignRadioButton().setSelected(true);
            String[] cronHourExpressionArray = cronHourExpression.split(",");
            for (String hour : cronHourExpressionArray) {
                HOUR_CHECK_BOX_MAP.get(Integer.parseInt(hour)).setSelected(true);
            }
        }

        // 日
        clearDayRadioButtons();
        String cronDayExpression = cronExpressionArray[3];
        if ("*".equals(cronDayExpression)) {
            cronForm.getDayPerRadioButton().setSelected(true);
        } else if ("?".equals(cronDayExpression)) {
            cronForm.getDayNotAssignRadioButton().setSelected(true);
        } else if (cronDayExpression.contains("/")) {
            cronForm.getDayCycle1RadioButton().setSelected(true);
            String[] cronDayExpressionArray = cronDayExpression.split("/");
            cronForm.getDayCycle1Spinner1().setValue(Integer.parseInt(cronDayExpressionArray[0]));
            cronForm.getDayCycle1Spinner2().setValue(Integer.parseInt(cronDayExpressionArray[1]));
        } else if (cronDayExpression.contains("-")) {
            cronForm.getDayCycle2RadioButton().setSelected(true);
            String[] cronDayExpressionArray = cronDayExpression.split("-");
            cronForm.getDayCycle2Spinner1().setValue(Integer.parseInt(cronDayExpressionArray[0]));
            cronForm.getDayCycle2Spinner2().setValue(Integer.parseInt(cronDayExpressionArray[1]));
        } else if (cronDayExpression.contains("W")) {
            cronForm.getDayPerMonthRadioButton().setSelected(true);
            String[] cronDayExpressionArray = cronDayExpression.split("W");
            cronForm.getDayPerMonthSpinner().setValue(Integer.parseInt(cronDayExpressionArray[0]));
        } else if ("L".equals(cronDayExpression)) {
            cronForm.getDayMonthLastRadioButton().setSelected(true);
        } else {
            cronForm.getDayAssignRadioButton().setSelected(true);
            String[] cronDayExpressionArray = cronDayExpression.split(",");
            for (String day : cronDayExpressionArray) {
                DAY_CHECK_BOX_MAP.get(Integer.parseInt(day)).setSelected(true);
            }
        }

        // 月
        clearMonthRadioButtons();
        String cronMonthExpression = cronExpressionArray[4];
        if ("*".equals(cronMonthExpression)) {
            cronForm.getMonthPerRadioButton().setSelected(true);
        } else if ("?".equals(cronMonthExpression)) {
            cronForm.getMonthNotAssignRadioButton().setSelected(true);
        } else if (cronMonthExpression.contains("/")) {
            cronForm.getMonthCycle1RadioButton().setSelected(true);
            String[] cronMonthExpressionArray = cronMonthExpression.split("/");
            cronForm.getMonthCycle1ComboBox1().setSelectedIndex(Integer.parseInt(cronMonthExpressionArray[0]) - 1);
            cronForm.getMonthCycle1ComboBox2().setSelectedIndex(Integer.parseInt(cronMonthExpressionArray[1]) - 1);
        } else if (cronMonthExpression.contains("-")) {
            cronForm.getMonthCycle2RadioButton().setSelected(true);
            String[] cronMonthExpressionArray = cronMonthExpression.split("-");
            cronForm.getMonthCycle2ComboBox1().setSelectedIndex(Integer.parseInt(cronMonthExpressionArray[0]) - 1);
            cronForm.getMonthCycle2ComboBox2().setSelectedIndex(Integer.parseInt(cronMonthExpressionArray[1]) - 1);
        } else {
            cronForm.getMonthAssignRadioButton().setSelected(true);
            String[] cronMonthExpressionArray = cronMonthExpression.split(",");
            for (String month : cronMonthExpressionArray) {
                MONTH_CHECK_BOX_MAP.get(Integer.parseInt(month)).setSelected(true);
            }
        }

        // 周
        clearWeekRadioButtons();
        String cronWeekExpression = cronExpressionArray[5];
        if ("?".equals(cronWeekExpression)) {
            cronForm.getWeekNotAssignRadioButton().setSelected(true);
        } else if ("*".equals(cronWeekExpression)) {
            cronForm.getWeekPerRadioButton().setSelected(true);
        } else if (cronWeekExpression.contains("-")) {
            cronForm.getWeekCycle1RadioButton().setSelected(true);
            String[] cronWeekExpressionArray = cronWeekExpression.split("-");
            cronForm.getWeekCycle1ComboBox1().setSelectedIndex(Integer.parseInt(cronWeekExpressionArray[0]) - 1);
            cronForm.getWeekCycle1ComboBox2().setSelectedIndex(Integer.parseInt(cronWeekExpressionArray[1]) - 1);
        } else if (cronWeekExpression.contains("#")) {
            cronForm.getWeekInMonthRadioButton().setSelected(true);
            String[] cronWeekExpressionArray = cronWeekExpression.split("#");
            cronForm.getWeekInMonthComboBox1().setSelectedIndex(Integer.parseInt(cronWeekExpressionArray[1]) - 1);
            cronForm.getWeekInMonthComboBox2().setSelectedIndex(Integer.parseInt(cronWeekExpressionArray[0]) - 1);
        } else if (cronWeekExpression.contains("L")) {
            cronForm.getWeekLastMonthRadioButton().setSelected(true);
            String[] cronWeekExpressionArray = cronWeekExpression.split("L");
            cronForm.getWeekLastMonthComboBox1().setSelectedIndex(Integer.parseInt(cronWeekExpressionArray[0]) - 1);
        } else {
            cronForm.getWeekAssignRadioButton().setSelected(true);
            String[] cronWeekExpressionArray = cronWeekExpression.split(",");
            for (String week : cronWeekExpressionArray) {
                WEEK_CHECK_BOX_MAP.get(Integer.parseInt(week)).setSelected(true);
            }
        }

        // 年
        clearYearRadioButtons();
        if (cronExpressionArray.length < 7) {
            return;
        }
        String cronYearExpression = cronExpressionArray[6];
        if (StringUtils.isNotBlank(cronYearExpression)) {
            if ("*".equals(cronYearExpression)) {
                cronForm.getYearPerRadioButton().setSelected(true);
            } else if (cronYearExpression.contains("-")) {
                cronForm.getYearCycleRadioButton().setSelected(true);
                String[] cronYearExpressionArray = cronYearExpression.split("-");
                cronForm.getYearCycleSpinner1().setValue(Integer.parseInt(cronYearExpressionArray[0]));
                cronForm.getYearCycleSpinner2().setValue(Integer.parseInt(cronYearExpressionArray[1]));
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
        cronPanel = new JPanel();
        cronPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(792);
        splitPane.setDividerSize(10);
        cronPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("秒", panel2);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        secCycle1RadioButton = new JRadioButton();
        secCycle1RadioButton.setText("周期：从");
        panel3.add(secCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        secCycle1Spinner1 = new JSpinner();
        panel3.add(secCycle1Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("开始，每");
        panel3.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCycle1Spinner2 = new JSpinner();
        panel3.add(secCycle1Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("秒执行一次");
        panel3.add(label2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        secPerRadioButton = new JRadioButton();
        secPerRadioButton.setText("每秒");
        panel4.add(secPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel4.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        secAssignRadioButton = new JRadioButton();
        secAssignRadioButton.setText("指定：");
        panel5.add(secAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel5.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(6, 10, new Insets(10, 10, 10, 10), -1, -1));
        panel2.add(panel6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        secCheckBox0 = new JCheckBox();
        secCheckBox0.setText("0");
        panel6.add(secCheckBox0, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox10 = new JCheckBox();
        secCheckBox10.setText("10");
        panel6.add(secCheckBox10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox20 = new JCheckBox();
        secCheckBox20.setText("20");
        panel6.add(secCheckBox20, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox30 = new JCheckBox();
        secCheckBox30.setText("30");
        panel6.add(secCheckBox30, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox1 = new JCheckBox();
        secCheckBox1.setText("1");
        panel6.add(secCheckBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox11 = new JCheckBox();
        secCheckBox11.setText("11");
        panel6.add(secCheckBox11, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox21 = new JCheckBox();
        secCheckBox21.setText("21");
        panel6.add(secCheckBox21, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox31 = new JCheckBox();
        secCheckBox31.setText("31");
        panel6.add(secCheckBox31, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox2 = new JCheckBox();
        secCheckBox2.setText("2");
        panel6.add(secCheckBox2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox12 = new JCheckBox();
        secCheckBox12.setText("12");
        panel6.add(secCheckBox12, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox22 = new JCheckBox();
        secCheckBox22.setText("22");
        panel6.add(secCheckBox22, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox32 = new JCheckBox();
        secCheckBox32.setText("32");
        panel6.add(secCheckBox32, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox3 = new JCheckBox();
        secCheckBox3.setText("3");
        panel6.add(secCheckBox3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox13 = new JCheckBox();
        secCheckBox13.setText("13");
        panel6.add(secCheckBox13, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox23 = new JCheckBox();
        secCheckBox23.setText("23");
        panel6.add(secCheckBox23, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox33 = new JCheckBox();
        secCheckBox33.setText("33");
        panel6.add(secCheckBox33, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox4 = new JCheckBox();
        secCheckBox4.setText("4");
        panel6.add(secCheckBox4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox14 = new JCheckBox();
        secCheckBox14.setText("14");
        panel6.add(secCheckBox14, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox24 = new JCheckBox();
        secCheckBox24.setText("24");
        panel6.add(secCheckBox24, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox34 = new JCheckBox();
        secCheckBox34.setText("34");
        panel6.add(secCheckBox34, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox5 = new JCheckBox();
        secCheckBox5.setText("5");
        panel6.add(secCheckBox5, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox15 = new JCheckBox();
        secCheckBox15.setText("15");
        panel6.add(secCheckBox15, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox25 = new JCheckBox();
        secCheckBox25.setText("25");
        panel6.add(secCheckBox25, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox35 = new JCheckBox();
        secCheckBox35.setText("35");
        panel6.add(secCheckBox35, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox6 = new JCheckBox();
        secCheckBox6.setText("6");
        panel6.add(secCheckBox6, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox16 = new JCheckBox();
        secCheckBox16.setText("16");
        panel6.add(secCheckBox16, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox26 = new JCheckBox();
        secCheckBox26.setText("26");
        panel6.add(secCheckBox26, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox36 = new JCheckBox();
        secCheckBox36.setText("36");
        panel6.add(secCheckBox36, new GridConstraints(3, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox46 = new JCheckBox();
        secCheckBox46.setText("46");
        panel6.add(secCheckBox46, new GridConstraints(4, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox56 = new JCheckBox();
        secCheckBox56.setText("56");
        panel6.add(secCheckBox56, new GridConstraints(5, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox7 = new JCheckBox();
        secCheckBox7.setText("7");
        panel6.add(secCheckBox7, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox17 = new JCheckBox();
        secCheckBox17.setText("17");
        panel6.add(secCheckBox17, new GridConstraints(1, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox27 = new JCheckBox();
        secCheckBox27.setText("27");
        panel6.add(secCheckBox27, new GridConstraints(2, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox37 = new JCheckBox();
        secCheckBox37.setText("37");
        panel6.add(secCheckBox37, new GridConstraints(3, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox47 = new JCheckBox();
        secCheckBox47.setText("47");
        panel6.add(secCheckBox47, new GridConstraints(4, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox57 = new JCheckBox();
        secCheckBox57.setText("57");
        panel6.add(secCheckBox57, new GridConstraints(5, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox8 = new JCheckBox();
        secCheckBox8.setText("8");
        panel6.add(secCheckBox8, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox18 = new JCheckBox();
        secCheckBox18.setText("18");
        panel6.add(secCheckBox18, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox28 = new JCheckBox();
        secCheckBox28.setText("28");
        panel6.add(secCheckBox28, new GridConstraints(2, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox38 = new JCheckBox();
        secCheckBox38.setText("38");
        panel6.add(secCheckBox38, new GridConstraints(3, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox48 = new JCheckBox();
        secCheckBox48.setText("48");
        panel6.add(secCheckBox48, new GridConstraints(4, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox58 = new JCheckBox();
        secCheckBox58.setText("58");
        panel6.add(secCheckBox58, new GridConstraints(5, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox9 = new JCheckBox();
        secCheckBox9.setText("9");
        panel6.add(secCheckBox9, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox19 = new JCheckBox();
        secCheckBox19.setText("19");
        panel6.add(secCheckBox19, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox29 = new JCheckBox();
        secCheckBox29.setText("29");
        panel6.add(secCheckBox29, new GridConstraints(2, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox39 = new JCheckBox();
        secCheckBox39.setText("39");
        panel6.add(secCheckBox39, new GridConstraints(3, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox49 = new JCheckBox();
        secCheckBox49.setText("49");
        panel6.add(secCheckBox49, new GridConstraints(4, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox59 = new JCheckBox();
        secCheckBox59.setText("59");
        panel6.add(secCheckBox59, new GridConstraints(5, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox40 = new JCheckBox();
        secCheckBox40.setText("40");
        panel6.add(secCheckBox40, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox50 = new JCheckBox();
        secCheckBox50.setText("50");
        panel6.add(secCheckBox50, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox41 = new JCheckBox();
        secCheckBox41.setText("41");
        panel6.add(secCheckBox41, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox51 = new JCheckBox();
        secCheckBox51.setText("51");
        panel6.add(secCheckBox51, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox42 = new JCheckBox();
        secCheckBox42.setText("42");
        panel6.add(secCheckBox42, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox52 = new JCheckBox();
        secCheckBox52.setText("52");
        panel6.add(secCheckBox52, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox43 = new JCheckBox();
        secCheckBox43.setText("43");
        panel6.add(secCheckBox43, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox53 = new JCheckBox();
        secCheckBox53.setText("53");
        panel6.add(secCheckBox53, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox44 = new JCheckBox();
        secCheckBox44.setText("44");
        panel6.add(secCheckBox44, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox54 = new JCheckBox();
        secCheckBox54.setText("54");
        panel6.add(secCheckBox54, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox45 = new JCheckBox();
        secCheckBox45.setText("45");
        panel6.add(secCheckBox45, new GridConstraints(4, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCheckBox55 = new JCheckBox();
        secCheckBox55.setText("55");
        panel6.add(secCheckBox55, new GridConstraints(5, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        secCycle2RadioButton = new JRadioButton();
        secCycle2RadioButton.setText("周期：从");
        panel7.add(secCycle2RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel7.add(spacer5, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        secCycle2Spinner1 = new JSpinner();
        panel7.add(secCycle2Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("-");
        panel7.add(label3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secCycle2Spinner2 = new JSpinner();
        panel7.add(secCycle2Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("秒");
        panel7.add(label4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("分钟", panel8);
        final Spacer spacer6 = new Spacer();
        panel8.add(spacer6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minuPerRadioButton = new JRadioButton();
        minuPerRadioButton.setText("每分钟");
        panel9.add(minuPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel9.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minuCycle1RadioButton = new JRadioButton();
        minuCycle1RadioButton.setText("周期：从");
        panel10.add(minuCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCycle1Spinner1 = new JSpinner();
        panel10.add(minuCycle1Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel10.add(spacer8, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("开始，每");
        panel10.add(label5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCycle1Spinner2 = new JSpinner();
        panel10.add(minuCycle1Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("分钟执行一次");
        panel10.add(label6, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel11, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minuAssignRadioButton = new JRadioButton();
        minuAssignRadioButton.setText("指定：");
        panel11.add(minuAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        panel11.add(spacer9, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(6, 10, new Insets(10, 10, 10, 10), -1, -1));
        panel8.add(panel12, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel12.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        minuCheckBox0 = new JCheckBox();
        minuCheckBox0.setText("0");
        panel12.add(minuCheckBox0, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox10 = new JCheckBox();
        minuCheckBox10.setText("10");
        panel12.add(minuCheckBox10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox20 = new JCheckBox();
        minuCheckBox20.setText("20");
        panel12.add(minuCheckBox20, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox30 = new JCheckBox();
        minuCheckBox30.setText("30");
        panel12.add(minuCheckBox30, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox40 = new JCheckBox();
        minuCheckBox40.setText("40");
        panel12.add(minuCheckBox40, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox50 = new JCheckBox();
        minuCheckBox50.setText("50");
        panel12.add(minuCheckBox50, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox1 = new JCheckBox();
        minuCheckBox1.setText("1");
        panel12.add(minuCheckBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox11 = new JCheckBox();
        minuCheckBox11.setText("11");
        panel12.add(minuCheckBox11, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox21 = new JCheckBox();
        minuCheckBox21.setText("21");
        panel12.add(minuCheckBox21, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox31 = new JCheckBox();
        minuCheckBox31.setText("31");
        panel12.add(minuCheckBox31, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox41 = new JCheckBox();
        minuCheckBox41.setText("41");
        panel12.add(minuCheckBox41, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox51 = new JCheckBox();
        minuCheckBox51.setText("51");
        panel12.add(minuCheckBox51, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox2 = new JCheckBox();
        minuCheckBox2.setText("2");
        panel12.add(minuCheckBox2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox12 = new JCheckBox();
        minuCheckBox12.setText("12");
        panel12.add(minuCheckBox12, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox22 = new JCheckBox();
        minuCheckBox22.setText("22");
        panel12.add(minuCheckBox22, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox32 = new JCheckBox();
        minuCheckBox32.setText("32");
        panel12.add(minuCheckBox32, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox42 = new JCheckBox();
        minuCheckBox42.setText("42");
        panel12.add(minuCheckBox42, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox52 = new JCheckBox();
        minuCheckBox52.setText("52");
        panel12.add(minuCheckBox52, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox3 = new JCheckBox();
        minuCheckBox3.setText("3");
        panel12.add(minuCheckBox3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox13 = new JCheckBox();
        minuCheckBox13.setText("13");
        panel12.add(minuCheckBox13, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox23 = new JCheckBox();
        minuCheckBox23.setText("23");
        panel12.add(minuCheckBox23, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox33 = new JCheckBox();
        minuCheckBox33.setText("33");
        panel12.add(minuCheckBox33, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox43 = new JCheckBox();
        minuCheckBox43.setText("43");
        panel12.add(minuCheckBox43, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox53 = new JCheckBox();
        minuCheckBox53.setText("53");
        panel12.add(minuCheckBox53, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox4 = new JCheckBox();
        minuCheckBox4.setText("4");
        panel12.add(minuCheckBox4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox14 = new JCheckBox();
        minuCheckBox14.setText("14");
        panel12.add(minuCheckBox14, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox24 = new JCheckBox();
        minuCheckBox24.setText("24");
        panel12.add(minuCheckBox24, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox34 = new JCheckBox();
        minuCheckBox34.setText("34");
        panel12.add(minuCheckBox34, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox44 = new JCheckBox();
        minuCheckBox44.setText("44");
        panel12.add(minuCheckBox44, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox54 = new JCheckBox();
        minuCheckBox54.setText("54");
        panel12.add(minuCheckBox54, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox5 = new JCheckBox();
        minuCheckBox5.setText("5");
        panel12.add(minuCheckBox5, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox15 = new JCheckBox();
        minuCheckBox15.setText("15");
        panel12.add(minuCheckBox15, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox25 = new JCheckBox();
        minuCheckBox25.setText("25");
        panel12.add(minuCheckBox25, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox35 = new JCheckBox();
        minuCheckBox35.setText("35");
        panel12.add(minuCheckBox35, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox45 = new JCheckBox();
        minuCheckBox45.setText("45");
        panel12.add(minuCheckBox45, new GridConstraints(4, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox55 = new JCheckBox();
        minuCheckBox55.setText("55");
        panel12.add(minuCheckBox55, new GridConstraints(5, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox6 = new JCheckBox();
        minuCheckBox6.setText("6");
        panel12.add(minuCheckBox6, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox16 = new JCheckBox();
        minuCheckBox16.setText("16");
        panel12.add(minuCheckBox16, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox26 = new JCheckBox();
        minuCheckBox26.setText("26");
        panel12.add(minuCheckBox26, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox36 = new JCheckBox();
        minuCheckBox36.setText("36");
        panel12.add(minuCheckBox36, new GridConstraints(3, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox46 = new JCheckBox();
        minuCheckBox46.setText("46");
        panel12.add(minuCheckBox46, new GridConstraints(4, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox56 = new JCheckBox();
        minuCheckBox56.setText("56");
        panel12.add(minuCheckBox56, new GridConstraints(5, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox7 = new JCheckBox();
        minuCheckBox7.setText("7");
        panel12.add(minuCheckBox7, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox17 = new JCheckBox();
        minuCheckBox17.setText("17");
        panel12.add(minuCheckBox17, new GridConstraints(1, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox27 = new JCheckBox();
        minuCheckBox27.setText("27");
        panel12.add(minuCheckBox27, new GridConstraints(2, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox37 = new JCheckBox();
        minuCheckBox37.setText("37");
        panel12.add(minuCheckBox37, new GridConstraints(3, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox47 = new JCheckBox();
        minuCheckBox47.setText("47");
        panel12.add(minuCheckBox47, new GridConstraints(4, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox57 = new JCheckBox();
        minuCheckBox57.setText("57");
        panel12.add(minuCheckBox57, new GridConstraints(5, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox8 = new JCheckBox();
        minuCheckBox8.setText("8");
        panel12.add(minuCheckBox8, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox18 = new JCheckBox();
        minuCheckBox18.setText("18");
        panel12.add(minuCheckBox18, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox28 = new JCheckBox();
        minuCheckBox28.setText("28");
        panel12.add(minuCheckBox28, new GridConstraints(2, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox38 = new JCheckBox();
        minuCheckBox38.setText("38");
        panel12.add(minuCheckBox38, new GridConstraints(3, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox48 = new JCheckBox();
        minuCheckBox48.setText("48");
        panel12.add(minuCheckBox48, new GridConstraints(4, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox58 = new JCheckBox();
        minuCheckBox58.setText("58");
        panel12.add(minuCheckBox58, new GridConstraints(5, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox9 = new JCheckBox();
        minuCheckBox9.setText("9");
        panel12.add(minuCheckBox9, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox19 = new JCheckBox();
        minuCheckBox19.setText("19");
        panel12.add(minuCheckBox19, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox29 = new JCheckBox();
        minuCheckBox29.setText("29");
        panel12.add(minuCheckBox29, new GridConstraints(2, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox39 = new JCheckBox();
        minuCheckBox39.setText("39");
        panel12.add(minuCheckBox39, new GridConstraints(3, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox49 = new JCheckBox();
        minuCheckBox49.setText("49");
        panel12.add(minuCheckBox49, new GridConstraints(4, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCheckBox59 = new JCheckBox();
        minuCheckBox59.setText("59");
        panel12.add(minuCheckBox59, new GridConstraints(5, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minuCycle2RadioButton = new JRadioButton();
        minuCycle2RadioButton.setText("周期：从");
        panel13.add(minuCycle2RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer10 = new Spacer();
        panel13.add(spacer10, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        minuCycle2Spinner1 = new JSpinner();
        panel13.add(minuCycle2Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("-");
        panel13.add(label7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minuCycle2Spinner2 = new JSpinner();
        panel13.add(minuCycle2Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("分钟");
        panel13.add(label8, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("小时", panel14);
        final Spacer spacer11 = new Spacer();
        panel14.add(spacer11, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel14.add(panel15, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hourPerRadioButton = new JRadioButton();
        hourPerRadioButton.setText("每小时");
        panel15.add(hourPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer12 = new Spacer();
        panel15.add(spacer12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel14.add(panel16, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hourAssignRadioButton = new JRadioButton();
        hourAssignRadioButton.setText("指定：");
        panel16.add(hourAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer13 = new Spacer();
        panel16.add(spacer13, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(4, 6, new Insets(10, 10, 10, 10), -1, -1));
        panel14.add(panel17, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel17.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        hourCheckBox0 = new JCheckBox();
        hourCheckBox0.setText("0");
        panel17.add(hourCheckBox0, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox6 = new JCheckBox();
        hourCheckBox6.setText("6");
        panel17.add(hourCheckBox6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox12 = new JCheckBox();
        hourCheckBox12.setText("12");
        panel17.add(hourCheckBox12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox18 = new JCheckBox();
        hourCheckBox18.setText("18");
        panel17.add(hourCheckBox18, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox1 = new JCheckBox();
        hourCheckBox1.setText("1");
        panel17.add(hourCheckBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox7 = new JCheckBox();
        hourCheckBox7.setText("7");
        panel17.add(hourCheckBox7, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox13 = new JCheckBox();
        hourCheckBox13.setText("13");
        panel17.add(hourCheckBox13, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox19 = new JCheckBox();
        hourCheckBox19.setText("19");
        panel17.add(hourCheckBox19, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox2 = new JCheckBox();
        hourCheckBox2.setText("2");
        panel17.add(hourCheckBox2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox8 = new JCheckBox();
        hourCheckBox8.setText("8");
        panel17.add(hourCheckBox8, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox14 = new JCheckBox();
        hourCheckBox14.setText("14");
        panel17.add(hourCheckBox14, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox20 = new JCheckBox();
        hourCheckBox20.setText("20");
        panel17.add(hourCheckBox20, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox3 = new JCheckBox();
        hourCheckBox3.setText("3");
        panel17.add(hourCheckBox3, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox9 = new JCheckBox();
        hourCheckBox9.setText("9");
        panel17.add(hourCheckBox9, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox15 = new JCheckBox();
        hourCheckBox15.setText("15");
        panel17.add(hourCheckBox15, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox21 = new JCheckBox();
        hourCheckBox21.setText("21");
        panel17.add(hourCheckBox21, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox4 = new JCheckBox();
        hourCheckBox4.setText("4");
        panel17.add(hourCheckBox4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox10 = new JCheckBox();
        hourCheckBox10.setText("10");
        panel17.add(hourCheckBox10, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox16 = new JCheckBox();
        hourCheckBox16.setText("16");
        panel17.add(hourCheckBox16, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox22 = new JCheckBox();
        hourCheckBox22.setText("22");
        panel17.add(hourCheckBox22, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox5 = new JCheckBox();
        hourCheckBox5.setText("5");
        panel17.add(hourCheckBox5, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox11 = new JCheckBox();
        hourCheckBox11.setText("11");
        panel17.add(hourCheckBox11, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox17 = new JCheckBox();
        hourCheckBox17.setText("17");
        panel17.add(hourCheckBox17, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCheckBox23 = new JCheckBox();
        hourCheckBox23.setText("23");
        panel17.add(hourCheckBox23, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel14.add(panel18, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hourCycle1RadioButton = new JRadioButton();
        hourCycle1RadioButton.setText("周期：从");
        panel18.add(hourCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer14 = new Spacer();
        panel18.add(spacer14, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        hourCycle1Spinner1 = new JSpinner();
        panel18.add(hourCycle1Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("开始，每");
        panel18.add(label9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCycle1Spinner2 = new JSpinner();
        panel18.add(hourCycle1Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("小时执行一次");
        panel18.add(label10, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel14.add(panel19, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hourCycle2RadioButton = new JRadioButton();
        hourCycle2RadioButton.setText("周期：从");
        panel19.add(hourCycle2RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer15 = new Spacer();
        panel19.add(spacer15, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        hourCycle2Spinner1 = new JSpinner();
        panel19.add(hourCycle2Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("-");
        panel19.add(label11, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hourCycle2Spinner2 = new JSpinner();
        panel19.add(hourCycle2Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("小时");
        panel19.add(label12, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(9, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("日", panel20);
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel21, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayPerRadioButton = new JRadioButton();
        dayPerRadioButton.setText("每天");
        panel21.add(dayPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer16 = new Spacer();
        panel21.add(spacer16, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer17 = new Spacer();
        panel20.add(spacer17, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel22, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayAssignRadioButton = new JRadioButton();
        dayAssignRadioButton.setText("指定：");
        panel22.add(dayAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer18 = new Spacer();
        panel22.add(spacer18, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(4, 10, new Insets(10, 10, 10, 10), -1, -1));
        panel20.add(panel23, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel23.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        dayCheckBox1 = new JCheckBox();
        dayCheckBox1.setText("1");
        panel23.add(dayCheckBox1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox11 = new JCheckBox();
        dayCheckBox11.setText("11");
        panel23.add(dayCheckBox11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox21 = new JCheckBox();
        dayCheckBox21.setText("21");
        panel23.add(dayCheckBox21, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox31 = new JCheckBox();
        dayCheckBox31.setText("31");
        panel23.add(dayCheckBox31, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox2 = new JCheckBox();
        dayCheckBox2.setText("2");
        panel23.add(dayCheckBox2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox12 = new JCheckBox();
        dayCheckBox12.setText("12");
        panel23.add(dayCheckBox12, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox22 = new JCheckBox();
        dayCheckBox22.setText("22");
        panel23.add(dayCheckBox22, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox3 = new JCheckBox();
        dayCheckBox3.setText("3");
        panel23.add(dayCheckBox3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox13 = new JCheckBox();
        dayCheckBox13.setText("13");
        panel23.add(dayCheckBox13, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox23 = new JCheckBox();
        dayCheckBox23.setText("23");
        panel23.add(dayCheckBox23, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox4 = new JCheckBox();
        dayCheckBox4.setText("4");
        panel23.add(dayCheckBox4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox14 = new JCheckBox();
        dayCheckBox14.setText("14");
        panel23.add(dayCheckBox14, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox24 = new JCheckBox();
        dayCheckBox24.setText("24");
        panel23.add(dayCheckBox24, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox5 = new JCheckBox();
        dayCheckBox5.setText("5");
        panel23.add(dayCheckBox5, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox15 = new JCheckBox();
        dayCheckBox15.setText("15");
        panel23.add(dayCheckBox15, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox25 = new JCheckBox();
        dayCheckBox25.setText("25");
        panel23.add(dayCheckBox25, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox6 = new JCheckBox();
        dayCheckBox6.setText("6");
        panel23.add(dayCheckBox6, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox16 = new JCheckBox();
        dayCheckBox16.setText("16");
        panel23.add(dayCheckBox16, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox26 = new JCheckBox();
        dayCheckBox26.setText("26");
        panel23.add(dayCheckBox26, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox7 = new JCheckBox();
        dayCheckBox7.setText("7");
        panel23.add(dayCheckBox7, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox17 = new JCheckBox();
        dayCheckBox17.setText("17");
        panel23.add(dayCheckBox17, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox27 = new JCheckBox();
        dayCheckBox27.setText("27");
        panel23.add(dayCheckBox27, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox8 = new JCheckBox();
        dayCheckBox8.setText("8");
        panel23.add(dayCheckBox8, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox18 = new JCheckBox();
        dayCheckBox18.setText("18");
        panel23.add(dayCheckBox18, new GridConstraints(1, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox28 = new JCheckBox();
        dayCheckBox28.setText("28");
        panel23.add(dayCheckBox28, new GridConstraints(2, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox9 = new JCheckBox();
        dayCheckBox9.setText("9");
        panel23.add(dayCheckBox9, new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox19 = new JCheckBox();
        dayCheckBox19.setText("19");
        panel23.add(dayCheckBox19, new GridConstraints(1, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox29 = new JCheckBox();
        dayCheckBox29.setText("29");
        panel23.add(dayCheckBox29, new GridConstraints(2, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox10 = new JCheckBox();
        dayCheckBox10.setText("10");
        panel23.add(dayCheckBox10, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox20 = new JCheckBox();
        dayCheckBox20.setText("20");
        panel23.add(dayCheckBox20, new GridConstraints(1, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCheckBox30 = new JCheckBox();
        dayCheckBox30.setText("30");
        panel23.add(dayCheckBox30, new GridConstraints(2, 9, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel24, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayCycle1RadioButton = new JRadioButton();
        dayCycle1RadioButton.setText("周期：从");
        panel24.add(dayCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer19 = new Spacer();
        panel24.add(spacer19, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dayCycle1Spinner1 = new JSpinner();
        panel24.add(dayCycle1Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("开始，每");
        panel24.add(label13, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCycle1Spinner2 = new JSpinner();
        panel24.add(dayCycle1Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("天执行一次");
        panel24.add(label14, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel25, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayCycle2RadioButton = new JRadioButton();
        dayCycle2RadioButton.setText("周期：从");
        panel25.add(dayCycle2RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer20 = new Spacer();
        panel25.add(spacer20, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dayCycle2Spinner1 = new JSpinner();
        panel25.add(dayCycle2Spinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("-");
        panel25.add(label15, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dayCycle2Spinner2 = new JSpinner();
        panel25.add(dayCycle2Spinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("日");
        panel25.add(label16, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel26, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayNotAssignRadioButton = new JRadioButton();
        dayNotAssignRadioButton.setText("不指定");
        panel26.add(dayNotAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer21 = new Spacer();
        panel26.add(spacer21, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel27, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayPerMonthRadioButton = new JRadioButton();
        dayPerMonthRadioButton.setText("每月");
        panel27.add(dayPerMonthRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer22 = new Spacer();
        panel27.add(spacer22, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dayPerMonthSpinner = new JSpinner();
        panel27.add(dayPerMonthSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label17 = new JLabel();
        label17.setText("日之后最近的那个工作日");
        panel27.add(label17, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel20.add(panel28, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dayMonthLastRadioButton = new JRadioButton();
        dayMonthLastRadioButton.setText("每月最后一天");
        panel28.add(dayMonthLastRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer23 = new Spacer();
        panel28.add(spacer23, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new GridLayoutManager(7, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("月", panel29);
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel30, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        monthPerRadioButton = new JRadioButton();
        monthPerRadioButton.setText("每月");
        panel30.add(monthPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer24 = new Spacer();
        panel30.add(spacer24, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer25 = new Spacer();
        panel29.add(spacer25, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel31, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        monthAssignRadioButton = new JRadioButton();
        monthAssignRadioButton.setText("指定：");
        panel31.add(monthAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer26 = new Spacer();
        panel31.add(spacer26, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new GridLayoutManager(2, 6, new Insets(10, 10, 10, 10), -1, -1));
        panel29.add(panel32, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel32.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        monthCheckBox1 = new JCheckBox();
        monthCheckBox1.setText("1");
        panel32.add(monthCheckBox1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox7 = new JCheckBox();
        monthCheckBox7.setText("7");
        panel32.add(monthCheckBox7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox2 = new JCheckBox();
        monthCheckBox2.setText("2");
        panel32.add(monthCheckBox2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox8 = new JCheckBox();
        monthCheckBox8.setText("8");
        panel32.add(monthCheckBox8, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox3 = new JCheckBox();
        monthCheckBox3.setText("3");
        panel32.add(monthCheckBox3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox9 = new JCheckBox();
        monthCheckBox9.setText("9");
        panel32.add(monthCheckBox9, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox4 = new JCheckBox();
        monthCheckBox4.setText("4");
        panel32.add(monthCheckBox4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox10 = new JCheckBox();
        monthCheckBox10.setText("10");
        panel32.add(monthCheckBox10, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox5 = new JCheckBox();
        monthCheckBox5.setText("5");
        panel32.add(monthCheckBox5, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox11 = new JCheckBox();
        monthCheckBox11.setText("11");
        panel32.add(monthCheckBox11, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox6 = new JCheckBox();
        monthCheckBox6.setText("6");
        panel32.add(monthCheckBox6, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCheckBox12 = new JCheckBox();
        monthCheckBox12.setText("12");
        panel32.add(monthCheckBox12, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel33 = new JPanel();
        panel33.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel33, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        monthNotAssignRadioButton = new JRadioButton();
        monthNotAssignRadioButton.setText("不指定");
        panel33.add(monthNotAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer27 = new Spacer();
        panel33.add(spacer27, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel34 = new JPanel();
        panel34.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel34, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        monthCycle1RadioButton = new JRadioButton();
        monthCycle1RadioButton.setText("周期：从");
        panel34.add(monthCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer28 = new Spacer();
        panel34.add(spacer28, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label18 = new JLabel();
        label18.setText("月开始，每");
        panel34.add(label18, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        label19.setText("个月");
        panel34.add(label19, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCycle1ComboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("1");
        defaultComboBoxModel1.addElement("2");
        defaultComboBoxModel1.addElement("3");
        defaultComboBoxModel1.addElement("4");
        defaultComboBoxModel1.addElement("5");
        defaultComboBoxModel1.addElement("6");
        defaultComboBoxModel1.addElement("7");
        defaultComboBoxModel1.addElement("8");
        defaultComboBoxModel1.addElement("9");
        defaultComboBoxModel1.addElement("10");
        defaultComboBoxModel1.addElement("11");
        defaultComboBoxModel1.addElement("12");
        monthCycle1ComboBox1.setModel(defaultComboBoxModel1);
        panel34.add(monthCycle1ComboBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCycle1ComboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("1");
        defaultComboBoxModel2.addElement("2");
        defaultComboBoxModel2.addElement("3");
        defaultComboBoxModel2.addElement("4");
        defaultComboBoxModel2.addElement("5");
        defaultComboBoxModel2.addElement("6");
        defaultComboBoxModel2.addElement("7");
        defaultComboBoxModel2.addElement("8");
        defaultComboBoxModel2.addElement("9");
        defaultComboBoxModel2.addElement("10");
        defaultComboBoxModel2.addElement("11");
        defaultComboBoxModel2.addElement("12");
        monthCycle1ComboBox2.setModel(defaultComboBoxModel2);
        panel34.add(monthCycle1ComboBox2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel35 = new JPanel();
        panel35.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel29.add(panel35, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        monthCycle2RadioButton = new JRadioButton();
        monthCycle2RadioButton.setText("周期：从");
        panel35.add(monthCycle2RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer29 = new Spacer();
        panel35.add(spacer29, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label20 = new JLabel();
        label20.setText("-");
        panel35.add(label20, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label21 = new JLabel();
        label21.setText("月");
        panel35.add(label21, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCycle2ComboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("1");
        defaultComboBoxModel3.addElement("2");
        defaultComboBoxModel3.addElement("3");
        defaultComboBoxModel3.addElement("4");
        defaultComboBoxModel3.addElement("5");
        defaultComboBoxModel3.addElement("6");
        defaultComboBoxModel3.addElement("7");
        defaultComboBoxModel3.addElement("8");
        defaultComboBoxModel3.addElement("9");
        defaultComboBoxModel3.addElement("10");
        defaultComboBoxModel3.addElement("11");
        defaultComboBoxModel3.addElement("12");
        monthCycle2ComboBox1.setModel(defaultComboBoxModel3);
        panel35.add(monthCycle2ComboBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monthCycle2ComboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("1");
        defaultComboBoxModel4.addElement("2");
        defaultComboBoxModel4.addElement("3");
        defaultComboBoxModel4.addElement("4");
        defaultComboBoxModel4.addElement("5");
        defaultComboBoxModel4.addElement("6");
        defaultComboBoxModel4.addElement("7");
        defaultComboBoxModel4.addElement("8");
        defaultComboBoxModel4.addElement("9");
        defaultComboBoxModel4.addElement("10");
        defaultComboBoxModel4.addElement("11");
        defaultComboBoxModel4.addElement("12");
        monthCycle2ComboBox2.setModel(defaultComboBoxModel4);
        panel35.add(monthCycle2ComboBox2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel36 = new JPanel();
        panel36.setLayout(new GridLayoutManager(8, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("星期", panel36);
        final JPanel panel37 = new JPanel();
        panel37.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel37, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekNotAssignRadioButton = new JRadioButton();
        weekNotAssignRadioButton.setText("不指定");
        panel37.add(weekNotAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer30 = new Spacer();
        panel37.add(spacer30, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer31 = new Spacer();
        panel36.add(spacer31, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel38 = new JPanel();
        panel38.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel38, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekAssignRadioButton = new JRadioButton();
        weekAssignRadioButton.setText("指定：");
        panel38.add(weekAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer32 = new Spacer();
        panel38.add(spacer32, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel39 = new JPanel();
        panel39.setLayout(new GridLayoutManager(1, 7, new Insets(10, 10, 10, 10), -1, -1));
        panel36.add(panel39, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel39.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        weekCheckBox1 = new JCheckBox();
        weekCheckBox1.setText("一");
        panel39.add(weekCheckBox1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox2 = new JCheckBox();
        weekCheckBox2.setText("二");
        panel39.add(weekCheckBox2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox3 = new JCheckBox();
        weekCheckBox3.setText("三");
        panel39.add(weekCheckBox3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox4 = new JCheckBox();
        weekCheckBox4.setText("四");
        panel39.add(weekCheckBox4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox5 = new JCheckBox();
        weekCheckBox5.setText("五");
        panel39.add(weekCheckBox5, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox6 = new JCheckBox();
        weekCheckBox6.setText("六");
        panel39.add(weekCheckBox6, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCheckBox7 = new JCheckBox();
        weekCheckBox7.setText("日");
        panel39.add(weekCheckBox7, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel40 = new JPanel();
        panel40.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel40, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekPerRadioButton = new JRadioButton();
        weekPerRadioButton.setText("每周");
        panel40.add(weekPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer33 = new Spacer();
        panel40.add(spacer33, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel41 = new JPanel();
        panel41.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel41, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekCycle1RadioButton = new JRadioButton();
        weekCycle1RadioButton.setText("周期：从");
        panel41.add(weekCycle1RadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer34 = new Spacer();
        panel41.add(spacer34, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        weekCycle1ComboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        defaultComboBoxModel5.addElement("星期日");
        defaultComboBoxModel5.addElement("星期一");
        defaultComboBoxModel5.addElement("星期二");
        defaultComboBoxModel5.addElement("星期三");
        defaultComboBoxModel5.addElement("星期四");
        defaultComboBoxModel5.addElement("星期五");
        defaultComboBoxModel5.addElement("星期六");
        weekCycle1ComboBox1.setModel(defaultComboBoxModel5);
        panel41.add(weekCycle1ComboBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label22 = new JLabel();
        label22.setText("-");
        panel41.add(label22, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekCycle1ComboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel6 = new DefaultComboBoxModel();
        defaultComboBoxModel6.addElement("星期日");
        defaultComboBoxModel6.addElement("星期一");
        defaultComboBoxModel6.addElement("星期二");
        defaultComboBoxModel6.addElement("星期三");
        defaultComboBoxModel6.addElement("星期四");
        defaultComboBoxModel6.addElement("星期五");
        defaultComboBoxModel6.addElement("星期六");
        weekCycle1ComboBox2.setModel(defaultComboBoxModel6);
        panel41.add(weekCycle1ComboBox2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel42 = new JPanel();
        panel42.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel42, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekInMonthRadioButton = new JRadioButton();
        weekInMonthRadioButton.setText("当月第");
        panel42.add(weekInMonthRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer35 = new Spacer();
        panel42.add(spacer35, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        weekInMonthComboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel7 = new DefaultComboBoxModel();
        defaultComboBoxModel7.addElement("1");
        defaultComboBoxModel7.addElement("2");
        defaultComboBoxModel7.addElement("3");
        defaultComboBoxModel7.addElement("4");
        weekInMonthComboBox1.setModel(defaultComboBoxModel7);
        panel42.add(weekInMonthComboBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label23 = new JLabel();
        label23.setText("周的");
        panel42.add(label23, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        weekInMonthComboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel8 = new DefaultComboBoxModel();
        defaultComboBoxModel8.addElement("星期日");
        defaultComboBoxModel8.addElement("星期一");
        defaultComboBoxModel8.addElement("星期二");
        defaultComboBoxModel8.addElement("星期三");
        defaultComboBoxModel8.addElement("星期四");
        defaultComboBoxModel8.addElement("星期五");
        defaultComboBoxModel8.addElement("星期六");
        weekInMonthComboBox2.setModel(defaultComboBoxModel8);
        panel42.add(weekInMonthComboBox2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel43 = new JPanel();
        panel43.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel36.add(panel43, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        weekLastMonthRadioButton = new JRadioButton();
        weekLastMonthRadioButton.setText("当月最后一个");
        panel43.add(weekLastMonthRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer36 = new Spacer();
        panel43.add(spacer36, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        weekLastMonthComboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel9 = new DefaultComboBoxModel();
        defaultComboBoxModel9.addElement("星期日");
        defaultComboBoxModel9.addElement("星期一");
        defaultComboBoxModel9.addElement("星期二");
        defaultComboBoxModel9.addElement("星期三");
        defaultComboBoxModel9.addElement("星期四");
        defaultComboBoxModel9.addElement("星期五");
        defaultComboBoxModel9.addElement("星期六");
        weekLastMonthComboBox1.setModel(defaultComboBoxModel9);
        panel43.add(weekLastMonthComboBox1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel44 = new JPanel();
        panel44.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("年", panel44);
        final JPanel panel45 = new JPanel();
        panel45.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel44.add(panel45, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        yearNotAssignRadioButton = new JRadioButton();
        yearNotAssignRadioButton.setText("不指定");
        panel45.add(yearNotAssignRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer37 = new Spacer();
        panel45.add(spacer37, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer38 = new Spacer();
        panel44.add(spacer38, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel46 = new JPanel();
        panel46.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel44.add(panel46, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        yearPerRadioButton = new JRadioButton();
        yearPerRadioButton.setText("每年");
        panel46.add(yearPerRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer39 = new Spacer();
        panel46.add(spacer39, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel47 = new JPanel();
        panel47.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel44.add(panel47, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        yearCycleRadioButton = new JRadioButton();
        yearCycleRadioButton.setText("周期：");
        panel47.add(yearCycleRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer40 = new Spacer();
        panel47.add(spacer40, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        yearCycleSpinner1 = new JSpinner();
        panel47.add(yearCycleSpinner1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label24 = new JLabel();
        label24.setText("-");
        panel47.add(label24, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yearCycleSpinner2 = new JSpinner();
        panel47.add(yearCycleSpinner2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel48 = new JPanel();
        panel48.setLayout(new GridLayoutManager(4, 1, new Insets(12, 0, 12, 12), -1, -1));
        splitPane.setRightComponent(panel48);
        final JPanel panel49 = new JPanel();
        panel49.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel48.add(panel49, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel50 = new JPanel();
        panel50.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel49.add(panel50, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cronToHumanReadableButton = new JButton();
        cronToHumanReadableButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        cronToHumanReadableButton.setText("");
        panel50.add(cronToHumanReadableButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button2 = new JButton();
        button2.setEnabled(false);
        button2.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        button2.setText("");
        panel50.add(button2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer41 = new Spacer();
        panel50.add(spacer41, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer42 = new Spacer();
        panel50.add(spacer42, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel51 = new JPanel();
        panel51.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel49.add(panel51, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label25 = new JLabel();
        label25.setText("自然语言");
        panel51.add(label25, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        humanReadableTextField = new JTextField();
        panel51.add(humanReadableTextField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        localComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel10 = new DefaultComboBoxModel();
        defaultComboBoxModel10.addElement("中文");
        defaultComboBoxModel10.addElement("英文");
        defaultComboBoxModel10.addElement("日文");
        localComboBox.setModel(defaultComboBoxModel10);
        panel51.add(localComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel52 = new JPanel();
        panel52.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel49.add(panel52, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label26 = new JLabel();
        label26.setText("Cron 表达式");
        panel52.add(label26, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronExpressionTextField = new JTextField();
        panel52.add(cronExpressionTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        commonCronButton = new JButton();
        commonCronButton.setText("常用Cron");
        panel52.add(commonCronButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        favoriteBookButton = new JButton();
        favoriteBookButton.setText("收藏夹");
        panel52.add(favoriteBookButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addToFavoriteButton = new JButton();
        addToFavoriteButton.setText("收藏");
        panel52.add(addToFavoriteButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel53 = new JPanel();
        panel53.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel48.add(panel53, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel53.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        nextExecutionTimeTextArea = new JTextArea();
        nextExecutionTimeTextArea.setText("");
        scrollPane1.setViewportView(nextExecutionTimeTextArea);
        final JPanel panel54 = new JPanel();
        panel54.setLayout(new GridLayoutManager(2, 7, new Insets(30, 0, 0, 0), -1, -1));
        panel48.add(panel54, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label27 = new JLabel();
        label27.setText("秒");
        panel54.add(label27, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label28 = new JLabel();
        label28.setText("分钟");
        panel54.add(label28, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label29 = new JLabel();
        label29.setText("小时");
        panel54.add(label29, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label30 = new JLabel();
        label30.setText("日");
        panel54.add(label30, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label31 = new JLabel();
        label31.setText("月");
        panel54.add(label31, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label32 = new JLabel();
        label32.setText("星期");
        panel54.add(label32, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label33 = new JLabel();
        label33.setText("年");
        panel54.add(label33, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cronMinuExpressionTextField = new JTextField();
        cronMinuExpressionTextField.setEditable(false);
        panel54.add(cronMinuExpressionTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronHourExpressionTextField = new JTextField();
        cronHourExpressionTextField.setEditable(false);
        panel54.add(cronHourExpressionTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronDayExpressionTextField = new JTextField();
        cronDayExpressionTextField.setEditable(false);
        panel54.add(cronDayExpressionTextField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronMonthExpressionTextField = new JTextField();
        cronMonthExpressionTextField.setEditable(false);
        panel54.add(cronMonthExpressionTextField, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronSecExpressionTextField = new JTextField();
        cronSecExpressionTextField.setEditable(false);
        panel54.add(cronSecExpressionTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronWeekExpressionTextField = new JTextField();
        cronWeekExpressionTextField.setEditable(false);
        panel54.add(cronWeekExpressionTextField, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cronYearExpressionTextField = new JTextField();
        cronYearExpressionTextField.setEditable(false);
        panel54.add(cronYearExpressionTextField, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        resolveToUIButton = new JButton();
        resolveToUIButton.setText("反解析到UI");
        panel48.add(resolveToUIButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return cronPanel;
    }

}
