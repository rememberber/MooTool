package com.luoboduner.moo.tool.ui.startup;

/**
 * 应用启动阶段。
 */
public enum StartupPhase {
    NEW,
    SHELL_VISIBLE,
    CRITICAL_LOADING,
    INTERACTIVE,
    DEFERRED_RUNNING,
    READY
}
