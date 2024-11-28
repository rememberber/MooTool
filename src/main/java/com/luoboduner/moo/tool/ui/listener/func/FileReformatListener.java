package com.luoboduner.moo.tool.ui.listener.func;

import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.ui.form.func.FileReformattingForm;
import com.luoboduner.moo.tool.util.FileReformatUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

@Slf4j
public class FileReformatListener {

    private JComboBox fileType;
    private JComboBox spaceNum;
    private JTextArea resultArea;
    private JButton uploadFileBtn;
    private JButton reformatBtn;

    private JLabel selectFileLabel;

    private File selectFile;

    private FileReformatListener() {
    }

    public static void addListeners() {
        FileReformattingForm fileReformattingForm = FileReformattingForm.getInstance();

        fileReformattingForm.getStringTypeComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                FileReformattingForm.changeStringType();
            }
        });

        fileReformattingForm.getFormatButton().addActionListener(e -> {
            FileReformattingForm.format();
        });
    }

    public static class ParameterBuilder {
        private JComboBox fileType;
        private JComboBox spaceNum;
        private JTextArea resultArea;
        private JButton uploadFileBtn;
        private JButton reformatBtn;

        private JLabel selectFileLabel;

        private ParameterBuilder() {}

        public static ParameterBuilder builder() {
            return new ParameterBuilder();
        }

        public ParameterBuilder buildFileType(JComboBox type) {
            this.fileType = type;
            return this;
        }

        public ParameterBuilder buildSpaceNum(JComboBox number) {
            this.spaceNum = number;
            return this;
        }

        public ParameterBuilder buildResultPanel(JTextArea resultArea) {
            this.resultArea = resultArea;
            return this;
        }

        public ParameterBuilder buildUploadBtn(JButton uploadFile) {
            this.uploadFileBtn = uploadFile;
            return this;
        }

        public ParameterBuilder buildReformatBtn(JButton reformatBtn) {
            this.reformatBtn = reformatBtn;
            return this;
        }

        public ParameterBuilder buildSelectFileLabel(JLabel selectFile) {
            this.selectFileLabel = selectFile;
            return this;
        }

        public void start() {
            FileReformatListener fileReformatListener = new FileReformatListener();
            fileReformatListener.fileType = this.fileType;
            fileReformatListener.spaceNum = this.spaceNum;
            fileReformatListener.resultArea = this.resultArea;
            fileReformatListener.uploadFileBtn = this.uploadFileBtn;
            fileReformatListener.reformatBtn = this.reformatBtn;
            fileReformatListener.selectFileLabel = this.selectFileLabel;
            fileReformatListener.init();
        }
    }

    private void init() {
        this.initOpenFileDialog();
        this.initReformatListener();
    }

    private void initReformatListener() {
        this.reformatBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    String fileTypeValue = fileType.getModel().getSelectedItem().toString();
                    String spaceNumValue = spaceNum.getModel().getSelectedItem().toString();
                    FileType fileType = FileType.valueOf(fileTypeValue);
                    if (fileType.canReformat(selectFile.getName())) {
                        String reformat = fileType.reformat(selectFile, Integer.valueOf(spaceNumValue));
                        resultArea.setText(reformat);
                    } else {
                        JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "错误：文件类型错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });
    }

    private void initOpenFileDialog() {
        this.uploadFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(resultArea);
                if (option == JFileChooser.APPROVE_OPTION){
                    File file = fileChooser.getSelectedFile();
                    selectFile = file;
                    selectFileLabel.setText(file.getName());
                    resultArea.setText(readContent(file));
                }
            }
        });
    }

    private void logException(Throwable throwable) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            resultArea.setText(stringWriter.toString());
        } catch (Exception e) {
           log.error("打印log异常！", e);
        }
    }

    private String readContent(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            bufferedReader.lines().forEach(content::append);
        } catch (Exception e) {
            log.error("读取文件内容异常！", e);
        }
        return content.toString();
    }

    public enum FileType implements FileReformat {
        XML {
            @Override
            public String reformat(File file, int spaceNum) throws Exception {
                return FileReformatUtil.reformatXML(file, spaceNum);
            }

            @Override
            public boolean canReformat(String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        },
        HTML {
            @Override
            public String reformat(File file, int spaceNum) throws Exception {
                return FileReformatUtil.reformatHTML(file, spaceNum);
            }

            @Override
            public boolean canReformat(String name) {
                return name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".htm");
            }
        },
        JAVA {
            @Override
            public String reformat(File file, int spaceNum) throws Exception {
                return FileReformatUtil.reformatJAVA(file, spaceNum);
            }

            @Override
            public boolean canReformat(String name) {
                return name.toLowerCase().endsWith(".java");
            }
        };
    }

    public interface FileReformat {
        String reformat(File file, int spaceNum) throws Exception;

        boolean canReformat(String name);
    }
}
