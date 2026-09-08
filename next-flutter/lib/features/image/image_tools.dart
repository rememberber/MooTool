typedef ImageOutputFormat = String;
typedef WatermarkPosition = String;

class CompressImageOptions {
  CompressImageOptions(
      {this.quality = 0.8, this.scale = 1, this.format = 'auto'});
  final double quality;
  final double scale;
  final String format;
}

class WatermarkImageOptions {
  WatermarkImageOptions({
    required this.text,
    this.opacity = 0.45,
    this.color = '#FFFFFF',
    this.position = 'bottom-right',
    this.fontSize = 'auto',
    this.diagonal = false,
  });
  final String text;
  final double opacity;
  final String color;
  final String position;
  final String fontSize;
  final bool diagonal;
}

class VectorizeOptions {
  VectorizeOptions({
    this.preset = 'poster',
    this.detail = 'medium',
    this.colorCount = 16,
    this.filterSpeckle = 4,
  });
  final String preset;
  final String detail;
  final int colorCount;
  final int filterSpeckle;
}

({int width, int height}) scaledDimensions(int width, int height, double scale) {
  final normalized = scale.clamp(0.1, 1.0);
  return (
    width: (width * normalized).round().clamp(1, width),
    height: (height * normalized).round().clamp(1, height),
  );
}

String processedImageName(String name, String suffix, [String format = 'auto']) {
  final extensionIndex = name.lastIndexOf('.');
  final base = extensionIndex > 0 ? name.substring(0, extensionIndex) : name;
  final currentExtension =
      extensionIndex > 0 ? name.substring(extensionIndex + 1).toLowerCase() : 'png';
  final extension = format == 'auto'
      ? (currentExtension == 'jpg' || currentExtension == 'jpeg' ? 'jpg' : 'png')
      : format == 'jpeg'
          ? 'jpg'
          : 'png';
  return '${base}_$suffix.$extension';
}

({double x, double y}) watermarkAnchor(int width, int height, double textWidth,
    double textHeight, String position, double margin) {
  if (position == 'top-left') return (x: margin, y: margin + textHeight);
  if (position == 'top-right') {
    return (x: width - textWidth - margin, y: margin + textHeight);
  }
  if (position == 'bottom-left') return (x: margin, y: height - margin);
  if (position == 'center') {
    return (x: (width - textWidth) / 2, y: (height + textHeight) / 2);
  }
  return (x: width - textWidth - margin, y: height - margin);
}

String ensureImageDataUrl(String value) {
  final trimmed = value.trim();
  if (RegExp(r'^data:image/[\w.+-]+;base64,').hasMatch(trimmed)) return trimmed;
  if (RegExp(r'^[A-Za-z0-9+/=\s]+$').hasMatch(trimmed)) {
    return 'data:image/png;base64,${trimmed.replaceAll(RegExp(r'\s+'), '')}';
  }
  throw const FormatException('Invalid image Base64');
}

String overwriteName(String name, String format) {
  if (format == 'auto') return name;
  final base = name.replaceAll(RegExp(r'\.[^.]+$'), '');
  return '$base.${format == 'jpeg' ? 'jpg' : 'png'}';
}

({String mime, String payload}) splitDataUrl(String dataUrl) {
  final match = RegExp(r'^data:(image/[\w.+-]+);base64,(.+)$', dotAll: true)
      .firstMatch(dataUrl.trim());
  if (match == null) throw const FormatException('Invalid image data URL');
  return (mime: match[1]!, payload: match[2]!.replaceAll(RegExp(r'\s+'), ''));
}
