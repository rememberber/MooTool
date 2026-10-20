use std::{
    fs,
    path::{Path, PathBuf},
    sync::atomic::{AtomicU64, Ordering},
    time::{SystemTime, UNIX_EPOCH},
};

const ATTACHMENT_EXTENSIONS: &[&str] = &[".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"];
const MAX_ATTACHMENT_BYTES: u64 = 20 * 1024 * 1024;
static ATTACHMENT_SEQUENCE: AtomicU64 = AtomicU64::new(1);

const NOTE_EXTENSIONS: &[&str] = &[
    "txt", "md", "json", "java", "js", "ts", "py", "html", "xml", "yaml", "yml", "sql",
];

#[derive(Clone, Debug, PartialEq)]
pub struct QuickNoteMetadata {
    pub id: String,
    pub title: String,
    pub style: String,
    pub syntax: String,
    pub font_name: String,
    pub font_size: u16,
    pub line_spacing: f32,
    pub color: String,
    pub line_wrap: bool,
    pub created_at: String,
    pub modified_at: String,
    pub tags: Vec<String>,
    pub pinned: bool,
    pub editor_font: String,
    pub line_height: String,
}

#[derive(Clone, Debug, PartialEq)]
pub struct QuickNoteFile {
    pub relative_path: String,
    pub content: String,
    pub metadata: QuickNoteMetadata,
}

#[derive(Clone, Debug, PartialEq)]
pub struct QuickNoteNode {
    pub name: String,
    pub relative_path: String,
    pub kind: &'static str,
    pub children: Vec<QuickNoteNode>,
}

pub struct QuickNoteVault {
    root: PathBuf,
}

#[derive(Clone, Debug, PartialEq)]
pub struct VaultAttachment {
    pub relative_path: String,
    pub markdown: String,
    pub name: String,
    pub mime_type: String,
    pub size_bytes: u64,
}

impl QuickNoteVault {
    pub fn root(&self) -> &Path {
        &self.root
    }

    pub fn open(root: PathBuf) -> Result<Self, String> {
        fs::create_dir_all(&root).map_err(|error| format!("failed to create quick note vault: {error}"))?;
        Ok(Self { root })
    }

    pub fn create(&self, title: &str, parent: &str, font_name: &str, font_size: u16) -> Result<QuickNoteFile, String> {
        let title = sanitize_name(title)?;
        let parent = normalize_directory(parent, true)?;
        let directory = self.resolve(&parent)?;
        fs::create_dir_all(&directory).map_err(|error| format!("failed to create note folder: {error}"))?;
        let relative = unique_note_path(&self.root, &parent, &title, ".txt")?;
        let now = iso_now();
        self.save(&QuickNoteFile {
            relative_path: relative,
            content: String::new(),
            metadata: QuickNoteMetadata {
                id: String::new(),
                title: title.clone(),
                style: String::new(),
                syntax: "text/plain".into(),
                font_name: font_name.chars().take(120).collect(),
                font_size: clamp_font_size(font_size),
                line_spacing: 1.0,
                color: "default".into(),
                line_wrap: true,
                created_at: now.clone(),
                modified_at: now,
                tags: Vec::new(),
                pinned: false,
                editor_font: "default".into(),
                line_height: "normal".into(),
            },
        })
    }

    pub fn create_folder(&self, path: &str) -> Result<(), String> {
        let relative = normalize_directory(path, false)?;
        let target = self.resolve(&relative)?;
        fs::create_dir_all(&target).map_err(|error| format!("failed to create folder: {error}"))
    }

    pub fn read(&self, relative_path: &str) -> Result<QuickNoteFile, String> {
        let relative = normalize_note_path(relative_path)?;
        let target = self.resolve(&relative)?;
        let raw = fs::read_to_string(&target).map_err(|error| format!("failed to read note: {error}"))?;
        let metadata = fs::metadata(&target).map_err(|error| format!("failed to stat note: {error}"))?;
        let fallback = Path::new(&relative)
            .file_stem()
            .and_then(|value| value.to_str())
            .unwrap_or("note");
        let parsed = parse_note(&raw, fallback);
        Ok(QuickNoteFile {
            relative_path: relative,
            content: parsed.0,
            metadata: parsed.1.unwrap_or_else(|| fallback_metadata(fallback, &metadata)),
        })
    }

    pub fn save(&self, file: &QuickNoteFile) -> Result<QuickNoteFile, String> {
        let relative = normalize_note_path(&file.relative_path)?;
        let target = self.resolve(&relative)?;
        if let Some(parent) = target.parent() {
            fs::create_dir_all(parent).map_err(|error| format!("failed to create note parent: {error}"))?;
        }
        let body = serialize_note(&file.metadata, &file.content);
        fs::write(&target, body).map_err(|error| format!("failed to write note: {error}"))?;
        self.read(&relative)
    }

    pub fn list(&self) -> Result<Vec<QuickNoteNode>, String> {
        list_directory(&self.root, &self.root)
    }

    pub fn note_files(&self) -> Result<Vec<QuickNoteFile>, String> {
        let mut files = Vec::new();
        collect_files(&self.root, &self.root, &mut files)?;
        Ok(files)
    }

    pub fn delete_entry(&self, relative_path: &str) -> Result<(), String> {
        let relative = normalize_entry(relative_path)?;
        let target = self.resolve(&relative)?;
        if target.is_dir() {
            fs::remove_dir_all(&target).map_err(|error| format!("failed to delete note folder: {error}"))
        } else {
            fs::remove_file(&target).map_err(|error| format!("failed to delete note: {error}"))
        }
    }

    pub fn store_attachment(&self, data: &[u8], extension: &str) -> Result<VaultAttachment, String> {
        let extension = normalize_attachment_extension(extension)?;
        if data.is_empty() || data.len() as u64 > MAX_ATTACHMENT_BYTES {
            return Err("Attachment exceeds 20 MB limit".into());
        }
        let directory = self.root.join("attachments");
        fs::create_dir_all(&directory).map_err(|error| format!("failed to create attachment folder: {error}"))?;
        let name = format!("{}{}{}", compact_timestamp(), attachment_suffix(), extension);
        let relative_path = format!("attachments/{name}");
        let target = self.resolve(&relative_path)?;
        fs::write(&target, data).map_err(|error| format!("failed to write attachment: {error}"))?;
        Ok(VaultAttachment {
            markdown: format!("![image]({relative_path})"),
            mime_type: image_mime(&extension).into(),
            size_bytes: data.len() as u64,
            name,
            relative_path,
        })
    }

    pub fn read_attachment(&self, relative_path: &str) -> Result<Vec<u8>, String> {
        let relative = normalize_attachment_path(relative_path)?;
        let target = self.resolve(&relative)?;
        let metadata = fs::metadata(&target).map_err(|error| format!("failed to stat attachment: {error}"))?;
        if !metadata.is_file() || metadata.len() > MAX_ATTACHMENT_BYTES {
            return Err("Attachment exceeds 20 MB limit".into());
        }
        fs::read(&target).map_err(|error| format!("failed to read attachment: {error}"))
    }

    pub fn delete_attachment(&self, relative_path: &str) -> Result<bool, String> {
        let relative = normalize_attachment_path(relative_path)?;
        let target = self.resolve(&relative)?;
        if !target.is_file() {
            return Ok(false);
        }
        fs::remove_file(&target).map_err(|error| format!("failed to delete attachment: {error}"))?;
        Ok(true)
    }

    pub fn resolve_entry(&self, relative: &str) -> Result<PathBuf, String> {
        let normalized = normalize_entry(relative)?;
        let target = self.resolve(&normalized)?;
        if !target.exists() {
            return Err("Vault entry does not exist".into());
        }
        Ok(target)
    }

    pub fn directories(&self) -> Result<Vec<String>, String> {
        let mut paths = Vec::new();
        collect_directories(&self.root, &self.root, &mut paths)?;
        paths.sort();
        Ok(paths)
    }

    fn resolve(&self, relative: &str) -> Result<PathBuf, String> {
        let target = if relative.is_empty() {
            self.root.clone()
        } else {
            self.root.join(relative)
        };
        let root = self.root.canonicalize().unwrap_or_else(|_| self.root.clone());
        if target.exists() {
            let canonical = target.canonicalize().map_err(|error| format!("invalid quick note path: {error}"))?;
            if !canonical.starts_with(&root) {
                return Err("Invalid Vault path".into());
            }
        } else if !target.starts_with(&self.root) {
            return Err("Invalid Vault path".into());
        }
        Ok(target)
    }
}

pub fn serialize_note(metadata: &QuickNoteMetadata, content: &str) -> String {
    let spacing = format!("{:.1}", clamp_line_spacing(metadata.line_spacing));
    let yaml = format!(
        "title: {}\nstyle: {}\nsyntax: {}\nfont_name: {}\nfont_size: \"{}\"\nline_spacing: \"{}\"\ncolor: {}\nline_wrap: \"{}\"\ncreated_at: {}\nmodified_at: {}\nid: {}\ntags: {}\npinned: \"{}\"\neditor_font: {}\nline_height: {}",
        yaml_scalar(&metadata.title),
        yaml_scalar(&metadata.style),
        yaml_scalar(&metadata.syntax),
        yaml_scalar(&metadata.font_name),
        clamp_font_size(metadata.font_size),
        spacing,
        yaml_scalar(&metadata.color),
        if metadata.line_wrap { "1" } else { "0" },
        yaml_scalar(&metadata.created_at),
        yaml_scalar(&metadata.modified_at),
        yaml_scalar(&metadata.id),
        yaml_scalar(&metadata.tags.join(",")),
        if metadata.pinned { "1" } else { "0" },
        yaml_scalar(&metadata.editor_font),
        yaml_scalar(&metadata.line_height)
    );
    let separator = if content.is_empty() || content.starts_with('\n') { "" } else { "\n" };
    format!("---\n{yaml}\n---{separator}{content}")
}

pub fn parse_note(raw: &str, fallback_title: &str) -> (String, Option<QuickNoteMetadata>) {
    let Some(rest) = raw.strip_prefix("---\n").or_else(|| raw.strip_prefix("---\r\n")) else {
        return (raw.to_string(), None);
    };
    let Some(end) = rest.find("\n---") else {
        return (raw.to_string(), None);
    };
    let yaml = &rest[..end];
    let mut content = &rest[end + 4..];
    if let Some(stripped) = content.strip_prefix('\n').or_else(|| content.strip_prefix("\r\n")) {
        content = stripped;
    }
    let mut values = std::collections::HashMap::new();
    for line in yaml.lines() {
        let Some((key, value)) = line.split_once(':') else { continue };
        values.insert(key.trim().to_string(), unquote(value.trim()));
    }
    let metadata = QuickNoteMetadata {
        title: values.get("title").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| fallback_title.into()),
        style: values.get("style").cloned().unwrap_or_default(),
        syntax: values.get("syntax").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "text/plain".into()),
        font_name: values.get("font_name").cloned().unwrap_or_default(),
        font_size: clamp_font_size(values.get("font_size").and_then(|value| value.parse().ok()).unwrap_or(14)),
        line_spacing: clamp_line_spacing(values.get("line_spacing").and_then(|value| value.parse().ok()).unwrap_or(1.0)),
        color: values.get("color").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "default".into()),
        line_wrap: values.get("line_wrap").map(|value| value == "1" || value == "true").unwrap_or(true),
        created_at: values.get("created_at").cloned().unwrap_or_else(iso_now),
        modified_at: values.get("modified_at").cloned().unwrap_or_else(iso_now),
        id: values.get("id").cloned().unwrap_or_default(),
        tags: values.get("tags").map(|value| value.split(',').filter(|tag| !tag.is_empty()).map(str::to_string).collect()).unwrap_or_default(),
        pinned: values.get("pinned").is_some_and(|value| value == "1" || value == "true"),
        editor_font: values.get("editor_font").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "default".into()),
        line_height: values.get("line_height").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "normal".into()),
    };
    (content.to_string(), Some(metadata))
}

fn list_directory(root: &Path, directory: &Path) -> Result<Vec<QuickNoteNode>, String> {
    let mut nodes = Vec::new();
    for entry in fs::read_dir(directory).map_err(|error| format!("failed to list notes: {error}"))? {
        let entry = entry.map_err(|error| format!("failed to read note entry: {error}"))?;
        let name = entry.file_name().to_string_lossy().to_string();
        if skipped_vault_entry(&name) {
            continue;
        }
        let path = entry.path();
        let relative = path.strip_prefix(root).unwrap_or(&path).to_string_lossy().replace('\\', "/");
        let file_type = entry.file_type().map_err(|error| format!("failed to inspect note entry: {error}"))?;
        if file_type.is_dir() {
            nodes.push(QuickNoteNode {
                name,
                relative_path: relative,
                kind: "directory",
                children: list_directory(root, &path)?,
            });
        } else if is_note_file(&name) {
            nodes.push(QuickNoteNode {
                name,
                relative_path: relative,
                kind: "file",
                children: Vec::new(),
            });
        }
    }
    nodes.sort_by(|left, right| {
        let kind = (left.kind != "directory").cmp(&(right.kind != "directory"));
        kind.then_with(|| left.name.cmp(&right.name))
    });
    Ok(nodes)
}

fn unique_note_path(root: &Path, parent: &str, title: &str, extension: &str) -> Result<String, String> {
    for index in 0..10_000 {
        let suffix = if index == 0 { String::new() } else { format!(" ({})", index + 1) };
        let name = format!("{title}{suffix}{extension}");
        let relative = if parent.is_empty() { name } else { format!("{parent}/{name}") };
        if !root.join(&relative).exists() {
            return Ok(relative);
        }
    }
    Err("unable to allocate a unique note path".into())
}

fn normalize_note_path(value: &str) -> Result<String, String> {
    let normalized = normalize_entry(value)?;
    let extension = Path::new(&normalized)
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or("");
    if !NOTE_EXTENSIONS.contains(&extension) {
        return Err("Invalid Quick Note file".into());
    }
    Ok(normalized)
}

fn normalize_directory(value: &str, allow_root: bool) -> Result<String, String> {
    if allow_root && value.is_empty() {
        return Ok(String::new());
    }
    let normalized = normalize_entry(value)?;
    if is_note_file(&normalized) {
        return Err("Invalid Vault directory".into());
    }
    Ok(normalized)
}

fn normalize_entry(value: &str) -> Result<String, String> {
    let normalized = value.trim().trim_start_matches("./").trim_end_matches('/').replace('\\', "/");
    if normalized.is_empty()
        || normalized.starts_with('/')
        || normalized.contains('\0')
        || normalized.split('/').any(|part| part.is_empty() || part == "." || part == ".." || part.starts_with('.'))
    {
        return Err("Invalid Vault path".into());
    }
    Ok(normalized)
}

fn is_note_file(name: &str) -> bool {
    Path::new(name)
        .extension()
        .and_then(|value| value.to_str())
        .is_some_and(|extension| NOTE_EXTENSIONS.contains(&extension))
}

fn sanitize_name(value: &str) -> Result<String, String> {
    let sanitized: String = value
        .trim()
        .chars()
        .map(|char| if matches!(char, '\\' | '/' | ':' | '*' | '?' | '"' | '<' | '>' | '|') { '_' } else { char })
        .collect();
    let sanitized = sanitized.trim_start_matches('.').trim();
    if sanitized.is_empty() || sanitized.chars().count() > 180 {
        return Err("Invalid title".into());
    }
    Ok(sanitized.to_string())
}

fn yaml_scalar(value: &str) -> String {
    if value.is_empty() || value.contains([':', '#', '"', '\n', ' ']) {
        format!("\"{}\"", value.replace('\\', "\\\\").replace('"', "\\\""))
    } else {
        value.to_string()
    }
}

fn unquote(value: &str) -> String {
    value.trim_matches('"').replace("\\\"", "\"").replace("\\\\", "\\")
}

fn skipped_vault_entry(name: &str) -> bool {
    name.starts_with('.') || name == "attachments"
}

fn normalize_attachment_extension(extension: &str) -> Result<String, String> {
    let extension = if extension.starts_with('.') {
        extension.to_ascii_lowercase()
    } else {
        format!(".{}", extension.to_ascii_lowercase())
    };
    if !ATTACHMENT_EXTENSIONS.contains(&extension.as_str()) {
        return Err("Unsupported image attachment".into());
    }
    Ok(if extension == ".jpeg" { ".jpg".into() } else { extension })
}

fn normalize_attachment_path(value: &str) -> Result<String, String> {
    let value = value.replace('\\', "/");
    if value.starts_with('/') || value.contains(':') || value.split('/').any(|part| part.is_empty() || part == "." || part == "..") {
        return Err("Invalid attachment path".into());
    }
    let mut parts = value.split('/');
    if parts.next() != Some("attachments") {
        return Err("Invalid attachment path".into());
    }
    let Some(name) = parts.next() else {
        return Err("Invalid attachment path".into());
    };
    if parts.next().is_some() || name.starts_with('.') {
        return Err("Invalid attachment path".into());
    }
    let extension = Path::new(name).extension().and_then(|value| value.to_str()).unwrap_or("");
    normalize_attachment_extension(&format!(".{extension}"))?;
    Ok(format!("attachments/{name}"))
}

fn compact_timestamp() -> String {
    format_millis(
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|duration| duration.as_millis() as i64)
            .unwrap_or(0),
    )
    .chars()
    .filter(|character| character.is_ascii_digit())
    .take(14)
    .collect()
}

fn attachment_suffix() -> String {
    let sequence = ATTACHMENT_SEQUENCE.fetch_add(1, Ordering::Relaxed);
    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_nanos() as u64)
        .unwrap_or(0);
    format!("_{:08x}", nanos ^ sequence.wrapping_mul(0x9E37_79B9_7F4A_7C15))
}

pub fn image_mime(extension: &str) -> &'static str {
    match extension {
        ".jpg" | ".jpeg" => "image/jpeg",
        ".png" => "image/png",
        ".gif" => "image/gif",
        ".bmp" => "image/bmp",
        ".webp" => "image/webp",
        _ => "application/octet-stream",
    }
}

fn clamp_font_size(value: u16) -> u16 {
    value.clamp(8, 48)
}

fn clamp_line_spacing(value: f32) -> f32 {
    (value.clamp(1.0, 2.0) * 10.0).round() / 10.0
}

fn iso_now() -> String {
    let millis = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_millis() as i64)
        .unwrap_or(0);
    format_millis(millis)
}

fn format_millis(millis: i64) -> String {
    let seconds = millis.div_euclid(1000);
    let fraction = millis.rem_euclid(1000) as u32;
    let Ok(time) = time::OffsetDateTime::from_unix_timestamp(seconds) else {
        return "1970-01-01T00:00:00.000Z".into();
    };
    let date = time.date();
    let (year, month, day) = date.to_calendar_date();
    format!(
        "{year:04}-{:02}-{day:02}T{:02}:{:02}:{:02}.{fraction:03}Z",
        u8::from(month),
        time.hour(),
        time.minute(),
        time.second()
    )
}

fn fallback_metadata(title: &str, metadata: &fs::Metadata) -> QuickNoteMetadata {
    let _ = metadata;
    let now = iso_now();
    QuickNoteMetadata {
        title: title.into(),
        style: String::new(),
        syntax: "text/plain".into(),
        font_name: String::new(),
        font_size: 14,
        line_spacing: 1.0,
        color: "default".into(),
        line_wrap: true,
        created_at: now.clone(),
        modified_at: now,
        id: String::new(),
        tags: Vec::new(),
        pinned: false,
        editor_font: "default".into(),
        line_height: "normal".into(),
    }
}

fn collect_files(root: &Path, directory: &Path, files: &mut Vec<QuickNoteFile>) -> Result<(), String> {
    for entry in fs::read_dir(directory).map_err(|error| format!("failed to list notes: {error}"))? {
        let entry = entry.map_err(|error| format!("failed to read note entry: {error}"))?;
        let name = entry.file_name().to_string_lossy().to_string();
        if skipped_vault_entry(&name) {
            continue;
        }
        let path = entry.path();
        if path.is_dir() {
            collect_files(root, &path, files)?;
        } else if is_note_file(&name) {
            let relative = path.strip_prefix(root).unwrap_or(&path).to_string_lossy().replace('\\', "/");
            let vault = QuickNoteVault { root: root.to_path_buf() };
            files.push(vault.read(&relative)?);
        }
    }
    Ok(())
}

fn collect_directories(root: &Path, directory: &Path, paths: &mut Vec<String>) -> Result<(), String> {
    for entry in fs::read_dir(directory).map_err(|error| format!("failed to list note folders: {error}"))? {
        let entry = entry.map_err(|error| format!("failed to read note folder: {error}"))?;
        let name = entry.file_name().to_string_lossy().to_string();
        if skipped_vault_entry(&name) || !entry.path().is_dir() {
            continue;
        }
        let relative = entry.path().strip_prefix(root).unwrap_or(&entry.path()).to_string_lossy().replace('\\', "/");
        paths.push(relative.clone());
        collect_directories(root, &entry.path(), paths)?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn creates_electron_compatible_frontmatter_and_nested_files() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        vault.create_folder("Work").expect("folder");
        let note = vault
            .create("API ideas", "Work", "PingFang SC", 15)
            .expect("create");
        let saved = vault
            .save(&QuickNoteFile {
                content: "# Hello\nneedle".into(),
                ..note
            })
            .expect("save");

        assert_eq!(saved.relative_path, "Work/API ideas.txt");
        assert_eq!(saved.metadata.font_name, "PingFang SC");
        assert_eq!(saved.metadata.font_size, 15);
        assert_eq!(saved.metadata.line_spacing, 1.0);
        let raw = fs::read_to_string(directory.path().join(&saved.relative_path)).expect("raw");
        assert!(raw.contains("font_name: PingFang SC"));
        assert!(raw.contains("font_size: \"15\""));
        assert!(raw.contains("line_spacing: \"1.0\""));
        assert_eq!(vault.list().expect("list").len(), 1);
        assert_eq!(vault.list().expect("list")[0].children.len(), 1);
    }

    #[test]
    fn reads_legacy_frontmatter() {
        let directory = tempfile::tempdir().expect("temp dir");
        fs::write(
            directory.path().join("Legacy.txt"),
            "---\ntitle: Legacy title\nsyntax: text/markdown\nline_spacing: \"1.6\"\nline_wrap: \"0\"\n---\nlegacy body",
        )
        .expect("write");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        let note = vault.read("Legacy.txt").expect("read");
        assert_eq!(note.content, "legacy body");
        assert_eq!(note.metadata.title, "Legacy title");
        assert_eq!(note.metadata.line_spacing, 1.6);
        assert!(!note.metadata.line_wrap);
    }

    #[test]
    fn stores_image_attachments_outside_the_note_tree() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        let stored = vault.store_attachment(b"\x89PNG\r\n\x1a\n", ".jpeg").expect("store");
        assert!(stored.relative_path.starts_with("attachments/"));
        assert!(stored.relative_path.ends_with(".jpg"));
        assert_eq!(stored.markdown, format!("![image]({})", stored.relative_path));
        assert_eq!(vault.read_attachment(&stored.relative_path).expect("read"), b"\x89PNG\r\n\x1a\n");
        assert!(vault.note_files().expect("notes").is_empty());
        assert!(vault.directories().expect("folders").is_empty());
        assert!(vault.store_attachment(b"text", ".txt").is_err());
        assert!(vault.read_attachment("../secret.png").is_err());
        assert!(vault.delete_attachment(&stored.relative_path).expect("delete"));
        assert!(!directory.path().join(&stored.relative_path).exists());
    }

    #[test]
    fn rejects_path_traversal() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        assert!(vault.read("../outside.txt").is_err());
    }
}
