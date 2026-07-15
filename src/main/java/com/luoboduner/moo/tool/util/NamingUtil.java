package com.luoboduner.moo.tool.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

public final class NamingUtil {

    private NamingUtil() {
    }

    public static String defaultUntitledName() {
        return I18n.get("common.untitledPrefix")
                + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
    }
}
