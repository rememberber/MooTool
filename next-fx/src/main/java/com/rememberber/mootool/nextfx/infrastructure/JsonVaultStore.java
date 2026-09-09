package com.rememberber.mootool.nextfx.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Product-local JSON Vault. Files live under {@code data/vaults/json}; Git is out of scope for P2.
 */
public final class JsonVaultStore {

    public static final int MAX_BYTES = 20 * 1024 * 1024;
    public static final int MAX_DEPTH = 16;
    public static final int MAX_ENTRIES = 2000;

    public record Node(String name, String relativePath, boolean directory, Instant modifiedAt, List<Node> children) {
        public Node {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    private final Path root;

    public JsonVaultStore(Path root) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create JSON Vault", exception);
        }
    }

    public List<Node> list() {
        ensureRoot();
        Counter counter = new Counter();
        return listDirectory(root, "", 0, counter);
    }

    public String read(String relativePath) {
        Path target = resolveExisting(relativePath, true);
        try {
            if (Files.size(target) > MAX_BYTES) {
                throw new IllegalArgumentException("Vault file exceeds 20 MB limit");
            }
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Vault file", exception);
        }
    }

    public void save(String relativePath, String content) {
        if (content == null || content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Invalid Vault file content");
        }
        Path target = resolveInside(relativePath);
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target) && Files.isDirectory(target)) {
                throw new IllegalArgumentException("Invalid Vault file");
            }
            if (Files.isSymbolicLink(target)) {
                throw new IllegalArgumentException("Invalid Vault file");
            }
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save Vault file", exception);
        }
    }

    public String createFile(String directory, String name) {
        String fileName = sanitizeName(name, true);
        String relative = join(directory, fileName);
        Path target = resolveInside(relative);
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                throw new IllegalArgumentException("Vault entry already exists");
            }
            Files.writeString(target, "{\n}\n", StandardCharsets.UTF_8);
            return relative;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Vault file", exception);
        }
    }

    public String createFolder(String directory, String name) {
        ensureRoot();
        String folderName = sanitizeName(name, false);
        String relative = join(directory, folderName);
        Path target = resolveInside(relative);
        try {
            Files.createDirectories(target.getParent() == null ? root : target.getParent());
            Files.createDirectory(target);
            return relative;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Vault folder", exception);
        }
    }

    public void delete(String relativePath) {
        Path target = resolveExisting(relativePath, false);
        try {
            if (Files.isDirectory(target)) {
                try (var stream = Files.walk(target)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("Unable to delete Vault entry", exception);
                        }
                    });
                }
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete Vault entry", exception);
        }
    }

    private List<Node> listDirectory(Path directory, String relative, int depth, Counter counter) {
        if (depth > MAX_DEPTH) {
            return List.of();
        }
        List<Node> nodes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            List<Path> entries = new ArrayList<>();
            stream.forEach(entries::add);
            entries.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
            for (Path entry : entries) {
                if (counter.count >= MAX_ENTRIES) {
                    break;
                }
                if (Files.isSymbolicLink(entry) || entry.getFileName().toString().startsWith(".")) {
                    continue;
                }
                counter.count++;
                String name = entry.getFileName().toString();
                String childRelative = relative.isEmpty() ? name : relative + "/" + name;
                Instant modified = Files.getLastModifiedTime(entry).toInstant();
                if (Files.isDirectory(entry)) {
                    nodes.add(new Node(name, childRelative, true, modified, listDirectory(entry, childRelative, depth + 1, counter)));
                } else {
                    nodes.add(new Node(name, childRelative, false, modified, List.of()));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list JSON Vault", exception);
        }
        return List.copyOf(nodes);
    }

    private Path resolveExisting(String relativePath, boolean fileOnly) {
        Path target = resolveInside(relativePath);
        if (!Files.exists(target)) {
            throw new IllegalArgumentException("Vault entry not found");
        }
        if (fileOnly && !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Invalid Vault file");
        }
        return target;
    }

    Path resolveInside(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return root;
        }
        Path current = root;
        for (String part : relativePath.replace('\\', '/').split("/")) {
            if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("Vault path escapes the library root");
            }
            current = current.resolve(part);
        }
        Path normalized = current.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IllegalArgumentException("Vault path escapes the library root");
        }
        return normalized;
    }

    static String sanitizeName(String name, boolean file) {
        if (name == null) {
            throw new IllegalArgumentException("Invalid Vault name");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")
                || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("\0")) {
            throw new IllegalArgumentException("Invalid Vault name");
        }
        if (file && !trimmed.toLowerCase(Locale.ROOT).endsWith(".json")) {
            trimmed = trimmed + ".json";
        }
        return trimmed;
    }

    private static String join(String directory, String name) {
        if (directory == null || directory.isBlank()) {
            return name;
        }
        return directory.replace('\\', '/') + "/" + name;
    }

    private static final class Counter {
        private int count;
    }
}
