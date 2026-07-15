package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 系统 hosts 文件读写
 */
@Slf4j
public class HostFileUtil {

    public static final String WIN_HOST_FILE_PATH = "C:\\Windows\\System32\\drivers\\etc\\hosts";
    public static final String MAC_HOST_FILE_PATH = "/etc/hosts";
    public static final String LINUX_HOST_FILE_PATH = "/etc/hosts";
    public static String getNotSupportedTips() {
        return I18n.get("host.notSupported");
    }

    private static final int MAC_AUTH_TIMEOUT_SECONDS = 120;

    private HostFileUtil() {
    }

    public static String readSystemHosts() {
        if (SystemUtil.isWindowsOs()) {
            return FileUtil.readUtf8String(WIN_HOST_FILE_PATH);
        }
        if (SystemUtil.isMacOs()) {
            return FileUtil.readUtf8String(MAC_HOST_FILE_PATH);
        }
        if (SystemUtil.isLinuxOs()) {
            return FileUtil.readUtf8String(LINUX_HOST_FILE_PATH);
        }
        return getNotSupportedTips();
    }

    public static void writeSystemHosts(String content) throws HostWriteException {
        if (SystemUtil.isWindowsOs()) {
            FileUtil.writeUtf8String(content, WIN_HOST_FILE_PATH);
            return;
        }
        if (SystemUtil.isMacOs()) {
            writeMacHosts(content);
            return;
        }
        if (SystemUtil.isLinuxOs()) {
            writeLinuxHosts(content);
            return;
        }
        throw new HostWriteException(getNotSupportedTips(), HostWriteException.Reason.NOT_SUPPORTED);
    }

    public static String getMacPermissionHintHtml() {
        return I18n.get("host.macPermissionHintHtml");
    }

    public static String getLinuxPermissionHint() {
        return I18n.get("host.linuxPermissionHint");
    }

    private static void writeMacHosts(String content) throws HostWriteException {
        try {
            FileUtil.writeUtf8String(content, MAC_HOST_FILE_PATH);
        } catch (Exception directWriteError) {
            log.debug("Direct write to /etc/hosts failed, trying administrator authorization", directWriteError);
            writeMacHostsWithAuthorization(content);
        }
    }

    private static void writeMacHostsWithAuthorization(String content) throws HostWriteException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("mootool-hosts-", ".tmp");
            FileUtil.writeUtf8String(content, tempFile);
            String escapedPath = escapeForAppleScriptShell(tempFile.getAbsolutePath());
            String script = "do shell script \"cp '" + escapedPath + "' /etc/hosts && chmod 644 /etc/hosts\" "
                    + "with administrator privileges";

            ProcessBuilder processBuilder = new ProcessBuilder("osascript", "-e", script);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = IoUtil.read(process.getInputStream(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(MAC_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new HostWriteException(I18n.get("host.authTimeout"), HostWriteException.Reason.TIMEOUT);
            }
            if (process.exitValue() != 0) {
                if (isUserCanceled(output)) {
                    throw new HostWriteException(getMacPermissionHintHtml(), HostWriteException.Reason.USER_CANCELED);
                }
                String message = StringUtils.isBlank(output) ? I18n.get("host.unknownError") : output.trim();
                throw new HostWriteException(I18n.format("host.writeFailed", message), HostWriteException.Reason.WRITE_FAILED);
            }
        } catch (HostWriteException e) {
            throw e;
        } catch (Exception e) {
            throw new HostWriteException(I18n.format("host.writeFailed", e.getMessage()),
                    HostWriteException.Reason.WRITE_FAILED, e);
        } finally {
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }

    private static void writeLinuxHosts(String content) throws HostWriteException {
        try {
            FileUtil.writeUtf8String(content, LINUX_HOST_FILE_PATH);
        } catch (Exception e) {
            throw new HostWriteException(getLinuxPermissionHint(), HostWriteException.Reason.PERMISSION_DENIED, e);
        }
    }

    private static boolean isUserCanceled(String output) {
        if (StringUtils.isBlank(output)) {
            return false;
        }
        return output.contains("User canceled") || output.contains("(-128)");
    }

    static String escapeForAppleScriptShell(String path) {
        return path.replace("'", "'\\''");
    }

    @Getter
    public static class HostWriteException extends Exception {
        private final Reason reason;

        public HostWriteException(String message, Reason reason) {
            super(message);
            this.reason = reason;
        }

        public HostWriteException(String message, Reason reason, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public enum Reason {
            NOT_SUPPORTED,
            PERMISSION_DENIED,
            USER_CANCELED,
            TIMEOUT,
            WRITE_FAILED
        }
    }
}
