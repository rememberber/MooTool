package com.luoboduner.moo.tool.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 代码来源：https://blog.csdn.net/qc1110/article/details/53217011
 *
 * @author mengchuan.li
 * @Title: Calculator.java
 * @desc: 加减乘除计算器，支持括号，小数，负数
 * @date 2016年11月14日 下午1:22:39
 */
public class Calculator {

    /**
     * @param str 计算式
     * @return 计算结果
     * @Title: PrepareParam
     * @Desc: 准备计算的数据，符号
     */
    public Double prepareParam(String str) throws Exception {
        // 空值校验
        if (null == str || "".equals(str)) {
            return null;
        }
        // 长度校验
        if (str.length() > CalculatorUtil.FORMAT_MAX_LENGTH) {
            throw new Exception("表达式过长");
        }
        // 预处理
        str = str.replaceAll(" ", "");// 去空格
        if ('-' == str.charAt(0)) {// 开头为负数，如-1，改为0-1
            str = 0 + str;
        }
        // 校验格式
        if (!CalculatorUtil.checkFormat(str)) {
            throw new Exception("表达式错误");
        }
        // 处理表达式，改为标准表达式
        str = CalculatorUtil.change2StandardFormat(str);
        // 拆分字符和数字
        String[] nums = str.split("[^.0-9]");
        List<Double> numLst = new ArrayList();
        for (int i = 0; i < nums.length; i++) {
            if (!"".equals(nums[i])) {
                numLst.add(Double.parseDouble(nums[i]));
            }
        }
        String symStr = str.replaceAll("[.0-9]", "");
        return doCalculate(symStr, numLst);
    }

    /**
     * @param symStr 符号串
     * @param numLst 数字集合
     * @return 计算结果
     * @Title: doCalculate
     * @Desc: 计算
     */
    public Double doCalculate(String symStr, List<Double> numLst) throws Exception {
        LinkedList<Character> symStack = new LinkedList<>();// 符号栈
        LinkedList<Double> numStack = new LinkedList<>();// 数字栈
        double result = 0;
        int i = 0;// numLst的标志位
        int j = 0;// symStr的标志位
        char sym;// 符号
        double num1, num2;// 符号前后两个数
        while (symStack.isEmpty() || !(symStack.getLast() == '=' && symStr.charAt(j) == '=')) {// 形如：
            // =8=
            // 则退出循环，结果为8
            if (symStack.isEmpty()) {
                symStack.add('=');
                numStack.add(numLst.get(i++));
            }
            if (CalculatorUtil.symLvMap.get(symStr.charAt(j)) > CalculatorUtil.symLvMap.get(symStack.getLast())) {// 比较符号优先级，若当前符号优先级大于前一个则压栈
                if (symStr.charAt(j) == '(') {
                    symStack.add(symStr.charAt(j++));
                    continue;
                }
                numStack.add(numLst.get(i++));
                symStack.add(symStr.charAt(j++));
            } else {// 当前符号优先级小于等于前一个 符号的优先级
                if (symStr.charAt(j) == ')' && symStack.getLast() == '(') {// 若（）之间没有符号，则“（”出栈
                    j++;
                    symStack.removeLast();
                    continue;
                }
                if (symStack.getLast() == '(') {// “（”直接压栈
                    numStack.add(numLst.get(i++));
                    symStack.add(symStr.charAt(j++));
                    continue;
                }
                num2 = numStack.removeLast();
                num1 = numStack.removeLast();
                sym = symStack.removeLast();
                switch (sym) {
                    case '+':
                        numStack.add(CalculatorUtil.plus(num1, num2));
                        break;
                    case '-':
                        numStack.add(CalculatorUtil.reduce(num1, num2));
                        break;
                    case '*':
                        numStack.add(CalculatorUtil.multiply(num1, num2));
                        break;
                    case '/':
                        if (num2 == 0) {// 除数为0
                            throw new Exception("存在除数为0");
                        }
                        numStack.add(CalculatorUtil.divide(num1, num2));
                        break;
                }
            }
        }
        return numStack.removeLast();
    }

}
