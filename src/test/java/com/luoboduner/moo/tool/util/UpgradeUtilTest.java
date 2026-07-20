package com.luoboduner.moo.tool.util;

import com.alibaba.fastjson.JSON;
import com.luoboduner.moo.tool.bean.VersionSummary;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpgradeUtilTest {

    @Test
    public void returnsEveryReleaseAfterCurrentVersionInIndexOrder() {
        VersionSummary summary = summary(
                "v1.10.0",
                Map.of("v1.8.0", "1", "v1.9.0", "2", "v1.10.0", "3"),
                List.of(version("v1.10.0"), version("v1.8.0"), version("v1.9.0"))
        );

        List<VersionSummary.Version> changes = UpgradeUtil.versionChangesAfter(summary, "v1.8.0");

        assertEquals(List.of("v1.9.0", "v1.10.0"), changes.stream().map(VersionSummary.Version::getVersion).toList());
    }

    @Test
    public void reportsNoUpdateWhenCurrentVersionIsLatest() {
        VersionSummary summary = summary(
                "v1.10.0",
                Map.of("v1.10.0", "1"),
                List.of(version("v1.10.0"))
        );

        assertEquals(List.of(), UpgradeUtil.versionChangesAfter(summary, "v1.10.0"));
    }

    @Test
    public void rejectsAnIncompleteReleaseHistory() {
        VersionSummary summary = summary(
                "v1.10.0",
                Map.of("v1.8.0", "1", "v1.9.0", "2", "v1.10.0", "3"),
                List.of(version("v1.8.0"), version("v1.10.0"))
        );

        assertThrows(IllegalStateException.class, () -> UpgradeUtil.versionChangesAfter(summary, "v1.8.0"));
    }

    private static VersionSummary summary(
            String currentVersion,
            Map<String, String> indexes,
            List<VersionSummary.Version> versions
    ) {
        VersionSummary summary = new VersionSummary();
        summary.setCurrentVersion(currentVersion);
        summary.setVersionIndex(JSON.toJSONString(new LinkedHashMap<>(indexes)));
        summary.setVersionDetailList(versions);
        return summary;
    }

    private static VersionSummary.Version version(String value) {
        VersionSummary.Version version = new VersionSummary.Version();
        version.setVersion(value);
        version.setTitle(value);
        version.setLog(value);
        return version;
    }
}
