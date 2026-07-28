package com.luoboduner.moo.tool.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded, shell-free helpers used by the desktop network diagnostics page.
 */
public final class NetworkScanUtil {

    private static final Pattern IPV4_PREFIX = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.?$");
    private static final Pattern PORT_RANGE = Pattern.compile("^(\\d{1,5})\\s*-\\s*(\\d{1,5})$");
    private static final int MAX_CUSTOM_PORTS = 4096;
    private static final int IP_SCAN_CONCURRENCY = 32;
    private static final int PORT_SCAN_CONCURRENCY = 96;

    private static final Map<Integer, String> COMMON_PORTS;

    static {
        Map<Integer, String> ports = new LinkedHashMap<>();
        ports.put(20, "ftp-data");
        ports.put(21, "ftp");
        ports.put(22, "ssh");
        ports.put(23, "telnet");
        ports.put(25, "smtp");
        ports.put(53, "dns");
        ports.put(67, "dhcp");
        ports.put(68, "dhcp");
        ports.put(69, "tftp");
        ports.put(80, "http");
        ports.put(110, "pop3");
        ports.put(123, "ntp");
        ports.put(135, "msrpc");
        ports.put(139, "netbios");
        ports.put(143, "imap");
        ports.put(161, "snmp");
        ports.put(389, "ldap");
        ports.put(443, "https");
        ports.put(445, "smb");
        ports.put(465, "smtps");
        ports.put(587, "smtp-submission");
        ports.put(631, "ipp");
        ports.put(636, "ldaps");
        ports.put(873, "rsync");
        ports.put(993, "imaps");
        ports.put(995, "pop3s");
        ports.put(1080, "socks");
        ports.put(1433, "mssql");
        ports.put(1521, "oracle");
        ports.put(2049, "nfs");
        ports.put(2181, "zookeeper");
        ports.put(2375, "docker");
        ports.put(3000, "http-alt");
        ports.put(3306, "mysql");
        ports.put(3389, "rdp");
        ports.put(5432, "postgresql");
        ports.put(5601, "kibana");
        ports.put(5672, "amqp");
        ports.put(5900, "vnc");
        ports.put(6379, "redis");
        ports.put(8080, "http-alt");
        ports.put(8081, "http-alt");
        ports.put(8443, "https-alt");
        ports.put(8888, "http-alt");
        ports.put(9092, "kafka");
        ports.put(9200, "elasticsearch");
        ports.put(9300, "elasticsearch");
        ports.put(11211, "memcached");
        ports.put(27017, "mongodb");
        COMMON_PORTS = Collections.unmodifiableMap(ports);
    }

    private NetworkScanUtil() {
    }

    /**
     * Expands a /24-style prefix such as {@code 192.168.10} to host addresses .1-.254.
     */
    public static List<String> expandIpv4Prefix(String value) {
        Matcher matcher = IPV4_PREFIX.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(I18n.get("net.error.invalidIpRange"));
        }
        for (int index = 1; index <= 3; index++) {
            if (Integer.parseInt(matcher.group(index)) > 255) {
                throw new IllegalArgumentException(I18n.get("net.error.invalidIpRange"));
            }
        }
        String prefix = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        List<String> addresses = new ArrayList<>(254);
        for (int host = 1; host <= 254; host++) {
            addresses.add(prefix + "." + host);
        }
        return addresses;
    }

    /**
     * Parses comma-separated ports and ranges. Blank input selects the built-in common-port list.
     */
    public static List<Integer> parsePorts(String value) {
        String input = value == null ? "" : value.trim();
        if (input.isEmpty()) {
            return List.copyOf(COMMON_PORTS.keySet());
        }
        TreeSet<Integer> ports = new TreeSet<>();
        for (String tokenValue : input.split(",")) {
            String token = tokenValue.trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException(I18n.get("net.error.invalidPorts"));
            }
            Matcher range = PORT_RANGE.matcher(token);
            if (range.matches()) {
                int start = parsePort(range.group(1));
                int end = parsePort(range.group(2));
                if (start > end || end - start + 1 > MAX_CUSTOM_PORTS) {
                    throw new IllegalArgumentException(I18n.get("net.error.invalidPorts"));
                }
                for (int port = start; port <= end; port++) {
                    ports.add(port);
                }
            } else {
                ports.add(parsePort(token));
            }
            if (ports.size() > MAX_CUSTOM_PORTS) {
                throw new IllegalArgumentException(I18n.get("net.error.tooManyPorts"));
            }
        }
        return List.copyOf(ports);
    }

    public static List<String> scanReachableHosts(String prefix, int timeoutMillis) throws InterruptedException {
        List<String> addresses = expandIpv4Prefix(prefix);
        return scan(addresses, IP_SCAN_CONCURRENCY,
                address -> pingOnce(address, timeoutMillis), address -> address);
    }

    public static List<PortResult> scanOpenPorts(String host, String portExpression, int timeoutMillis)
            throws InterruptedException {
        String target = normalizeHost(host);
        List<Integer> ports = parsePorts(portExpression);
        return scan(ports, PORT_SCAN_CONCURRENCY, port -> canConnect(target, port, timeoutMillis),
                port -> new PortResult(port, COMMON_PORTS.getOrDefault(port, "")));
    }

    public static String serviceName(int port) {
        return COMMON_PORTS.getOrDefault(port, "");
    }

    private static int parsePort(String value) {
        if (!value.matches("\\d{1,5}")) {
            throw new IllegalArgumentException(I18n.get("net.error.invalidPorts"));
        }
        int port = Integer.parseInt(value);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(I18n.get("net.error.invalidPorts"));
        }
        return port;
    }

    private static String normalizeHost(String value) {
        String host = value == null ? "" : value.trim();
        if (host.isEmpty() || host.length() > 253 || !host.matches("[a-zA-Z0-9._:-]+")) {
            throw new IllegalArgumentException(I18n.get("net.error.invalidHost"));
        }
        return host;
    }

    private static boolean pingOnce(String address, int timeoutMillis) {
        int timeout = Math.max(200, Math.min(5000, timeoutMillis));
        List<String> command = new ArrayList<>();
        command.add(SystemUtil.isWindowsOs() ? "ping.exe" : "/sbin/ping");
        if (SystemUtil.isWindowsOs()) {
            Collections.addAll(command, "-n", "1", "-w", String.valueOf(timeout), address);
        } else if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            Collections.addAll(command, "-c", "1", "-W", String.valueOf(timeout), address);
        } else {
            Collections.addAll(command, "-c", "1", "-W",
                    String.valueOf(Math.max(1, (int) Math.ceil(timeout / 1000.0))), address);
        }
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(timeout + 1000L, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            try {
                return InetAddress.getByName(address).isReachable(timeout);
            } catch (IOException ignored) {
                return false;
            }
        } catch (InterruptedException ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean canConnect(String host, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.max(100, Math.min(5000, timeoutMillis)));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static <T, R> List<R> scan(
            List<T> targets, int concurrency, Probe<T> probe, ResultFactory<T, R> resultFactory)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(concurrency, targets.size()),
                runnable -> {
                    Thread thread = new Thread(runnable, "mootool-network-scan");
                    thread.setDaemon(true);
                    return thread;
                });
        List<Future<R>> futures = new ArrayList<>(targets.size());
        try {
            for (T target : targets) {
                futures.add(executor.submit(() -> probe.test(target) ? resultFactory.create(target) : null));
            }
            List<R> results = new ArrayList<>();
            for (Future<R> future : futures) {
                try {
                    R result = future.get();
                    if (result != null) {
                        results.add(result);
                    }
                } catch (ExecutionException ignored) {
                    // A single unreachable host/port must not abort the rest of the diagnostic scan.
                }
            }
            return results;
        } catch (InterruptedException ex) {
            for (Future<R> future : futures) {
                future.cancel(true);
            }
            throw ex;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface Probe<T> {
        boolean test(T value);
    }

    @FunctionalInterface
    private interface ResultFactory<T, R> {
        R create(T value);
    }

    public record PortResult(int port, String service) {
    }
}
