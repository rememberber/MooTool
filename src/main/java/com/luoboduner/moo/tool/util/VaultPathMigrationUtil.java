package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Vault 目录路径变更时，将旧目录内容复制到新目录。
 */
@Slf4j
public final class VaultPathMigrationUtil {

    private VaultPathMigrationUtil() {
    }

    public static void migrateIfChanged(String oldConfiguredPath, String newConfiguredPath, String defaultPath) {
        File source = effectiveVaultDir(oldConfiguredPath, defaultPath);
        File target = effectiveVaultDir(newConfiguredPath, defaultPath);
        if (samePath(source, target)) {
            return;
        }
        Path sourcePath = source.toPath().toAbsolutePath().normalize();
        Path targetPath = target.toPath().toAbsolutePath().normalize();
        if (targetPath.startsWith(sourcePath)) {
            throw new IllegalArgumentException("新 Vault 路径不能位于原 Vault 目录内");
        }
        if (!source.isDirectory()) {
            log.debug("Vault migration skipped, source missing: {}", source);
            return;
        }
        File[] children = source.listFiles();
        if (children == null || children.length == 0) {
            return;
        }
        FileUtil.mkdir(target);
        FileUtil.copyContent(source, target, false);
        log.info("Migrated vault from {} to {}", source.getAbsolutePath(), target.getAbsolutePath());
    }

    private static File effectiveVaultDir(String configuredPath, String defaultPath) {
        String path = StringUtils.isNotBlank(configuredPath) ? configuredPath.trim() : defaultPath;
        return new File(path).getAbsoluteFile();
    }

    private static boolean samePath(File left, File right) {
        try {
            return left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException e) {
            return left.getAbsoluteFile().equals(right.getAbsoluteFile());
        }
    }
}
