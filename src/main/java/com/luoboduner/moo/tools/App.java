package com.luoboduner.moo.tools;

import com.luoboduner.moo.tools.ui.frame.MainFrame;
import com.luoboduner.moo.tools.util.ConfigUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 * Main Enter!
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/8/10.
 */
@Slf4j
public class App {

    public static ConfigUtil config = ConfigUtil.getInstance();

    public static MainFrame mainFrame;

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
