package com.luoboduner.moo.tool.ui.startup;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.luoboduner.moo.tool.ui.listener.func.ImageListener;
import com.luoboduner.moo.tool.util.I18n;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * 图片助手后台加载快照。
 */
@Getter
public final class ImageLoadData {

    private final List<String> fileNames;
    private final String selectedFileName;
    private final BufferedImage previewImage;
    private final String previewInfoText;

    public ImageLoadData(List<String> fileNames,
                         String selectedFileName,
                         BufferedImage previewImage,
                         String previewInfoText) {
        this.fileNames = fileNames == null ? List.of() : List.copyOf(fileNames);
        this.selectedFileName = selectedFileName;
        this.previewImage = previewImage;
        this.previewInfoText = previewInfoText;
    }

    public static ImageLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        if (!FileUtil.exist(ImageListener.IMAGE_PATH_PRE_FIX)) {
            FileUtil.mkdir(ImageListener.IMAGE_PATH_PRE_FIX);
        }
        List<String> fileNames = Lists.newArrayList();
        List<File> files = FileUtil.loopFiles(ImageListener.IMAGE_PATH_PRE_FIX);
        files.stream().sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                .forEach(file -> fileNames.add(file.getName()));

        if (fileNames.isEmpty()) {
            return new ImageLoadData(fileNames, null, null, null);
        }

        String previousSelectedName = ImageListener.selectedName;
        String selectedFileName = fileNames.get(0);
        if (StringUtils.isNotBlank(previousSelectedName)) {
            for (String fileName : fileNames) {
                if (previousSelectedName.equals(FileUtil.mainName(fileName))) {
                    selectedFileName = fileName;
                    break;
                }
            }
        }

        BufferedImage preview = null;
        String info = null;
        try {
            File imageFile = FileUtil.newFile(ImageListener.IMAGE_PATH_PRE_FIX + selectedFileName);
            preview = ImageIO.read(imageFile);
            if (preview != null) {
                String pixel = preview.getWidth() + " x " + preview.getHeight();
                String size = FileUtil.readableFileSize(imageFile.length());
                info = I18n.format("image.info.size", pixel, size);
            }
        } catch (Exception ignored) {
            // bind 阶段可回退到同步展示
        }
        return new ImageLoadData(fileNames, selectedFileName, preview, info);
    }
}
