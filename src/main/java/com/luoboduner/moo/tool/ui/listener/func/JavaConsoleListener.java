package com.luoboduner.moo.tool.ui.listener.func;

import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

@Getter
public class JavaConsoleListener {

    private static String TMP_FILE = System.getProperty("java.io.tmpdir") + File.separator + "temp.groovy";
    private StringBuffer log;

    private JButton run;
    private JButton clean;

    private JTextArea codeArea;
    private JTextArea resultArea;

    public JavaConsoleListener(JButton run, JButton clean, JTextArea codeArea, JTextArea resultArea) {
        this.run = run;
        this.clean = clean;
        this.codeArea = codeArea;
        this.resultArea = resultArea;
        this.log = new StringBuffer();
    }

    public void addListener() {
        this.executeClickEventListener();
        this.cleanClickEventListener();
    }

    private void cleanClickEventListener() {
        this.clean.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                getResultArea().setText("");
                log = new StringBuffer();
            }
        });
    }

    private void executeClickEventListener() {
        run.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String code = getCodeArea().getText();
                if (StringUtils.isBlank(code)) {
                    return;
                }

                try {
                    addStr("----- " + DateFormatUtils.format(new Date(), TimeConvertForm.TIME_FORMAT) + " -----");
                    Object res = compileAndExecute(code);
                    if (null != res) {
                        addStr(res.toString());
                    }
                } catch (Exception ex) {
                    logException(ex);
                }
                getResultArea().setText(log.toString());
            }
        });
    }

    private Object compileAndExecute(String code) throws Exception {
        GroovyShell shell = new GroovyShell();
        File file = new File(TMP_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }
        writeFile(file, code);
        Script parse = shell.parse(new FileReader(file));
        return changeOut(() -> parse.run());
    }

    private void addStr(String str) {
        if (StringUtils.isNotBlank(str)) {
            log.append(str + "\r\n");
        }
    }

    private void writeFile(File file, String data) throws Exception {
        Objects.requireNonNull(file, "临时文件不能为空！");
        Objects.requireNonNull(data, "代码不能为空！");

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(data.getBytes());
        } catch (Exception e) {
            throw e;
        }
    }

    private <T> T changeOut(Supplier<T> supplier) {
        PrintStream oldStream = System.out;
        try (ByteArrayOutputStream tmpStream = new ByteArrayOutputStream(1024);
             PrintStream cacheStream = new PrintStream(tmpStream)) {
            System.setOut(cacheStream);
            T value = supplier.get();
            String strMsg = tmpStream.toString();
            addStr(strMsg);
            return value;
        } catch (Exception e) {
            logException(e);
        } finally {
            System.setOut(oldStream);
        }
        return null;
    }

    private void logException(Throwable throwable) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            addStr(stringWriter.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
