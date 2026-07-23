package com.luoboduner.moo.tool.ui.startup;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.base.Strings;
import com.luoboduner.moo.tool.bean.ContributorInfo;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.util.ImageDisplayUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * About 页后台加载：贡献者列表与头像预解码。
 */
@Slf4j
@Getter
public final class AboutLoadData {

    private final ContributorInfo contributorInfo;
    private final Map<String, BufferedImage> avatarsByName;

    public AboutLoadData(ContributorInfo contributorInfo, Map<String, BufferedImage> avatarsByName) {
        this.contributorInfo = contributorInfo;
        this.avatarsByName = avatarsByName == null ? Map.of() : Map.copyOf(avatarsByName);
    }

    public static AboutLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        ContributorInfo info = fetchContributorInfo();
        Map<String, BufferedImage> avatars = new LinkedHashMap<>();
        if (info != null && info.getContributorList() != null) {
            for (ContributorInfo.Contributor contributor : info.getContributorList()) {
                if (contributor == null || Strings.isNullOrEmpty(contributor.getName())
                        || Strings.isNullOrEmpty(contributor.getAvatarUrl())) {
                    continue;
                }
                try {
                    BufferedImage avatar = ImageDisplayUtil.readImage(new URL(contributor.getAvatarUrl()));
                    if (avatar != null) {
                        avatars.put(contributor.getName(), avatar);
                    }
                } catch (Exception e) {
                    log.warn("预加载贡献者 {} 头像失败", contributor.getName(), e);
                }
            }
        }
        return new AboutLoadData(info, avatars);
    }

    public List<ContributorInfo.Contributor> contributors() {
        if (contributorInfo == null || contributorInfo.getContributorList() == null) {
            return List.of();
        }
        return new ArrayList<>(contributorInfo.getContributorList());
    }

    private static ContributorInfo fetchContributorInfo() {
        try {
            String remoteContent = HttpUtil.get(UiConsts.CONTRIBUTOR_URL, 10000);
            ContributorInfo remoteInfo = parseContributorInfo(remoteContent);
            if (remoteInfo != null) {
                return remoteInfo;
            }
        } catch (Exception e) {
            log.warn("远程获取贡献者列表失败，尝试使用内置数据", e);
        }
        try {
            return parseContributorInfo(readBundledContributorJson());
        } catch (Exception e) {
            log.error("读取内置贡献者列表失败", e);
            return null;
        }
    }

    private static ContributorInfo parseContributorInfo(String content) {
        if (Strings.isNullOrEmpty(content)) {
            return null;
        }
        ContributorInfo contributorInfo = JSON.parseObject(content, ContributorInfo.class);
        if (contributorInfo == null || contributorInfo.getContributorList() == null
                || contributorInfo.getContributorList().isEmpty()) {
            return null;
        }
        return contributorInfo;
    }

    private static String readBundledContributorJson() throws Exception {
        try (InputStream in = AboutLoadData.class.getResourceAsStream("/contributor.json")) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
