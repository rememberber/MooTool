package com.luoboduner.moo.tool.util;

import cn.hutool.core.io.FileUtil;
import jankovicsandras.imagetracer.ImageTracer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Offline raster-to-SVG tracing for the image assistant.
 *
 * <p>The output contains editable vector paths; the source bitmap is never embedded in the SVG.
 * A deterministic median-cut palette keeps repeated conversions byte-identical.</p>
 */
public final class ImageSvgUtil {

    private static final long MAX_PIXELS = 16_000_000L;

    private ImageSvgUtil() {
    }

    public enum Preset {
        POSTER,
        PHOTO,
        BLACK_AND_WHITE
    }

    public enum Detail {
        LOW,
        MEDIUM,
        HIGH
    }

    public record SvgOptions(Preset preset, int colorCount, Detail detail, int filterSpeckle) {
        public SvgOptions {
            if (preset == null || detail == null) {
                throw new IllegalArgumentException("Preset and detail are required");
            }
            if (colorCount < 2 || colorCount > 64) {
                throw new IllegalArgumentException("Color count must be between 2 and 64");
            }
            if (filterSpeckle < 0 || filterSpeckle > 128) {
                throw new IllegalArgumentException("Speckle filter must be between 0 and 128");
            }
        }

        public static SvgOptions defaults() {
            return new SvgOptions(Preset.POSTER, 16, Detail.MEDIUM, 8);
        }
    }

    public record ConversionResult(boolean success, File outputFile, String errorMessage) {
        private static ConversionResult success(File outputFile) {
            return new ConversionResult(true, outputFile, null);
        }

        private static ConversionResult failure(String errorMessage) {
            return new ConversionResult(false, null, errorMessage);
        }
    }

    public static ConversionResult convert(File sourceFile, File outputDirectory, SvgOptions options) {
        try {
            if (sourceFile == null || !sourceFile.isFile()) {
                throw new IOException("Source image does not exist");
            }
            Files.createDirectories(outputDirectory.toPath());
            BufferedImage image = ImageIO.read(sourceFile);
            if (image == null) {
                throw new IOException("Unsupported or invalid image");
            }
            String svg = convertToSvg(image, options);
            File outputFile = availableOutputFile(outputDirectory, FileUtil.mainName(sourceFile.getName()));
            Files.writeString(outputFile.toPath(), svg, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return ConversionResult.success(outputFile);
        } catch (Exception exception) {
            String message = exception.getMessage();
            return ConversionResult.failure(message == null || message.isBlank()
                    ? exception.getClass().getSimpleName() : message);
        }
    }

    public static String convertToSvg(BufferedImage image, SvgOptions options) throws Exception {
        if (image == null) {
            throw new IllegalArgumentException("Image is required");
        }
        if ((long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
            throw new IllegalArgumentException("Image exceeds the 16 megapixel vectorization limit");
        }
        HashMap<String, Float> tracerOptions = tracerOptions(options);
        byte[][] palette = options.preset() == Preset.BLACK_AND_WHITE
                ? blackAndWhitePalette(hasTransparency(image))
                : buildPalette(image, options.colorCount());
        return ImageTracer.imageToSVG(image, tracerOptions, palette);
    }

    private static HashMap<String, Float> tracerOptions(SvgOptions options) {
        HashMap<String, Float> values = new HashMap<>();
        float lineThreshold;
        float quadraticThreshold;
        float simplifyTolerance;
        int roundCoordinates;
        switch (options.detail()) {
            case LOW -> {
                lineThreshold = 2f;
                quadraticThreshold = 2f;
                simplifyTolerance = 1f;
                roundCoordinates = 1;
            }
            case HIGH -> {
                lineThreshold = 0.5f;
                quadraticThreshold = 0.5f;
                simplifyTolerance = 0f;
                roundCoordinates = 3;
            }
            default -> {
                lineThreshold = 1f;
                quadraticThreshold = 1f;
                simplifyTolerance = 0.25f;
                roundCoordinates = 2;
            }
        }
        values.put("ltres", lineThreshold);
        values.put("qtres", quadraticThreshold);
        values.put("pathomit", (float) options.filterSpeckle());
        values.put("colorsampling", 0f);
        values.put("numberofcolors", (float) options.colorCount());
        values.put("mincolorratio", 0f);
        values.put("colorquantcycles", options.preset() == Preset.PHOTO ? 4f : 3f);
        values.put("scale", 1f);
        values.put("simplifytolerance", simplifyTolerance);
        values.put("roundcoords", (float) roundCoordinates);
        values.put("lcpr", 0f);
        values.put("qcpr", 0f);
        values.put("desc", 0f);
        values.put("viewbox", 1f);
        values.put("blurradius", options.preset() == Preset.PHOTO ? 1f : 0f);
        values.put("blurdelta", 20f);
        return values;
    }

    private static byte[][] buildPalette(BufferedImage image, int requestedColors) {
        HistogramEntry[] histogram = histogram(image);
        List<HistogramEntry> colors = new ArrayList<>();
        boolean transparent = false;
        for (HistogramEntry entry : histogram) {
            if (entry != null) {
                colors.add(entry);
                transparent |= entry.transparentCount > 0;
            }
        }
        int opaqueColorLimit = Math.max(1, requestedColors - (transparent ? 1 : 0));
        List<ColorBox> boxes = new ArrayList<>();
        List<HistogramEntry> opaqueColors = colors.stream().filter(entry -> entry.count > 0).toList();
        if (!opaqueColors.isEmpty()) {
            boxes.add(new ColorBox(new ArrayList<>(opaqueColors)));
        }
        while (boxes.size() < opaqueColorLimit) {
            ColorBox box = boxes.stream()
                    .filter(candidate -> candidate.colors.size() > 1)
                    .max(Comparator.comparingLong(ColorBox::splitScore))
                    .orElse(null);
            if (box == null) {
                break;
            }
            boxes.remove(box);
            boxes.addAll(box.split());
        }
        List<byte[]> palette = new ArrayList<>();
        boxes.stream().sorted(Comparator.comparingInt(ColorBox::averageRgb))
                .map(ColorBox::paletteColor).forEach(palette::add);
        if (transparent) {
            palette.add(toTracerColor(0, 0, 0, 0));
        }
        if (palette.isEmpty()) {
            palette.add(toTracerColor(0, 0, 0, 0));
        }
        return palette.toArray(byte[][]::new);
    }

    private static HistogramEntry[] histogram(BufferedImage image) {
        HistogramEntry[] entries = new HistogramEntry[32 * 32 * 32];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                int index = ((red >>> 3) << 10) | ((green >>> 3) << 5) | (blue >>> 3);
                HistogramEntry entry = entries[index];
                if (entry == null) {
                    entry = new HistogramEntry();
                    entries[index] = entry;
                }
                if (alpha < 16) {
                    entry.transparentCount++;
                } else {
                    entry.count++;
                    entry.red += red;
                    entry.green += green;
                    entry.blue += blue;
                    entry.alpha += alpha;
                }
            }
        }
        return entries;
    }

    private static boolean hasTransparency(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) < 16) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[][] blackAndWhitePalette(boolean transparent) {
        byte[][] palette = new byte[transparent ? 3 : 2][];
        palette[0] = toTracerColor(0, 0, 0, 255);
        palette[1] = toTracerColor(255, 255, 255, 255);
        if (transparent) {
            palette[2] = toTracerColor(0, 0, 0, 0);
        }
        return palette;
    }

    private static byte[] toTracerColor(int red, int green, int blue, int alpha) {
        return new byte[]{(byte) (red - 128), (byte) (green - 128),
                (byte) (blue - 128), (byte) (alpha - 128)};
    }

    private static File availableOutputFile(File directory, String baseName) {
        File candidate = new File(directory, baseName + ".svg");
        int suffix = 2;
        while (candidate.exists()) {
            candidate = new File(directory, baseName + "_" + suffix + ".svg");
            suffix++;
        }
        return candidate;
    }

    private static final class HistogramEntry {
        private long count;
        private long transparentCount;
        private long red;
        private long green;
        private long blue;
        private long alpha;

        private int averageRed() {
            return (int) (red / count);
        }

        private int averageGreen() {
            return (int) (green / count);
        }

        private int averageBlue() {
            return (int) (blue / count);
        }

    }

    private static final class ColorBox {
        private final List<HistogramEntry> colors;
        private final long pixelCount;
        private final int redRange;
        private final int greenRange;
        private final int blueRange;

        private ColorBox(List<HistogramEntry> colors) {
            this.colors = colors;
            this.pixelCount = colors.stream().mapToLong(entry -> entry.count).sum();
            this.redRange = range(colors, HistogramEntry::averageRed);
            this.greenRange = range(colors, HistogramEntry::averageGreen);
            this.blueRange = range(colors, HistogramEntry::averageBlue);
        }

        private long splitScore() {
            return pixelCount * Math.max(redRange, Math.max(greenRange, blueRange));
        }

        private List<ColorBox> split() {
            java.util.function.ToIntFunction<HistogramEntry> channel;
            if (greenRange >= redRange && greenRange >= blueRange) {
                channel = HistogramEntry::averageGreen;
            } else if (blueRange >= redRange && blueRange >= greenRange) {
                channel = HistogramEntry::averageBlue;
            } else {
                channel = HistogramEntry::averageRed;
            }
            colors.sort(Comparator.comparingInt(channel));
            long midpoint = Math.max(1, pixelCount / 2);
            long accumulated = 0;
            int splitAt = 1;
            for (int i = 0; i < colors.size() - 1; i++) {
                accumulated += colors.get(i).count;
                splitAt = i + 1;
                if (accumulated >= midpoint) {
                    break;
                }
            }
            return List.of(new ColorBox(new ArrayList<>(colors.subList(0, splitAt))),
                    new ColorBox(new ArrayList<>(colors.subList(splitAt, colors.size()))));
        }

        private byte[] paletteColor() {
            long red = 0;
            long green = 0;
            long blue = 0;
            long alpha = 0;
            for (HistogramEntry color : colors) {
                red += color.red;
                green += color.green;
                blue += color.blue;
                alpha += color.alpha;
            }
            return toTracerColor((int) (red / pixelCount), (int) (green / pixelCount),
                    (int) (blue / pixelCount), (int) (alpha / pixelCount));
        }

        private int averageRgb() {
            byte[] color = paletteColor();
            return ((color[0] + 128) << 16) | ((color[1] + 128) << 8) | (color[2] + 128);
        }

        private static int range(List<HistogramEntry> colors,
                                 java.util.function.ToIntFunction<HistogramEntry> channel) {
            int minimum = 255;
            int maximum = 0;
            for (HistogramEntry color : colors) {
                int value = channel.applyAsInt(color);
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
            return maximum - minimum;
        }
    }
}
