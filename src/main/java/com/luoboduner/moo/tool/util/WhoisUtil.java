package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Validator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WHOIS 查询工具（TCP 43 端口）
 */
public class WhoisUtil {

    private static final int WHOIS_PORT = 43;
    private static final int TIMEOUT_MS = 15000;

    private static final Pattern REFER_PATTERN = Pattern.compile("(?im)^(?:refer|whois|ReferralServer):\\s*(?:whois://)?(\\S+)");
    private static final Map<String, String> TLD_SERVERS = new HashMap<>();

    static {
        TLD_SERVERS.put("com", "whois.verisign-grs.com");
        TLD_SERVERS.put("net", "whois.verisign-grs.com");
        TLD_SERVERS.put("org", "whois.pir.org");
        TLD_SERVERS.put("info", "whois.afilias.net");
        TLD_SERVERS.put("biz", "whois.biz");
        TLD_SERVERS.put("cn", "whois.cnnic.cn");
        TLD_SERVERS.put("io", "whois.nic.io");
        TLD_SERVERS.put("me", "whois.nic.me");
        TLD_SERVERS.put("cc", "whois.nic.cc");
        TLD_SERVERS.put("tv", "whois.nic.tv");
        TLD_SERVERS.put("us", "whois.nic.us");
        TLD_SERVERS.put("uk", "whois.nominet.uk");
        TLD_SERVERS.put("de", "whois.denic.de");
        TLD_SERVERS.put("jp", "whois.jprs.jp");
        TLD_SERVERS.put("ru", "whois.tcinet.ru");
        TLD_SERVERS.put("top", "whois.nic.top");
        TLD_SERVERS.put("xyz", "whois.nic.xyz");
    }

    private WhoisUtil() {
    }

    public static String query(String input) throws IOException {
        String query = normalizeQuery(input);
        if (query.isEmpty()) {
            throw new IllegalArgumentException("查询内容不能为空");
        }

        if (Validator.isIpv4(query) || Validator.isIpv6(query)) {
            return queryIp(query);
        }
        return queryDomain(query);
    }

    private static String normalizeQuery(String input) {
        String query = input.trim();
        if (query.startsWith("http://")) {
            query = query.substring(7);
        } else if (query.startsWith("https://")) {
            query = query.substring(8);
        }
        int slashIndex = query.indexOf('/');
        if (slashIndex > 0) {
            query = query.substring(0, slashIndex);
        }
        return query.trim();
    }

    private static String queryIp(String ip) throws IOException {
        String result = queryServer("whois.arin.net", "n " + ip);
        String referral = parseReferral(result);
        if (referral != null && !referral.equalsIgnoreCase("whois.arin.net")) {
            result = queryServer(referral, ip);
        }
        return result.trim();
    }

    private static String queryDomain(String domain) throws IOException {
        String tld = extractTld(domain);
        String server = TLD_SERVERS.get(tld);
        if (server == null) {
            String ianaResult = queryServer("whois.iana.org", tld);
            server = parseReferral(ianaResult);
            if (server == null) {
                return ianaResult.trim();
            }
        }
        return queryServer(server, domain).trim();
    }

    private static String extractTld(String domain) {
        int dotIndex = domain.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == domain.length() - 1) {
            throw new IllegalArgumentException("无效的域名或 IP：" + domain);
        }
        return domain.substring(dotIndex + 1).toLowerCase();
    }

    private static String queryServer(String server, String query) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server, WHOIS_PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            out.write((query + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            return IoUtil.read(socket.getInputStream(), StandardCharsets.UTF_8);
        }
    }

    private static String parseReferral(String response) {
        Matcher matcher = REFER_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
