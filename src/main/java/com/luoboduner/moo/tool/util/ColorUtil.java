package com.luoboduner.moo.tool.util;

import java.awt.*;

/**
 * <pre>
 * ColorUtil
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/18.
 */
public class ColorUtil {

    /**
     * colorHex to Color
     *
     * @param colorHex
     * @return
     */
    public static Color fromHex(String colorHex) {
        if (colorHex.startsWith("#")) {
            colorHex = colorHex.substring(1);
        }

        if (colorHex.length() == 3) {
            return new Color(17 * Integer.valueOf(String.valueOf(colorHex.charAt(0)), 16), 17 * Integer.valueOf(String.valueOf(colorHex.charAt(1)), 16), 17 * Integer.valueOf(String.valueOf(colorHex.charAt(2)), 16));
        } else if (colorHex.length() == 6) {
            return Color.decode("0x" + colorHex);
        } else {
            throw new IllegalArgumentException("Should be String of 3 or 6 chars length.");
        }
    }

    /**
     * color object to Hex String
     *
     * @param color
     * @return
     */
    public static String toHex(Color color) {
        String r, g, b;
        StringBuilder stringBuilder = new StringBuilder();
        r = Integer.toHexString(color.getRed());
        g = Integer.toHexString(color.getGreen());
        b = Integer.toHexString(color.getBlue());
        r = r.length() == 1 ? "0" + r : r;
        g = g.length() == 1 ? "0" + g : g;
        b = b.length() == 1 ? "0" + b : b;
        stringBuilder.append(r);
        stringBuilder.append(g);
        stringBuilder.append(b);
        return stringBuilder.toString().toUpperCase();
    }

    /**
     * color RGB String to Hex String
     *
     * @param rgb
     * @return
     */
    public static String rgbStrToHex(String rgb) {
        rgb = rgb.replace("，", ",").replace(" ", "");
        String[] rgbArray = rgb.split(",");
        String r, g, b;
        StringBuilder stringBuilder = new StringBuilder();
        r = Integer.toHexString(Integer.parseInt(rgbArray[0]));
        g = Integer.toHexString(Integer.parseInt(rgbArray[1]));
        b = Integer.toHexString(Integer.parseInt(rgbArray[2]));
        r = r.length() == 1 ? "0" + r : r;
        g = g.length() == 1 ? "0" + g : g;
        b = b.length() == 1 ? "0" + b : b;
        stringBuilder.append(r);
        stringBuilder.append(g);
        stringBuilder.append(b);
        return stringBuilder.toString().toUpperCase();
    }

    /**
     * color object to RGB String
     *
     * @param color
     * @return
     */
    public static String toRgbStr(Color color) {
        String r, g, b;
        StringBuilder stringBuilder = new StringBuilder();
        r = String.valueOf(color.getRed());
        g = String.valueOf(color.getGreen());
        b = String.valueOf(color.getBlue());
        stringBuilder.append(r).append(",");
        stringBuilder.append(g).append(",");
        stringBuilder.append(b);
        return stringBuilder.toString().toUpperCase();
    }

    /**
     * 取反色
     */
    public static Color invert(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
    }

    /**
     * 相交（正片叠底）
     */
    public static Color intersect(Color a, Color b) {
        return new Color(channelMultiply(a.getRed(), b.getRed()),
                channelMultiply(a.getGreen(), b.getGreen()),
                channelMultiply(a.getBlue(), b.getBlue()));
    }

    /**
     * 相加
     */
    public static Color add(Color a, Color b) {
        return new Color(clampChannel(a.getRed() + b.getRed()),
                clampChannel(a.getGreen() + b.getGreen()),
                clampChannel(a.getBlue() + b.getBlue()));
    }

    /**
     * 差值
     */
    public static Color difference(Color a, Color b) {
        return new Color(Math.abs(a.getRed() - b.getRed()),
                Math.abs(a.getGreen() - b.getGreen()),
                Math.abs(a.getBlue() - b.getBlue()));
    }

    /**
     * 平均
     */
    public static Color average(Color a, Color b) {
        return new Color((a.getRed() + b.getRed()) / 2,
                (a.getGreen() + b.getGreen()) / 2,
                (a.getBlue() + b.getBlue()) / 2);
    }

    private static int channelMultiply(int a, int b) {
        return a * b / 255;
    }

    private static int clampChannel(int value) {
        return Math.min(255, Math.max(0, value));
    }

}
