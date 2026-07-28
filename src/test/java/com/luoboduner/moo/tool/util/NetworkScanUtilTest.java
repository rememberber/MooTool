package com.luoboduner.moo.tool.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.ServerSocket;
import java.util.List;

public class NetworkScanUtilTest {

    @Test
    public void expandsIpv4PrefixToUsableHostRange() {
        List<String> addresses = NetworkScanUtil.expandIpv4Prefix("192.168.10");
        Assert.assertEquals(254, addresses.size());
        Assert.assertEquals("192.168.10.1", addresses.get(0));
        Assert.assertEquals("192.168.10.254", addresses.get(253));
    }

    @Test
    public void rejectsInvalidIpv4Prefix() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkScanUtil.expandIpv4Prefix("192.168.999"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkScanUtil.expandIpv4Prefix("192.168"));
    }

    @Test
    public void parsesPortsAndRangesInOrder() {
        Assert.assertEquals(List.of(22, 80, 81, 82, 3306),
                NetworkScanUtil.parsePorts("3306,80-82,22"));
        Assert.assertTrue(NetworkScanUtil.parsePorts("").containsAll(List.of(22, 3306, 5432, 6379)));
    }

    @Test
    public void findsAnOpenTcpPort() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            List<NetworkScanUtil.PortResult> results = NetworkScanUtil.scanOpenPorts(
                    "127.0.0.1", String.valueOf(server.getLocalPort()), 500);
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(server.getLocalPort(), results.get(0).port());
        }
    }
}
