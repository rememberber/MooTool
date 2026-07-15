package com.luoboduner.moo.tool.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 下载并执行 protoc 编译器（跨平台）
 */
class ProtocRunner {

    private static final String PROTOC_VERSION = "3.25.5";
    private static final String MAVEN_BASE = "https://repo.maven.apache.org/maven2/com/google/protobuf/protoc/"
            + PROTOC_VERSION + "/protoc-" + PROTOC_VERSION;
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    private ProtocRunner() {
    }

    static int runProtoc(String[] args) throws IOException, InterruptedException {
        String protocCommand = resolveProtocCommand();
        List<String> command = new ArrayList<>();
        command.add(protocCommand);
        for (String arg : args) {
            command.add(arg);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("protoc 编译失败 (exit " + exitCode + "):\n" + output);
        }
        return exitCode;
    }

    private static String resolveProtocCommand() throws IOException {
        if (isSystemProtocAvailable()) {
            return "protoc";
        }
        return resolveCachedProtocExecutable().toString();
    }

    private static boolean isSystemProtocAvailable() {
        try {
            Process process = new ProcessBuilder("protoc", "--version").start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static Path resolveCachedProtocExecutable() throws IOException {
        String classifier = detectClassifier();
        Path cacheDir = Path.of(SystemUtil.CONFIG_HOME, "protoc", PROTOC_VERSION);
        Files.createDirectories(cacheDir);
        String fileName = SystemUtil.isWindowsOs() ? "protoc.exe" : "protoc";
        Path cached = cacheDir.resolve(fileName);
        if (Files.exists(cached) && Files.size(cached) > 0) {
            return cached;
        }
        String url = MAVEN_BASE + "-" + classifier + ".exe";
        try {
            downloadProtoc(url, cached);
        } catch (IOException e) {
            throw new IOException("无法获取 protoc（请检查网络，或安装 protoc 并加入 PATH）: " + e.getMessage(), e);
        }
        if (!SystemUtil.isWindowsOs()) {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE
            );
            Files.setPosixFilePermissions(cached, perms);
        }
        return cached;
    }

    private static void downloadProtoc(String url, Path target) throws IOException {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(DOWNLOAD_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " (" + url + ")");
            }
            try (InputStream in = response.body()) {
                Files.copy(in, target);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载 protoc 被中断", e);
        }
    }

    private static String detectClassifier() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            if (arch.contains("aarch64") || arch.contains("arm")) {
                return "osx-aarch_64";
            }
            return "osx-x86_64";
        }
        if (os.contains("win")) {
            return "windows-x86_64";
        }
        if (arch.contains("aarch64") || arch.contains("arm")) {
            return "linux-aarch_64";
        }
        return "linux-x86_64";
    }
}
