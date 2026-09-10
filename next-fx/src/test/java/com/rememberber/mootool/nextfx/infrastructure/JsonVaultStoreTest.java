package com.rememberber.mootool.nextfx.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonVaultStoreTest {

    @TempDir
    Path temp;

    @Test
    void createsReadsAndRejectsPathEscape() throws Exception {
        JsonVaultStore vault = new JsonVaultStore(temp.resolve("vaults/json"));
        String file = vault.createFile("", "sample");
        assertThat(file).isEqualTo("sample.json");
        vault.save(file, "{\"ok\":true}");
        assertThat(vault.read(file)).isEqualTo("{\"ok\":true}");
        assertThat(vault.list()).extracting(JsonVaultStore.Node::name).contains("sample.json");
        assertThat(Files.exists(temp.resolve("vaults/json/sample.json"))).isTrue();
        assertThatThrownBy(() -> vault.read("../secret.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes");
        assertThatThrownBy(() -> vault.createFile("", "a/b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foldersStayInsideRoot() {
        JsonVaultStore vault = new JsonVaultStore(temp.resolve("vault"));
        String folder = vault.createFolder("", "notes");
        String nested = vault.createFile(folder, "one.json");
        assertThat(nested).isEqualTo("notes/one.json");
        vault.delete(nested);
        vault.delete(folder);
        assertThat(vault.list()).isEmpty();
    }
}
