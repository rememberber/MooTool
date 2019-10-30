package com.luoboduner.moo.tool.ui.form.func;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.util.UndoUtil;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * <pre>
 * CalculatorForm
 * 计算部分代码来源：https://blog.csdn.net/qq_34120430/article/details/79674993
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/29.
 */
@Getter
public class CalculatorForm {
    private JPanel calculatorPanel;
    private JTextField inputExpressTextField;
    private JButton calculateButton;
    private JTextField resultTextField;
    private JTextArea outputTextArea;
    private JSplitPane splitPane;
    private JButton 转换Button;
    private JTextField textField1;
    private JTextField textField2;
    private JButton 转换Button1;
    private JTextField textField3;
    private JButton 转换Button2;
    private JButton 转换Button3;
    private JTextField textField4;
    private JButton 计算最大公约数Button;
    private JButton 计算最小公倍数Button;
    private JTextField textField5;
    private JTextField textField6;
    private JTextField textField7;
    private JButton aNMButton;
    private JButton cNMButton;
    private JTextField textField8;
    private JTextField textField9;

    private static CalculatorForm calculatorForm;

    private static final Log logger = LogFactory.get();

    static Set<Character> brace = new HashSet<>();

    private CalculatorForm() {
        UndoUtil.register(this);
        calculateButton.addActionListener(e -> calculate());

        inputExpressTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    calculate();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }

    private void calculate() {
        try {
            String inputExpress = calculatorForm.getInputExpressTextField().getText();
            int result = calculate(inputExpress + "#");
            String resultStr = result + "";
            calculatorForm.getResultTextField().setText(resultStr);
            calculatorForm.getOutputTextArea().append(inputExpress + " = " + resultStr + "\n\n");
            App.config.setCalculatorInputExpress(inputExpress);
            App.config.save();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(ex));
            JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static CalculatorForm getInstance() {
        if (calculatorForm == null) {
            calculatorForm = new CalculatorForm();
        }
        return calculatorForm;
    }

    public static void init() {
        calculatorForm = getInstance();

        initUi();

        calculatorForm.getInputExpressTextField().setText(App.config.getCalculatorInputExpress());
        brace.add('{');
        brace.add('(');
        brace.add('[');
    }

    private static void initUi() {
        calculatorForm.getSplitPane().setDividerLocation((int) (App.mainFrame.getWidth() / 2));
        if ("Darcula(推荐)".equals(App.config.getTheme())) {
            Color bgColor = new Color(30, 30, 30);
            Color foreColor = new Color(187, 187, 187);
            calculatorForm.getOutputTextArea().setBackground(bgColor);
            calculatorForm.getOutputTextArea().setForeground(foreColor);
        }
        calculatorForm.getCalculatorPanel().updateUI();
    }

    /**
     * 代码来源：https://blog.csdn.net/qq_34120430/article/details/79674993
     *
     * @param exp
     * @return
     */
    private static int calculate(String exp) {


        // 初始化栈
        Stack<Integer> opStack = new Stack<>();
        Stack<Character> otStack = new Stack<>();

        // 整数记录器  当num是多位时将 num += c 知道遇见运算符
        String num = "";
        for (int i = 0; i < exp.length(); i++) {
            // 抽取字符
            char c = exp.charAt(i);
            // 如果字符是数字，则加这个数字累加到num后面
            if (Character.isDigit(c)) {
                num += c;
            }
            // 如果不是数字
            else {
                // 如果有字符串被记录，则操作数入栈，并清空
                if (!num.isEmpty()) {
                    int n = Integer.parseInt(num);
                    num = "";
                    opStack.push(n);
                }
                // 如果遇上了终结符则退出
                if (c == '#')
                    break;
                    // 如果遇上了+-
                else if (c == '+' || c == '-') {
                    // 空栈或者操作符栈顶遇到正括号，则入栈
                    if (otStack.isEmpty() || brace.contains(otStack.peek())) {
                        otStack.push(c);
                    } else {
                        // 否则一直做弹栈计算，直到空或者遇到正括号为止，最后入栈
                        while (!otStack.isEmpty() && !brace.contains(otStack.peek()))
                            popAndCal(opStack, otStack);
                        otStack.push(c);
                    }
                }
                // 如果遇上*/
                else if (c == '*' || c == '/') {
                    // 空栈或者遇到操作符栈顶是括号，或者遇到优先级低的运算符，则入栈
                    if (otStack.isEmpty()
                            || brace.contains(otStack.peek())
                            || otStack.peek() == '+' || otStack.peek() == '-') {
                        otStack.push(c);
                    } else {
                        // 否则遇到*或/则一直做弹栈计算，直到栈顶是优先级比自己低的符号，最后入栈
                        while (!otStack.isEmpty()
                                && otStack.peek() != '+' && otStack.peek() != '-'
                                && !brace.contains(otStack.peek()))
                            popAndCal(opStack, otStack);
                        otStack.push(c);
                    }
                } else {
                    // 如果是正括号就压栈
                    if (brace.contains(c))
                        otStack.push(c);
                    else {
                        // 反括号就一直做弹栈计算，直到遇到正括号为止
                        char r = getBrace(c);
                        while (otStack.peek() != r) {
                            popAndCal(opStack, otStack);
                        }
                        // 最后弹出正括号
                        otStack.pop();
                    }
                }
            }
        }
        // 将剩下的计算完，直到运算符栈为空
        while (!otStack.isEmpty())
            popAndCal(opStack, otStack);
        // 返回结果
        return opStack.pop();
    }

    /**
     * 代码来源：https://blog.csdn.net/qq_34120430/article/details/79674993
     *
     * @param opStack
     * @param otStack
     */
    private static void popAndCal(Stack<Integer> opStack, Stack<Character> otStack) {
        int op2 = opStack.pop();
        int op1 = opStack.pop();
        char ot = otStack.pop();
        int res = 0;
        switch (ot) {
            case '+':
                res = op1 + op2;
                break;
            case '-':
                res = op1 - op2;
                break;
            case '*':
                res = op1 * op2;
                break;
            case '/':
                res = op1 / op2;
                break;
        }
        opStack.push(res);
    }

    /**
     * 代码来源：https://blog.csdn.net/qq_34120430/article/details/79674993
     *
     * @param brace
     * @return
     */
    private static char getBrace(char brace) {
        switch (brace) {
            case ')':
                return '(';
            case ']':
                return '[';
            case '}':
                return '{';
        }
        return '#';
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        calculatorPanel = new JPanel();
        calculatorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(418);
        splitPane.setDividerSize(2);
        calculatorPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel1);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(true);
        outputTextArea.setMargin(new Insets(5, 5, 5, 5));
        scrollPane1.setViewportView(outputTextArea);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 0, 5), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultTextField = new JTextField();
        resultTextField.setEditable(false);
        Font resultTextFieldFont = this.$$$getFont$$$(null, -1, 26, resultTextField.getFont());
        if (resultTextFieldFont != null) resultTextField.setFont(resultTextFieldFont);
        resultTextField.setToolTipText("计算结果");
        panel2.add(resultTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        splitPane.setLeftComponent(scrollPane2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 0, 5), -1, -1));
        scrollPane2.setViewportView(panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "四则运算", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel4.getFont())));
        inputExpressTextField = new JTextField();
        Font inputExpressTextFieldFont = this.$$$getFont$$$(null, -1, 26, inputExpressTextField.getFont());
        if (inputExpressTextFieldFont != null) inputExpressTextField.setFont(inputExpressTextFieldFont);
        inputExpressTextField.setToolTipText("输入四则运算表达式");
        panel4.add(inputExpressTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        calculateButton = new JButton();
        Font calculateButtonFont = this.$$$getFont$$$(null, -1, 24, calculateButton.getFont());
        if (calculateButtonFont != null) calculateButton.setFont(calculateButtonFont);
        calculateButton.setText("=");
        panel4.add(calculateButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(5, 2, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "进制转换", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel5.getFont())));
        textField2 = new JTextField();
        Font textField2Font = this.$$$getFont$$$(null, -1, -1, textField2.getFont());
        if (textField2Font != null) textField2.setFont(textField2Font);
        textField2.setToolTipText("输入四则运算表达式");
        panel5.add(textField2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textField1 = new JTextField();
        textField1.setEditable(true);
        Font textField1Font = this.$$$getFont$$$(null, -1, -1, textField1.getFont());
        if (textField1Font != null) textField1.setFont(textField1Font);
        textField1.setToolTipText("计算结果");
        panel5.add(textField1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        textField3 = new JTextField();
        textField3.setEditable(true);
        Font textField3Font = this.$$$getFont$$$(null, -1, -1, textField3.getFont());
        if (textField3Font != null) textField3.setFont(textField3Font);
        textField3.setToolTipText("计算结果");
        panel5.add(textField3, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        转换Button = new JButton();
        转换Button.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        转换Button.setText("转换");
        panel6.add(转换Button, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        转换Button1 = new JButton();
        转换Button1.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        转换Button1.setText("转换");
        panel6.add(转换Button1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("十六进制");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("十进制");
        panel5.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("二进制");
        panel5.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel7, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        转换Button3 = new JButton();
        转换Button3.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-down.png")));
        转换Button3.setText("转换");
        panel7.add(转换Button3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        转换Button2 = new JButton();
        转换Button2.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-up.png")));
        转换Button2.setText("转换");
        panel7.add(转换Button2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel3.add(spacer5, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "最大公约数", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel8.getFont())));
        textField4 = new JTextField();
        panel8.add(textField4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        计算最大公约数Button = new JButton();
        计算最大公约数Button.setText("计算最大公约数");
        panel8.add(计算最大公约数Button, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel9, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel9.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "最小公倍数", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel9.getFont())));
        textField5 = new JTextField();
        panel9.add(textField5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        计算最小公倍数Button = new JButton();
        计算最小公倍数Button.setText("计算最小公倍数");
        panel9.add(计算最小公倍数Button, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 5, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel10, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel10.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "排列数", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel10.getFont())));
        final JLabel label4 = new JLabel();
        label4.setText("n");
        panel10.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField6 = new JTextField();
        panel10.add(textField6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("m");
        panel10.add(label5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField7 = new JTextField();
        panel10.add(textField7, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        aNMButton = new JButton();
        aNMButton.setText("A(n,m)");
        panel10.add(aNMButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 5, new Insets(15, 15, 15, 5), -1, -1));
        panel3.add(panel11, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel11.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "组合数", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel11.getFont())));
        final JLabel label6 = new JLabel();
        label6.setText("n");
        panel11.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField9 = new JTextField();
        panel11.add(textField9, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("m");
        panel11.add(label7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField8 = new JTextField();
        panel11.add(textField8, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        cNMButton = new JButton();
        cNMButton.setText("C(n,m)");
        panel11.add(cNMButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return calculatorPanel;
    }

}
