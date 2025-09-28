package com.luoboduner.moo.tool.bean.textdiff;

import com.luoboduner.moo.tool.enums.DiffTypeEnum;

/**
 * 差异信息类，用于UI高亮显示
 * @author CassianFlorin
 * @email flowercard591@gmail.com
 * @date 2025/9/27 18:37
 */
public record DiffInfo(int lineNumber, String content, DiffTypeEnum type) {
}
