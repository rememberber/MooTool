package com.rememberber.mootool.nextfx.domain.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FindReplace {

    private FindReplace() {
    }

    public static List<FindMatch> findAll(String content, String query, FindReplaceOptions options) {
        Pattern pattern = compile(query, options);
        if (pattern == null || content == null || content.isEmpty()) {
            return List.of();
        }
        List<FindMatch> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            if (matcher.start() == matcher.end()) {
                if (matcher.start() >= content.length()) {
                    break;
                }
                matcher.region(matcher.start() + 1, content.length());
                continue;
            }
            matches.add(new FindMatch(matcher.start(), matcher.end()));
        }
        return List.copyOf(matches);
    }

    public static FindMatch findNext(String content, String query, FindReplaceOptions options, int fromIndex, boolean forward) {
        List<FindMatch> matches = findAll(content, query, options);
        if (matches.isEmpty()) {
            return null;
        }
        if (forward) {
            for (FindMatch match : matches) {
                if (match.start() >= fromIndex) {
                    return match;
                }
            }
            return matches.getFirst();
        }
        for (int index = matches.size() - 1; index >= 0; index--) {
            FindMatch match = matches.get(index);
            if (match.end() <= fromIndex) {
                return match;
            }
        }
        return matches.getLast();
    }

    public static ReplaceAllResult replaceAll(String content, String query, String replacement, FindReplaceOptions options) {
        List<FindMatch> matches = findAll(content, query, options);
        if (matches.isEmpty()) {
            return new ReplaceAllResult(content, 0);
        }
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        String safeReplacement = replacement == null ? "" : replacement;
        for (FindMatch match : matches) {
            builder.append(content, cursor, match.start());
            builder.append(safeReplacement);
            cursor = match.end();
        }
        builder.append(content, cursor, content.length());
        return new ReplaceAllResult(builder.toString(), matches.size());
    }

    public static String replaceCurrent(String content, FindMatch match, String replacement) {
        if (match == null) {
            return content;
        }
        String safeReplacement = replacement == null ? "" : replacement;
        return content.substring(0, match.start()) + safeReplacement + content.substring(match.end());
    }

    private static Pattern compile(String query, FindReplaceOptions options) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        try {
            String source = options.regex() ? query : Pattern.quote(query);
            if (options.wholeWord()) {
                source = "\\b(?:" + source + ")\\b";
            }
            int flags = Pattern.UNICODE_CHARACTER_CLASS;
            if (!options.matchCase()) {
                flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            return Pattern.compile(source, flags);
        } catch (PatternSyntaxException exception) {
            return null;
        }
    }

    public record ReplaceAllResult(String text, int count) {
    }
}
