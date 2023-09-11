package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.dialog.CommonCronDialog;
import com.luoboduner.moo.tool.ui.dialog.FavoriteCronDialog;
import com.luoboduner.moo.tool.ui.form.func.CronForm;
import com.luoboduner.moo.tool.ui.frame.FavoriteCronFrame;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * <pre>
 * CronListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2023/09/03.
 */
public class CronListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        CronForm cronForm = CronForm.getInstance();
        // cron转自然语言按钮
        cronForm.getCronToHumanReadableButton().addActionListener(e -> {
            try {
                String cronExpression = cronForm.getCronExpressionTextField().getText();

                String selectedLocaleStr = cronForm.getLocalComboBox().getSelectedItem().toString();

                Locale selectedLocale = Locale.getDefault();
                switch (selectedLocaleStr) {
                    case "中文" -> selectedLocale = Locale.CHINESE;
                    case "英文" -> selectedLocale = Locale.ENGLISH;
                    case "日文" -> selectedLocale = Locale.JAPANESE;
                    default -> {
                    }
                }
                CronDescriptor descriptor = CronDescriptor.instance(selectedLocale);
                CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                CronParser parser = new CronParser(cronDefinition);
                String description = descriptor.describe(parser.parse(cronExpression));

                cronForm.getHumanReadableTextField().setText(description);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(App.mainFrame, "转换失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        cronForm.getCommonCronButton().addActionListener(e -> {
            CommonCronDialog commonCronDialog = new CommonCronDialog();
            commonCronDialog.pack();
            commonCronDialog.setVisible(true);
        });

        cronForm.getFavoriteBookButton().addActionListener(e -> FavoriteCronFrame.showWindow());
        cronForm.getAddToFavoriteButton().addActionListener(e -> {
            FavoriteCronDialog favoriteCronDialog = new FavoriteCronDialog();
            favoriteCronDialog.pack();
            favoriteCronDialog.init(cronForm.getCronExpressionTextField().getText());
            favoriteCronDialog.setVisible(true);
        });

        // cronExpressionTextField变更事件
        cronForm.getCronExpressionTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCronExpression();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCronExpression();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCronExpression();
            }

            private void updateCronExpression() {
                try {
                    String cronExpression = cronForm.getCronExpressionTextField().getText();

                    CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                    CronParser parser = new CronParser(cronDefinition);

                    // 获取未来10次运行时间：
                    List<String> nextExecutionTimes = Lists.newArrayList();
                    ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
                    ZonedDateTime now = ZonedDateTime.now();
                    Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);
                    for (int i = 0; i < 10; i++) {
                        if (nextExecution.isPresent()) {
                            // yyyy-MM-dd HH:mm:ss
                            LocalDateTime localDateTime = nextExecution.get().toLocalDateTime();
                            String format = DateUtil.format(localDateTime, "yyyy-MM-dd HH:mm:ss");
                            nextExecutionTimes.add(format);
                            nextExecution = executionTime.nextExecution(nextExecution.get());
                        }
                    }
                    cronForm.getNextExecutionTimeTextArea().setText("最近10次运行时间：\n" + String.join("\n", nextExecutionTimes));
                } catch (Exception ex) {
                    cronForm.getNextExecutionTimeTextArea().setText("最近10次运行时间：\n" + ex.getMessage());
                }
            }
        });

        // 秒
        cronForm.getSecPerRadioButton().addActionListener(e -> {
            CronForm.clearSecRadioButtons();
            cronForm.getSecPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getSecCycle1RadioButton().addActionListener(e -> {
            CronForm.clearSecRadioButtons();
            cronForm.getSecCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getSecCycle2RadioButton().addActionListener(e -> {
            CronForm.clearSecRadioButtons();
            cronForm.getSecCycle2RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getSecAssignRadioButton().addActionListener(e -> {
            CronForm.clearSecRadioButtons();
            cronForm.getSecAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getSecCycle1Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getSecCycle1Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getSecCycle2Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getSecCycle2Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        ActionListener secCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.SEC_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(secCheckBoxActionListener));

        // 分
        cronForm.getMinuPerRadioButton().addActionListener(e -> {
            CronForm.clearMinuRadioButtons();
            cronForm.getMinuPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMinuCycle1RadioButton().addActionListener(e -> {
            CronForm.clearMinuRadioButtons();
            cronForm.getMinuCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMinuCycle2RadioButton().addActionListener(e -> {
            CronForm.clearMinuRadioButtons();
            cronForm.getMinuCycle2RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMinuAssignRadioButton().addActionListener(e -> {
            CronForm.clearMinuRadioButtons();
            cronForm.getMinuAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMinuCycle1Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getMinuCycle1Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getMinuCycle2Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getMinuCycle2Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        ActionListener minuCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.MINU_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(minuCheckBoxActionListener));

        // 时
        cronForm.getHourPerRadioButton().addActionListener(e -> {
            CronForm.clearHourRadioButtons();
            cronForm.getHourPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getHourCycle1RadioButton().addActionListener(e -> {
            CronForm.clearHourRadioButtons();
            cronForm.getHourCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getHourCycle2RadioButton().addActionListener(e -> {
            CronForm.clearHourRadioButtons();
            cronForm.getHourCycle2RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getHourAssignRadioButton().addActionListener(e -> {
            CronForm.clearHourRadioButtons();
            cronForm.getHourAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getHourCycle1Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getHourCycle1Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getHourCycle2Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getHourCycle2Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        ActionListener hourCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.HOUR_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(hourCheckBoxActionListener));

        // 日
        cronForm.getDayPerRadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayNotAssignRadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayNotAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayCycle1RadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayCycle2RadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayCycle2RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayPerMonthRadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayPerMonthRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayMonthLastRadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayMonthLastRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayAssignRadioButton().addActionListener(e -> {
            CronForm.clearDayRadioButtons();
            cronForm.getDayAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getDayCycle1Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getDayCycle1Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getDayCycle2Spinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getDayCycle2Spinner2().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getDayPerMonthSpinner().addChangeListener(e -> CronForm.generateCronExpression());
        ActionListener dayCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.DAY_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(dayCheckBoxActionListener));

        // 月
        cronForm.getMonthPerRadioButton().addActionListener(e -> {
            CronForm.clearMonthRadioButtons();
            cronForm.getMonthPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMonthNotAssignRadioButton().addActionListener(e -> {
            CronForm.clearMonthRadioButtons();
            cronForm.getMonthNotAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMonthCycle1RadioButton().addActionListener(e -> {
            CronForm.clearMonthRadioButtons();
            cronForm.getMonthCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMonthCycle2RadioButton().addActionListener(e -> {
            CronForm.clearMonthRadioButtons();
            cronForm.getMonthCycle2RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMonthAssignRadioButton().addActionListener(e -> {
            CronForm.clearMonthRadioButtons();
            cronForm.getMonthAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getMonthCycle1ComboBox1().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getMonthCycle1ComboBox2().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getMonthCycle2ComboBox1().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getMonthCycle2ComboBox2().addActionListener(e -> CronForm.generateCronExpression());
        ActionListener monthCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.MONTH_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(monthCheckBoxActionListener));

        // 周
        cronForm.getWeekPerRadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekNotAssignRadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekNotAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekCycle1RadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekCycle1RadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekInMonthRadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekInMonthRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekLastMonthRadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekLastMonthRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekAssignRadioButton().addActionListener(e -> {
            CronForm.clearWeekRadioButtons();
            cronForm.getWeekAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getWeekCycle1ComboBox1().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getWeekCycle1ComboBox2().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getWeekInMonthComboBox1().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getWeekInMonthComboBox2().addActionListener(e -> CronForm.generateCronExpression());
        cronForm.getWeekLastMonthComboBox1().addActionListener(e -> CronForm.generateCronExpression());
        ActionListener weekCheckBoxActionListener = e -> CronForm.generateCronExpression();
        CronForm.WEEK_CHECK_BOX_MAP.forEach((key, value) -> value.addActionListener(weekCheckBoxActionListener));

        // 年
        cronForm.getYearPerRadioButton().addActionListener(e -> {
            CronForm.clearYearRadioButtons();
            cronForm.getYearPerRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getYearNotAssignRadioButton().addActionListener(e -> {
            CronForm.clearYearRadioButtons();
            cronForm.getYearNotAssignRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getYearCycleRadioButton().addActionListener(e -> {
            CronForm.clearYearRadioButtons();
            cronForm.getYearCycleRadioButton().setSelected(true);

            CronForm.generateCronExpression();
        });
        cronForm.getYearCycleSpinner1().addChangeListener(e -> CronForm.generateCronExpression());
        cronForm.getYearCycleSpinner2().addChangeListener(e -> CronForm.generateCronExpression());

    }
}
