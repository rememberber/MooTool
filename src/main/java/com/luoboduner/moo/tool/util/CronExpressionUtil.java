package com.luoboduner.moo.tool.util;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CronExpressionUtil {

    private CronExpressionUtil() {
    }

    public static String describe(String cronExpression, Locale locale) {
        CronDescriptor descriptor = CronDescriptor.instance(locale);
        return descriptor.describe(parse(cronExpression));
    }

    public static List<ZonedDateTime> nextExecutions(String cronExpression, ZonedDateTime startTime, int count) {
        List<ZonedDateTime> nextExecutionTimes = new ArrayList<>();
        if (count <= 0) {
            return nextExecutionTimes;
        }

        ExecutionTime executionTime = ExecutionTime.forCron(parse(cronExpression));
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(startTime);
        for (int i = 0; i < count && nextExecution.isPresent(); i++) {
            ZonedDateTime execution = nextExecution.get();
            nextExecutionTimes.add(execution);
            nextExecution = executionTime.nextExecution(execution);
        }
        return nextExecutionTimes;
    }

    public static Cron parse(String cronExpression) {
        String normalizedCronExpression = normalize(cronExpression);
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(resolveCronType(normalizedCronExpression));
        CronParser parser = new CronParser(cronDefinition);
        return parser.parse(normalizedCronExpression);
    }

    private static CronType resolveCronType(String cronExpression) {
        if (cronExpression.startsWith("@")) {
            return CronType.UNIX;
        }

        int fieldCount = cronExpression.split("\\s+").length;
        if (fieldCount == 5) {
            return CronType.UNIX;
        }
        if (fieldCount == 6 || fieldCount == 7) {
            return CronType.QUARTZ;
        }
        throw new IllegalArgumentException("Cron表达式格式错误，仅支持Linux 5段或Quartz 6/7段表达式");
    }

    private static String normalize(String cronExpression) {
        if (cronExpression == null) {
            throw new IllegalArgumentException("Cron表达式不能为空");
        }

        String normalizedCronExpression = cronExpression.trim();
        if (normalizedCronExpression.isEmpty()) {
            throw new IllegalArgumentException("Cron表达式不能为空");
        }
        return normalizedCronExpression;
    }
}
