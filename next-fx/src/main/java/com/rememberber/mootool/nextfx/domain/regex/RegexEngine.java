package com.rememberber.mootool.nextfx.domain.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex matching aligned with Electron {@code regexTools.ts}.
 * Engine is Java {@link Pattern}; {@code global} is a traversal strategy, not a Java flag.
 */
public final class RegexEngine {

    public static final String ENGINE_NAME = "Java Pattern";
    public static final int DEFAULT_MAX_MATCHES = 10_000;
    public static final long DEFAULT_TIMEOUT_NANOS = 2_000_000_000L;

    public static final List<CommonPattern> COMMON = List.of(
            common("phone", "regex.common.phone", "1[3-9]\\d{9}"),
            common("email", "regex.common.email", "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"),
            common("domain", "regex.common.domain", "^((http:\\/\\/)|(https:\\/\\/))?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(\\/)"),
            common("ipv4", "regex.common.ipv4", "((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))"),
            common("account", "regex.common.account", "^[a-zA-Z][a-zA-Z0-9_]{4,15}$"),
            common("htmlId", "regex.common.htmlId", "(?<=id=\")[\\s\\S]*?(?=\")"),
            common("color", "regex.common.color", "#([a-fA-F0-9]{6})"),
            common("jpg", "regex.common.jpg", "http[s:]{1,2}//[^\\s'\"<>]*?.jpg"),
            common("magnet", "regex.common.magnet", "magnet:\\?xt=urn:btih:[0-9a-fA-F]{40,}"),
            common("chinese", "regex.common.chinese", "^[\\u4e00-\\u9fa5]{0,}$"),
            common("alnum", "regex.common.alnum", "^[A-Za-z0-9]+$"),
            common("len3to20", "regex.common.len3to20", "^.{3,20}$"),
            common("letters26", "regex.common.letters26", "^[A-Za-z]+$"),
            common("wordUnderscore", "regex.common.wordUnderscore", "^\\w+$"),
            common("cnEnNum", "regex.common.cnEnNum", "^[\\u4E00-\\u9FA5A-Za-z0-9_]+$"),
            common("noSpecial", "regex.common.noSpecial", "[^%&',;=?$\\x22]+"),
            common("integer", "regex.common.integer", "^-?[1-9]\\d*$"),
            common("positiveInt", "regex.common.positiveInt", "^[1-9]\\d*$"),
            common("negativeInt", "regex.common.negativeInt", "^-[1-9]\\d*$"),
            common("nonNegativeInt", "regex.common.nonNegativeInt", "^(?:[1-9]\\d*|0)$"),
            common("float", "regex.common.float", "^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$")
    );

    private RegexEngine() {
    }

    public static List<Match> match(String pattern, String source, Options options) {
        return match(pattern, source, options, DEFAULT_MAX_MATCHES, DEFAULT_TIMEOUT_NANOS);
    }

    public static List<Match> match(String pattern, String source, Options options, int maxMatches, long timeoutNanos) {
        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern == null ? "" : pattern, toFlags(options));
        } catch (PatternSyntaxException exception) {
            throw new RegexException("regex.invalid", exception.getDescription());
        }
        TimedCharSequence timed = new TimedCharSequence(source == null ? "" : source, System.nanoTime() + timeoutNanos);
        List<Match> matches = new ArrayList<>();
        try {
            Matcher matcher = compiled.matcher(timed);
            if (!options.global()) {
                if (matcher.find()) {
                    matches.add(toMatch(matcher));
                }
                return List.copyOf(matches);
            }
            int start = 0;
            int length = timed.length();
            while (start <= length && matcher.find(start)) {
                matches.add(toMatch(matcher));
                if (matches.size() > maxMatches) {
                    throw new RegexException("regex.error.tooMany", String.valueOf(maxMatches));
                }
                int end = matcher.end();
                start = end == matcher.start() ? end + 1 : end;
                if (end == matcher.start() && start > length) {
                    break;
                }
            }
            return List.copyOf(matches);
        } catch (RegexTimeout timeout) {
            throw new RegexException("regex.error.timeout", "");
        }
    }

    private static int toFlags(Options options) {
        int flags = 0;
        if (options.ignoreCase()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        if (options.multiline()) {
            flags |= Pattern.MULTILINE;
        }
        if (options.dotAll()) {
            flags |= Pattern.DOTALL;
        }
        return flags;
    }

    private static Match toMatch(Matcher matcher) {
        List<String> groups = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String group = matcher.group(i);
            groups.add(group == null ? "" : group);
        }
        String value = matcher.group();
        return new Match(matcher.start(), value == null ? "" : value, List.copyOf(groups));
    }

    private static CommonPattern common(String id, String labelKey, String pattern) {
        return new CommonPattern(id, labelKey, pattern);
    }

    public record Options(boolean global, boolean ignoreCase, boolean multiline, boolean dotAll) {
        public static Options defaults() {
            return new Options(true, false, false, false);
        }
    }

    public record Match(int index, String value, List<String> groups) {
    }

    public record CommonPattern(String id, String labelKey, String pattern) {
    }

    private static final class RegexTimeout extends RuntimeException {
    }

    private static final class TimedCharSequence implements CharSequence {
        private final String value;
        private final long deadlineNanos;

        private TimedCharSequence(String value, long deadlineNanos) {
            this.value = value;
            this.deadlineNanos = deadlineNanos;
        }

        @Override
        public int length() {
            check();
            return value.length();
        }

        @Override
        public char charAt(int index) {
            check();
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            check();
            return new TimedCharSequence(value.substring(start, end), deadlineNanos);
        }

        @Override
        public String toString() {
            return value;
        }

        private void check() {
            if (System.nanoTime() >= deadlineNanos) {
                throw new RegexTimeout();
            }
        }
    }
}
