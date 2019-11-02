package com.luoboduner.moo.tool.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 代码来源：https://blog.csdn.net/qc1110/article/details/53217011
 *
 * @author mengchuan.li
 * @Title: MyUtils.java
 * @desc: 计算器工具类
 * @date 2016年11月14日 下午3:20:44
 */
public class CalculatorUtil {
    public static final int FORMAT_MAX_LENGTH = 500;// 表达式最大长度限制
    public static final int RESULT_DECIMAL_MAX_LENGTH = 8;// 结果小数点最大长度限制
    public static final Map<Character, Integer> symLvMap = new HashMap<Character, Integer>();// 符号优先级map

    static {
        symLvMap.put('=', 0);
        symLvMap.put('-', 1);
        symLvMap.put('+', 1);
        symLvMap.put('*', 2);
        symLvMap.put('/', 2);
        symLvMap.put('(', 3);
        symLvMap.put(')', 1);
        // symLvMap.put('^', 3);
        // symLvMap.put('%', 3);
    }

    /**
     * @param str 表达式
     * @return true表达式正确，false表达式错误
     * @Title: checkFormat
     * @Desc: 检查表达式格式是否正确
     */
    public static boolean checkFormat(String str) {
        // 校验是否以“=”结尾
        if ('=' != str.charAt(str.length() - 1)) {
            return false;
        }
        // 校验开头是否为数字或者“(”
        if (!(isCharNum(str.charAt(0)) || str.charAt(0) == '(')) {
            return false;
        }
        char c;
        // 校验
        for (int i = 1; i < str.length() - 1; i++) {
            c = str.charAt(i);
            if (!isCorrectChar(c)) {// 字符不合法
                return false;
            }
            if (!(isCharNum(c))) {
                if (c == '-' || c == '+' || c == '*' || c == '/') {
                    if (c == '-' && str.charAt(i - 1) == '(') {// 1*(-2+3)的情况
                        continue;
                    }
                    if (!(isCharNum(str.charAt(i - 1)) || str.charAt(i - 1) == ')')) {// 若符号前一个不是数字或者“）”时
                        return false;
                    }
                }
                if (c == '.') {
                    if (!isCharNum(str.charAt(i - 1)) || !isCharNum(str.charAt(i + 1))) {// 校验“.”的前后是否位数字
                        return false;
                    }
                }
            }
        }
        return isBracketCouple(str);// 校验括号是否配对
    }

    /**
     * @param str
     * @return 标准表达式
     * @Title: change2StandardFormat
     * @Desc: 处理表达式格式为标准格式，如2(-1+2)(3+4)改为2*(0-1+2)*(3+4)
     */
    public static String change2StandardFormat(String str) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if (i != 0 && c == '(' && (isCharNum(str.charAt(i - 1)) || str.charAt(i - 1) == ')')) {
                sb.append("*(");
                continue;
            }
            if (c == '-' && str.charAt(i - 1) == '(') {
                sb.append("0-");
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * @param str
     * @return 参数
     * @Title: isBracketCouple
     * @Desc: 校验括号是否配对
     */
    public static boolean isBracketCouple(String str) {
        LinkedList<Character> stack = new LinkedList<>();
        for (char c : str.toCharArray()) {
            if (c == '(') {
                stack.add(c);
            } else if (c == ')') {
                if (stack.isEmpty()) {
                    return false;
                }
                stack.removeLast();
            }
        }
        if (stack.isEmpty()) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param str 计算结果
     * @return 规范的计算结果
     * @Title: formatResult
     * @Desc: 处理计算结果的显示
     */
    public static String formatResult(String str) {
        String[] ss = str.split("\\.");
        if (Integer.parseInt(ss[1]) == 0) {
            return ss[0];// 整数
        }
        String s1 = new StringBuilder(ss[1]).reverse().toString();
        int start = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != '0') {
                start = i;
                break;
            }
        }
        return ss[0] + "." + new StringBuilder(s1.substring(start, s1.length())).reverse();
    }

    /**
     * @param c
     * @return 参数
     * @Title: isCorrectChar
     * @Desc: 校验字符是否合法
     */
    public static boolean isCorrectChar(Character c) {
        if (('0' <= c && c <= '9') || c == '-' || c == '+' || c == '*' || c == '/' || c == '(' || c == ')'
                || c == '.') {
            return true;
        }
        return false;
    }

    /**
     * @param c
     * @return
     * @Title: isCharNum
     * @Desc: 判断是否为数字
     */
    public static boolean isCharNum(Character c) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        return false;

    }

    /**
     * @param num1
     * @param num2
     * @return 计算结果
     * @Title: plus
     * @Desc: 加
     */
    public static double plus(double num1, double num2) {
        return num1 + num2;
    }

    /**
     * @param num1
     * @param num2
     * @return 计算结果
     * @Title: reduce
     * @Desc: 减
     */
    public static double reduce(double num1, double num2) {
        return num1 - num2;
    }

    /**
     * @param num1
     * @param num2
     * @return 计算结果
     * @Title: multiply
     * @Desc: 乘
     */
    public static double multiply(double num1, double num2) {
        return num1 * num2;

    }

    /**
     * @param num1
     * @param num2
     * @return 计算结果
     * @Title: divide
     * @Desc: 除
     */
    public static double divide(double num1, double num2) {
        return num1 / num2;
    }
}
