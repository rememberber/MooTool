package com.luoboduner.moo.tool.ui;

import java.awt.*;

/**
 * <pre>
 * UI相关的常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/8/10.
 */
public class UiConsts {

    /**
     * 软件名称,版本
     */
    public final static String APP_NAME = "MooTool";
    public final static String APP_VERSION = "v1.0.0";

    /**
     * 主窗口图标-大
     */
    public final static Image IMAGE_ICON_LG = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-512.png"));

    /**
     * 主窗口图标-中
     */
    public final static Image IMAGE_ICON_MD = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-128.png"));

    /**
     * 主窗口图标-小
     */
    public final static Image IMAGE_ICON_SM = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-64.png"));

    /**
     * 主窗口图标-超小
     */
    public final static Image IMAGE_ICON_XS = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-32.png"));

    /**
     * 帮助图标
     */
    public final static Image HELP_ICON = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/helpButton.png"));

    /**
     * 帮助图标-focused
     */
    public final static Image HELP_FOCUSED_ICON = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/helpButtonFocused.png"));

    /**
     * 软件版本检查url
     */
    public final static String CHECK_VERSION_URL = "https://raw.githubusercontent.com/rememberber/MooTool/master/src/main/resources/version_summary.json";

    /**
     * 二维码url
     */
    public final static String QR_CODE_URL = "http://download.zhoubochina.com/file/mootool_qrcode.json";

    /**
     * 介绍二维码URL
     */
    public final static String INTRODUCE_QRCODE_URL = "http://download.zhoubochina.com/qrcode/introduce-mootool-qrcode.png";

}
