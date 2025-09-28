package com.luoboduner.moo.tool.bean.textdiff;

import com.luoboduner.moo.tool.enums.UnifiedSpanTypeEnum;

/**
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 18:54
 */
public record Span(int start, int end, UnifiedSpanTypeEnum type) {
}
