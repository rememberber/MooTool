package com.luoboduner.moo.tool.util;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;

public class DownloadLinkSelectorTest {

    @Test
    public void selectsAppleSiliconDownloadBeforeGenericMacDownload() {
        DocumentContext links = JsonPath.parse("{"
                + "\"mac\":\"https://example.com/MooTool.dmg\","
                + "\"macSilicon\":\"https://example.com/MooTool-AppleSilicon.dmg\""
                + "}");

        String selected = DownloadLinkSelector.select("Mac OS X", "aarch64", links);

        Assert.assertEquals("https://example.com/MooTool-AppleSilicon.dmg", selected);
    }

    @Test
    public void fallsBackToGenericMacDownloadWhenAppleSiliconDownloadIsMissing() {
        DocumentContext links = JsonPath.parse("{"
                + "\"mac\":\"https://example.com/MooTool.dmg\""
                + "}");

        String selected = DownloadLinkSelector.select("Mac OS X", "aarch64", links);

        Assert.assertEquals("https://example.com/MooTool.dmg", selected);
    }

    @Test
    public void selectsGenericMacDownloadForIntelMac() {
        DocumentContext links = JsonPath.parse("{"
                + "\"mac\":\"https://example.com/MooTool.dmg\","
                + "\"macSilicon\":\"https://example.com/MooTool-AppleSilicon.dmg\""
                + "}");

        String selected = DownloadLinkSelector.select("Mac OS X", "x86_64", links);

        Assert.assertEquals("https://example.com/MooTool.dmg", selected);
    }
}
