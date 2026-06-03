package com.luoboduner.moo.tool.util;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CronExpressionUtilTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Test
    public void shouldCalculateNextExecutionsForUnixCron() {
        ZonedDateTime now = ZonedDateTime.of(2026, 6, 3, 10, 1, 30, 0, ZONE_ID);

        List<ZonedDateTime> nextExecutionTimes = CronExpressionUtil.nextExecutions("*/5 * * * *", now, 3);

        assertEquals(ZonedDateTime.of(2026, 6, 3, 10, 5, 0, 0, ZONE_ID), nextExecutionTimes.get(0));
        assertEquals(ZonedDateTime.of(2026, 6, 3, 10, 10, 0, 0, ZONE_ID), nextExecutionTimes.get(1));
        assertEquals(ZonedDateTime.of(2026, 6, 3, 10, 15, 0, 0, ZONE_ID), nextExecutionTimes.get(2));
    }

    @Test
    public void shouldKeepQuartzCronCompatibility() {
        ZonedDateTime now = ZonedDateTime.of(2026, 6, 3, 11, 59, 0, 0, ZONE_ID);

        List<ZonedDateTime> nextExecutionTimes = CronExpressionUtil.nextExecutions("0 0 12 * * ?", now, 1);

        assertEquals(ZonedDateTime.of(2026, 6, 3, 12, 0, 0, 0, ZONE_ID), nextExecutionTimes.get(0));
    }

    @Test
    public void shouldDescribeUnixCron() {
        String description = CronExpressionUtil.describe("*/5 * * * *", Locale.ENGLISH);

        assertFalse(description.isBlank());
    }
}
