package com.luoboduner.moo.tool.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and modifies persistent operating-system environment variables.
 */
public final class EnvironmentVariableService {
    private static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern ASSIGNMENT = Pattern.compile("^\\s*(?:export\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.*)$");
    private static final String WINDOWS_USER_KEY = "Environment";
    private static final String WINDOWS_SYSTEM_KEY = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
    private static final String PROFILE_MARKER_START = "# >>> MooTool environment >>>";
    private static final String PROFILE_MARKER_END = "# <<< MooTool environment <<<";
    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(2);

    public enum Scope {
        CURRENT,
        USER,
        SYSTEM
    }

    public record Entry(String key, String value) {
    }

    private EnvironmentVariableService() {
    }

    public static List<Entry> list(Scope scope) throws IOException {
        if (scope == Scope.CURRENT) {
            return System.getenv().entrySet().stream()
                    .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(Entry::key, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        if (SystemUtil.isWindowsOs()) {
            return listWindows(scope);
        }
        return parseEnvironmentContent(readIfExists(environmentFile(scope)));
    }

    public static void set(Scope scope, String key, String value) throws IOException {
        requirePersistentScope(scope);
        validate(key, value);
        if (SystemUtil.isWindowsOs()) {
            writeWindows(scope, key, value);
            return;
        }
        writeUnix(scope, key, value);
    }

    public static void delete(Scope scope, String key) throws IOException {
        requirePersistentScope(scope);
        validateKey(key);
        if (SystemUtil.isWindowsOs()) {
            deleteWindows(scope, key);
            return;
        }
        writeUnix(scope, key, null);
    }

    public static boolean supportsDirectModification() {
        return SystemUtil.isWindowsOs() || SystemUtil.isMacOs() || SystemUtil.isLinuxOs();
    }

    private static List<Entry> listWindows(Scope scope) {
        WinReg.HKEY root = scope == Scope.USER ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE;
        String path = scope == Scope.USER ? WINDOWS_USER_KEY : WINDOWS_SYSTEM_KEY;
        Map<String, Object> values = Advapi32Util.registryGetValues(root, path);
        return values.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), String.valueOf(entry.getValue())))
                .sorted(Comparator.comparing(Entry::key, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void writeWindows(Scope scope, String key, String value) throws IOException {
        String target = scope == Scope.USER ? "User" : "Machine";
        String script = windowsMutationScript(target, key, value);
        if (scope == Scope.SYSTEM) {
            runElevatedPowerShell(script);
        } else {
            runPowerShell(script);
        }
    }

    private static void deleteWindows(Scope scope, String key) throws IOException {
        String target = scope == Scope.USER ? "User" : "Machine";
        String script = windowsMutationScript(target, key, null);
        if (scope == Scope.SYSTEM) {
            runElevatedPowerShell(script);
        } else {
            runPowerShell(script);
        }
    }

    private static String windowsMutationScript(String target, String key, String value) {
        String encodedKey = powershellQuote(key);
        String encodedValue = value == null ? "$null" : "'" + powershellQuote(value) + "'";
        return "[Environment]::SetEnvironmentVariable('" + encodedKey + "'," + encodedValue
                + ",[EnvironmentVariableTarget]::" + target + ")";
    }

    private static void runElevatedPowerShell(String script) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        String outer = "$p=Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru "
                + "-ArgumentList '-NoProfile','-EncodedCommand','" + encoded + "'; exit $p.ExitCode";
        runCommand(List.of("powershell.exe", "-NoProfile", "-Command", outer));
    }

    private static void runPowerShell(String script) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        runCommand(List.of("powershell.exe", "-NoProfile", "-EncodedCommand", encoded));
    }

    private static String powershellQuote(String value) {
        return value.replace("'", "''");
    }

    private static void writeUnix(Scope scope, String key, String value) throws IOException {
        Path destination = environmentFile(scope);
        boolean shellExport = scope == Scope.USER || SystemUtil.isMacOs();
        String current = readIfExists(destination);
        String updated = updateEnvironmentContent(current, key, value, shellExport);
        if (scope == Scope.USER) {
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, updated, StandardCharsets.UTF_8);
            ensureUserProfileSources(destination);
            applyToCurrentMacSession(key, value);
            return;
        }
        writeSystemFile(destination, updated);
    }

    private static Path environmentFile(Scope scope) {
        if (scope == Scope.USER) {
            return Path.of(System.getProperty("user.home"), ".MooTool", "environment");
        }
        return SystemUtil.isMacOs() ? Path.of("/etc/zshenv") : Path.of("/etc/environment");
    }

    private static void ensureUserProfileSources(Path environmentFile) throws IOException {
        Path profile = Path.of(System.getProperty("user.home"), SystemUtil.isMacOs() ? ".zshenv" : ".profile");
        String current = readIfExists(profile);
        if (current.contains(PROFILE_MARKER_START)) {
            return;
        }
        String quotedPath = shellQuote(environmentFile.toAbsolutePath().toString());
        String source = ". " + quotedPath;
        String block = PROFILE_MARKER_START + System.lineSeparator()
                + "[ -f " + quotedPath + " ] && " + source + System.lineSeparator()
                + PROFILE_MARKER_END + System.lineSeparator();
        String separator = current.isEmpty() || current.endsWith("\n") ? "" : System.lineSeparator();
        Files.writeString(profile, current + separator + block, StandardCharsets.UTF_8);
    }

    private static void applyToCurrentMacSession(String key, String value) {
        if (!SystemUtil.isMacOs()) {
            return;
        }
        try {
            List<String> command = value == null
                    ? List.of("/bin/launchctl", "unsetenv", key)
                    : List.of("/bin/launchctl", "setenv", key, value);
            runCommand(command);
        } catch (IOException ignored) {
            // The persistent shell configuration is already saved.
        }
    }

    private static void writeSystemFile(Path destination, String content) throws IOException {
        try {
            Files.writeString(destination, content, StandardCharsets.UTF_8);
            return;
        } catch (IOException directWriteError) {
            Path temporary = Files.createTempFile("mootool-environment-", ".tmp");
            try {
                Files.writeString(temporary, content, StandardCharsets.UTF_8);
                if (SystemUtil.isMacOs()) {
                    String command = "/bin/cp " + shellQuote(temporary.toString()) + " "
                            + shellQuote(destination.toString()) + " && /bin/chmod 644 " + shellQuote(destination.toString());
                    String appleScript = "do shell script \"" + appleScriptQuote(command) + "\" with administrator privileges";
                    runCommand(List.of("/usr/bin/osascript", "-e", appleScript));
                } else if (SystemUtil.isLinuxOs()) {
                    runCommand(List.of("pkexec", "/bin/sh", "-c",
                            "/bin/cp \"$1\" \"$2\" && /bin/chmod 644 \"$2\"", "mootool", temporary.toString(), destination.toString()));
                } else {
                    throw directWriteError;
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void runCommand(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished;
        try {
            finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Environment update was interrupted", error);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Environment update timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            throw new IOException(output.isEmpty() ? "Environment update was canceled or denied" : output);
        }
    }

    static List<Entry> parseEnvironmentContent(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : content.split("\\R")) {
            Matcher matcher = ASSIGNMENT.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            values.put(matcher.group(1), unquote(matcher.group(2).trim()));
        }
        return values.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Entry::key, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static String updateEnvironmentContent(String content, String key, String value, boolean shellExport) {
        validateKey(key);
        if (value != null) {
            validate(key, value);
        }
        List<String> lines = new ArrayList<>(List.of(content.split("\\R", -1)));
        String replacement = value == null ? null : serialize(key, value, shellExport);
        boolean replaced = false;
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = ASSIGNMENT.matcher(line);
            if (matcher.matches() && matcher.group(1).equals(key)) {
                if (!replaced && replacement != null) {
                    result.add(replacement);
                    replaced = true;
                }
                continue;
            }
            result.add(line);
        }
        if (replacement != null && !replaced) {
            while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
                result.remove(result.size() - 1);
            }
            result.add(replacement);
        }
        while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result.isEmpty() ? "" : String.join("\n", result) + "\n";
    }

    private static String serialize(String key, String value, boolean shellExport) {
        if (shellExport) {
            return "export " + key + "='" + value.replace("'", "'\\''") + "'";
        }
        return key + "=\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1).replace("'\\''", "'");
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }

    private static String readIfExists(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
    }

    private static void validate(String key, String value) {
        validateKey(key);
        if (value == null || value.length() > 65_535 || value.indexOf('\0') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid environment variable value");
        }
    }

    private static void validateKey(String key) {
        if (key == null || !VARIABLE_NAME.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid environment variable name");
        }
    }

    private static void requirePersistentScope(Scope scope) {
        if (scope == Scope.CURRENT) {
            throw new IllegalArgumentException("Current process environment is read-only");
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String appleScriptQuote(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
