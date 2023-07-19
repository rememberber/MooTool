package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.CalculatorForm;
import com.luoboduner.moo.tool.util.Calculator;
import com.luoboduner.moo.tool.util.CalculatorUtil;
import com.luoboduner.moo.tool.util.ConsoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * <pre>
 * CalculatorListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/1.
 */
public class CalculatorListener {

    private static final Log logger = LogFactory.get();

    public static void addListeners() {

        CalculatorForm calculatorForm = CalculatorForm.getInstance();

        calculatorForm.getCalculateButton().addActionListener(e -> calculate());

        calculatorForm.getInputExpressTextField().addKeyListener(new KeyListener() {
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
        calculatorForm.getHexToDecButton().addActionListener(e -> {
            try {
                String hex = calculatorForm.getHexTextField().getText().trim();
                long dec = Long.parseLong(hex, 16);
                calculatorForm.getDecTextField().setText(String.valueOf(dec));
                output("DEC(" + hex + ") = " + dec);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getDecToHexButton().addActionListener(e -> {
            try {
                long dec = Long.parseLong(calculatorForm.getDecTextField().getText().trim());
                String strHex = Long.toHexString(dec);
                calculatorForm.getHexTextField().setText(strHex);
                output("HEX(" + dec + ") = " + strHex);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getDecToBinaryButton().addActionListener(e -> {
            try {
                long dec = Long.parseLong(calculatorForm.getDecTextField().getText().trim());
                String binaryString = Long.toBinaryString(dec);
                calculatorForm.getBinaryTextField().setText(binaryString);
                output("BIN(" + dec + ") = " + binaryString);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getBinaryToDecButton().addActionListener(e -> {
            try {
                String binaryStr = calculatorForm.getBinaryTextField().getText().trim();
                long dec = NumberUtil.binaryToLong(binaryStr);
                calculatorForm.getDecTextField().setText(String.valueOf(dec));
                output("DEC(" + binaryStr + ") = " + dec);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getDivisorButton().addActionListener(e -> {
            try {
                String num1 = calculatorForm.getDivisorNum1TextField().getText().trim();
                String num2 = calculatorForm.getDivisorNum2TextField().getText().trim();
                int divisor = NumberUtil.divisor(Integer.parseInt(num1), Integer.parseInt(num2));
                calculatorForm.getResultTextField().setText(String.valueOf(divisor));
                output(num1 + "和" + num2 + "的最大公约数" + " = " + divisor);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getMultipleButton().addActionListener(e -> {
            try {
                String num1 = calculatorForm.getMultipleNum1TextField().getText().trim();
                String num2 = calculatorForm.getMultipleNum2TextField().getText().trim();
                int multiple = NumberUtil.multiple(Integer.parseInt(num1), Integer.parseInt(num2));
                calculatorForm.getResultTextField().setText(String.valueOf(multiple));
                output(num1 + "和" + num2 + "的最小公倍数" + " = " + multiple);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getArrangementCountButton().addActionListener(e -> {
            try {
                String n = calculatorForm.getArrangementCountNTextField().getText().trim();
                String m = calculatorForm.getArrangementCountMTextField().getText().trim();
                long arrangementCount = MathUtil.arrangementCount(Integer.parseInt(n), Integer.parseInt(m));
                calculatorForm.getResultTextField().setText(String.valueOf(arrangementCount));
                output("A(" + n + "," + m + ")" + " = " + arrangementCount);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
        calculatorForm.getCombinationCountButton().addActionListener(e -> {
            try {
                String n = calculatorForm.getCombinationCountNTextField().getText().trim();
                String m = calculatorForm.getCombinationCountMTextField().getText().trim();
                long combinationCount = MathUtil.combinationCount(Integer.parseInt(n), Integer.parseInt(m));
                calculatorForm.getResultTextField().setText(String.valueOf(combinationCount));
                output("C(" + n + "," + m + ")" + " = " + combinationCount);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 文本域按键事件
        calculatorForm.getOutputTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
                CalculatorForm.saveContent();
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if ((evt.isControlDown() || evt.isMetaDown()) && evt.getKeyCode() == KeyEvent.VK_S) {
                    CalculatorForm.saveContent();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });
    }

    public static void calculate() {
        CalculatorForm calculatorForm = CalculatorForm.getInstance();
        try {
            String inputExpress = calculatorForm.getInputExpressTextField().getText().replace("（", "(").replace("）", ")");
            inputExpress = inputExpress.replace(",", "");
            inputExpress = inputExpress.replace("\t", "");
            Calculator calc = new Calculator();
            Double str = calc.prepareParam(inputExpress + "=");
            String resultStr = CalculatorUtil.formatResult(String.format("%." + CalculatorUtil.RESULT_DECIMAL_MAX_LENGTH + "f", str));
            calculatorForm.getResultTextField().setText(resultStr);
            output(inputExpress + " = " + resultStr);
            App.config.setCalculatorInputExpress(inputExpress);
            App.config.save();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(ex));
            JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void output(String text) {
        CalculatorForm calculatorForm = CalculatorForm.getInstance();
        ConsoleUtil.consoleOnly(calculatorForm.getOutputTextArea(), text);
        CalculatorForm.saveContent();
    }
}
