package com.rememberber.mootool.nextfx;

import com.rememberber.mootool.nextfx.app.MooToolFxApplication;

/**
 * Process entry that does not extend {@code Application}. JavaFX is started from
 * {@link MooToolFxApplication} so packaging and tests can inject identity first.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MooToolFxApplication.launchApp(args);
    }
}
