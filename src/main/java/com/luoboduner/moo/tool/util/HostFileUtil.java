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
    public static final String NOT_SUPPORTED_TIPS = "暂不支持该操作系统！";

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
        return NOT_SUPPORTED_TIPS;
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
        throw new HostWriteException(NOT_SUPPORTED_TIPS, HostWriteException.Reason.NOT_SUPPORTED);
    }

    public static String getMacPermissionHintHtml() {
        return "<h2>需要管理员权限</h2>"
                + "<p>修改 <code>/etc/hosts</code> 需要管理员权限。</p>"
                + "<p>切换 host 时，macOS 会弹出<strong>系统授权窗口</strong>，请输入当前 Mac 登录密码并确认。</p>"
                + "<p>若刚才取消了授权，请再次点击「切换 host」重试。</p>"
                + "<p>也可以手动修改：在终端执行 <code>sudo nano /etc/hosts</code></p>";
    }

    public static String getLinuxPermissionHint() {
        return "需要管理员或 root 权限才能修改 /etc/hosts。\n\n"
                + "请使用 sudo 启动 MooTool，或在终端执行：\n"
                + "  sudo nano /etc/hosts";
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
                throw new HostWriteException("授权操作超时，请重试。", HostWriteException.Reason.TIMEOUT);
            }
            if (process.exitValue() != 0) {
                if (isUserCanceled(output)) {
                    throw new HostWriteException(getMacPermissionHintHtml(), HostWriteException.Reason.USER_CANCELED);
                }
                String message = StringUtils.isBlank(output) ? "未知错误" : output.trim();
                throw new HostWriteException("写入 hosts 失败：" + message, HostWriteException.Reason.WRITE_FAILED);
            }
        } catch (HostWriteException e) {
            throw e;
        } catch (Exception e) {
            throw new HostWriteException("写入 hosts 失败：" + e.getMessage(), HostWriteException.Reason.WRITE_FAILED, e);
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
