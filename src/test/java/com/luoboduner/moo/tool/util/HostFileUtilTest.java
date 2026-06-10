package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostFileUtilTest {

    @Test
    void escapeForAppleScriptShell_escapesSingleQuotes() {
        assertEquals("/tmp/normal-path", HostFileUtil.escapeForAppleScriptShell("/tmp/normal-path"));
        assertEquals("/tmp/it'\\''s-hosts.tmp", HostFileUtil.escapeForAppleScriptShell("/tmp/it's-hosts.tmp"));
    }
}
