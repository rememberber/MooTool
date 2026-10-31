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

    pub fn create(&self, title: &str, parent: &str, font_name: &str, font_size: u16, line_wrap: bool) -> Result<QuickNoteFile, String> {
        let title = normalize_title(title)?;
        let file_name = sanitize_name(&title).map_err(|_| "Invalid file name".to_string())?;
        let parent = normalize_directory(parent, true)?;
        let directory = self.resolve(&parent)?;
        fs::create_dir_all(&directory).map_err(|error| format!("failed to create note folder: {error}"))?;
        let relative = unique_note_path(&self.root, &parent, &file_name, ".txt")?;
        let now = iso_now();
        self.save(&QuickNoteFile {
            relative_path: relative,
            content: String::new(),
            metadata: QuickNoteMetadata {
                id: String::new(),
                title,
                style: String::new(),
                syntax: "text/plain".into(),
                font_name: font_name.chars().take(120).collect(),
                font_size: clamp_font_size(font_size),
                line_spacing: 1.0,
                color: "default".into(),
                line_wrap,
                created_at: now.clone(),
                modified_at: now,
                tags: Vec::new(),
                pinned: false,
                editor_font: "default".into(),
                line_height: "normal".into(),
            },
        })
    }

    pub fn duplicate(&self, relative_path: &str) -> Result<QuickNoteFile, String> {
        let source = self.read(relative_path)?;
        let copy_title = format!("{} Copy", source.metadata.title.trim());
        if copy_title.chars().any(|character| character == '\n' || character == '\r' || character == '\0')
            || copy_title.chars().count() > 180
        {
            return Err("Invalid title".into());
        }
        let parent = match source.relative_path.rsplit_once('/') {
            Some((folder, _)) => folder.to_string(),
            None => String::new(),
        };
        let extension = format!(".{}", extension_for_note_syntax(&source.metadata.syntax));
        let relative = unique_note_path(&self.root, &parent, &sanitize_name(&copy_title)?, &extension)?;
        let now = iso_now();
        self.save(&QuickNoteFile {
            relative_path: relative,
            content: source.content,
            metadata: QuickNoteMetadata {
                id: String::new(),
                title: copy_title,
                style: source.metadata.style,
                syntax: source.metadata.syntax,
                font_name: source.metadata.font_name,
                font_size: source.metadata.font_size,
                line_spacing: source.metadata.line_spacing,
                color: source.metadata.color,
                line_wrap: source.metadata.line_wrap,
                created_at: now.clone(),
                modified_at: now,
                tags: source.metadata.tags,
                pinned: false,
                editor_font: source.metadata.editor_font,
                line_height: source.metadata.line_height,
            },
        })
    }

    pub fn rename_entry(&self, relative_path: &str, name: &str) -> Result<String, String> {
        let source_relative = normalize_entry(relative_path)?;
        let source = self.resolve(&source_relative)?;
        if !source.exists() {
            return Err("Vault entry does not exist".into());
        }
        let file_entry = source.is_file();
        let title = name.trim();
        if title.is_empty() || title.chars().count() > 180 || title.chars().any(|character| character == '\n' || character == '\r' || character == '\0') {
            return Err("Invalid title".into());
        }
        let sanitized = sanitize_name(title)?;
        let parent = match source_relative.rsplit_once('/') {
            Some((folder, _)) => folder.to_string(),
            None => String::new(),
        };
        let next_name = if file_entry {
            match Path::new(&source_relative).extension().and_then(|value| value.to_str()) {
                Some(extension) if !extension.is_empty() => format!("{sanitized}.{extension}"),
                _ => sanitized,
            }
        } else {
            sanitized
        };
        let mut next_relative = if parent.is_empty() { next_name } else { format!("{parent}/{next_name}") };
        if next_relative != source_relative && self.resolve(&next_relative)?.exists() {
            return Err("An entry with that name already exists".into());
        }
        if next_relative != source_relative {
            let target = self.resolve(&next_relative)?;
            if let Some(directory) = target.parent() {
                fs::create_dir_all(directory).map_err(|error| format!("failed to create note folder: {error}"))?;
            }
            fs::rename(&source, &target).map_err(|error| format!("failed to rename note: {error}"))?;
        }
        if file_entry {
            let mut file = self.read(&next_relative)?;
            file.metadata.title = title.to_string();
            let syntax_extension = extension_for_note_syntax(&file.metadata.syntax);
            let current_extension = Path::new(&next_relative).extension().and_then(|value| value.to_str()).unwrap_or("");
            if current_extension != syntax_extension {
                let stem = Path::new(&next_relative).file_stem().and_then(|value| value.to_str()).unwrap_or("note");
                let rewritten = match next_relative.rsplit_once('/') {
                    Some((folder, _)) => format!("{folder}/{stem}.{syntax_extension}"),
                    None => format!("{stem}.{syntax_extension}"),
                };
                if rewritten != next_relative && self.resolve(&rewritten)?.exists() {
                    return Err("An entry with that name already exists".into());
                }
                fs::rename(self.resolve(&next_relative)?, self.resolve(&rewritten)?).map_err(|error| format!("failed to rename note: {error}"))?;
                file.relative_path = rewritten.clone();
                next_relative = rewritten;
            }
            self.save(&file)?;
        }
        Ok(next_relative)
    }

    pub fn move_entry(&self, relative_path: &str, target_directory: &str) -> Result<String, String> {
        let source_relative = normalize_entry(relative_path)?;
        let target_directory = normalize_directory(target_directory, true)?;
        if target_directory == source_relative || target_directory.starts_with(&format!("{source_relative}/")) {
            return Err("Cannot move a folder into itself".into());
        }
        let current_parent = match source_relative.rsplit_once('/') {
            Some((folder, _)) => folder.to_string(),
            None => String::new(),
        };
        if current_parent == target_directory {
            return Ok(source_relative);
        }
        let source = self.resolve(&source_relative)?;
        if !source.exists() {
            return Err("Vault entry does not exist".into());
        }
        let directory = if target_directory.is_empty() { self.root.clone() } else { self.resolve(&target_directory)? };
        fs::create_dir_all(&directory).map_err(|error| format!("failed to create note folder: {error}"))?;
        let file_name = source
            .file_name()
            .and_then(|value| value.to_str())
            .ok_or_else(|| "Invalid Vault path".to_string())?;
        let mut target = directory.join(file_name);
        if target.exists() {
            if source.is_dir() {
                return Err("Target folder already contains this entry".into());
            }
            let stem = Path::new(file_name).file_stem().and_then(|value| value.to_str()).unwrap_or("note");
            let extension = Path::new(file_name)
                .extension()
                .and_then(|value| value.to_str())
                .map(|value| format!(".{value}"))
                .unwrap_or_default();
            let unique = unique_note_path(&self.root, &target_directory, stem, &extension)?;
            target = self.resolve(&unique)?;
        }
        fs::rename(&source, &target).map_err(|error| format!("failed to move note: {error}"))?;
        Ok(target.strip_prefix(&self.root).unwrap_or(&target).to_string_lossy().replace('\\', "/"))
    }

    pub fn create_folder(&self, path: &str) -> Result<(), String> {
        let relative = normalize_directory(path, false)?;
        let target = self.resolve(&relative)?;
        match fs::create_dir(&target) {
            Ok(()) => Ok(()),
            Err(error) if error.kind() == std::io::ErrorKind::AlreadyExists => Err("An entry with that name already exists".into()),
            Err(error) => Err(format!("failed to create folder: {error}")),
        }
    }

    pub fn read(&self, relative_path: &str) -> Result<QuickNoteFile, String> {
        let relative = normalize_note_path(relative_path)?;
        let target = self.resolve(&relative)?;
        let raw = fs::read_to_string(&target).map_err(|error| format!("failed to read note: {error}"))?;
        let metadata = fs::metadata(&target).map_err(|error| format!("failed to stat note: {error}"))?;
        let fallback = Path::new(&relative)
            .file_stem()
            .and_then(|value| value.to_str())
            .unwrap_or("note")
            .to_string();
        let parsed = parse_note(&raw, &fallback);
        Ok(QuickNoteFile {
            relative_path: relative,
            content: parsed.0,
            metadata: parsed.1.unwrap_or_else(|| fallback_metadata(&fallback, &metadata)),
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
        "title: {}\nstyle: {}\nsyntax: {}\nfont_name: {}\nfont_size: \"{}\"\nline_spacing: \"{}\"\ncolor: {}\nline_wrap: \"{}\"\ncreated_at: {}\nmodified_at: {}\nid: {}\ntags: {}\npinned: \"{}\"",
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
        if metadata.pinned { "1" } else { "0" }
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
    let legacy_font = values.get("editor_font").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "default".into());
    let legacy_height = values.get("line_height").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "normal".into());
    let font_name = values.get("font_name").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| match legacy_font.as_str() {
        "mono" => "ui-monospace".into(),
        "serif" => "Georgia".into(),
        _ => String::new(),
    });
    let line_spacing = if values.contains_key("line_spacing") {
        clamp_line_spacing(values.get("line_spacing").and_then(|value| value.parse().ok()).unwrap_or(1.0))
    } else {
        match legacy_height.as_str() {
            "relaxed" => 1.6,
            "compact" => 1.0,
            _ => 1.0,
        }
    };
    let metadata = QuickNoteMetadata {
        title: values.get("title").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| fallback_title.into()),
        style: values.get("style").cloned().unwrap_or_default(),
        syntax: values.get("syntax").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "text/plain".into()),
        font_name,
        font_size: clamp_font_size(values.get("font_size").and_then(|value| value.parse().ok()).unwrap_or(14)),
        line_spacing,
        color: values.get("color").cloned().filter(|value| !value.is_empty()).unwrap_or_else(|| "default".into()),
        line_wrap: values.get("line_wrap").map(|value| value == "1" || value == "true").unwrap_or(true),
        created_at: values.get("created_at").cloned().unwrap_or_else(iso_now),
        modified_at: values.get("modified_at").cloned().unwrap_or_else(iso_now),
        id: values.get("id").cloned().unwrap_or_default(),
        tags: values.get("tags").map(|value| value.split(',').filter(|tag| !tag.is_empty()).map(str::to_string).collect()).unwrap_or_default(),
        pinned: values.get("pinned").is_some_and(|value| value == "1" || value == "true"),
        editor_font: legacy_font,
        line_height: legacy_height,
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

fn extension_for_note_syntax(syntax: &str) -> &'static str {
    match syntax {
        "markdown" | "text/markdown" => "md",
        "json" | "application/json" => "json",
        "java" | "text/java" => "java",
        "javascript" | "text/javascript" => "js",
        "typescript" | "text/typescript" => "ts",
        "python" | "text/python" => "py",
        "html" | "text/html" => "html",
        "xml" | "text/xml" => "xml",
        "yaml" | "text/yaml" => "yaml",
        "sql" | "text/sql" => "sql",
        _ => "txt",
    }
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
        || normalized.encode_utf16().count() > 768
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

fn normalize_title(value: &str) -> Result<String, String> {
    let title = value.trim();
    if title.is_empty() || title.chars().count() > 180 || title.chars().any(|character| character == '\n' || character == '\r' || character == '\0') {
        return Err("Invalid title".into());
    }
    Ok(title.to_string())
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
            .create("API ideas", "Work", "PingFang SC", 15, true)
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
        assert!(raw.contains("font_name: \"PingFang SC\""));
        assert!(raw.contains("font_size: \"15\""));
        assert!(raw.contains("line_spacing: \"1.0\""));
        assert!(!raw.contains("editor_font:"));
        assert!(!raw.contains("line_height:"));
        let (legacy_content, legacy) = parse_note("---\ntitle: Old\neditor_font: mono\nline_height: relaxed\n---\nbody", "Old");
        let legacy = legacy.expect("legacy");
        assert_eq!(legacy_content, "body");
        assert_eq!(legacy.font_name, "ui-monospace");
        assert_eq!(legacy.line_spacing, 1.6);
        assert_eq!(vault.list().expect("list").len(), 1);
        assert_eq!(vault.list().expect("list")[0].children.len(), 1);
        let copy = vault.duplicate(&saved.relative_path).expect("duplicate");
        assert_eq!(copy.relative_path, "Work/API ideas Copy.txt");
        assert_eq!(copy.metadata.title, "API ideas Copy");
        assert_eq!(copy.content, "# Hello\nneedle");
        assert!(!copy.metadata.pinned);
        let markdown = vault
            .save(&QuickNoteFile {
                relative_path: "Work/Query.md".into(),
                content: "select 1".into(),
                metadata: QuickNoteMetadata {
                    syntax: "text/markdown".into(),
                    title: "Query".into(),
                    pinned: true,
                    tags: vec!["sql".into()],
                    ..saved.metadata.clone()
                },
            })
            .expect("markdown");
        let markdown_copy = vault.duplicate(&markdown.relative_path).expect("markdown copy");
        assert_eq!(markdown_copy.relative_path, "Work/Query Copy.md");
        assert_eq!(markdown_copy.metadata.tags, vec!["sql".to_string()]);
        assert!(!markdown_copy.metadata.pinned);
        let second = vault.duplicate(&markdown.relative_path).expect("second copy");
        assert_eq!(second.relative_path, "Work/Query Copy (2).md");
        assert_eq!(second.metadata.title, "Query Copy");
    }

    #[test]
    fn creates_a_plain_text_note_and_keeps_the_raw_title() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        let note = vault.create("  a/b  ", "", "ui-monospace", 14, false).expect("create");
        assert_eq!(note.metadata.title, "a/b");
        assert_eq!(note.relative_path, "a_b.txt");
        assert_eq!(note.metadata.syntax, "text/plain");
        assert!(!note.metadata.line_wrap);
        assert_eq!(vault.create("   ", "", "ui-monospace", 14, true).unwrap_err(), "Invalid title");
        assert_eq!(vault.create("...", "", "ui-monospace", 14, true).unwrap_err(), "Invalid file name");
    }

    #[test]
    fn creates_a_folder_only_when_its_parent_exists() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        vault.create_folder("Work").expect("work");
        vault.create_folder("Work/Nested").expect("nested");
        assert!(directory.path().join("Work/Nested").is_dir());
        assert_eq!(vault.create_folder("Work").unwrap_err(), "An entry with that name already exists");
        assert!(vault.create_folder("Missing/Child").is_err());
        assert!(vault.create_folder(".hidden").is_err());
    }

    #[test]
    fn moves_a_note_without_overwriting_an_existing_file() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        vault.create_folder("Work").expect("work");
        vault.create_folder("Other").expect("other");
        let note = vault.create("API ideas", "Work", "ui-monospace", 14, true).expect("create");
        let saved = vault
            .save(&QuickNoteFile { content: "keep me".into(), ..note })
            .expect("save");
        vault.create("API ideas", "Other", "ui-monospace", 14, true).expect("collision");
        let moved = vault.move_entry(&saved.relative_path, "Other").expect("move");
        assert_eq!(moved, "Other/API ideas (2).txt");
        assert_eq!(vault.read(&moved).expect("read").content, "keep me");
        assert!(vault.move_entry("Other", "Other/nested").is_err());
        vault.create_folder("Archive").expect("archive");
        vault.create_folder("Archive/Work").expect("existing folder");
        assert_eq!(
            vault.move_entry("Work", "Archive").expect_err("folder collision"),
            "Target folder already contains this entry"
        );
    }

    #[test]
    fn renames_a_note_without_replacing_an_existing_file() {
        let directory = tempfile::tempdir().expect("temp dir");
        let vault = QuickNoteVault::open(directory.path().to_path_buf()).expect("vault");
        vault.create_folder("Work").expect("work");
        let note = vault.create("API ideas", "Work", "ui-monospace", 14, true).expect("create");
        let saved = vault
            .save(&QuickNoteFile {
                content: "body".into(),
                metadata: QuickNoteMetadata { syntax: "text/markdown".into(), ..note.metadata },
                ..note
            })
            .expect("save");
        vault.create("Query", "Work", "ui-monospace", 14, true).expect("existing");
        assert_eq!(
            vault.rename_entry(&saved.relative_path, "Query").expect_err("collision"),
            "An entry with that name already exists"
        );
        let renamed = vault.rename_entry(&saved.relative_path, "a/b").expect("rename");
        assert_eq!(renamed, "Work/a_b.md");
        let file = vault.read(&renamed).expect("read");
        assert_eq!(file.metadata.title, "a/b");
        assert_eq!(file.content, "body");
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
