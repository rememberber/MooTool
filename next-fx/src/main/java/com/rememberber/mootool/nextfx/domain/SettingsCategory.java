package com.rememberber.mootool.nextfx.domain;

/**
 * Eleven settings categories in Electron order. P1 implements general/appearance/layout/editor/about.
 */
public enum SettingsCategory {
    GENERAL,
    APPEARANCE,
    LAYOUT,
    EDITOR,
    NETWORK,
    DATA,
    VAULT,
    RUNTIME,
    TOOLS,
    SHORTCUTS,
    ABOUT;

    public String id() {
        return name().toLowerCase();
    }

    public String titleKey() {
        return "settings.category." + id();
    }

    public boolean hasWorkingControls() {
        return this == GENERAL || this == APPEARANCE || this == LAYOUT || this == EDITOR || this == ABOUT || this == DATA;
    }
}
