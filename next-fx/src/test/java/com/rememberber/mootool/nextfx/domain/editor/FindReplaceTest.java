package com.rememberber.mootool.nextfx.domain.editor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindReplaceTest {

    @Test
    void findsCaseInsensitiveMatchesAndReplacesCurrent() {
        String content = "Alpha alpha ALPHA";
        assertThat(FindReplace.findAll(content, "alpha", FindReplaceOptions.defaults())).hasSize(3);
        FindMatch first = FindReplace.findNext(content, "alpha", FindReplaceOptions.defaults(), 0, true);
        assertThat(FindReplace.replaceCurrent(content, first, "beta")).startsWith("beta");
        assertThat(FindReplace.replaceAll(content, "alpha", "x", new FindReplaceOptions(false, false, false)).count()).isEqualTo(3);
    }

    @Test
    void ignoresInvalidRegexInsteadOfLooping() {
        assertThat(FindReplace.findAll("abc", "(", new FindReplaceOptions(false, false, true))).isEmpty();
    }
}
