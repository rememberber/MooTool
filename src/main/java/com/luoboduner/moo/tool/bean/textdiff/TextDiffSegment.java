package com.luoboduner.moo.tool.bean.textdiff;

import com.luoboduner.moo.tool.enums.DiffTypeEnum;

/**
 * 用于 UI 的精细化段（按字符区间）
 *
 * @param leftStart  -1 表示无
 * @param leftEnd    -1 表示无
 * @param rightStart -1 表示无
 * @param rightEnd   -1 表示无
 *
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 18:52
 */
public record TextDiffSegment(DiffTypeEnum type, int leftStart, int leftEnd, int rightStart, int rightEnd) {
}
