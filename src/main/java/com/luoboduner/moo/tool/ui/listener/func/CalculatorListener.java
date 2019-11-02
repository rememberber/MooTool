package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.ui.form.func.CalculatorForm;
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

        calculatorForm.getCalculateButton().addActionListener(e -> CalculatorForm.calculate());

        calculatorForm.getInputExpressTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    CalculatorForm.calculate();
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
                calculatorForm.getOutputTextArea().append("DEC(" + hex + ") = " + dec + "\n\n");
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
                calculatorForm.getOutputTextArea().append("HEX(" + dec + ") = " + strHex + "\n\n");
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
                calculatorForm.getOutputTextArea().append("BIN(" + dec + ") = " + binaryString + "\n\n");
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
                calculatorForm.getOutputTextArea().append("DEC(" + binaryStr + ") = " + dec + "\n\n");
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
                calculatorForm.getOutputTextArea().append(num1 + "和" + num2 + "的最大公约数" + " = " + divisor + "\n\n");
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
                calculatorForm.getOutputTextArea().append(num1 + "和" + num2 + "的最小公倍数" + " = " + multiple + "\n\n");
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
                calculatorForm.getOutputTextArea().append("A(" + n + "," + m + ")" + " = " + arrangementCount + "\n\n");
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
                calculatorForm.getOutputTextArea().append("C(" + n + "," + m + ")" + " = " + combinationCount + "\n\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error(ExceptionUtils.getStackTrace(ex));
                JOptionPane.showMessageDialog(calculatorForm.getCalculatorPanel(), ex.getMessage(), "计算失败！", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
