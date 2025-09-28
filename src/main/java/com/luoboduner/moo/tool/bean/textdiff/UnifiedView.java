package com.luoboduner.moo.tool.bean.textdiff;

import java.util.List;

/**
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 18:54
 */
public record UnifiedView(String text, List<Span> lineSpans, List<Span> charSpans, int charEventCount) {
}
