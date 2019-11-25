package com.luoboduner.moo.tool.ui.frame;

import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.ui.UiConsts;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * FrameUtil
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/19.
 */
public class FrameUtil {

    public static void setFrameIcon(JFrame jFrame) {
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_LOGO_1024);
        images.add(UiConsts.IMAGE_LOGO_512);
        images.add(UiConsts.IMAGE_LOGO_256);
        images.add(UiConsts.IMAGE_LOGO_128);
        images.add(UiConsts.IMAGE_LOGO_64);
        images.add(UiConsts.IMAGE_LOGO_48);
        images.add(UiConsts.IMAGE_LOGO_32);
        images.add(UiConsts.IMAGE_LOGO_24);
        images.add(UiConsts.IMAGE_LOGO_16);
        jFrame.setIconImages(images);
    }
}
