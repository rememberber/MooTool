package com.rememberber.mootool.nextfx.domain.regex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegexEngineTest {

    @Test
    void returnsAllMatchPositionsAndCaptureGroups() {
        List<RegexEngine.Match> matches = RegexEngine.match(
                "(moo)(\\d+)",
                "moo1 moo22",
                new RegexEngine.Options(true, false, false, false)
        );
        assertThat(matches).containsExactly(
                new RegexEngine.Match(0, "moo1", List.of("moo", "1")),
                new RegexEngine.Match(5, "moo22", List.of("moo", "22"))
        );
    }

    @Test
    void handlesZeroWidthGlobalExpressionsWithoutLooping() {
        assertThat(RegexEngine.match("(?=a)", "aa", RegexEngine.Options.defaults())).hasSize(2);
    }

    @Test
    void keepsTheCompleteCommonPatternCatalog() {
        assertThat(RegexEngine.COMMON).hasSize(21);
        assertThat(RegexEngine.COMMON.getFirst().id()).isEqualTo("phone");
        assertThat(RegexEngine.COMMON.getLast().id()).isEqualTo("float");
    }

    @Test
    void matchesLookbehindAndNamedGroupsInJavaPattern() {
        assertThat(RegexEngine.match("(?<=id=\")[\\s\\S]*?(?=\")", "id=\"tool\"", RegexEngine.Options.defaults()))
                .extracting(RegexEngine.Match::value)
                .containsExactly("tool");
        assertThat(RegexEngine.match("(?<year>\\d{4})-\\k<year>", "2020-2020", RegexEngine.Options.defaults()))
                .extracting(RegexEngine.Match::value)
                .containsExactly("2020-2020");
    }

    @Test
    void recordsJsStyleUnicodeBraceEscapeAsJavaSyntaxError() {
        assertThatThrownBy(() -> RegexEngine.match("\\u{1F680}", "🚀", RegexEngine.Options.defaults()))
                .isInstanceOf(RegexException.class)
                .satisfies(error -> assertThat(((RegexException) error).messageKey()).isEqualTo("regex.invalid"));
    }

    @Test
    void timesOutWhenDeadlineHasAlreadyExpired() {
        assertThatThrownBy(() -> RegexEngine.match(
                "(a+)+b",
                "a".repeat(40),
                RegexEngine.Options.defaults(),
                100,
                0L
        ))
                .isInstanceOf(RegexException.class)
                .satisfies(error -> assertThat(((RegexException) error).messageKey()).isEqualTo("regex.error.timeout"));
    }

    @Test
    void rejectsInvalidPatternWithoutClearingCallersSource() {
        assertThatThrownBy(() -> RegexEngine.match("(", "keep me", RegexEngine.Options.defaults()))
                .isInstanceOf(RegexException.class)
                .satisfies(error -> assertThat(((RegexException) error).messageKey()).isEqualTo("regex.invalid"));
    }
}
