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
            calculatorForm.getOutputTextArea().append(inputExpress + " = " + resultStr + "\n");
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

        if ("Darcula(推荐)".equals(App.config.getTheme())) {
            Color bgColor = new Color(30, 30, 30);
            Color foreColor = new Color(187, 187, 187);
            calculatorForm.getOutputTextArea().setBackground(bgColor);
            calculatorForm.getOutputTextArea().setForeground(foreColor);
        }
        calculatorForm.getCalculatorPanel().updateUI();
    }

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
        calculatorPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        calculatorPanel.add(panel1, new GridConstraints(0, 0, 3, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        inputExpressTextField = new JTextField();
        Font inputExpressTextFieldFont = this.$$$getFont$$$(null, -1, 26, inputExpressTextField.getFont());
        if (inputExpressTextFieldFont != null) inputExpressTextField.setFont(inputExpressTextFieldFont);
        inputExpressTextField.setToolTipText("输入四则运算表达式");
        panel1.add(inputExpressTextField, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        calculateButton = new JButton();
        calculateButton.setText("=");
        panel1.add(calculateButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        resultTextField = new JTextField();
        resultTextField.setEditable(false);
        Font resultTextFieldFont = this.$$$getFont$$$(null, -1, 26, resultTextField.getFont());
        if (resultTextFieldFont != null) resultTextField.setFont(resultTextFieldFont);
        resultTextField.setToolTipText("计算结果");
        panel1.add(resultTextField, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        calculatorPanel.add(scrollPane1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(true);
        outputTextArea.setMargin(new Insets(5, 5, 5, 5));
        scrollPane1.setViewportView(outputTextArea);
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
