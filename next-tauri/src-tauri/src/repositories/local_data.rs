use std::{fs, path::PathBuf, sync::Mutex, time::Duration};

use rusqlite::{Connection, OptionalExtension, TransactionBehavior, backup::Backup, params};

use crate::contracts::{
    image::ImageAssetSummary,
    local_data::{
        BoardMessage, HostProfile, OperationHistory, QuickNote, QuickNoteAttachment,
        SaveHostProfileInput, SaveTranslationHistoryInput, SaveTranslationWordInput,
        validate_host_content, validate_host_name, validate_translation_word_fields,
        QuickNoteFolder, ToolFavorite, TranslationHistory, TranslationWord,
        validate_note_folder_path,
    },
    network::{validate_saved_http_request, HttpRequestHistory, HttpRequestSpec, HttpResponseData, SaveHttpRequestInput, SavedHttpRequest},
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
                "SELECT id, title, content, tags_json, color, folder_path, editor_font, line_height, line_wrapping, syntax, pinned, created_at, updated_at
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
                    tags: serde_json::from_str(&row.get::<_, String>(3)?).unwrap_or_default(),
                    color: row.get(4)?,
                    folder_path: row.get(5)?,
                    relative_path: String::new(),
                    editor_font: row.get(6)?,
                    line_height: row.get(7)?,
                    font_size: 14,
                    font_name: String::new(),
                    line_spacing: 1.2,
                    line_wrapping: row.get(8)?,
                    syntax: row.get(9)?,
                    pinned: row.get(10)?,
                    created_at: row.get(11)?,
                    updated_at: row.get(12)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_note(&self, note: QuickNote) -> Result<QuickNote, String> {
        note.validate()?;
        let tags_json = serde_json::to_string(&note.tags)
            .map_err(|error| format!("failed to encode note tags: {error}"))?;
        let connection = self.lock()?;
        connection
            .execute(
                "INSERT INTO quick_notes (id, title, content, tags_json, color, folder_path, editor_font, line_height, line_wrapping, syntax, pinned, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)
                 ON CONFLICT(id) DO UPDATE SET
                   title = excluded.title,
                   content = excluded.content,
                   tags_json = excluded.tags_json,
                   color = excluded.color,
                   folder_path = excluded.folder_path,
                   editor_font = excluded.editor_font,
                   line_height = excluded.line_height,
                   line_wrapping = excluded.line_wrapping,
                   syntax = excluded.syntax,
                   pinned = excluded.pinned,
                   updated_at = excluded.updated_at",
                params![
                    note.id,
                    note.title,
                    note.content,
                    tags_json,
                    note.color,
                    note.folder_path,
                    note.editor_font,
                    note.line_height,
                    note.line_wrapping,
                    note.syntax,
                    note.pinned,
                    note.created_at,
                    note.updated_at
                ],
            )
            .map_err(database_error)?;
        Ok(note)
    }

    pub fn list_note_folders(&self) -> Result<Vec<QuickNoteFolder>, String> {
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT path, MIN(created_at), MAX(updated_at) FROM (
                   SELECT path, created_at, updated_at FROM quick_note_folders
                   UNION ALL
                   SELECT folder_path AS path, created_at, updated_at FROM quick_notes WHERE folder_path <> ''
                 ) GROUP BY path ORDER BY path COLLATE NOCASE ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok(QuickNoteFolder {
                    path: row.get(0)?,
                    created_at: row.get(1)?,
                    updated_at: row.get(2)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_note_folder(&self, folder: QuickNoteFolder) -> Result<QuickNoteFolder, String> {
        folder.validate()?;
        self.lock()?
            .execute(
                "INSERT INTO quick_note_folders (path, created_at, updated_at)
                 VALUES (?1, ?2, ?3)
                 ON CONFLICT(path) DO UPDATE SET updated_at = excluded.updated_at",
                params![folder.path, folder.created_at, folder.updated_at],
            )
            .map_err(database_error)?;
        Ok(folder)
    }

    pub fn rename_note_folder(
        &self,
        path: &str,
        next_path: &str,
        updated_at: i64,
    ) -> Result<Vec<QuickNoteFolder>, String> {
        validate_note_folder_path(path, false)?;
        validate_note_folder_path(next_path, false)?;
        if path == next_path || updated_at < 0 {
            return Err("invalid note folder rename".into());
        }
        let mut connection = self.lock()?;
        let transaction = connection
            .transaction_with_behavior(TransactionBehavior::Immediate)
            .map_err(database_error)?;
        let sources = {
            let mut statement = transaction
                .prepare(
                    "SELECT path, MIN(created_at), MAX(updated_at) FROM (
                       SELECT path, created_at, updated_at FROM quick_note_folders
                       UNION ALL SELECT folder_path, created_at, updated_at FROM quick_notes WHERE folder_path <> ''
                     ) WHERE path = ?1 OR substr(path, 1, length(?1) + 1) = ?1 || '/'
                     GROUP BY path ORDER BY length(path) ASC",
                )
                .map_err(database_error)?;
            statement
                .query_map([path], |row| {
                    Ok((
                        row.get::<_, String>(0)?,
                        row.get::<_, i64>(1)?,
                        row.get::<_, i64>(2)?,
                    ))
                })
                .map_err(database_error)?
                .collect::<Result<Vec<_>, _>>()
                .map_err(database_error)?
        };
        if sources.is_empty() {
            return Err("note folder not found".into());
        }
        let source_paths = sources
            .iter()
            .map(|item| item.0.as_str())
            .collect::<Vec<_>>();
        let mut renamed = Vec::with_capacity(sources.len());
        for (source, created_at, _) in &sources {
            let target = format!("{next_path}{}", &source[path.len()..]);
            let collision: bool = transaction
                .query_row(
                    "SELECT EXISTS(
                       SELECT 1 FROM quick_note_folders WHERE path = ?1
                       UNION ALL SELECT 1 FROM quick_notes WHERE folder_path = ?1
                     )",
                    [&target],
                    |row| row.get(0),
                )
                .map_err(database_error)?;
            if collision && !source_paths.contains(&target.as_str()) {
                return Err(format!("note folder already exists: {target}"));
            }
            renamed.push(QuickNoteFolder {
                path: target,
                created_at: *created_at,
                updated_at,
            });
        }
        transaction
            .execute(
                "UPDATE quick_notes SET
                   folder_path = ?2 || substr(folder_path, length(?1) + 1), updated_at = ?3
                 WHERE folder_path = ?1 OR substr(folder_path, 1, length(?1) + 1) = ?1 || '/'",
                params![path, next_path, updated_at],
            )
            .map_err(database_error)?;
        transaction
            .execute(
                "DELETE FROM quick_note_folders
                 WHERE path = ?1 OR substr(path, 1, length(?1) + 1) = ?1 || '/'",
                [path],
            )
            .map_err(database_error)?;
        let target_parts = next_path.split('/').collect::<Vec<_>>();
        for index in 1..target_parts.len() {
            let parent = target_parts[..index].join("/");
            transaction
                .execute(
                    "INSERT OR IGNORE INTO quick_note_folders (path, created_at, updated_at)
                     VALUES (?1, ?2, ?2)",
                    params![parent, updated_at],
                )
                .map_err(database_error)?;
        }
        for folder in &renamed {
            transaction
                .execute(
                    "INSERT INTO quick_note_folders (path, created_at, updated_at) VALUES (?1, ?2, ?3)",
                    params![folder.path, folder.created_at, folder.updated_at],
                )
                .map_err(database_error)?;
        }
        transaction.commit().map_err(database_error)?;
        Ok(renamed)
    }

    pub fn delete_note_folder(&self, path: &str, updated_at: i64) -> Result<usize, String> {
        validate_note_folder_path(path, false)?;
        if updated_at < 0 {
            return Err("invalid note folder update timestamp".into());
        }
        let mut connection = self.lock()?;
        let transaction = connection
            .transaction_with_behavior(TransactionBehavior::Immediate)
            .map_err(database_error)?;
        let moved = transaction
            .execute(
                "DELETE FROM quick_notes
                 WHERE folder_path = ?1 OR substr(folder_path, 1, length(?1) + 1) = ?1 || '/'",
                [path],
            )
            .map_err(database_error)?;
        transaction
            .execute(
                "DELETE FROM quick_note_folders
                 WHERE path = ?1 OR substr(path, 1, length(?1) + 1) = ?1 || '/'",
                [path],
            )
            .map_err(database_error)?;
        transaction.commit().map_err(database_error)?;
        Ok(moved)
    }

    pub fn list_tool_favorites(&self, tool_id: &str) -> Result<Vec<ToolFavorite>, String> {
        validate_tool_id(tool_id)?;
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, tool_id, name, payload_json, created_at, updated_at
                 FROM tool_favorites WHERE tool_id = ?1
                 ORDER BY updated_at DESC, name COLLATE NOCASE ASC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([tool_id], |row| {
                Ok(ToolFavorite {
                    id: row.get(0)?,
                    tool_id: row.get(1)?,
                    name: row.get(2)?,
                    payload_json: row.get(3)?,
                    created_at: row.get(4)?,
                    updated_at: row.get(5)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_tool_favorite(&self, mut favorite: ToolFavorite) -> Result<ToolFavorite, String> {
        favorite.name = favorite.name.trim().to_string();
        favorite.validate()?;
        let connection = self.lock()?;
        let existing = connection
            .query_row(
                "SELECT id, created_at FROM tool_favorites WHERE tool_id = ?1 AND name = ?2",
                params![favorite.tool_id, favorite.name],
                |row| Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?)),
            )
            .optional()
            .map_err(database_error)?;
        if let Some((id, created_at)) = existing {
            favorite.id = id;
            favorite.created_at = created_at;
        }
        connection
            .execute(
                "INSERT INTO tool_favorites (id, tool_id, name, payload_json, created_at, updated_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                 ON CONFLICT(id) DO UPDATE SET name = excluded.name, payload_json = excluded.payload_json, updated_at = excluded.updated_at",
                params![favorite.id, favorite.tool_id, favorite.name, favorite.payload_json, favorite.created_at, favorite.updated_at],
            )
            .map_err(database_error)?;
        Ok(favorite)
    }

    pub fn delete_tool_favorite(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM tool_favorites WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn delete_note(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM quick_notes WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_note_attachments(&self, note_id: &str) -> Result<Vec<QuickNoteAttachment>, String> {
        validate_delete_id(note_id)?;
        let connection = self.lock()?;
        let mut statement = connection
            .prepare(
                "SELECT id, note_id, name, mime_type, size_bytes, created_at
                 FROM quick_note_attachments WHERE note_id = ?1
                 ORDER BY created_at DESC, id ASC",
            )
            .map_err(database_error)?;
        let rows = statement
            .query_map([note_id], |row| {
                Ok(QuickNoteAttachment {
                    id: row.get(0)?,
                    note_id: row.get(1)?,
                    name: row.get(2)?,
                    mime_type: row.get(3)?,
                    size_bytes: row.get::<_, i64>(4)?.try_into().unwrap_or_default(),
                    created_at: row.get(5)?,
                    relative_path: String::new(),
                    markdown: String::new(),
                    data_url: String::new(),
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_note_attachment(
        &self,
        attachment: QuickNoteAttachment,
        data: &[u8],
    ) -> Result<QuickNoteAttachment, String> {
        attachment.validate()?;
        if data.len() as u64 != attachment.size_bytes {
            return Err("attachment byte count does not match its metadata".into());
        }
        let connection = self.lock()?;
        let note_exists: bool = connection
            .query_row(
                "SELECT EXISTS(SELECT 1 FROM quick_notes WHERE id = ?1)",
                [&attachment.note_id],
                |row| row.get(0),
            )
            .map_err(database_error)?;
        if !note_exists {
            return Err("attachment note does not exist".into());
        }
        let current_bytes: i64 = connection
            .query_row(
                "SELECT COALESCE(SUM(size_bytes), 0) FROM quick_note_attachments WHERE note_id = ?1",
                [&attachment.note_id],
                |row| row.get(0),
            )
            .map_err(database_error)?;
        if current_bytes.saturating_add(i64::try_from(data.len()).unwrap_or(i64::MAX))
            > 50 * 1024 * 1024
        {
            return Err("note attachments cannot exceed 50 MiB in total".into());
        }
        connection
            .execute(
                "INSERT INTO quick_note_attachments
                   (id, note_id, name, mime_type, size_bytes, data, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                params![
                    attachment.id,
                    attachment.note_id,
                    attachment.name,
                    attachment.mime_type,
                    i64::try_from(attachment.size_bytes).unwrap_or(i64::MAX),
                    data,
                    attachment.created_at
                ],
            )
            .map_err(database_error)?;
        Ok(attachment)
    }

    pub fn note_attachment_data(&self, id: &str) -> Result<(QuickNoteAttachment, Vec<u8>), String> {
        validate_delete_id(id)?;
        self.lock()?
            .query_row(
                "SELECT id, note_id, name, mime_type, size_bytes, created_at, data
                 FROM quick_note_attachments WHERE id = ?1",
                [id],
                |row| {
                    Ok((
                        QuickNoteAttachment {
                            id: row.get(0)?,
                            note_id: row.get(1)?,
                            name: row.get(2)?,
                            mime_type: row.get(3)?,
                            size_bytes: row.get::<_, i64>(4)?.try_into().unwrap_or_default(),
                            created_at: row.get(5)?,
                            relative_path: String::new(),
                            markdown: String::new(),
                            data_url: String::new(),
                        },
                        row.get(6)?,
                    ))
                },
            )
            .map_err(database_error)
    }

    pub fn delete_note_attachment(&self, id: &str) -> Result<bool, String> {
        validate_delete_id(id)?;
        self.lock()?
            .execute("DELETE FROM quick_note_attachments WHERE id = ?1", [id])
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

    pub fn list_host_profiles(
        &self,
        keyword: &str,
        include_content: bool,
    ) -> Result<Vec<HostProfile>, String> {
        if keyword.chars().count() > 200 {
            return Err("Invalid string value".into());
        }
        let connection = self.lock()?;
        let pattern = format!("%{}%", keyword.trim());
        let (sql, values): (&str, Vec<String>) = if keyword.trim().is_empty() {
            (
                "SELECT id, name, content, create_time, modified_time
                 FROM t_host ORDER BY modified_time DESC, id DESC",
                Vec::new(),
            )
        } else if include_content {
            (
                "SELECT id, name, content, create_time, modified_time
                 FROM t_host WHERE name LIKE ?1 OR content LIKE ?2
                 ORDER BY modified_time DESC, id DESC",
                vec![pattern.clone(), pattern],
            )
        } else {
            (
                "SELECT id, name, content, create_time, modified_time
                 FROM t_host WHERE name LIKE ?1
                 ORDER BY modified_time DESC, id DESC",
                vec![pattern],
            )
        };
        let mut statement = connection.prepare(sql).map_err(database_error)?;
        let rows = statement
            .query_map(rusqlite::params_from_iter(values.iter()), |row| {
                Ok(HostProfile {
                    id: row.get(0)?,
                    name: row.get(1)?,
                    content: row.get(2)?,
                    create_time: row.get(3)?,
                    modified_time: row.get(4)?,
                })
            })
            .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_host_profile(&self, input: SaveHostProfileInput) -> Result<HostProfile, String> {
        let name = input.name.trim();
        validate_host_name(name)?;
        validate_host_content(&input.content)?;
        let id = input.id.filter(|id| *id > 0);
        let connection = self.lock()?;
        let existing = match id {
            Some(id) => connection
                .query_row("SELECT id FROM t_host WHERE id = ?1", [id], |row| row.get::<_, i64>(0))
                .optional()
                .map_err(database_error)?,
            None => connection
                .query_row("SELECT id FROM t_host WHERE name = ?1", [name], |row| row.get::<_, i64>(0))
                .optional()
                .map_err(database_error)?,
        };
        let now = sqlite_date_now();
        let saved_id = if let Some(id) = existing {
            connection
                .execute(
                    "UPDATE t_host SET name = ?1, content = ?2, modified_time = ?3 WHERE id = ?4",
                    params![name, input.content, now, id],
                )
                .map_err(database_error)?;
            id
        } else {
            connection
                .execute(
                    "INSERT INTO t_host (name, content, create_time, modified_time) VALUES (?1, ?2, ?3, ?3)",
                    params![name, input.content, now],
                )
                .map_err(database_error)?;
            connection.last_insert_rowid()
        };
        connection
            .query_row(
                "SELECT id, name, content, create_time, modified_time FROM t_host WHERE id = ?1",
                [saved_id],
                |row| {
                    Ok(HostProfile {
                        id: row.get(0)?,
                        name: row.get(1)?,
                        content: row.get(2)?,
                        create_time: row.get(3)?,
                        modified_time: row.get(4)?,
                    })
                },
            )
            .map_err(database_error)
    }

    pub fn delete_host_profile(&self, id: i64) -> Result<bool, String> {
        if id <= 0 {
            return Err("Invalid host profile".into());
        }
        self.lock()?
            .execute("DELETE FROM t_host WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_http_requests(&self, query: &str) -> Result<Vec<SavedHttpRequest>, String> {
        let connection = self.lock()?;
        let trimmed = query.trim();
        let mut statement = connection
            .prepare(if trimmed.is_empty() {
                "SELECT id, msg_name, method, url, params, headers, cookies, body, body_type, create_time, modified_time, response_body, response_headers, response_cookies
                 FROM t_msg_http ORDER BY modified_time DESC, id DESC"
            } else {
                "SELECT id, msg_name, method, url, params, headers, cookies, body, body_type, create_time, modified_time, response_body, response_headers, response_cookies
                 FROM t_msg_http WHERE msg_name LIKE ?1 ORDER BY modified_time DESC, id DESC"
            })
            .map_err(database_error)?;
        let pattern = format!("%{trimmed}%");
        let rows = if trimmed.is_empty() {
            statement.query_map([], map_saved_http_request)
        } else {
            statement.query_map(params![pattern], map_saved_http_request)
        }
        .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_http_request(&self, input: SaveHttpRequestInput) -> Result<SavedHttpRequest, String> {
        validate_saved_http_request(&input)?;
        let name = {
            let trimmed = input.name.trim();
            if trimmed.is_empty() { "Untitled" } else { trimmed }
        };
        let body_type = if input.body_type.trim().is_empty() { "application/json".to_string() } else { input.body_type.trim().to_string() };
        let id = input.id.filter(|id| *id > 0);
        let connection = self.lock()?;
        let existing = match id {
            Some(id) => connection
                .query_row("SELECT id FROM t_msg_http WHERE id = ?1", [id], |row| row.get::<_, i64>(0))
                .optional()
                .map_err(database_error)?,
            None => connection
                .query_row("SELECT id FROM t_msg_http WHERE msg_name = ?1", [name], |row| row.get::<_, i64>(0))
                .optional()
                .map_err(database_error)?,
        };
        let now = sqlite_date_now();
        let params_json = json_text(&input.params)?;
        let headers_json = json_text(&input.headers)?;
        let cookies_json = json_text(&input.cookies)?;
        let saved_id = if let Some(id) = existing {
            connection.execute(
                "UPDATE t_msg_http SET msg_name = ?1, method = ?2, url = ?3, params = ?4, headers = ?5, cookies = ?6, body = ?7, body_type = ?8, modified_time = ?9, response_body = ?10, response_headers = ?11, response_cookies = ?12 WHERE id = ?13",
                params![name, input.method, input.url.trim(), params_json, headers_json, cookies_json, input.body, body_type, now, input.response_body, input.response_headers, input.response_cookies, id],
            ).map_err(database_error)?;
            id
        } else {
            connection.execute(
                "INSERT INTO t_msg_http (msg_name, method, url, params, headers, cookies, body, body_type, create_time, modified_time, response_body, response_headers, response_cookies)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?9, ?10, ?11, ?12)",
                params![name, input.method, input.url.trim(), params_json, headers_json, cookies_json, input.body, body_type, now, input.response_body, input.response_headers, input.response_cookies],
            ).map_err(database_error)?;
            connection.last_insert_rowid()
        };
        connection.query_row(
            "SELECT id, msg_name, method, url, params, headers, cookies, body, body_type, create_time, modified_time, response_body, response_headers, response_cookies FROM t_msg_http WHERE id = ?1",
            [saved_id],
            map_saved_http_request,
        ).map_err(database_error)
    }

    pub fn delete_http_request(&self, id: i64) -> Result<bool, String> {
        if id <= 0 { return Err("Invalid HTTP request".into()); }
        self.lock()?.execute("DELETE FROM t_msg_http WHERE id = ?1", [id]).map(|affected| affected > 0).map_err(database_error)
    }

    pub fn record_http_history(&self, request: &HttpRequestSpec, response: &HttpResponseData) -> Result<(), String> {
        let connection = self.lock()?;
        let now = sqlite_date_now();
        let title = {
            let trimmed = request.name.trim();
            if trimmed.is_empty() { "Untitled" } else { trimmed }
        };
        let status = format!("{} {}", response.status, response.status_text.trim());
        let status = status.trim();
        connection.execute(
            "INSERT INTO t_http_request_history (request_id, title, method, url, params, headers, cookies, body, body_type, response_body, response_headers, response_cookies, status, cost_time, create_time, modified_time)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?15)",
            params![
                request.saved_id.filter(|id| *id > 0),
                title,
                request.method,
                request.url,
                json_text(&request.params)?,
                json_text(&request.headers)?,
                json_text(&request.cookies)?,
                request.body,
                request.body_type,
                response.body_text,
                response.headers.iter().map(|(name, value)| format!("{name}: {value}")).collect::<Vec<_>>().join("\n"),
                response.headers.iter().filter(|(name, _)| name.eq_ignore_ascii_case("set-cookie")).map(|(_, value)| value.as_str()).collect::<Vec<_>>().join("\n"),
                status,
                i64::try_from(response.duration_ms).unwrap_or(i64::MAX),
                now
            ],
        ).map_err(database_error)?;
        connection.execute(
            "DELETE FROM t_http_request_history WHERE id NOT IN (SELECT id FROM t_http_request_history ORDER BY id DESC LIMIT 500)",
            [],
        ).map_err(database_error)?;
        Ok(())
    }

    pub fn list_http_history(&self, query: &str) -> Result<Vec<HttpRequestHistory>, String> {
        let connection = self.lock()?;
        let trimmed = query.trim();
        let mut statement = connection
            .prepare(if trimmed.is_empty() {
                "SELECT id, request_id, title, method, url, params, headers, cookies, body, body_type, response_body, response_headers, response_cookies, status, cost_time, create_time, modified_time
                 FROM t_http_request_history ORDER BY id DESC LIMIT 500"
            } else {
                "SELECT id, request_id, title, method, url, params, headers, cookies, body, body_type, response_body, response_headers, response_cookies, status, cost_time, create_time, modified_time
                 FROM t_http_request_history WHERE title LIKE ?1 OR url LIKE ?1 ORDER BY id DESC LIMIT 500"
            })
            .map_err(database_error)?;
        let pattern = format!("%{trimmed}%");
        let rows = if trimmed.is_empty() {
            statement.query_map([], map_http_history)
        } else {
            statement.query_map(params![pattern], map_http_history)
        }
        .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn delete_http_history(&self, id: i64) -> Result<bool, String> {
        if id <= 0 { return Err("Invalid HTTP request".into()); }
        self.lock()?.execute("DELETE FROM t_http_request_history WHERE id = ?1", [id]).map(|affected| affected > 0).map_err(database_error)
    }

    pub fn clear_http_history(&self) -> Result<usize, String> {
        self.lock()?.execute("DELETE FROM t_http_request_history", []).map_err(database_error)
    }

    pub fn list_translation_words(&self, keyword: &str) -> Result<Vec<TranslationWord>, String> {
        let connection = self.lock()?;
        let trimmed = keyword.trim();
        let mut statement = connection
            .prepare(if trimmed.is_empty() {
                "SELECT id, source_text, target_text, source_lang, target_lang, remark, create_time, modified_time
                 FROM t_translation_word ORDER BY modified_time DESC, id DESC"
            } else {
                "SELECT id, source_text, target_text, source_lang, target_lang, remark, create_time, modified_time
                 FROM t_translation_word
                 WHERE source_text LIKE ?1 OR target_text LIKE ?1 OR remark LIKE ?1
                 ORDER BY modified_time DESC, id DESC"
            })
            .map_err(database_error)?;
        let pattern = format!("%{trimmed}%");
        let rows = if trimmed.is_empty() {
            statement.query_map([], map_translation_word)
        } else {
            statement.query_map(params![pattern], map_translation_word)
        }
        .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_translation_word(&self, input: SaveTranslationWordInput) -> Result<TranslationWord, String> {
        let source_text = input.source_text.trim();
        validate_translation_word_fields(source_text, &input.target_text, &input.source_lang, &input.target_lang, &input.remark)?;
        let id = input.id.filter(|id| *id > 0);
        let connection = self.lock()?;
        let existing = match id {
            Some(id) => connection
                .query_row("SELECT id FROM t_translation_word WHERE id = ?1", [id], |row| row.get::<_, i64>(0))
                .optional()
                .map_err(database_error)?,
            None => connection
                .query_row(
                    "SELECT id FROM t_translation_word WHERE source_text = ?1 AND source_lang = ?2 AND target_lang = ?3",
                    params![source_text, input.source_lang, input.target_lang],
                    |row| row.get::<_, i64>(0),
                )
                .optional()
                .map_err(database_error)?,
        };
        let now = sqlite_date_now();
        let saved_id = if let Some(id) = existing {
            connection
                .execute(
                    "UPDATE t_translation_word
                     SET source_text = ?1, target_text = ?2, source_lang = ?3, target_lang = ?4, remark = ?5, modified_time = ?6
                     WHERE id = ?7",
                    params![source_text, input.target_text, input.source_lang, input.target_lang, input.remark, now, id],
                )
                .map_err(database_error)?;
            id
        } else {
            connection
                .execute(
                    "INSERT INTO t_translation_word
                       (source_text, target_text, source_lang, target_lang, remark, create_time, modified_time)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?6)",
                    params![source_text, input.target_text, input.source_lang, input.target_lang, input.remark, now],
                )
                .map_err(database_error)?;
            connection.last_insert_rowid()
        };
        connection
            .query_row(
                "SELECT id, source_text, target_text, source_lang, target_lang, remark, create_time, modified_time
                 FROM t_translation_word WHERE id = ?1",
                [saved_id],
                map_translation_word,
            )
            .map_err(database_error)
    }

    pub fn delete_translation_word(&self, id: i64) -> Result<bool, String> {
        if id <= 0 {
            return Err("Invalid word".into());
        }
        self.lock()?
            .execute("DELETE FROM t_translation_word WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn list_translation_history(&self, keyword: &str) -> Result<Vec<TranslationHistory>, String> {
        let connection = self.lock()?;
        let trimmed = keyword.trim();
        let mut statement = connection
            .prepare(if trimmed.is_empty() {
                "SELECT id, source_text, target_text, source_lang, target_lang, translator_type, create_time
                 FROM t_translation_history ORDER BY id DESC LIMIT 500"
            } else {
                "SELECT id, source_text, target_text, source_lang, target_lang, translator_type, create_time
                 FROM t_translation_history
                 WHERE source_text LIKE ?1 OR target_text LIKE ?1 OR source_lang LIKE ?1 OR target_lang LIKE ?1
                 ORDER BY id DESC LIMIT 500"
            })
            .map_err(database_error)?;
        let pattern = format!("%{trimmed}%");
        let rows = if trimmed.is_empty() {
            statement.query_map([], map_translation_history)
        } else {
            statement.query_map(params![pattern], map_translation_history)
        }
        .map_err(database_error)?;
        rows.collect::<Result<Vec<_>, _>>().map_err(database_error)
    }

    pub fn save_translation_history(&self, input: SaveTranslationHistoryInput) -> Result<TranslationHistory, String> {
        validate_translation_word_fields(&input.source_text, &input.target_text, &input.source_lang, &input.target_lang, "")?;
        let connection = self.lock()?;
        let now = sqlite_date_now();
        connection
            .execute(
                "INSERT INTO t_translation_history
                   (source_text, target_text, source_lang, target_lang, translator_type, create_time)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                params![
                    input.source_text.trim(),
                    input.target_text,
                    input.source_lang,
                    input.target_lang,
                    translator_type_name(input.translator_type),
                    now
                ],
            )
            .map_err(database_error)?;
        let id = connection.last_insert_rowid();
        connection
            .execute(
                "DELETE FROM t_translation_history WHERE id NOT IN (
                   SELECT id FROM t_translation_history ORDER BY id DESC LIMIT 500
                 )",
                [],
            )
            .map_err(database_error)?;
        connection
            .query_row(
                "SELECT id, source_text, target_text, source_lang, target_lang, translator_type, create_time
                 FROM t_translation_history WHERE id = ?1",
                [id],
                map_translation_history,
            )
            .map_err(database_error)
    }

    pub fn delete_translation_history(&self, id: i64) -> Result<bool, String> {
        if id <= 0 {
            return Err("Invalid word".into());
        }
        self.lock()?
            .execute("DELETE FROM t_translation_history WHERE id = ?1", [id])
            .map(|affected| affected > 0)
            .map_err(database_error)
    }

    pub fn clear_translation_history(&self) -> Result<usize, String> {
        self.lock()?
            .execute("DELETE FROM t_translation_history", [])
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
                "INSERT INTO operation_history
                   (id, tool_id, action, summary, status, input_text, output_text, metadata_json, created_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",
                params![
                    entry.id,
                    entry.tool_id,
                    entry.action,
                    entry.summary,
                    entry.status,
                    entry.input_text,
                    entry.output_text,
                    entry.metadata_json,
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
                "SELECT id, tool_id, action, summary, status, input_text, output_text, metadata_json, created_at
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
                    input_text: row.get(5)?,
                    output_text: row.get(6)?,
                    metadata_json: row.get(7)?,
                    created_at: row.get(8)?,
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
                    "INSERT OR IGNORE INTO quick_notes (id, title, content, tags_json, color, folder_path, editor_font, line_height, line_wrapping, syntax, pinned, created_at, updated_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)",
                    params![note.id, note.title, note.content, serde_json::to_string(&note.tags).map_err(|error| format!("failed to encode imported note tags: {error}"))?, note.color, note.folder_path, note.editor_font, note.line_height, note.line_wrapping, note.syntax, note.pinned, note.created_at, note.updated_at],
                )
                .map_err(database_error)?;
        }
        for profile in records.host_profiles {
            imported.host_profiles += transaction
                .execute(
                    "INSERT OR IGNORE INTO t_host (id, name, content, create_time, modified_time)
                     VALUES (?1, ?2, ?3, ?4, ?5)",
                    params![profile.id, profile.name, profile.content, profile.create_time, profile.modified_time],
                )
                .map_err(database_error)?;
        }
        for word in records.translation_words {
            imported.translation_words += transaction
                .execute(
                    "INSERT OR IGNORE INTO t_translation_word
                       (id, source_text, target_text, source_lang, target_lang, remark, create_time, modified_time)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
                    params![word.id, word.source_text, word.target_text, word.source_lang, word.target_lang, word.remark, word.create_time, word.modified_time],
                )
                .map_err(database_error)?;
        }
        for history in records.translation_history {
            imported.translation_history += transaction
                .execute(
                    "INSERT OR IGNORE INTO t_translation_history
                       (id, source_text, target_text, source_lang, target_lang, translator_type, create_time)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                    params![
                        history.id,
                        history.source_text,
                        history.target_text,
                        history.source_lang,
                        history.target_lang,
                        translator_type_name(history.translator_type),
                        history.create_time
                    ],
                )
                .map_err(database_error)?;
        }
        for operation in records.operation_history {
            imported.operation_history += transaction
                .execute(
                    "INSERT OR IGNORE INTO operation_history
                       (id, tool_id, action, summary, status, input_text, output_text, metadata_json, created_at)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",
                    params![
                        operation.id,
                        operation.tool_id,
                        operation.action,
                        operation.summary,
                        operation.status,
                        operation.input_text,
                        operation.output_text,
                        operation.metadata_json,
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
                "DELETE FROM t_translation_history WHERE id NOT IN (
                   SELECT id FROM t_translation_history ORDER BY id DESC LIMIT 500
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
               tags_json TEXT NOT NULL DEFAULT '[]',
               color TEXT NOT NULL DEFAULT 'default',
               folder_path TEXT NOT NULL DEFAULT '',
               editor_font TEXT NOT NULL DEFAULT 'default',
               line_height TEXT NOT NULL DEFAULT 'normal',
               line_wrapping INTEGER NOT NULL DEFAULT 1,
               syntax TEXT NOT NULL DEFAULT 'markdown',
               pinned INTEGER NOT NULL DEFAULT 0,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS quick_notes_order
               ON quick_notes (pinned DESC, updated_at DESC);
             CREATE TABLE IF NOT EXISTS quick_note_folders (
               path TEXT PRIMARY KEY NOT NULL,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL
             );
             CREATE TABLE IF NOT EXISTS tool_favorites (
               id TEXT PRIMARY KEY NOT NULL,
               tool_id TEXT NOT NULL,
               name TEXT NOT NULL,
               payload_json TEXT NOT NULL,
               created_at INTEGER NOT NULL,
               updated_at INTEGER NOT NULL,
               UNIQUE(tool_id, name)
             );
             CREATE INDEX IF NOT EXISTS tool_favorites_tool
               ON tool_favorites (tool_id, updated_at DESC);
             CREATE TABLE IF NOT EXISTS quick_note_attachments (
               id TEXT PRIMARY KEY NOT NULL,
               note_id TEXT NOT NULL REFERENCES quick_notes(id) ON DELETE CASCADE,
               name TEXT NOT NULL,
               mime_type TEXT NOT NULL,
               size_bytes INTEGER NOT NULL,
               data BLOB NOT NULL,
               created_at INTEGER NOT NULL
             );
             CREATE INDEX IF NOT EXISTS quick_note_attachments_note
               ON quick_note_attachments (note_id, created_at DESC);
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
             CREATE TABLE IF NOT EXISTS t_host (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               name TEXT UNIQUE,
               content TEXT,
               create_time TEXT,
               modified_time TEXT
             );
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
             CREATE TABLE IF NOT EXISTS t_msg_http (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               msg_name TEXT UNIQUE,
               method TEXT,
               url TEXT,
               params TEXT,
               headers TEXT,
               cookies TEXT,
               body TEXT,
               body_type TEXT,
               create_time TEXT,
               modified_time TEXT,
               response_body TEXT,
               response_headers TEXT,
               response_cookies TEXT
             );
             CREATE TABLE IF NOT EXISTS t_http_request_history (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               request_id INTEGER,
               title TEXT,
               method TEXT,
               url TEXT,
               params TEXT,
               headers TEXT,
               cookies TEXT,
               body TEXT,
               body_type TEXT,
               response_body TEXT,
               response_headers TEXT,
               response_cookies TEXT,
               status TEXT,
               cost_time INTEGER,
               create_time TEXT,
               modified_time TEXT
             );
             CREATE INDEX IF NOT EXISTS t_http_history_create_time_index
               ON t_http_request_history (create_time DESC);
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
             CREATE TABLE IF NOT EXISTS t_translation_word (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               source_text TEXT NOT NULL,
               target_text TEXT,
               source_lang TEXT,
               target_lang TEXT,
               remark TEXT,
               create_time TEXT,
               modified_time TEXT
             );
             CREATE INDEX IF NOT EXISTS t_translation_word_modified_time_index
               ON t_translation_word (modified_time DESC);
             CREATE TABLE IF NOT EXISTS t_translation_history (
               id INTEGER PRIMARY KEY AUTOINCREMENT,
               source_text TEXT NOT NULL,
               target_text TEXT,
               source_lang TEXT,
               target_lang TEXT,
               translator_type TEXT,
               create_time TEXT
             );
             CREATE INDEX IF NOT EXISTS t_translation_history_create_time_index
               ON t_translation_history (create_time DESC);
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
               input_text TEXT NOT NULL DEFAULT '',
               output_text TEXT NOT NULL DEFAULT '',
               metadata_json TEXT NOT NULL DEFAULT '{}',
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
             PRAGMA user_version = 12;",
        )
        .map_err(database_error)?;
    ensure_column(
        connection,
        "quick_notes",
        "tags_json",
        "TEXT NOT NULL DEFAULT '[]'",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "folder_path",
        "TEXT NOT NULL DEFAULT ''",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "color",
        "TEXT NOT NULL DEFAULT 'default'",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "editor_font",
        "TEXT NOT NULL DEFAULT 'default'",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "line_height",
        "TEXT NOT NULL DEFAULT 'normal'",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "line_wrapping",
        "INTEGER NOT NULL DEFAULT 1",
    )?;
    ensure_column(
        connection,
        "quick_notes",
        "syntax",
        "TEXT NOT NULL DEFAULT 'markdown'",
    )?;
    connection
        .execute(
            "CREATE INDEX IF NOT EXISTS quick_notes_folder
             ON quick_notes (folder_path, pinned DESC, updated_at DESC)",
            [],
        )
        .map_err(database_error)?;
    ensure_column(
        connection,
        "operation_history",
        "input_text",
        "TEXT NOT NULL DEFAULT ''",
    )?;
    ensure_column(
        connection,
        "operation_history",
        "output_text",
        "TEXT NOT NULL DEFAULT ''",
    )?;
    ensure_column(
        connection,
        "operation_history",
        "metadata_json",
        "TEXT NOT NULL DEFAULT '{}'",
    )?;
    migrate_host_profiles(connection)?;
    migrate_translation_records(connection)?;
    migrate_http_records(connection)?;
    connection
        .pragma_update(None, "user_version", 15)
        .map_err(database_error)
}

fn migrate_host_profiles(connection: &Connection) -> Result<(), String> {
    let exists: i64 = connection
        .query_row(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'host_profiles'",
            [],
            |row| row.get(0),
        )
        .map_err(database_error)?;
    if exists == 0 {
        return Ok(());
    }
    let mut statement = connection
        .prepare("SELECT name, content, created_at, updated_at FROM host_profiles ORDER BY updated_at ASC, id ASC")
        .map_err(database_error)?;
    let rows = statement
        .query_map([], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, i64>(2)?,
                row.get::<_, i64>(3)?,
            ))
        })
        .map_err(database_error)?
        .collect::<Result<Vec<_>, _>>()
        .map_err(database_error)?;
    for (name, content, created_at, updated_at) in rows {
        let name = unique_host_name(connection, name.trim())?;
        if name.is_empty() {
            continue;
        }
        connection
            .execute(
                "INSERT INTO t_host (name, content, create_time, modified_time) VALUES (?1, ?2, ?3, ?4)",
                params![
                    name,
                    content,
                    sqlite_date_from_millis(created_at),
                    sqlite_date_from_millis(updated_at)
                ],
            )
            .map_err(database_error)?;
    }
    connection
        .execute("DROP TABLE host_profiles", [])
        .map(|_| ())
        .map_err(database_error)
}

fn migrate_translation_records(connection: &Connection) -> Result<(), String> {
    if table_exists(connection, "translation_words")? {
        let mut statement = connection
            .prepare("SELECT source_text, target_text, source_lang, target_lang, remark, created_at, updated_at FROM translation_words ORDER BY updated_at ASC, id ASC")
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok((
                    row.get::<_, String>(0)?,
                    row.get::<_, String>(1)?,
                    row.get::<_, String>(2)?,
                    row.get::<_, String>(3)?,
                    row.get::<_, String>(4)?,
                    row.get::<_, i64>(5)?,
                    row.get::<_, i64>(6)?,
                ))
            })
            .map_err(database_error)?
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        drop(statement);
        for (source_text, target_text, source_lang, target_lang, remark, created_at, updated_at) in rows {
            if source_text.trim().is_empty() {
                continue;
            }
            connection
                .execute(
                    "INSERT INTO t_translation_word
                       (source_text, target_text, source_lang, target_lang, remark, create_time, modified_time)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                    params![
                        source_text.trim(),
                        target_text,
                        source_lang,
                        target_lang,
                        remark,
                        sqlite_date_from_millis(created_at),
                        sqlite_date_from_millis(updated_at)
                    ],
                )
                .map_err(database_error)?;
        }
        connection.execute("DROP TABLE translation_words", []).map_err(database_error)?;
    }
    if table_exists(connection, "translation_history")? {
        let mut statement = connection
            .prepare("SELECT source_text, target_text, source_lang, target_lang, provider, created_at FROM translation_history ORDER BY created_at ASC, id ASC")
            .map_err(database_error)?;
        let rows = statement
            .query_map([], |row| {
                Ok((
                    row.get::<_, String>(0)?,
                    row.get::<_, String>(1)?,
                    row.get::<_, String>(2)?,
                    row.get::<_, String>(3)?,
                    row.get::<_, String>(4)?,
                    row.get::<_, i64>(5)?,
                ))
            })
            .map_err(database_error)?
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        drop(statement);
        for (source_text, target_text, source_lang, target_lang, provider, created_at) in rows {
            if source_text.trim().is_empty() {
                continue;
            }
            connection
                .execute(
                    "INSERT INTO t_translation_history
                       (source_text, target_text, source_lang, target_lang, translator_type, create_time)
                     VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
                    params![
                        source_text.trim(),
                        target_text,
                        source_lang,
                        target_lang,
                        translator_type_name(provider_from_stored(&provider)),
                        sqlite_date_from_millis(created_at)
                    ],
                )
                .map_err(database_error)?;
        }
        connection.execute("DROP TABLE translation_history", []).map_err(database_error)?;
    }
    Ok(())
}

fn table_exists(connection: &Connection, name: &str) -> Result<bool, String> {
    connection
        .query_row(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?1",
            [name],
            |row| row.get::<_, i64>(0),
        )
        .map(|count| count > 0)
        .map_err(database_error)
}

fn unique_host_name(connection: &Connection, name: &str) -> Result<String, String> {
    let mut candidate = name.chars().take(200).collect::<String>();
    let mut suffix = 2;
    loop {
        let taken: i64 = connection
            .query_row(
                "SELECT COUNT(*) FROM t_host WHERE name = ?1",
                [&candidate],
                |row| row.get(0),
            )
            .map_err(database_error)?;
        if taken == 0 {
            return Ok(candidate);
        }
        let extra = format!(" ({suffix})");
        let room = 200usize.saturating_sub(extra.chars().count());
        candidate = format!("{}{extra}", name.chars().take(room).collect::<String>());
        suffix += 1;
    }
}

fn json_text<T: serde::Serialize>(value: &T) -> Result<String, String> {
    serde_json::to_string(value).map_err(|error| format!("failed to encode HTTP fields: {error}"))
}

fn json_headers(value: &str) -> Vec<crate::contracts::network::HttpHeader> {
    let mut entries: Vec<crate::contracts::network::HttpHeader> = serde_json::from_str(value).unwrap_or_default();
    for (index, entry) in entries.iter_mut().enumerate() {
        if entry.id.is_empty() {
            entry.id = format!("entry-{index}");
        }
    }
    entries
}

fn json_cookies(value: &str) -> Vec<crate::contracts::network::HttpCookie> {
    let mut entries: Vec<crate::contracts::network::HttpCookie> = serde_json::from_str(value).unwrap_or_default();
    for (index, entry) in entries.iter_mut().enumerate() {
        if entry.id.is_empty() {
            entry.id = format!("cookie-{index}");
        }
    }
    entries
}

fn map_saved_http_request(row: &rusqlite::Row<'_>) -> rusqlite::Result<SavedHttpRequest> {
    let params: String = row.get(4)?;
    let headers: String = row.get(5)?;
    let cookies: String = row.get(6)?;
    Ok(SavedHttpRequest {
        id: row.get(0)?,
        name: row.get(1)?,
        method: row.get(2)?,
        url: row.get(3)?,
        params: json_headers(&params),
        headers: json_headers(&headers),
        cookies: json_cookies(&cookies),
        body: row.get(7)?,
        body_type: row.get(8)?,
        create_time: row.get(9)?,
        modified_time: row.get(10)?,
        response_body: row.get(11)?,
        response_headers: row.get(12)?,
        response_cookies: row.get(13)?,
    })
}

fn map_http_history(row: &rusqlite::Row<'_>) -> rusqlite::Result<HttpRequestHistory> {
    let title: String = row.get(2)?;
    let params: String = row.get(5)?;
    let headers: String = row.get(6)?;
    let cookies: String = row.get(7)?;
    Ok(HttpRequestHistory {
        id: row.get(0)?,
        request_id_value: row.get(1)?,
        title: title.clone(),
        name: title,
        method: row.get(3)?,
        url: row.get(4)?,
        params: json_headers(&params),
        headers: json_headers(&headers),
        cookies: json_cookies(&cookies),
        body: row.get(8)?,
        body_type: row.get(9)?,
        response_body: row.get(10)?,
        response_headers: row.get(11)?,
        response_cookies: row.get(12)?,
        status: row.get(13)?,
        cost_time: row.get(14)?,
        create_time: row.get(15)?,
        modified_time: row.get(16)?,
    })
}

fn migrate_http_records(connection: &Connection) -> Result<(), String> {
    if table_exists(connection, "http_saved_requests")? {
        let mut statement = connection
            .prepare("SELECT payload_json, created_at, updated_at FROM http_saved_requests ORDER BY updated_at ASC, id ASC")
            .map_err(database_error)?;
        let rows = statement.query_map([], |row| Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?, row.get::<_, i64>(2)?))).map_err(database_error)?.collect::<Result<Vec<_>, _>>().map_err(database_error)?;
        drop(statement);
        for (payload, created_at, updated_at) in rows {
            let Ok(legacy) = serde_json::from_str::<LegacySavedHttpRequest>(&payload) else { continue };
            let _ = connection.execute(
                "INSERT INTO t_msg_http (msg_name, method, url, params, headers, cookies, body, body_type, create_time, modified_time, response_body, response_headers, response_cookies)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)",
                params![
                    if legacy.name.trim().is_empty() { legacy.request.name } else { legacy.name },
                    legacy.request.method,
                    legacy.request.url,
                    json_text(&legacy.request.params).unwrap_or_else(|_| "[]".into()),
                    json_text(&legacy.request.headers).unwrap_or_else(|_| "[]".into()),
                    json_text(&legacy.request.cookies).unwrap_or_else(|_| "[]".into()),
                    legacy.request.body,
                    legacy.request.body_type,
                    sqlite_date_from_millis(created_at),
                    sqlite_date_from_millis(updated_at),
                    legacy.response.as_ref().map(|response| response.body_text.clone()).unwrap_or_default(),
                    legacy.response.as_ref().map(|response| response.headers.iter().map(|(name, value)| format!("{name}: {value}")).collect::<Vec<_>>().join("\n")).unwrap_or_default(),
                    ""
                ],
            );
        }
        connection.execute("DROP TABLE http_saved_requests", []).map_err(database_error)?;
    }
    if table_exists(connection, "http_request_history")? {
        let mut statement = connection.prepare("SELECT payload_json, created_at FROM http_request_history ORDER BY created_at ASC, id ASC").map_err(database_error)?;
        let rows = statement.query_map([], |row| Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?))).map_err(database_error)?.collect::<Result<Vec<_>, _>>().map_err(database_error)?;
        drop(statement);
        for (payload, created_at) in rows {
            let Ok(legacy) = serde_json::from_str::<LegacyHttpHistory>(&payload) else { continue };
            let _ = connection.execute(
                "INSERT INTO t_http_request_history (request_id, title, method, url, params, headers, cookies, body, body_type, response_body, response_headers, response_cookies, status, cost_time, create_time, modified_time)
                 VALUES (NULL, ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, '', ?11, ?12, ?13, ?13)",
                params![
                    if legacy.request.name.trim().is_empty() { legacy.request.url.clone() } else { legacy.request.name.clone() },
                    legacy.request.method,
                    legacy.request.url,
                    json_text(&legacy.request.params).unwrap_or_else(|_| "[]".into()),
                    json_text(&legacy.request.headers).unwrap_or_else(|_| "[]".into()),
                    json_text(&legacy.request.cookies).unwrap_or_else(|_| "[]".into()),
                    legacy.request.body,
                    legacy.request.body_type,
                    legacy.response.body_text,
                    legacy.response.headers.iter().map(|(name, value)| format!("{name}: {value}")).collect::<Vec<_>>().join("\n"),
                    legacy.response.status.to_string(),
                    i64::try_from(legacy.response.duration_ms).unwrap_or(i64::MAX),
                    sqlite_date_from_millis(created_at)
                ],
            );
        }
        connection.execute("DROP TABLE http_request_history", []).map_err(database_error)?;
    }
    Ok(())
}

#[derive(serde::Deserialize)]
#[serde(rename_all = "camelCase")]
struct LegacySavedHttpRequest {
    name: String,
    request: HttpRequestSpec,
    response: Option<HttpResponseData>,
}

#[derive(serde::Deserialize)]
#[serde(rename_all = "camelCase")]
struct LegacyHttpHistory {
    request: HttpRequestSpec,
    response: HttpResponseData,
}

fn map_translation_word(row: &rusqlite::Row<'_>) -> rusqlite::Result<TranslationWord> {
    Ok(TranslationWord {
        id: row.get(0)?,
        source_text: row.get(1)?,
        target_text: row.get(2)?,
        source_lang: row.get(3)?,
        target_lang: row.get(4)?,
        remark: row.get(5)?,
        create_time: row.get(6)?,
        modified_time: row.get(7)?,
    })
}

fn map_translation_history(row: &rusqlite::Row<'_>) -> rusqlite::Result<TranslationHistory> {
    let translator_type: String = row.get(5)?;
    Ok(TranslationHistory {
        id: row.get(0)?,
        source_text: row.get(1)?,
        target_text: row.get(2)?,
        source_lang: row.get(3)?,
        target_lang: row.get(4)?,
        translator_type: provider_from_stored(&translator_type),
        create_time: row.get(6)?,
    })
}

fn translator_type_name(provider: TranslationProvider) -> &'static str {
    match provider {
        TranslationProvider::Google => "GOOGLE",
        TranslationProvider::Bing => "BING",
        TranslationProvider::DeepL => "DEEPL",
    }
}

fn provider_from_stored(value: &str) -> TranslationProvider {
    match value.to_ascii_lowercase().as_str() {
        "bing" => TranslationProvider::Bing,
        "deepl" => TranslationProvider::DeepL,
        _ => TranslationProvider::Google,
    }
}

fn sqlite_date_now() -> String {
    sqlite_date(time::OffsetDateTime::now_local().unwrap_or_else(|_| time::OffsetDateTime::now_utc()))
}

fn sqlite_date_from_millis(millis: i64) -> String {
    let utc = time::OffsetDateTime::from_unix_timestamp_nanos((millis as i128).saturating_mul(1_000_000))
        .unwrap_or(time::OffsetDateTime::UNIX_EPOCH);
    let local = time::UtcOffset::current_local_offset()
        .map(|offset| utc.to_offset(offset))
        .unwrap_or(utc);
    sqlite_date(local)
}

fn sqlite_date(value: time::OffsetDateTime) -> String {
    format!(
        "{:04}-{:02}-{:02} {:02}:{:02}:{:02}",
        value.year(),
        value.month() as u8,
        value.day(),
        value.hour(),
        value.minute(),
        value.second()
    )
}

fn ensure_column(
    connection: &Connection,
    table: &str,
    column: &str,
    declaration: &str,
) -> Result<(), String> {
    let mut statement = connection
        .prepare(&format!("PRAGMA table_info({table})"))
        .map_err(database_error)?;
    let exists = statement
        .query_map([], |row| row.get::<_, String>(1))
        .map_err(database_error)?
        .collect::<Result<Vec<_>, _>>()
        .map_err(database_error)?
        .iter()
        .any(|name| name == column);
    drop(statement);
    if !exists {
        connection
            .execute(
                &format!("ALTER TABLE {table} ADD COLUMN {column} {declaration}"),
                [],
            )
            .map_err(database_error)?;
    }
    Ok(())
}

fn validate_delete_id(id: &str) -> Result<(), String> {
    if id.is_empty() || id.len() > 128 {
        return Err("invalid local record ID".into());
    }
    Ok(())
}

fn validate_tool_id(value: &str) -> Result<(), String> {
    if value.is_empty()
        || value.len() > 64
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'-')
    {
        return Err("invalid tool ID".into());
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
            tags: Vec::new(),
            color: "default".into(),
            folder_path: String::new(),
            relative_path: String::new(),
            editor_font: "default".into(),
            line_height: "normal".into(),
            font_size: 14,
            font_name: String::new(),
            line_spacing: 1.0,
            line_wrapping: true,
            syntax: "markdown".into(),
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
    fn organizes_notes_in_nested_folders_and_moves_them_safely() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        repository
            .save_note_folder(QuickNoteFolder {
                path: "work".into(),
                created_at: 1,
                updated_at: 1,
            })
            .expect("save root folder");
        repository
            .save_note_folder(QuickNoteFolder {
                path: "work/tauri".into(),
                created_at: 2,
                updated_at: 2,
            })
            .expect("save nested folder");
        let mut nested = note("folder-note", 3, false);
        nested.folder_path = "work/tauri".into();
        repository.save_note(nested).expect("save nested note");

        let renamed = repository
            .rename_note_folder("work", "archive/projects", 4)
            .expect("rename folder tree");
        assert_eq!(renamed.len(), 2);
        assert_eq!(
            repository.list_notes().expect("list notes")[0].folder_path,
            "archive/projects/tauri"
        );
        assert_eq!(
            repository
                .list_note_folders()
                .expect("list folders")
                .iter()
                .map(|folder| folder.path.as_str())
                .collect::<Vec<_>>(),
            ["archive", "archive/projects", "archive/projects/tauri"]
        );

        assert_eq!(
            repository
                .delete_note_folder("archive", 5)
                .expect("delete folder tree"),
            1
        );
        assert!(repository.list_notes().expect("deleted notes").is_empty());
        assert!(
            repository
                .list_note_folders()
                .expect("empty folders")
                .is_empty()
        );
    }

    #[test]
    fn persists_quick_note_attachments_and_cascades_note_deletion() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        repository
            .save_note(note("attached", 10, false))
            .expect("save note");
        let attachment = QuickNoteAttachment {
            id: "attachment-1".into(),
            note_id: "attached".into(),
            name: "example.txt".into(),
            mime_type: "text/plain".into(),
            size_bytes: 4,
            created_at: 11,
            relative_path: String::new(),
            markdown: String::new(),
            data_url: String::new(),
        };

        repository
            .save_note_attachment(attachment.clone(), b"moo!")
            .expect("save attachment");
        assert_eq!(
            repository
                .list_note_attachments("attached")
                .expect("list attachments"),
            std::slice::from_ref(&attachment)
        );
        assert_eq!(
            repository
                .note_attachment_data("attachment-1")
                .expect("read attachment"),
            (attachment, b"moo!".to_vec())
        );
        assert!(repository.delete_note("attached").expect("delete note"));
        assert!(
            repository
                .list_note_attachments("attached")
                .expect("list after delete")
                .is_empty()
        );
    }

    #[test]
    fn persists_updates_and_deletes_tool_favorites() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let first = ToolFavorite {
            id: "favorite-1".into(),
            tool_id: "regex".into(),
            name: "Identifiers".into(),
            payload_json: r#"{"pattern":"[a-z]+"}"#.into(),
            created_at: 1,
            updated_at: 1,
        };
        repository
            .save_tool_favorite(first.clone())
            .expect("save favorite");
        let updated = ToolFavorite {
            id: "replacement-id".into(),
            payload_json: r#"{"pattern":"[A-Z]+"}"#.into(),
            updated_at: 2,
            ..first
        };
        let saved = repository
            .save_tool_favorite(updated)
            .expect("update favorite by tool and name");

        assert_eq!(saved.id, "favorite-1");
        assert_eq!(saved.created_at, 1);
        assert_eq!(
            repository
                .list_tool_favorites("regex")
                .expect("list favorites")[0]
                .payload_json,
            r#"{"pattern":"[A-Z]+"}"#
        );
        assert!(
            repository
                .delete_tool_favorite("favorite-1")
                .expect("delete favorite")
        );
        assert!(
            repository
                .list_tool_favorites("regex")
                .expect("list empty favorites")
                .is_empty()
        );
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
    fn migrates_pre_folder_and_history_payload_databases() {
        let directory = tempfile::TempDir::new().expect("temporary directory");
        let path = directory.path().join(DATABASE_FILE_NAME);
        let connection = Connection::open(&path).expect("open legacy database");
        connection
            .execute_batch(
                "CREATE TABLE quick_notes (
                   id TEXT PRIMARY KEY NOT NULL,
                   title TEXT NOT NULL,
                   content TEXT NOT NULL,
                   tags_json TEXT NOT NULL DEFAULT '[]',
                   color TEXT NOT NULL DEFAULT 'default',
                   pinned INTEGER NOT NULL DEFAULT 0,
                   created_at INTEGER NOT NULL,
                   updated_at INTEGER NOT NULL
                 );
                 CREATE TABLE operation_history (
                   id TEXT PRIMARY KEY NOT NULL,
                   tool_id TEXT NOT NULL,
                   action TEXT NOT NULL,
                   summary TEXT NOT NULL,
                   status TEXT NOT NULL,
                   created_at INTEGER NOT NULL
                 );
                 PRAGMA user_version = 8;",
            )
            .expect("create legacy schema");
        drop(connection);

        let repository = LocalDataRepository::open(path).expect("migrate repository");
        let migrated = note("migrated", 1, false);
        repository.save_note(migrated).expect("save migrated note");
        repository
            .record_operation(
                OperationHistory {
                    id: "operation-1".into(),
                    tool_id: "json".into(),
                    action: "Format".into(),
                    summary: "Migrated".into(),
                    status: "success".into(),
                    input_text: "{}".into(),
                    output_text: "{}".into(),
                    metadata_json: "{}".into(),
                    created_at: 1,
                },
                10,
            )
            .expect("save migrated history");
        assert_eq!(
            repository.list_notes().expect("list notes")[0].folder_path,
            ""
        );
        assert_eq!(
            repository.list_operations(10).expect("list history")[0].input_text,
            "{}"
        );
        assert_eq!(
            repository
                .lock()
                .expect("lock database")
                .query_row("PRAGMA user_version", [], |row| row.get::<_, u32>(0))
                .expect("read schema version"),
            15
        );
    }

    #[test]
    fn persists_host_profiles_independently() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let saved = repository
            .save_host_profile(SaveHostProfileInput {
                id: None,
                name: "Development".into(),
                content: "127.0.0.1 api.local\n".into(),
            })
            .expect("save profile");
        assert_eq!(saved.id, 1);
        assert_eq!(saved.create_time, saved.modified_time);
        let renamed = repository
            .save_host_profile(SaveHostProfileInput {
                id: None,
                name: "Development".into(),
                content: "127.0.0.1 api.local\n10.0.0.2 build\n".into(),
            })
            .expect("update by name");
        assert_eq!(renamed.id, saved.id);
        assert_eq!(repository.list_host_profiles("build", true).expect("search").len(), 1);
        assert!(repository.list_host_profiles("missing", false).expect("name search").is_empty());
        assert!(repository.delete_host_profile(saved.id).expect("delete"));
    }

    #[test]
    fn migrates_string_host_profiles_into_integer_rows() {
        let directory = tempfile::TempDir::new().expect("temporary directory");
        let path = directory.path().join(DATABASE_FILE_NAME);
        let connection = Connection::open(&path).expect("open legacy database");
        connection
            .execute_batch(
                "CREATE TABLE host_profiles (
                   id TEXT PRIMARY KEY NOT NULL,
                   name TEXT NOT NULL,
                   content TEXT NOT NULL,
                   created_at INTEGER NOT NULL,
                   updated_at INTEGER NOT NULL
                 );
                 INSERT INTO host_profiles (id, name, content, created_at, updated_at)
                 VALUES ('hosts-dev', 'Development', '127.0.0.1 api.local', 1000, 2000);",
            )
            .expect("create legacy hosts");
        drop(connection);

        let repository = LocalDataRepository::open(path).expect("migrate repository");
        let profiles = repository.list_host_profiles("", true).expect("list hosts");
        assert_eq!(profiles.len(), 1);
        assert_eq!(profiles[0].id, 1);
        assert_eq!(profiles[0].name, "Development");
        assert_eq!(profiles[0].create_time.len(), 19);
        assert!(repository.list_host_profiles("api.local", true).expect("content search").len() == 1);
    }

    #[test]
    fn persists_http_collections_and_limits_request_history() {
        use crate::contracts::network::{HttpHeader, HttpRequestSpec, HttpResponseData, SaveHttpRequestInput};

        fn request(name: &str) -> HttpRequestSpec {
            HttpRequestSpec {
                request_id: "request".into(),
                name: name.into(),
                method: "POST".into(),
                url: "https://example.com/items".into(),
                params: vec![HttpHeader { id: "page".into(), name: "page".into(), value: "1".into(), enabled: true }],
                headers: vec![],
                cookies: vec![],
                body: "{}".into(),
                body_type: "application/json".into(),
                timeout_ms: 30_000,
                follow_redirects: true,
                saved_id: None,
            }
        }

        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let saved = repository.save_http_request(SaveHttpRequestInput {
            id: None,
            name: " Create item ".into(),
            method: "POST".into(),
            url: "https://example.com/items".into(),
            params: vec![HttpHeader { id: "page".into(), name: "page".into(), value: "1".into(), enabled: true }],
            headers: vec![],
            cookies: vec![],
            body: "{}".into(),
            body_type: String::new(),
            response_body: "{}".into(),
            response_headers: "content-type: application/json".into(),
            response_cookies: String::new(),
        }).expect("save request");
        assert_eq!(saved.id, 1);
        assert_eq!(saved.name, "Create item");
        assert_eq!(saved.body_type, "application/json");
        let updated = repository.save_http_request(SaveHttpRequestInput {
            id: None,
            name: "Create item".into(),
            method: "PUT".into(),
            url: "https://example.com/items".into(),
            params: vec![],
            headers: vec![],
            cookies: vec![],
            body: String::new(),
            body_type: "text/plain".into(),
            response_body: String::new(),
            response_headers: String::new(),
            response_cookies: String::new(),
        }).expect("update by name");
        assert_eq!(updated.id, saved.id);
        assert_eq!(updated.method, "PUT");
        assert_eq!(updated.create_time, saved.create_time);
        assert_eq!(repository.list_http_requests("create").expect("name search").len(), 1);
        assert!(repository.list_http_requests("example.com").expect("url is not searched").is_empty());

        let response = HttpResponseData {
            status: 201,
            final_url: "https://example.com/items".into(),
            headers: vec![("content-type".into(), "application/json".into())],
            body_text: "{}".into(),
            body_base64: String::new(),
            content_type: "application/json".into(),
            size_bytes: 2,
            truncated: false,
            duration_ms: 12,
            status_text: String::new(),
        };
        for index in 0..501 {
            let mut spec = request(&format!("item {index}"));
            spec.saved_id = Some(saved.id);
            repository.record_http_history(&spec, &response).expect("record history");
        }
        let history = repository.list_http_history("").expect("history");
        assert_eq!(history.len(), 500);
        assert_eq!(history[0].id, 501);
        assert_eq!(history[0].request_id_value, Some(1));
        assert_eq!(history[0].status, "201");
        assert_eq!(history[0].cost_time, 12);
        assert!(repository.list_http_history("example.com").expect("url search").len() == 500);
        assert!(repository.delete_http_request(saved.id).expect("delete"));
        assert_eq!(repository.clear_http_history().expect("clear"), 500);
    }

    #[test]
    fn persists_translation_words_history_and_limits_history() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let saved = repository
            .save_translation_word(SaveTranslationWordInput {
                id: None,
                source_text: " hello ".into(),
                target_text: "你好".into(),
                source_lang: "en".into(),
                target_lang: "zh-CN".into(),
                remark: "greeting".into(),
            })
            .expect("save word");
        assert_eq!(saved.id, 1);
        assert_eq!(saved.source_text, "hello");
        assert_eq!(saved.create_time, saved.modified_time);
        let updated = repository
            .save_translation_word(SaveTranslationWordInput {
                id: None,
                source_text: "hello".into(),
                target_text: "您好".into(),
                source_lang: "en".into(),
                target_lang: "zh-CN".into(),
                remark: "greeting".into(),
            })
            .expect("update word by language pair");
        assert_eq!(updated.id, saved.id);
        assert_eq!(updated.create_time, saved.create_time);
        assert_eq!(repository.list_translation_words("您好").expect("search").len(), 1);
        assert!(repository.list_translation_words("missing").expect("miss").is_empty());

        let history = repository
            .save_translation_history(SaveTranslationHistoryInput {
                source_text: "hello".into(),
                target_text: "你好".into(),
                source_lang: "en".into(),
                target_lang: "zh-CN".into(),
                translator_type: TranslationProvider::Google,
            })
            .expect("save history");
        assert_eq!(history.id, 1);
        assert_eq!(history.translator_type, TranslationProvider::Google);
        assert_eq!(history.create_time.len(), 19);
        for index in 0..500 {
            repository
                .save_translation_history(SaveTranslationHistoryInput {
                    source_text: format!("line {index}"),
                    target_text: "x".into(),
                    source_lang: "en".into(),
                    target_lang: "zh-CN".into(),
                    translator_type: TranslationProvider::Bing,
                })
                .expect("save extra history");
        }
        let listed = repository.list_translation_history("").expect("list history");
        assert_eq!(listed.len(), 500);
        assert_eq!(listed[0].id, 501);
        assert!(listed.iter().all(|item| item.source_text != "hello"));
        assert_eq!(repository.clear_translation_history().expect("clear"), 500);
        assert!(repository.delete_translation_word(saved.id).expect("delete"));
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
                        input_text: format!("{{\"index\":{index}}}"),
                        output_text: format!("index={index}"),
                        metadata_json: "{\"mode\":\"format\"}".into(),
                        created_at: index,
                    },
                    10,
                )
                .expect("record operation");
        }
        let operations = repository.list_operations(50).expect("list operations");
        assert_eq!(operations.len(), 10);
        assert_eq!(operations[0].id, "operation-11");
        assert_eq!(operations[0].input_text, "{\"index\":11}");
        assert_eq!(operations[0].metadata_json, "{\"mode\":\"format\"}");
        assert!(repository.delete_operation("operation-11").expect("delete"));
        assert_eq!(repository.clear_operations().expect("clear"), 9);
    }

    #[test]
    fn imports_product_records_transactionally_and_marks_the_source_snapshot() {
        let repository = LocalDataRepository::open_in_memory().expect("open repository");
        let records = ProductImportRecords {
            quick_notes: vec![note("import-note", 10, false)],
            host_profiles: vec![HostProfile {
                id: 7,
                name: "Imported hosts".into(),
                content: "127.0.0.1 localhost".into(),
                create_time: "2026-10-21 12:00:00".into(),
                modified_time: "2026-10-21 12:00:00".into(),
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
        assert_eq!(repository.list_host_profiles("", true).expect("hosts").len(), 1);
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
