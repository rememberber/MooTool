package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.ui.form.func.NetForm;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.SystemUtil;
import com.luoboduner.moo.tool.util.WhoisUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;

/**
 * <pre>
 * NetListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/1.
 */
public class NetListener {

    private static final Log logger = LogFactory.get();

    public static void addListeners() {

        NetForm netForm = NetForm.getInstance();
        netForm.getIpConfigButton().addActionListener(e -> refreshIpConfigAsync());
        netForm.getIpConfigAllButton().addActionListener(e -> refreshIpConfigAllAsync());
        netForm.getFlushDnsButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                try {
                    String flushDnsStr;
                    if (SystemUtil.isWindowsOs()) {
                        flushDnsStr = RuntimeUtil.execForStr("ipconfig /flushdns");
                    } else {
                        flushDnsStr = RuntimeUtil.execForStr("killall -HUP mDNSResponder");
                    }
                    final String result = flushDnsStr;
                    SwingUtilities.invokeLater(() -> {
                        netForm.getIpConfigTextArea().setText(result);
                        netForm.getIpConfigTextArea().setCaretPosition(0);
                    });
                } catch (Exception ex) {
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            });
        });
        netForm.getIpv4ToLongButton().addActionListener(e -> {
            try {
                String ipv4 = netForm.getIpv4TextField().getText().trim();
                long ipv4Long = NetUtil.ipv4ToLong(ipv4);
                netForm.getLongTextField().setText(String.valueOf(ipv4Long));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.convertFailedTitle", ex.getMessage());
            }
        });
        netForm.getLongToIpv4Button().addActionListener(e -> {
            try {
                String ipv4Long = netForm.getLongTextField().getText().trim();
                String ipv4 = NetUtil.longToIpv4(Long.parseLong(ipv4Long));
                netForm.getIpv4TextField().setText(ipv4);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.convertFailedTitle", ex.getMessage());
            }
        });
        netForm.getRefreshIpv4ListButton().addActionListener(e -> {
            try {
                LinkedHashSet<String> ipv4Set = NetUtil.localIpv4s();
                netForm.getIpv4ListTextArea().setText(String.join("\n", ipv4Set));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.refreshFailedTitle", ex.getMessage());
            }
        });
        netForm.getRefreshIpv6ListButton().addActionListener(e -> {
            try {
                LinkedHashSet<String> ipv6Set = NetUtil.localIpv6s();
                netForm.getIpv6ListTextArea().setText(String.join("\n", ipv6Set));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.refreshFailedTitle", ex.getMessage());
            }
        });
        netForm.getHostToIpButton().addActionListener(e -> {
            try {
                String hostStr = netForm.getHostTextField().getText().trim();
                String ipByHost = NetUtil.getIpByHost(hostStr);
                netForm.getIpTextField().setText(ipByHost);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.getFailedTitle", ex.getMessage());
            }
        });

        // PING
        netForm.getPingButton().addActionListener(e -> {
            try {
                String pingIp = netForm.getPingTextField().getText().trim();
                netForm.getIpConfigTextArea().setText("");
                Process process = RuntimeUtil.exec("ping " + pingIp);
                InputStream inputStream = process.getInputStream();
                ThreadUtil.execute(() -> {
                    InputStreamReader inputStreamReader = null;
                    BufferedReader bufferedReader = null;
                    try {
                        inputStreamReader = new InputStreamReader(inputStream, CharsetUtil.GBK);
                        bufferedReader = new BufferedReader(inputStreamReader);
                        String line;
                        while (true) {
                            try {
                                if ((line = bufferedReader.readLine()) == null) {
                                    break;
                                }
                            } catch (IOException ex) {
                                logger.error(ExceptionUtils.getStackTrace(ex));
                                break;
                            }
                            final String lineToAppend = line;
                            SwingUtilities.invokeLater(() ->
                                    netForm.getIpConfigTextArea().append(lineToAppend + "\n"));
                        }
                    } catch (UnsupportedEncodingException ex) {
                        logger.error(ExceptionUtils.getStackTrace(ex));
                    } finally {
                        process.destroy();
                        if (inputStreamReader != null) {
                            try {
                                inputStreamReader.close();
                            } catch (IOException ex) {
                                logger.error(ExceptionUtils.getStackTrace(ex));
                            }
                        }
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException ex) {
                                logger.error(ExceptionUtils.getStackTrace(ex));
                            }
                        }
                    }

                });
                netForm.getIpConfigTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                MsgUtil.errorDetail(netForm.getNetPanel(), "msg.failedTitle", ex.getMessage());
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });

        // WHOIS
        netForm.getWhoisButton().addActionListener(e -> {
            String query = netForm.getWhoisTextField().getText().trim();
            if (query.isEmpty()) {
                MsgUtil.info(netForm.getNetPanel(), "msg.enterDomainOrIp");
                return;
            }
            netForm.getWhoisButton().setEnabled(false);
            netForm.getIpConfigTextArea().setText(I18n.get("net.querying"));
            ThreadUtil.execute(() -> {
                try {
                    String result = WhoisUtil.query(query);
                    SwingUtilities.invokeLater(() -> {
                        netForm.getIpConfigTextArea().setText(result);
                        netForm.getIpConfigTextArea().setCaretPosition(0);
                        netForm.getWhoisButton().setEnabled(true);
                    });
                } catch (Exception ex) {
                    logger.error(ExceptionUtils.getStackTrace(ex));
                    SwingUtilities.invokeLater(() -> {
                        MsgUtil.errorDetail(netForm.getNetPanel(), "msg.queryFailedTitle", ex.getMessage());
                        netForm.getWhoisButton().setEnabled(true);
                    });
                }
            });
        });
    }

    public static void refreshIpConfigAsync() {
        NetForm netForm = NetForm.getInstance();
        ThreadUtil.execute(() -> {
            try {
                String ipConfigStr;
                if (SystemUtil.isWindowsOs()) {
                    ipConfigStr = RuntimeUtil.execForStr("ipconfig");
                } else {
                    ipConfigStr = RuntimeUtil.execForStr("ifconfig");
                }
                final String result = ipConfigStr;
                SwingUtilities.invokeLater(() -> {
                    netForm.getIpConfigTextArea().setText(result);
                    netForm.getIpConfigTextArea().setCaretPosition(0);
                });
            } catch (Exception ex) {
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
    }

    public static void refreshIpConfigAllAsync() {
        NetForm netForm = NetForm.getInstance();
        ThreadUtil.execute(() -> {
            try {
                String ipConfigStr;
                if (SystemUtil.isWindowsOs()) {
                    ipConfigStr = RuntimeUtil.execForStr("ipconfig /all");
                } else {
                    ipConfigStr = RuntimeUtil.execForStr("netstat -nat");
                }
                final String result = ipConfigStr;
                SwingUtilities.invokeLater(() -> {
                    netForm.getIpConfigTextArea().setText(result);
                    netForm.getIpConfigTextArea().setCaretPosition(0);
                });
            } catch (Exception ex) {
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
    }
}
