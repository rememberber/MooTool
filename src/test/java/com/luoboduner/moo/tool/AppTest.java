package com.luoboduner.moo.tool;

import cn.hutool.core.util.HexUtil;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        String s = HexUtil.toHex(12);
        System.err.println(s);
    }
}
