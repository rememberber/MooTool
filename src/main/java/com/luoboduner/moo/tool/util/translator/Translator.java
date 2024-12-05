package com.luoboduner.moo.tool.util.translator;

import cn.hutool.json.JSONUtil;

import java.util.Map;

public interface Translator {
    Map<String, String> languageCodeToNameMap = JSONUtil.toBean("{\"auto\":\"自动检测\",\"zh-CN\":\"中文（简体）\",\"en\":\"英语\",\"yue\":\"粤语\",\"wyw\":\"文言文\",\"jp\":\"日语\",\"kor\":\"韩语\",\"fra\":\"法语\",\"spa\":\"西班牙语\",\"th\":\"泰语\",\"ara\":\"阿拉伯语\",\"ru\":\"俄语\",\"pt\":\"葡萄牙语\",\"de\":\"德语\",\"it\":\"意大利语\",\"el\":\"希腊语\",\"nl\":\"荷兰语\",\"pl\":\"波兰语\",\"bul\":\"保加利亚语\",\"est\":\"爱沙尼亚语\",\"dan\":\"丹麦语\",\"fin\":\"芬兰语\",\"cs\":\"捷克语\",\"rom\":\"罗马尼亚语\",\"slo\":\"斯洛文尼亚语\",\"swe\":\"瑞典语\",\"hu\":\"匈牙利语\",\"cht\":\"繁体中文\",\"vie\":\"越南语\"}", Map.class);
    Map<String, String> languageNameToCodeMap = JSONUtil.toBean("{\"自动检测\":\"auto\",\"中文（简体）\":\"zh-CN\",\"英语\":\"en\",\"粤语\":\"yue\",\"文言文\":\"wyw\",\"日语\":\"jp\",\"韩语\":\"kor\",\"法语\":\"fra\",\"西班牙语\":\"spa\",\"泰语\":\"th\",\"阿拉伯语\":\"ara\",\"俄语\":\"ru\",\"葡萄牙语\":\"pt\",\"德语\":\"de\",\"意大利语\":\"it\",\"希腊语\":\"el\",\"荷兰语\":\"nl\",\"波兰语\":\"pl\",\"保加利亚语\":\"bul\",\"爱沙尼亚语\":\"est\",\"丹麦语\":\"dan\",\"芬兰语\":\"fin\",\"捷克语\":\"cs\",\"罗马尼亚语\":\"rom\",\"斯洛文尼亚语\":\"slo\",\"瑞典语\":\"swe\",\"匈牙利语\":\"hu\",\"繁体中文\":\"cht\",\"越南语\":\"vie\"}", Map.class);

    String translate(String text, String from, String to);
}
