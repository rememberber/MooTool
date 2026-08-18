use std::{fs, path::PathBuf, sync::Mutex, time::Duration};

use rusqlite::{Connection, OptionalExtension, TransactionBehavior, backup::Backup, params};

use crate::contracts::{
    image::ImageAssetSummary,
    local_data::{
        BoardMessage, HostProfile, OperationHistory, QuickNote, TranslationHistory, TranslationWord,
    },
    network::{HttpRequestHistory, SavedHttpRequest},
    product_import::{ProductImportCounts, ProductImportRecords},
    translation::TranslationProvider,
};

pub const DATABASE_FILE_NAME: &str = "mootool-tauri.sqlite3";

pub struct LocalDataRepository {
    connection: Mutex<Connection>,
}

impl LocalDataRepository {
    pub fn open(file_path: PathBuf) -> Result<Self, String> {
        if let Some(parent) = file_path.parent() {
            fs::create_dir_all(parent).map_err(|error| {
                format!(
                    "failed to create local data directory {}: {error}",
                    parent.display()
                )
            })?;
        }
        let connection = Connection::open(&file_path).map_err(|error| {
            format!(
                "failed to open local database {}: {error}",
                file_path.display()
            )
        })?;
        initialize(&connection)?;
        Ok(Self {
            connection: Mutex::new(connection),
        })
    }

    #[cfg(test)]
    pub fn open_in_memory() -> Result<Self, String> {
        let connection = Connection::open_in_memory()
            .map_err(|error| format!("failed to open in-memory database: {error}"))?;
        initialize(&connection)?;
        Ok(Self {
            connection: Mutex::new(connection),
        })
    }

    pub fn list_notes(&self) -> Result<Vec<QuickNote>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, title, content, pinned, created_at, updated_at
                 FROM quick_notes
                 ORDER BY pinned DESC, updated_at DESC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok(QuickNote {
                    id: row.get(0)?,
                    title: row.get(1)?,
                    content: row.get(2)?,
                    pinned: row.get(3)?,
                    created_at: row.get(4)?,
                    updated_at: row.get(5)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_note(&self, note: QuickNote) -> Result<QuickNote, String> {
        note.validate()?;
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO quick_notes (id, title, content, pinned, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                 ON CONFLICT(id) DO UPDATE SET
                   title = excluded.title,
                   content = excluded.content,
                   pinned = excluded.pinned,
                   updated_at = excluded.updated_at",
                params![
                    note.id,
                    note.title,
                    note.content,
                    note.pinned,
                    note.created_at,
                    note.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(note)
    }

    pub fn delete_note(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM quick_notes WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_messages(&self) -> Result<Vec<BoardMessage>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, content, color, pinned, created_at, updated_at
                 FROM board_messages
                 ORDER BY pinned DESC, updated_at DESC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok(BoardMessage {
                    id: row.get(0)?,
                    content: row.get(1)?,
                    color: row.get(2)?,
                    pinned: row.get(3)?,
                    created_at: row.get(4)?,
                    updated_at: row.get(5)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_message(&self, message: BoardMessage) -> Result<BoardMessage, String> {
        message.validate()?;
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO board_messages (id, content, color, pinned, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                 ON CONFLICT(id) DO UPDATE SET
                   content = excluded.content,
                   color = excluded.color,
                   pinned = excluded.pinned,
                   updated_at = excluded.updated_at",
                params![
                    message.id,
                    message.content,
                    message.color,
                    message.pinned,
                    message.created_at,
                    message.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(message)
    }

    pub fn delete_message(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM board_messages WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_host_profiles(&self) -> Result<Vec<HostProfile>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, name, content, created_at, updated_at
                 FROM host_profiles ORDER BY updated_at DESC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok(HostProfile {
                    id: row.get(0)?,
                    name: row.get(1)?,
                    content: row.get(2)?,
                    created_at: row.get(3)?,
                    updated_at: row.get(4)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_host_profile(&self, profile: HostProfile) -> Result<HostProfile, String> {
        profile.validate()?;
        self.lock()?
            .execute(
                "INSERT INTO host_profiles (id, name, content, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5)
                 ON CONFLICT(id) DO UPDATE SET
                   name = excluded.name,
                   content = excluded.content,
                   updated_at = excluded.updated_at",
                params![
                    profile.id,
                    profile.name,
                    profile.content,
                    profile.created_at,
                    profile.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(profile)
    }

    pub fn delete_host_profile(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM host_profiles WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_http_requests(&self, query: &str) -> Result<Vec<SavedHttpRequest>, String> {
        if query.chars().count() > 256 {
            return Err("HTTP saved-request search cannot exceed 256 characters".into());
        }
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT payload_json FROM http_saved_requests
                 ORDER BY updated_at DESC, id ASC LIMIT 1000",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| row.get::<_, String>(0))
            .map_err(database_error)?;
        let needle = query.trim().to_lowercase();
        let mut items = Vec::new();
        for row in rows {
            let payload = row.map_err(database_error)?;
            let item = serde_json::from_str::<SavedHttpRequest>(&payload)
                .map_err(|error| format!("failed to decode saved HTTP request: {error}"))?;
            if needle.is_empty()
                || item.name.to_lowercase().contains(&needle)
                || item.request.url.to_lowercase().contains(&needle)
            {
                items.push(item);
            }
        }
        Ok(items)
    }

    pub fn save_http_request(
        &self,
        mut item: SavedHttpRequest,
    ) -> Result<SavedHttpRequest, String> {
        item.validate()?;
        let connection = self.lock()?;
        let existing = connection
            .query_row(
                "SELECT id, created_at FROM http_saved_requests WHERE name = ?1",
                [item.name.trim()],
                |row| Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?)),
            )
            .optional()
            .map_err(database_error)?;
        if let Some((id, created_at)) = existing {
            item.id = id;
            item.created_at = created_at;
        }
        item.name = item.name.trim().to_string();
        let payload = serde_json::to_string(&item)
            .map_err(|error| format!("failed to encode saved HTTP request: {error}"))?;
        if payload.len() > 12 * 1024 * 1024 {
            return Err("saved HTTP request exceeds the 12 MiB storage limit".into());
        }
        connection
            .execute(
                "INSERT INTO http_saved_requests (id, name, payload_json, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5)
                 ON CONFLICT(id) DO UPDATE SET
                   name = excluded.name,
                   payload_json = excluded.payload_json,
                   updated_at = excluded.updated_at",
                params![
                    item.id,
                    item.name,
                    payload,
                    item.created_at,
                    item.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(item)
    }

    pub fn delete_http_request(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM http_saved_requests WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn record_http_history(&self, item: HttpRequestHistory) -> Result<(), String> {
        item.validate()?;
        let payload = serde_json::to_string(&item)
            .map_err(|error| format!("failed to encode HTTP request history: {error}"))?;
        if payload.len() > 12 * 1024 * 1024 {
            return Err("HTTP request history exceeds the 12 MiB storage limit".into());
        }
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO http_request_history (id, payload_json, created_at)
                 VALUES (?1, ?2, ?3)",
                params![item.id, payload, item.created_at],
            )
            .map_err(database_error)?;
        connection
            .execute(
                "DELETE FROM http_request_history WHERE id NOT IN (
                   SELECT id FROM http_request_history ORDER BY created_at DESC, id DESC LIMIT 500
                 )",
                [],
            )
            .map_err(database_error)?;
        Ok(())
    }

    pub fn list_http_history(&self, query: &str) -> Result<Vec<HttpRequestHistory>, String> {
        if query.chars().count() > 256 {
            return Err("HTTP history search cannot exceed 256 characters".into());
        }
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT payload_json FROM http_request_history
                 ORDER BY created_at DESC, id DESC LIMIT 500",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| row.get::<_, String>(0))
            .map_err(database_error)?;
        let needle = query.trim().to_lowercase();
        let mut items = Vec::new();
        for row in rows {
            let payload = row.map_err(database_error)?;
            let item = serde_json::from_str::<HttpRequestHistory>(&payload)
                .map_err(|error| format!("failed to decode HTTP request history: {error}"))?;
            if needle.is_empty()
                || item.request.name.to_lowercase().contains(&needle)
                || item.request.url.to_lowercase().contains(&needle)
            {
                items.push(item);
            }
        }
        Ok(items)
    }

    pub fn delete_http_history(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM http_request_history WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn clear_http_history(&self) -> Result<usize, String> {
        self.lock()?
            .execute("DELETE FROM http_request_history", [])
            .map_err(database_error)
    }

    pub fn list_translation_words(&self) -> Result<Vec<TranslationWord>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, source_text, target_text, source_lang, target_lang, remark,
                        created_at, updated_at
                 FROM translation_words ORDER BY updated_at DESC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok(TranslationWord {
                    id: row.get(0)?,
                    source_text: row.get(1)?,
                    target_text: row.get(2)?,
                    source_lang: row.get(3)?,
                    target_lang: row.get(4)?,
                    remark: row.get(5)?,
                    created_at: row.get(6)?,
                    updated_at: row.get(7)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_translation_word(&self, word: TranslationWord) -> Result<TranslationWord, String> {
        word.validate()?;
        self.lock()?
            .execute(
                "INSERT INTO translation_words
                   (id, source_text, target_text, source_lang, target_lang, remark, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                 ON CONFLICT(id) DO UPDATE SET
                   source_text = excluded.source_text,
                   target_text = excluded.target_text,
                   source_lang = excluded.source_lang,
                   target_lang = excluded.target_lang,
                   remark = excluded.remark,
                   updated_at = excluded.updated_at",
                params![
                    word.id,
                    word.source_text,
                    word.target_text,
                    word.source_lang,
                    word.target_lang,
                    word.remark,
                    word.created_at,
                    word.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(word)
    }

    pub fn delete_translation_word(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM translation_words WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_translation_history(&self) -> Result<Vec<TranslationHistory>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, source_text, target_text, source_lang, target_lang, provider, created_at
                 FROM translation_history ORDER BY created_at DESC, id DESC LIMIT 500",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                let provider: String = row.get(5)?;
                Ok(TranslationHistory {
                    id: row.get(0)?,
                    source_text: row.get(1)?,
                    target_text: row.get(2)?,
                    source_lang: row.get(3)?,
                    target_lang: row.get(4)?,
                    provider: if provider == "bing" {
                        TranslationProvider::Bing
                    } else {
                        TranslationProvider::Google
                    },
                    created_at: row.get(6)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_translation_history(
        &self,
        history: TranslationHistory,
    ) -> Result<TranslationHistory, String> {
        history.validate()?;
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO translation_history
                   (id, source_text, target_text, source_lang, target_lang, provider, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                params![
                    history.id,
                    history.source_text,
                    history.target_text,
                    history.source_lang,
                    history.target_lang,
                    match history.provider {
                        TranslationProvider::Google => "google",
                        TranslationProvider::Bing => "bing",
                    },
                    history.created_at
                ],
            )
            .map_err(database_error)?;
        connection
            .execute(
                "DELETE FROM translation_history
                 WHERE id NOT IN (
                   SELECT id FROM translation_history ORDER BY created_at DESC, id DESC LIMIT 500
                 )",
                [],
            )
            .map_err(database_error)?;
        Ok(history)
    }

    pub fn delete_translation_history(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM translation_history WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn clear_translation_history(&self) -> Result<usize, String> {
        self.lock()?
            .execute("DELETE FROM translation_history", [])
            .map_err(database_error)
    }

    pub fn list_image_assets(&self) -> Result<Vec<ImageAssetSummary>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT name, mime_type, width, height, size_bytes, updated_at
                 FROM image_assets ORDER BY updated_at DESC, name ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], image_summary_from_row)
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn get_image_asset(&self, name: &str) -> Result<Option<ImageAssetSummary>, String> {
        validate_image_asset_name(name)?;
        self.lock()?
            .query_row(
                "SELECT name, mime_type, width, height, size_bytes, updated_at
                 FROM image_assets WHERE name = ?1",
                [name],
                image_summary_from_row,
            )
            .map(Some)
            .or_else(|error| match error {
                rusqlite::Error::QueryReturnedNoRows => Ok(None),
                other => Err(other),
            })
            .map_err(database_error)
    }

    pub fn save_image_asset(&self, asset: &ImageAssetSummary) -> Result<(), String> {
        self.lock()?
            .execute(
                "INSERT INTO image_assets (name, mime_type, width, height, size_bytes, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                 ON CONFLICT(name) DO UPDATE SET
                   mime_type = excluded.mime_type,
                   width = excluded.width,
                   height = excluded.height,
                   size_bytes = excluded.size_bytes,
                   updated_at = excluded.updated_at",
                params![
                    asset.name,
                    asset.mime_type,
                    i64::from(asset.width),
                    i64::from(asset.height),
                    i64::try_from(asset.size_bytes).unwrap_or(i64::MAX),
                    asset.updated_at
                ],
            )
            .map(|_| ())
            .map_err(database_error)
    }

    pub fn rename_image_asset(
        &self,
        name: &str,
        next_name: &str,
        updated_at: i64,
    ) -> Result<ImageAssetSummary, String> {
        validate_image_asset_name(name)?;
        validate_image_asset_name(next_name)?;
        let connection = self.lock()?;
        let affected = connection
            .execute(
                "UPDATE image_assets SET name = ?2, updated_at = ?3 WHERE name = ?1",
                params![name, next_name, updated_at],
            )
            .map_err(database_error)?;
        if affected == 0 {
            return Err("image asset not found".into());
        }
        connection
            .query_row(
                "SELECT name, mime_type, width, height, size_bytes, updated_at
                 FROM image_assets WHERE name = ?1",
                [next_name],
                image_summary_from_row,
            )
            .map_err(database_error)
    }

    pub fn delete_image_asset(&self, name: &str) -> Result<bool, String> {
        validate_image_asset_name(name)?;
        self.lock()?
            .execute("DELETE FROM image_assets WHERE name = ?1", [name])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn record_operation(
        &self,
        entry: OperationHistory,
        history_limit: u16,
    ) -> Result<OperationHistory, String> {
        entry.validate()?;
        if !(10..=5000).contains(&history_limit) {
            return Err("operation history limit must be between 10 and 5000".into());
        }
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO operation_history (id, tool_id, action, summary, status, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                params![
                    entry.id,
                    entry.tool_id,
                    entry.action,
                    entry.summary,
                    entry.status,
                    entry.created_at
                ],
            )
            .map_err(database_error)?;
        connection
            .execute(
                "DELETE FROM operation_history
                 WHERE id NOT IN (
                   SELECT id FROM operation_history ORDER BY created_at DESC, id DESC LIMIT ?1
                 )",
                [i64::from(history_limit)],
            )
            .map_err(database_error)?;
        Ok(entry)
    }

    pub fn list_operations(&self, limit: u16) -> Result<Vec<OperationHistory>, String> {
        if !(1..=5000).contains(&limit) {
            return Err("operation history query limit must be between 1 and 5000".into());
        }
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, tool_id, action, summary, status, created_at
                 FROM operation_history ORDER BY created_at DESC, id DESC LIMIT ?1",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([i64::from(limit)], |row| {
                Ok(OperationHistory {
                    id: row.get(0)?,
                    tool_id: row.get(1)?,
                    action: row.get(2)?,
                    summary: row.get(3)?,
                    status: row.get(4)?,
                    created_at: row.get(5)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn delete_operation(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM operation_history WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn clear_operations(&self) -> Result<usize, String> {
        self.lock()?
            .execute("DELETE FROM operation_history", [])
            .map_err(database_error)
    }

    pub fn has_product_import(&self, fingerprint: &str) -> Result<bool, String> {
        validate_import_marker(fingerprint, "fingerprint")?;
        self.lock()?
            .query_row(
                "SELECT EXISTS(SELECT 1 FROM product_import_runs WHERE fingerprint = ?1)",
                [fingerprint],
                |row| row.get(0),
            )
            .map_err(database_error)
    }

    pub fn import_product_records(
        &self,
        source_product: &str,
        source_path: &str,
        fingerprint: &str,
        records: ProductImportRecords,
        history_limit: u16,
        imported_at: i64,
    ) -> Result<(ProductImportCounts, ProductImportCounts), String> {
        validate_import_marker(source_product, "source product")?;
        validate_import_marker(fingerprint, "fingerprint")?;
        if source_path.is_empty() || source_path.len() > 4_096 || imported_at < 0 {
            return Err("invalid product import metadata".into());
        }
        if !(10..=5000).contains(&history_limit) {
            return Err("product import history limit must be between 10 and 5000".into());
        }
        for note in &records.quick_notes {
            note.validate()?;
        }
        for profile in &records.host_profiles {
            profile.validate()?;
        }
        for word in &records.translation_words {
            word.validate()?;
        }
        for history in &records.translation_history {
            history.validate()?;
        }
        for operation in &records.operation_history {
            operation.validate()?;
        }
        for image in &records.images {
            validate_image_asset_name(&image.name)?;
            if !matches!(
                image.mime_type.as_str(),
                "image/png" | "image/jpeg" | "image/webp" | "image/gif"
            ) || image.width == 0
                || image.height == 0
                || image.width > 50_000
                || image.height > 50_000
                || image.size_bytes == 0
                || image.size_bytes > 20 * 1024 * 1024
            {
                return Err("invalid imported image metadata".into());
            }
        }

        let expected = records.counts();
        let mut imported = ProductImportCounts::default();
        let mut connection = self.lock()?;
        let transaction = connection
            .transaction_with_behavior(TransactionBehavior::Immediate)
            .map_err(database_error)?;
        let exists: bool = transaction
            .query_row(
                "SELECT EXISTS(SELECT 1 FROM product_import_runs WHERE fingerprint = ?1)",
                [fingerprint],
                |row| row.get(0),
            )
            .map_err(database_error)?;
        if exists {
            return Err("this product source snapshot was already imported".into());
        }

        for note in records.quick_notes {
            imported.quick_notes += transaction
                .execute(
                    "INSERT OR IGNORE INTO quick_notes (id, title, content, pinned, created_at, updated_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                    params![note.id, note.title, note.content, note.pinned, note.created_at, note.updated_at],
                )
                .map_err(database_error)?;
        }
        for profile in records.host_profiles {
            imported.host_profiles += transaction
                .execute(
                    "INSERT OR IGNORE INTO host_profiles (id, name, content, created_at, updated_at)
                     VALUES (?1, ?2, ?3, ?4, ?5)",
                    params![profile.id, profile.name, profile.content, profile.created_at, profile.updated_at],
                )
                .map_err(database_error)?;
        }
        for word in records.translation_words {
            imported.translation_words += transaction
                .execute(
                    "INSERT OR IGNORE INTO translation_words
                       (id, source_text, target_text, source_lang, target_lang, remark, created_at, updated_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
                    params![word.id, word.source_text, word.target_text, word.source_lang, word.target_lang, word.remark, word.created_at, word.updated_at],
                )
                .map_err(database_error)?;
        }
        for history in records.translation_history {
            imported.translation_history += transaction
                .execute(
                    "INSERT OR IGNORE INTO translation_history
                       (id, source_text, target_text, source_lang, target_lang, provider, created_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                    params![
                        history.id,
                        history.source_text,
                        history.target_text,
                        history.source_lang,
                        history.target_lang,
                        match history.provider {
                            TranslationProvider::Google => "google",
                            TranslationProvider::Bing => "bing",
                        },
                        history.created_at
                    ],
                )
                .map_err(database_error)?;
        }
        for operation in records.operation_history {
            imported.operation_history += transaction
                .execute(
                    "INSERT OR IGNORE INTO operation_history
                       (id, tool_id, action, summary, status, created_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                    params![
                        operation.id,
                        operation.tool_id,
                        operation.action,
                        operation.summary,
                        operation.status,
                        operation.created_at
                    ],
                )
                .map_err(database_error)?;
        }
        for image in records.images {
            imported.images += transaction
                .execute(
                    "INSERT OR IGNORE INTO image_assets
                       (name, mime_type, width, height, size_bytes, updated_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                    params![
                        image.name,
                        image.mime_type,
                        i64::from(image.width),
                        i64::from(image.height),
                        i64::try_from(image.size_bytes).unwrap_or(i64::MAX),
                        image.updated_at
                    ],
                )
                .map_err(database_error)?;
        }
        transaction
            .execute(
                "DELETE FROM operation_history WHERE id NOT IN (
                   SELECT id FROM operation_history ORDER BY created_at DESC, id DESC LIMIT ?1
                 )",
                [i64::from(history_limit)],
            )
            .map_err(database_error)?;
        transaction
            .execute(
                "DELETE FROM translation_history WHERE id NOT IN (
                   SELECT id FROM translation_history ORDER BY created_at DESC, id DESC LIMIT 500
                 )",
                [],
            )
            .map_err(database_error)?;
        let report = serde_json::to_string(&imported)
            .map_err(|error| format!("failed to serialize product import report: {error}"))?;
        transaction
            .execute(
                "INSERT INTO product_import_runs
                   (fingerprint, source_product, source_path, imported_at, report_json)
                 VALUES (?1, ?2, ?3, ?4, ?5)",
                params![
                    fingerprint,
                    source_product,
                    source_path,
                    imported_at,
                    report
                ],
            )
            .map_err(database_error)?;
        transaction.commit().map_err(database_error)?;

        let skipped = ProductImportCounts {
            quick_notes: expected.quick_notes.saturating_sub(imported.quick_notes),
            host_profiles: expected
                .host_profiles
                .saturating_sub(imported.host_profiles),
            translation_words: expected
                .translation_words
                .saturating_sub(imported.translation_words),
            translation_history: expected
                .translation_history
                .saturating_sub(imported.translation_history),
            operation_history: expected
                .operation_history
                .saturating_sub(imported.operation_history),
            images: expected.images.saturating_sub(imported.images),
            ..ProductImportCounts::default()
        };
        Ok((imported, skipped))
    }

    pub fn backup_to(&self, destination: &PathBuf) -> Result<(), String> {
        if let Some(parent) = destination.parent() {
            fs::create_dir_all(parent)
                .map_err(|error| format!("failed to create database backup directory: {error}"))?;
        }
        if destination.exists() {
            fs::remove_file(destination)
                .map_err(|error| format!("failed to replace database backup: {error}"))?;
        }
        let source = self.lock()?;
        let mut target = Connection::open(destination)
            .map_err(|error| format!("failed to create database backup: {error}"))?;
        let backup = Backup::new(&source, &mut target)
            .map_err(|error| format!("failed to initialize database backup: {error}"))?;
        backup
            .run_to_completion(64, Duration::from_millis(5), None)
            .map_err(|error| format!("failed to write database backup: {error}"))
    }

    pub fn restore_from(&self, source_path: &PathBuf) -> Result<(), String> {
        let source = Connection::open(source_path)
            .map_err(|error| format!("failed to open backup database: {error}"))?;
        let mut target = self.lock()?;
        {
            let backup = Backup::new(&source, &mut target)
                .map_err(|error| format!("failed to initialize database restore: {error}"))?;
            backup
                .run_to_completion(64, Duration::from_millis(5), None)
                .map_err(|error| format!("failed to restore database: {error}"))?;
        }
        initialize(&target)
    }

    fn lock(&self) -> Result<std::sync::MutexGuard<'_, Connection>, String> {
        self.connection
            .lock()
            .map_err(|_| "local data repository state poisoned".to_string())
    }
}

fn initialize(connection: &Connection) -> Result<(), String> {
    connection
        .execute_batch(
            "PRAGMA journal_mode = WAL;
             PRAGMA foreign_keys = ON;
             PRAGMA busy_timeout = 5000;
             CREATE TABLE IF NOT EXISTS quick_notes (
               id TEXT PRIMARY KEY NOT NULL,
               title TEXT NOT NULL,
               content TEXT NOT NULL,
               pinned INTEGER NOT NULL DEFAULT 0,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS quick_notes_order
               ON quick_notes (pinned DESC, updated_at DESC);
             CREATE TABLE IF NOT EXISTS board_messages (
               id TEXT PRIMARY KEY NOT NULL,
               content TEXT NOT NULL,
               color TEXT NOT NULL,
               pinned INTEGER NOT NULL DEFAULT 0,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS board_messages_order
               ON board_messages (pinned DESC, updated_at DESC);
             CREATE TABLE IF NOT EXISTS host_profiles (
               id TEXT PRIMARY KEY NOT NULL,
               name TEXT NOT NULL,
               content TEXT NOT NULL,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS host_profiles_order
               ON host_profiles (updated_at DESC);
             CREATE TABLE IF NOT EXISTS http_saved_requests (
               id TEXT PRIMARY KEY NOT NULL,
               name TEXT NOT NULL UNIQUE,
               payload_json TEXT NOT NULL,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS http_saved_requests_order
               ON http_saved_requests (updated_at DESC);
             CREATE TABLE IF NOT EXISTS http_request_history (
               id TEXT PRIMARY KEY NOT NULL,
               payload_json TEXT NOT NULL,
               created_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS http_request_history_order
               ON http_request_history (created_at DESC);
             CREATE TABLE IF NOT EXISTS translation_words (
               id TEXT PRIMARY KEY NOT NULL,
               source_text TEXT NOT NULL,
               target_text TEXT NOT NULL,
               source_lang TEXT NOT NULL,
               target_lang TEXT NOT NULL,
               remark TEXT NOT NULL DEFAULT '',
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS translation_words_order
               ON translation_words (updated_at DESC);
             CREATE TABLE IF NOT EXISTS translation_history (
               id TEXT PRIMARY KEY NOT NULL,
               source_text TEXT NOT NULL,
               target_text TEXT NOT NULL,
               source_lang TEXT NOT NULL,
               target_lang TEXT NOT NULL,
               provider TEXT NOT NULL,
               created_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS translation_history_order
               ON translation_history (created_at DESC);
             CREATE TABLE IF NOT EXISTS image_assets (
               name TEXT PRIMARY KEY NOT NULL,
               mime_type TEXT NOT NULL,
               width INTEGER NOT NULL,
               height INTEGER NOT NULL,
               size_bytes INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS image_assets_order
               ON image_assets (updated_at DESC);
             CREATE TABLE IF NOT EXISTS operation_history (
               id TEXT PRIMARY KEY NOT NULL,
               tool_id TEXT NOT NULL,
               action TEXT NOT NULL,
               summary TEXT NOT NULL,
               status TEXT NOT NULL,
               created_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS operation_history_order
               ON operation_history (created_at DESC);
             CREATE INDEX IF NOT EXISTS operation_history_tool
               ON operation_history (tool_id, created_at DESC);
             CREATE TABLE IF NOT EXISTS product_import_runs (
               fingerprint TEXT PRIMARY KEY NOT NULL,
               source_product TEXT NOT NULL,
               source_path TEXT NOT NULL,
               imported_at INTEGER NOT NULL,
               report_json TEXT NOT NULL
             );
             PRAGMA user_version = 7;",
        )
        .map_err(database_error)
}

fn validate_delete_id(id: &str) -> Result<(), String> {
    if id.is_empty() || id.len() > 128 {
        return Err("invalid local record ID".into());
    }
    Ok(())
}

fn validate_image_asset_name(name: &str) -> Result<(), String> {
    if name.is_empty()
        || name.len() > 720
        || name
            .chars()
            .any(|character| character.is_control() || matches!(character, '/' | '\\'))
    {
        return Err("invalid image asset name".into());
    }
    Ok(())
}

fn validate_import_marker(value: &str, label: &str) -> Result<(), String> {
    if value.is_empty()
        || value.len() > 128
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
    {
        return Err(format!("invalid product import {label}"));
    }
    Ok(())
}

fn database_error(error: rusqlite::Error) -> String {
    format!("local database operation failed: {error}")
}

fn image_summary_from_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<ImageAssetSummary> {
    let width: i64 = row.get(2)?;
    let height: i64 = row.get(3)?;
    let size_bytes: i64 = row.get(4)?;
    Ok(ImageAssetSummary {
        name: row.get(0)?,
        mime_type: row.get(1)?,
        width: width.try_into().unwrap_or_default(),
        height: height.try_into().unwrap_or_default(),
        size_bytes: size_bytes.try_into().unwrap_or_default(),
        updated_at: row.get(5)?,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn note(id: &str, updated_at: i64, pinned: bool) -> QuickNote {
        QuickNote {
            id: id.into(),
            title: format!("Note {id}"),
            content: "content".into(),
            pinned,
            created_at: 1,
            updated_at,
        }
    }

    fn message(id: &str) -> BoardMessage {
        BoardMessage {
            id: id.into(),
            content: format!("Message {id}"),
            color: "yellow".into(),
            pinned: false,
            created_at: 2,
            updated_at: 2,
        }
    }

    #[test]
    fn persists_and_orders_quick_notes() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        repository
            .save_note(note("one", 10, false))
            .expect("save one");
        repository
            .save_note(note("two", 5, true))
            .expect("save two");
        let mut updated = note("one", 20, false);
        updated.content = "updated".into();
        repository.save_note(updated).expect("update one");

        let notes = repository.list_notes().expect("list notes");
        assert_eq!(
            notes
                .iter()
                .map(|item| item.id.as_str())
                .collect::<Vec<_>>(),
            ["two", "one"]
        );
        assert_eq!(notes[1].content, "updated");
        assert!(repository.delete_note("one").expect("delete"));
        assert_eq!(repository.list_notes().expect("list").len(), 1);
    }

    #[test]
    fn persists_updates_and_deletes_board_messages() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        repository.save_message(message("one")).expect("save");
        let mut updated = message("one");
        updated.content = "updated".into();
        updated.pinned = true;
        repository.save_message(updated).expect("update");

        let messages = repository.list_messages().expect("list");
        assert_eq!(messages[0].content, "updated");
        assert!(messages[0].pinned);
        assert!(repository.delete_message("one").expect("delete"));
        assert!(repository.list_messages().expect("list").is_empty());
    }

    #[test]
    fn keeps_the_database_inside_the_requested_tauri_path() {
        let directory = tempfile::TempDir::new().expect("temporary directory");
        let path = directory.path().join(DATABASE_FILE_NAME);
        let repository = LocalDataRepository::open(path.clone()).expect("open repository");
        repository.save_note(note("local", 1, false)).expect("save");
        drop(repository);
        assert!(path.exists());
    }

    #[test]
    fn persists_host_profiles_independently() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let profile = HostProfile {
            id: "hosts-dev".into(),
            name: "Development".into(),
            content: "127.0.0.1 api.local\n".into(),
            created_at: 3,
            updated_at: 3,
        };
        repository
            .save_host_profile(profile.clone())
            .expect("save profile");
        assert_eq!(repository.list_host_profiles().expect("list"), [profile]);
        assert!(repository.delete_host_profile("hosts-dev").expect("delete"));
    }

    #[test]
    fn persists_http_collections_and_limits_request_history() {
        use crate::contracts::network::{
            HttpHeader, HttpRequestHistory, HttpRequestSpec, HttpResponseData, SavedHttpRequest,
        };

        fn request(id: &str, name: &str) -> HttpRequestSpec {
            HttpRequestSpec {
                request_id: id.into(),
                name: name.into(),
                method: "POST".into(),
                url: "https://example.com/items".into(),
                params: vec![HttpHeader {
                    name: "page".into(),
                    value: "1".into(),
                    enabled: true,
                }],
                headers: vec![],
                cookies: vec![],
                body: "{}".into(),
                body_type: "application/json".into(),
                timeout_ms: 30_000,
                follow_redirects: true,
            }
        }

        fn response(status: u16) -> HttpResponseData {
            HttpResponseData {
                status,
                final_url: "https://example.com/items?page=1".into(),
                headers: vec![("content-type".into(), "application/json".into())],
                body_text: "{}".into(),
                body_base64: String::new(),
                content_type: "application/json".into(),
                size_bytes: 2,
                truncated: false,
                duration_ms: 12,
            }
        }

        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let saved = SavedHttpRequest {
            id: "saved-1".into(),
            name: "Create item".into(),
            request: request("request-1", "Create item"),
            response: Some(response(201)),
            created_at: 10,
            updated_at: 10,
        };
        repository
            .save_http_request(saved.clone())
            .expect("save request");
        assert_eq!(
            repository.list_http_requests("create").expect("list"),
            [saved]
        );

        for index in 0..505 {
            repository
                .record_http_history(HttpRequestHistory {
                    id: format!("history-{index}"),
                    request: request(&format!("request-{index}"), "Create item"),
                    response: response(201),
                    created_at: index,
                })
                .expect("record history");
        }
        assert_eq!(
            repository.list_http_history("").expect("history").len(),
            500
        );
        assert!(repository.delete_http_request("saved-1").expect("delete"));
        assert_eq!(repository.clear_http_history().expect("clear"), 500);
    }

    #[test]
    fn persists_translation_words_history_and_limits_history() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let word = TranslationWord {
            id: "word-1".into(),
            source_text: "hello".into(),
            target_text: "你好".into(),
            source_lang: "en".into(),
            target_lang: "zh-CN".into(),
            remark: "greeting".into(),
            created_at: 10,
            updated_at: 10,
        };
        repository
            .save_translation_word(word.clone())
            .expect("save word");
        assert_eq!(
            repository.list_translation_words().expect("list words"),
            [word]
        );

        let history = TranslationHistory {
            id: "history-1".into(),
            source_text: "hello".into(),
            target_text: "你好".into(),
            source_lang: "en".into(),
            target_lang: "zh-CN".into(),
            provider: TranslationProvider::Google,
            created_at: 11,
        };
        repository
            .save_translation_history(history.clone())
            .expect("save history");
        assert_eq!(
            repository.list_translation_history().expect("list history"),
            [history]
        );
        assert_eq!(repository.clear_translation_history().expect("clear"), 1);
        assert!(
            repository
                .delete_translation_word("word-1")
                .expect("delete")
        );
    }

    #[test]
    fn persists_image_asset_metadata_independently() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let asset = ImageAssetSummary {
            name: "moo.png".into(),
            mime_type: "image/png".into(),
            width: 640,
            height: 480,
            size_bytes: 12_345,
            updated_at: 12,
        };
        repository.save_image_asset(&asset).expect("save image");
        assert_eq!(
            repository.list_image_assets().expect("list images"),
            [asset]
        );
        let renamed = repository
            .rename_image_asset("moo.png", "mootool.png", 13)
            .expect("rename image");
        assert_eq!(renamed.name, "mootool.png");
        assert!(
            repository
                .delete_image_asset("mootool.png")
                .expect("delete")
        );
    }

    #[test]
    fn records_and_trims_global_operation_history() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        for index in 0..12 {
            repository
                .record_operation(
                    OperationHistory {
                        id: format!("operation-{index}"),
                        tool_id: "json".into(),
                        action: "open".into(),
                        summary: format!("Opened JSON {index}"),
                        status: "info".into(),
                        created_at: index,
                    },
                    10,
                )
                .expect("record operation");
        }
        let operations = repository.list_operations(50).expect("list operations");
        assert_eq!(operations.len(), 10);
        assert_eq!(operations[0].id, "operation-11");
        assert!(repository.delete_operation("operation-11").expect("delete"));
        assert_eq!(repository.clear_operations().expect("clear"), 9);
    }

    #[test]
    fn imports_product_records_transactionally_and_marks_the_source_snapshot() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let records = ProductImportRecords {
            quick_notes: vec![note("import-note", 10, false)],
            host_profiles: vec![HostProfile {
                id: "import-host".into(),
                name: "Imported hosts".into(),
                content: "127.0.0.1 localhost".into(),
                created_at: 10,
                updated_at: 10,
            }],
            ..ProductImportRecords::default()
        };

        let (imported, skipped) = repository
            .import_product_records(
                "java",
                "/read-only/source",
                &"a".repeat(64),
                records,
                500,
                20,
            )
            .expect("import records");

        assert_eq!(imported.quick_notes, 1);
        assert_eq!(imported.host_profiles, 1);
        assert_eq!(skipped.total(), 0);
        assert!(
            repository
                .has_product_import(&"a".repeat(64))
                .expect("marker")
        );
        assert_eq!(repository.list_notes().expect("notes").len(), 1);
        assert_eq!(repository.list_host_profiles().expect("hosts").len(), 1);
    }

    #[test]
    fn creates_and_restores_consistent_sqlite_backups() {
        let directory = tempfile::TempDir::new().expect("temporary directory");
        let database = directory.path().join("live.sqlite3");
        let backup = directory.path().join("backup.sqlite3");
        let repository = LocalDataRepository::open(database).expect("open repository");
        repository
            .save_note(note("before-backup", 10, false))
            .expect("save original");
        repository.backup_to(&backup).expect("create backup");
        repository
            .save_note(note("after-backup", 20, false))
            .expect("save later note");
        assert_eq!(
            repository.list_notes().expect("list before restore").len(),
            2
        );

        repository.restore_from(&backup).expect("restore backup");

        let notes = repository.list_notes().expect("list restored");
        assert_eq!(notes.len(), 1);
        assert_eq!(notes[0].id, "before-backup");
    }
}
