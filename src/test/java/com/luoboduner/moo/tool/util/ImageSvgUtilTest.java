package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageSvgUtilTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsDeterministicEditableVectorPaths() throws Exception {
        BufferedImage image = sampleImage();
        ImageSvgUtil.SvgOptions options = ImageSvgUtil.SvgOptions.defaults();

        String first = ImageSvgUtil.convertToSvg(image, options);
        String second = ImageSvgUtil.convertToSvg(image, options);

        assertEquals(first, second);
        assertTrue(first.startsWith("<svg "));
        assertTrue(first.contains("viewBox=\"0 0 48 36\""));
        assertTrue(first.contains("<path "));
        assertFalse(first.contains("<image"));
        assertFalse(first.contains("data:image"));
    }

    @Test
    void keepsExistingSvgAndChoosesAUniqueOutputName() throws Exception {
        Path source = temporaryDirectory.resolve("logo.png");
        ImageIO.write(sampleImage(), "png", source.toFile());

        ImageSvgUtil.ConversionResult first = ImageSvgUtil.convert(
                source.toFile(), temporaryDirectory.toFile(), ImageSvgUtil.SvgOptions.defaults());
        ImageSvgUtil.ConversionResult second = ImageSvgUtil.convert(
                source.toFile(), temporaryDirectory.toFile(), ImageSvgUtil.SvgOptions.defaults());

        assertTrue(first.success(), first.errorMessage());
        assertTrue(second.success(), second.errorMessage());
        assertEquals("logo.svg", first.outputFile().getName());
        assertEquals("logo_2.svg", second.outputFile().getName());
        assertTrue(first.outputFile().isFile());
        assertTrue(second.outputFile().isFile());
    }

    @Test
    void blackAndWhitePresetUsesOnlyVectorShapes() throws Exception {
        ImageSvgUtil.SvgOptions options = new ImageSvgUtil.SvgOptions(
                ImageSvgUtil.Preset.BLACK_AND_WHITE, 16, ImageSvgUtil.Detail.HIGH, 0);

        String svg = ImageSvgUtil.convertToSvg(sampleImage(), options);

        assertTrue(svg.contains("<path "));
        assertFalse(svg.contains("<image"));
    }

    private static BufferedImage sampleImage() {
        BufferedImage image = new BufferedImage(48, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 48, 36);
        graphics.setColor(new Color(220, 32, 48));
        graphics.fillRoundRect(4, 4, 24, 24, 6, 6);
        graphics.setColor(new Color(20, 90, 210));
        graphics.fillOval(24, 8, 18, 18);
        graphics.dispose();
        return image;
    }
}
