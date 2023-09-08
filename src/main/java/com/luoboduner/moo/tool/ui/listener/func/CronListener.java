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

    }
}
