package com.rememberber.mootool.nextfx.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rememberber.mootool.nextfx.app.ProductIdentity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Single-writer SQLite store for this product. Tests inject {@link AppPaths}.
 */
public final class SqliteDatabase implements AutoCloseable {

    private static final int SCHEMA_VERSION = 2;
    private final AppPaths paths;
    private final BlockingQueue<WriteTask<?>> writes = new ArrayBlockingQueue<>(256);
    private final Thread writer;
    private volatile boolean running = true;
    private Connection connection;

    public SqliteDatabase(AppPaths paths) {
        this.paths = Objects.requireNonNull(paths);
        this.writer = Thread.ofVirtual().name("mootool-fx-sqlite").unstarted(this::writeLoop);
    }

    public synchronized void open() {
        try {
            Files.createDirectories(paths.dataRoot());
            Files.createDirectories(paths.configRoot());
            Files.createDirectories(paths.stateRoot());
            writeProductMarker();
            connection = DriverManager.getConnection("jdbc:sqlite:" + paths.databaseFile());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            migrate();
            writer.start();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Unable to open SQLite database", exception);
        }
    }

    public Connection connection() {
        return connection;
    }

    public <T> T read(SqlWork<T> work) {
        try {
            return work.run(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public <T> CompletableFuture<T> write(SqlWork<T> work) {
        WriteTask<T> task = new WriteTask<>(work);
        if (!writes.offer(task)) {
            task.future.completeExceptionally(new IllegalStateException("SQLite write queue is full"));
        }
        return task.future;
    }

    public void writeNow(SqlWork<Void> work) {
        try {
            write(work).get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                      version INTEGER PRIMARY KEY,
                      applied_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS window_state (
                      id TEXT PRIMARY KEY,
                      payload TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS history_entry (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      tool_id TEXT NOT NULL,
                      summary TEXT NOT NULL,
                      input_text TEXT NOT NULL,
                      output_text TEXT NOT NULL,
                      created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tool_draft (
                      tool_id TEXT PRIMARY KEY,
                      payload TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )
                    """);
        }
        int current = read(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
                return result.next() ? result.getInt(1) : 0;
            }
        });
        if (current < SCHEMA_VERSION) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO schema_version(version, applied_at) VALUES (?, ?)")) {
                statement.setInt(1, SCHEMA_VERSION);
                statement.setString(2, Instant.now().toString());
                statement.executeUpdate();
            }
        }
    }

    private void writeProductMarker() throws IOException {
        Files.createDirectories(paths.dataRoot());
        Path marker = paths.productMarker();
        ObjectMapper mapper = new ObjectMapper();
        if (Files.exists(marker)) {
            String existingProductId;
            try {
                existingProductId = mapper.readTree(Files.readString(marker)).path("productId").asText("");
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to read product marker: " + marker, exception);
            }
            if (!existingProductId.isBlank() && !ProductIdentity.PRODUCT_ID.equals(existingProductId)) {
                throw new IllegalStateException("Data root belongs to another product: " + marker);
            }
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("productId", ProductIdentity.PRODUCT_ID);
        node.put("schema", SCHEMA_VERSION);
        node.put("profile", paths.identity().profile().name().toLowerCase());
        Files.writeString(marker, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }

    private void writeLoop() {
        while (running || !writes.isEmpty()) {
            try {
                WriteTask<?> task = writes.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                task.run(connection);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void close() {
        running = false;
        writer.interrupt();
        try {
            writer.join(2_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Closed on shutdown.
            }
        }
    }

    public WindowStateStore windowState() {
        return new WindowStateStore(this);
    }

    public HistoryStore history() {
        return new HistoryStore(this);
    }

    public DraftStore drafts() {
        return new DraftStore(this);
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final class WriteTask<T> {
        private final SqlWork<T> work;
        private final CompletableFuture<T> future = new CompletableFuture<>();

        private WriteTask(SqlWork<T> work) {
            this.work = work;
        }

        private void run(Connection connection) {
            try {
                future.complete(work.run(connection));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        }
    }

    public static final class WindowStateStore {
        private final SqliteDatabase database;

        private WindowStateStore(SqliteDatabase database) {
            this.database = database;
        }

        public void save(String id, String payload) {
            database.writeNow(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO window_state(id, payload, updated_at) VALUES (?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at
                        """)) {
                    statement.setString(1, id);
                    statement.setString(2, payload);
                    statement.setString(3, Instant.now().toString());
                    statement.executeUpdate();
                }
                return null;
            });
        }

        public String load(String id) {
            return database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT payload FROM window_state WHERE id = ?")) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getString(1) : null;
                    }
                }
            });
        }
    }

    public static final class HistoryStore {
        private final SqliteDatabase database;

        private HistoryStore(SqliteDatabase database) {
            this.database = database;
        }

        public void save(String toolId, String summary, String input, String output) {
            database.writeNow(connection -> {
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO history_entry(tool_id, summary, input_text, output_text, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    insert.setString(1, toolId);
                    insert.setString(2, summary);
                    insert.setString(3, input);
                    insert.setString(4, output);
                    insert.setString(5, Instant.now().toString());
                    insert.executeUpdate();
                }
                try (PreparedStatement trim = connection.prepareStatement("""
                        DELETE FROM history_entry WHERE tool_id = ? AND id NOT IN (
                          SELECT id FROM history_entry WHERE tool_id = ? ORDER BY id DESC LIMIT 200
                        )
                        """)) {
                    trim.setString(1, toolId);
                    trim.setString(2, toolId);
                    trim.executeUpdate();
                }
                return null;
            });
        }

        public List<HistoryRow> latest(String toolId, int limit) {
            return database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT summary, input_text, output_text, created_at
                        FROM history_entry WHERE tool_id = ? ORDER BY id DESC LIMIT ?
                        """)) {
                    statement.setString(1, toolId);
                    statement.setInt(2, limit);
                    try (ResultSet result = statement.executeQuery()) {
                        List<HistoryRow> rows = new ArrayList<>();
                        while (result.next()) {
                            rows.add(new HistoryRow(
                                    result.getString(1),
                                    result.getString(2),
                                    result.getString(3),
                                    result.getString(4)
                            ));
                        }
                        return List.copyOf(rows);
                    }
                }
            });
        }
    }

    public record HistoryRow(String summary, String input, String output, String createdAt) {
    }

    public static final class DraftStore {
        private final SqliteDatabase database;

        private DraftStore(SqliteDatabase database) {
            this.database = database;
        }

        public void save(String toolId, String payload) {
            database.writeNow(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO tool_draft(tool_id, payload, updated_at) VALUES (?, ?, ?)
                        ON CONFLICT(tool_id) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at
                        """)) {
                    statement.setString(1, toolId);
                    statement.setString(2, payload);
                    statement.setString(3, Instant.now().toString());
                    statement.executeUpdate();
                }
                return null;
            });
        }

        public String load(String toolId) {
            return database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT payload FROM tool_draft WHERE tool_id = ?")) {
                    statement.setString(1, toolId);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next() ? result.getString(1) : null;
                    }
                }
            });
        }
    }
}
