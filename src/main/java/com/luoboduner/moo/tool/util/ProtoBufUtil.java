package com.luoboduner.moo.tool.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Protobuf 编解码工具
 */
public class ProtoBufUtil {

    private static final String[] WIRE_TYPE_NAMES = {
            "Varint", "64-bit", "Length-delimited", "Start group", "End group", "32-bit"
    };

    private ProtoBufUtil() {
    }

    /**
     * JSON 转 Protobuf 二进制
     */
    public static byte[] jsonToBinary(String protoContent, String messageName, String json) throws Exception {
        Descriptors.Descriptor descriptor = resolveMessageDescriptor(protoContent, messageName);
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder.build().toByteArray();
    }

    /**
     * Protobuf 二进制转 JSON
     */
    public static String binaryToJson(String protoContent, String messageName, byte[] binary) throws Exception {
        Descriptors.Descriptor descriptor = resolveMessageDescriptor(protoContent, messageName);
        DynamicMessage message = DynamicMessage.parseFrom(descriptor, binary);
        return JsonFormat.printer().includingDefaultValueFields().print(message);
    }

    /**
     * 解码 Protobuf Wire Format（无需 .proto 定义）
     */
    public static String decodeWireFormat(byte[] data) throws IOException {
        StringBuilder sb = new StringBuilder();
        CodedInputStream input = CodedInputStream.newInstance(data);
        int fieldIndex = 0;
        while (!input.isAtEnd()) {
            int tag = input.readTag();
            if (tag == 0) {
                break;
            }
            int fieldNumber = tag >>> 3;
            int wireType = tag & 0x7;
            fieldIndex++;
            sb.append(String.format("#%d  field=%d  wire_type=%d (%s)",
                    fieldIndex, fieldNumber, wireType, wireTypeName(wireType)));
            sb.append("  value=");
            sb.append(readWireValue(input, wireType));
            sb.append('\n');
        }
        if (fieldIndex == 0) {
            sb.append("(空数据或无法解析)");
        }
        return sb.toString().trim();
    }

    /**
     * 格式化 .proto 文件（基础缩进）
     */
    public static String formatProto(String proto) {
        if (StrUtil.isBlank(proto)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        char stringChar = 0;
        for (int i = 0; i < proto.length(); i++) {
            char c = proto.charAt(i);
            if (inString) {
                result.append(c);
                if (c == stringChar && (i == 0 || proto.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                result.append(c);
                continue;
            }
            if (c == '{') {
                result.append(" {\n");
                indent++;
                result.append("    ".repeat(indent));
                continue;
            }
            if (c == '}') {
                indent = Math.max(0, indent - 1);
                result.append("\n").append("    ".repeat(indent)).append('}');
                continue;
            }
            if (c == ';') {
                result.append(";\n").append("    ".repeat(indent));
                continue;
            }
            if (c == '\n' || c == '\r') {
                continue;
            }
            result.append(c);
        }
        return result.toString().trim();
    }

    public static byte[] parseBinaryInput(String text, String format) {
        String trimmed = text.replaceAll("\\s+", "");
        if ("Base64".equals(format)) {
            return Base64.decode(trimmed);
        }
        return HexUtil.decodeHex(trimmed);
    }

    public static String formatBinaryOutput(byte[] data, String format) {
        if ("Base64".equals(format)) {
            return Base64.encode(data);
        }
        return HexUtil.encodeHexStr(data);
    }

    private static Descriptors.Descriptor resolveMessageDescriptor(String protoContent, String messageName) throws Exception {
        if (StrUtil.isBlank(protoContent)) {
            throw new IllegalArgumentException(".proto 定义不能为空");
        }
        if (StrUtil.isBlank(messageName)) {
            throw new IllegalArgumentException("Message 名称不能为空");
        }
        Path tempDir = Files.createTempDirectory("mootool-protobuf");
        try {
            Path protoFile = tempDir.resolve("message.proto");
            Files.writeString(protoFile, protoContent, StandardCharsets.UTF_8);
            Path descFile = tempDir.resolve("descriptor.pb");
            ProtocRunner.runProtoc(new String[]{
                    "--proto_path=" + tempDir,
                    "--descriptor_set_out=" + descFile,
                    "--include_imports",
                    protoFile.toString()
            });
            DescriptorProtos.FileDescriptorSet set =
                    DescriptorProtos.FileDescriptorSet.parseFrom(Files.readAllBytes(descFile));
            return findMessageDescriptor(set, messageName.trim());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static Descriptors.Descriptor findMessageDescriptor(
            DescriptorProtos.FileDescriptorSet set, String messageName) throws Descriptors.DescriptorValidationException {
        Map<String, Descriptors.FileDescriptor> descriptorMap = new HashMap<>();
        for (DescriptorProtos.FileDescriptorProto fdp : set.getFileList()) {
            Descriptors.FileDescriptor[] deps = new Descriptors.FileDescriptor[fdp.getDependencyCount()];
            for (int i = 0; i < fdp.getDependencyCount(); i++) {
                deps[i] = descriptorMap.get(fdp.getDependency(i));
            }
            Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(fdp, deps);
            descriptorMap.put(fdp.getName(), fd);
        }
        String simpleName = messageName.contains(".")
                ? messageName.substring(messageName.lastIndexOf('.') + 1)
                : messageName;
        for (Descriptors.FileDescriptor fd : descriptorMap.values()) {
            Descriptors.Descriptor desc = fd.findMessageTypeByName(simpleName);
            if (desc != null && (messageName.equals(desc.getName())
                    || messageName.equals(desc.getFullName())
                    || messageName.endsWith("." + desc.getName()))) {
                return desc;
            }
        }
        throw new IllegalArgumentException("未找到 Message: " + messageName);
    }

    private static String wireTypeName(int wireType) {
        if (wireType >= 0 && wireType < WIRE_TYPE_NAMES.length) {
            return WIRE_TYPE_NAMES[wireType];
        }
        return "Unknown";
    }

    private static String readWireValue(CodedInputStream input, int wireType) throws IOException {
        return switch (wireType) {
            case 0 -> String.valueOf(input.readInt64());
            case 1 -> "0x" + Long.toHexString(input.readFixed64());
            case 2 -> formatLengthDelimited(input.readByteArray());
            case 5 -> "0x" + Integer.toHexString(input.readFixed32());
            default -> "(unsupported wire type)";
        };
    }

    private static String formatLengthDelimited(byte[] bytes) {
        if (bytes.length == 0) {
            return "\"\"";
        }
        if (isPrintableAscii(bytes)) {
            return "\"" + new String(bytes, StandardCharsets.UTF_8) + "\"";
        }
        return "bytes[" + bytes.length + "]=" + HexUtil.encodeHexStr(bytes);
    }

    private static boolean isPrintableAscii(byte[] bytes) {
        for (byte b : bytes) {
            if (b < 32 || b > 126) {
                return false;
            }
        }
        return true;
    }

    private static void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                List<Path> children = new ArrayList<>();
                try (var stream = Files.list(path)) {
                    stream.forEach(children::add);
                }
                for (Path child : children) {
                    deleteRecursively(child);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
