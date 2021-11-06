package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.ui.form.func.NetForm;
import com.luoboduner.moo.tool.util.SystemUtil;
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
        netForm.getIpConfigButton().addActionListener(e -> {
            try {

                String ipConfigStr;
                if (SystemUtil.isWindowsOs()) {
                    ipConfigStr = RuntimeUtil.execForStr("ipconfig");
                } else {
                    ipConfigStr = RuntimeUtil.execForStr("ifconfig");
                }
                netForm.getIpConfigTextArea().setText(ipConfigStr);
                netForm.getIpConfigTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        netForm.getIpConfigAllButton().addActionListener(e -> {
            try {
                String ipConfigStr;
                if (SystemUtil.isWindowsOs()) {
                    ipConfigStr = RuntimeUtil.execForStr("ipconfig /all");
                } else {
                    ipConfigStr = RuntimeUtil.execForStr("netstat -nat");
                }
                netForm.getIpConfigTextArea().setText(ipConfigStr);
                netForm.getIpConfigTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        netForm.getFlushDnsButton().addActionListener(e -> {
            try {
                String flushDnsStr;
                if (SystemUtil.isWindowsOs()) {
                    flushDnsStr = RuntimeUtil.execForStr("ipconfig /flushdns");
                } else {
                    flushDnsStr = RuntimeUtil.execForStr("killall -HUP mDNSResponder");
                }
                netForm.getIpConfigTextArea().setText(flushDnsStr);
                netForm.getIpConfigTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
        netForm.getIpv4ToLongButton().addActionListener(e -> {
            try {
                String ipv4 = netForm.getIpv4TextField().getText().trim();
                long ipv4Long = NetUtil.ipv4ToLong(ipv4);
                netForm.getLongTextField().setText(String.valueOf(ipv4Long));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        netForm.getRefreshIpv4ListButton().addActionListener(e -> {
            try {
                LinkedHashSet<String> ipv4Set = NetUtil.localIpv4s();
                netForm.getIpv4ListTextArea().setText(String.join("\n", ipv4Set));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "刷新失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        netForm.getRefreshIpv6ListButton().addActionListener(e -> {
            try {
                LinkedHashSet<String> ipv6Set = NetUtil.localIpv6s();
                netForm.getIpv6ListTextArea().setText(String.join("\n", ipv6Set));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "刷新失败！", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "获取失败！", JOptionPane.ERROR_MESSAGE);
            }
        });

        // PING
        netForm.getPingButton().addActionListener(e -> {
            try {
                String pingIp = netForm.getPingTextField().getText().trim();
                netForm.getIpConfigTextArea().setText("");
//                if (SystemUtil.isWindowsOs()) {
                Process process = RuntimeUtil.exec("ping " + pingIp);
                InputStream inputStream = process.getInputStream();
                ThreadUtil.execute(() -> {
                    InputStreamReader inputStreamReader = null;
                    BufferedReader bufferedReader = null;
                    try {
                        inputStreamReader = new InputStreamReader(inputStream, CharsetUtil.GBK);
                        bufferedReader = new BufferedReader(inputStreamReader);
                        String line = null;
                        while (true) {
                            try {
                                if ((line = bufferedReader.readLine()) == null) {
                                    break;
                                }
                            } catch (IOException ex) {
                                logger.error(ExceptionUtils.getStackTrace(ex));
                            }
                            netForm.getIpConfigTextArea().append(line + "\n");
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
//                } else {
//                }
                netForm.getIpConfigTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(netForm.getNetPanel(), ex.getMessage(), "失败！", JOptionPane.ERROR_MESSAGE);
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        });
    }
}
