package com.rememberber.mootool.nextfx.infrastructure;

import com.rememberber.mootool.nextfx.app.ProductIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqliteDatabaseTest {

    @TempDir
    Path temp;

    @Test
    void roundTripsWindowStateAndHistoryWithoutTouchingUserData() throws Exception {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.DEV);
        AppPaths paths = AppPaths.isolated(identity, temp);
        try (SqliteDatabase database = new SqliteDatabase(paths)) {
            database.open();
            database.windowState().save("main", "{\"width\":1440}");
            assertThat(database.windowState().load("main")).contains("1440");
            database.history().save("json", "format", "{\"a\":1}", "{\n  \"a\": 1\n}");
            assertThat(database.history().latest("json", 10)).hasSize(1);
            database.history().save("encode", "URL 编码", "你好 a/b", "%E4%BD%A0%E5%A5%BD%20a%2Fb", "{\"tab\":\"url\"}");
            assertThat(database.history().latest("encode", 1).getFirst().extra()).contains("url");
            database.drafts().save("json", "{\"text\":\"{}\"}");
            assertThat(database.drafts().load("json")).contains("{}");
            assertThat(Files.readString(paths.productMarker())).contains("next-fx");
        }
    }

    @Test
    void reopensJacksonPrettyPrintedOwnMarker() throws Exception {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.RELEASE);
        AppPaths paths = AppPaths.isolated(identity, temp.resolve("release"));
        try (SqliteDatabase database = new SqliteDatabase(paths)) {
            database.open();
        }
        assertThat(Files.readString(paths.productMarker())).contains("\"productId\" : \"next-fx\"");
        try (SqliteDatabase database = new SqliteDatabase(paths)) {
            database.open();
            database.windowState().save("main", "{\"width\":1100}");
            assertThat(database.windowState().load("main")).contains("1100");
        }
    }

    @Test
    void refusesForeignProductMarker() throws Exception {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.DEV);
        AppPaths paths = AppPaths.isolated(identity, temp.resolve("foreign"));
        Files.createDirectories(paths.dataRoot());
        Files.writeString(paths.productMarker(), """
                {
                  "productId" : "next-electron",
                  "schema" : 1
                }
                """);
        try (SqliteDatabase database = new SqliteDatabase(paths)) {
            assertThatThrownBy(database::open)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("another product");
        }
    }

    @Test
    void migratesHistoryExtraDataFromSchema2() throws Exception {
        ProductIdentity identity = ProductIdentity.load(ProductIdentity.Profile.DEV);
        AppPaths paths = AppPaths.isolated(identity, temp.resolve("schema2"));
        Files.createDirectories(paths.dataRoot());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + paths.databaseFile());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schema_version (version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            statement.execute("INSERT INTO schema_version(version, applied_at) VALUES (2, '2026-09-09T00:00:00Z')");
            statement.execute("""
                    CREATE TABLE history_entry (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      tool_id TEXT NOT NULL,
                      summary TEXT NOT NULL,
                      input_text TEXT NOT NULL,
                      output_text TEXT NOT NULL,
                      created_at TEXT NOT NULL
                    )
                    """);
        }
        try (SqliteDatabase database = new SqliteDatabase(paths)) {
            database.open();
            database.history().save("encode", "URL 编码", "你好", "%E4%BD%A0", "{\"tab\":\"url\"}");
            assertThat(database.history().latest("encode", 1).getFirst().extra()).contains("url");
        }
    }
}
