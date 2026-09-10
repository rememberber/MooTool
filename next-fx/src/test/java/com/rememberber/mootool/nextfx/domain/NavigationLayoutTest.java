package com.rememberber.mootool.nextfx.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationLayoutTest {

    @Test
    void hiddenTitlesUseEightyFourAndCompactOnlyChangesRowHeight() {
        assertThat(NavigationLayout.width(false)).isEqualTo(248);
        assertThat(NavigationLayout.width(true)).isEqualTo(84);
        assertThat(NavigationLayout.rowHeight(true)).isEqualTo(32);
        assertThat(NavigationLayout.rowHeight(false)).isEqualTo(36);
    }
}
