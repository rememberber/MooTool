use std::{
    collections::{HashMap, HashSet},
    fs,
    io::ErrorKind,
    path::{Path, PathBuf},
    sync::{Mutex, TryLockError},
    time::{SystemTime, UNIX_EPOCH},
};

use serde::{Deserialize, Serialize};
use serde_json::Value;
use tauri::{AppHandle, Manager};

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RenameFile {
    pub id: String,
    pub name: String,
    pub path: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RenameRow {
    pub id: String,
    pub name: String,
    pub path: String,
    pub new_name: String,
    pub status: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub issue: Option<String>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RenamePreview {
    pub token: Option<String>,
    pub rows: Vec<RenameRow>,
    pub changed_count: usize,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RenameStatus {
    pub undo_available: bool,
    pub recovery_required: bool,
    pub journal_path: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RenameResult {
    pub changed_count: usize,
    pub files: Vec<RenameFile>,
    pub status: RenameStatus,
}

#[derive(Clone, Debug)]
struct Options {
    find: String,
    replacement: String,
    prefix: String,
    suffix: String,
    case_sensitive: bool,
    preserve_extension: bool,
    numbering: bool,
    start: i64,
    width: usize,
    separator: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
struct Identity {
    dev: String,
    ino: String,
    size: String,
    #[serde(rename = "mtimeNs")]
    mtime_ns: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
struct Move {
    source: String,
    target: String,
    temporary: String,
    identity: Identity,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
struct Journal {
    version: u8,
    phase: String,
    moves: Vec<Move>,
    #[serde(skip_serializing_if = "Option::is_none")]
    previous: Option<Box<Journal>>,
}

struct Plan {
    owner: u64,
    #[allow(dead_code)]
    rows: Vec<RenameRow>,
    moves: Vec<Move>,
}

struct Selection {
    files: Vec<RenameFile>,
    identities: HashMap<String, Identity>,
}

pub struct BatchRenameService {
    journal_path: PathBuf,
    selections: HashMap<u64, Selection>,
    plans: HashMap<String, Plan>,
}

impl BatchRenameService {
    pub fn new(directory: PathBuf) -> Self {
        Self {
            journal_path: directory.join("batch-rename-journal.json"),
            selections: HashMap::new(),
            plans: HashMap::new(),
        }
    }

    fn select(&mut self, owner: u64, paths: &[String]) -> Result<Vec<RenameFile>, String> {
        if paths.len() > 500 {
            return fail("limit");
        }
        let mut selected = Vec::<RenameFile>::new();
        let mut identities = HashMap::new();
        for path in paths {
            let original = PathBuf::from(path);
            if !original.is_absolute() {
                return fail("file");
            }
            let parent = fs::canonicalize(original.parent().ok_or_else(|| coded("file"))?)
                .map_err(|_| coded("file"))?;
            let requested = original.file_name().and_then(|name| name.to_str()).ok_or_else(|| coded("file"))?;
            let names = directory_names(&parent)?;
            let name = names
                .iter()
                .find(|name| name.as_str() == requested)
                .or_else(|| names.iter().find(|name| rename_name_key(name) == rename_name_key(requested)))
                .ok_or_else(|| coded("file"))?;
            let canonical = parent.join(name);
            let identity = file_identity(&canonical)?.ok_or_else(|| coded("file"))?;
            let file = selected
                .iter()
                .find(|file| file.path == canonical.to_string_lossy())
                .cloned()
                .unwrap_or_else(|| RenameFile {
                    id: uuid(),
                    path: canonical.to_string_lossy().into_owned(),
                    name: name.clone(),
                });
            if !selected.iter().any(|item| item.id == file.id) {
                selected.push(file.clone());
            }
            identities.insert(file.id, identity);
        }
        if selected.len() > 500 {
            return fail("limit");
        }
        self.plans.retain(|_, plan| plan.owner != owner);
        let files = selected.clone();
        self.selections.insert(owner, Selection { files, identities });
        Ok(selected)
    }

    fn preview(&mut self, owner: u64, ids: &Value, input: &Value) -> Result<RenamePreview, String> {
        if self.status()?.recovery_required {
            return fail("recovery");
        }
        let ids = ids.as_array().ok_or_else(|| coded("selection"))?;
        if ids.is_empty() || ids.len() > 500 || ids.iter().collect::<HashSet<_>>().len() != ids.len() {
            return fail("selection");
        }
        let options = normalize_options(input)?;
        let selection = self.selections.get(&owner).ok_or_else(|| coded("selection"))?;
        let mut files = Vec::new();
        for id in ids {
            let Some(id) = id.as_str() else { return fail("selection"); };
            let Some(file) = selection.files.iter().find(|file| file.id == id) else { return fail("selection"); };
            files.push(file.clone());
        }
        let mut rows = Vec::new();
        let mut moves = Vec::new();
        let mut target_keys: HashMap<String, Vec<usize>> = HashMap::new();
        let mut identities: HashMap<String, Vec<usize>> = HashMap::new();
        for (index, file) in files.iter().enumerate() {
            let new_name = rename_candidate(&file.name, &options, index);
            let mut row = RenameRow {
                id: file.id.clone(),
                name: file.name.clone(),
                path: file.path.clone(),
                new_name: new_name.clone(),
                status: if new_name == file.name { "unchanged".into() } else { "ready".into() },
                issue: None,
            };
            let current = file_identity(Path::new(&file.path))?;
            let granted = selection.identities.get(&file.id);
            if current.as_ref().zip(granted).is_none_or(|(current, granted)| current != granted) {
                row.status = "conflict".into();
                row.issue = Some("changed".into());
                rows.push(row);
                continue;
            }
            let identity = current.unwrap();
            let identity_key = format!("{}:{}", identity.dev, identity.ino);
            identities.entry(identity_key).or_default().push(rows.len());
            if row.status != "unchanged" {
                if !valid_rename_name(&new_name) {
                    row.status = "conflict".into();
                    row.issue = Some("invalidName".into());
                } else {
                    let key = Path::new(&file.path).parent().unwrap_or(Path::new("")).join(rename_name_key(&new_name));
                    target_keys.entry(key.to_string_lossy().into_owned()).or_default().push(rows.len());
                    moves.push(Move {
                        source: file.path.clone(),
                        target: Path::new(&file.path).parent().unwrap_or(Path::new("")).join(&new_name).to_string_lossy().into_owned(),
                        temporary: Path::new(&file.path).parent().unwrap_or(Path::new("")).join(format!(".mootool-rename-{}", uuid())).to_string_lossy().into_owned(),
                        identity,
                    });
                }
            }
            rows.push(row);
        }
        for indexes in target_keys.values() {
            if indexes.len() > 1 {
                for index in indexes {
                    rows[*index].status = "conflict".into();
                    rows[*index].issue = Some("duplicate".into());
                }
            }
        }
        for indexes in identities.values() {
            if indexes.len() > 1 {
                for index in indexes {
                    rows[*index].status = "conflict".into();
                    rows[*index].issue = Some("sameFile".into());
                }
            }
        }
        let source_keys: HashSet<String> = moves.iter().map(|move_| nfc(&move_.source)).collect();
        for row in rows.iter_mut().filter(|row| row.status == "ready") {
            let parent = Path::new(&row.path).parent().unwrap_or(Path::new(""));
            let occupied = directory_names(parent)?.into_iter().filter(|name| rename_name_key(name) == rename_name_key(&row.new_name));
            if occupied.into_iter().any(|name| !source_keys.contains(&nfc(&parent.join(name).to_string_lossy()))) {
                row.status = "conflict".into();
                row.issue = Some("occupied".into());
            }
        }
        self.plans.retain(|_, plan| plan.owner != owner);
        let changed_count = rows.iter().filter(|row| row.status == "ready").count();
        let token = if rows.iter().any(|row| row.status == "conflict") || moves.is_empty() {
            None
        } else {
            Some(uuid())
        };
        if let Some(token) = &token {
            self.plans.insert(token.clone(), Plan { owner, rows: rows.clone(), moves });
        }
        Ok(RenamePreview { token, rows, changed_count })
    }

    fn execute(&mut self, owner: u64, token: &Value) -> Result<RenameResult, String> {
        let token = token.as_str().ok_or_else(|| coded("preview"))?;
        let plan = self.plans.get(token).ok_or_else(|| coded("preview"))?;
        if plan.owner != owner {
            return fail("preview");
        }
        let moves = plan.moves.clone();
        self.preflight(&moves)?;
        let previous = self.read_journal()?;
        if previous.as_ref().is_some_and(|journal| journal.phase == "pending") {
            return fail("recovery");
        }
        self.plans.remove(token);
        let journal = Journal { version: 1, phase: "pending".into(), moves: moves.clone(), previous: previous.map(Box::new) };
        self.save_journal(&journal)?;
        if let Err(error) = self.run(&moves) {
            self.rollback(&journal)?;
            if unsupported_io(&error) {
                return fail("unsupported");
            }
            return Err(error.to_string());
        }
        self.save_journal(&Journal { version: 1, phase: "done".into(), moves: moves.clone(), previous: None })?;
        self.update_selections(&moves);
        Ok(RenameResult { changed_count: moves.len(), files: self.files(owner), status: self.status()? })
    }

    fn undo(&mut self, owner: u64) -> Result<RenameResult, String> {
        let journal = self.read_journal()?.ok_or_else(|| coded("undo"))?;
        if journal.phase == "pending" {
            self.rollback(&journal)?;
            let reversed = journal.moves.iter().map(|move_| Move {
                source: move_.target.clone(),
                target: move_.source.clone(),
                temporary: move_.temporary.clone(),
                identity: move_.identity.clone(),
            }).collect::<Vec<_>>();
            self.update_selections(&reversed);
            return Ok(RenameResult { changed_count: journal.moves.len(), files: self.files(owner), status: self.status()? });
        }
        let moves = journal.moves.iter().map(|move_| Move {
            source: move_.target.clone(),
            target: move_.source.clone(),
            temporary: Path::new(&move_.source).parent().unwrap_or(Path::new("")).join(format!(".mootool-rename-{}", uuid())).to_string_lossy().into_owned(),
            identity: move_.identity.clone(),
        }).collect::<Vec<_>>();
        self.preflight(&moves)?;
        let pending = Journal { version: 1, phase: "pending".into(), moves: moves.clone(), previous: Some(Box::new(journal)) };
        self.save_journal(&pending)?;
        if let Err(error) = self.run(&moves) {
            self.rollback(&pending)?;
            return Err(error.to_string());
        }
        self.remove_journal()?;
        self.update_selections(&moves);
        Ok(RenameResult { changed_count: moves.len(), files: self.files(owner), status: self.status()? })
    }

    fn status(&self) -> Result<RenameStatus, String> {
        let journal = self.read_journal()?;
        Ok(RenameStatus {
            undo_available: journal.is_some(),
            recovery_required: journal.as_ref().is_some_and(|journal| journal.phase == "pending"),
            journal_path: self.journal_path.to_string_lossy().into_owned(),
        })
    }

    fn files(&self, owner: u64) -> Vec<RenameFile> {
        self.selections.get(&owner).map(|selection| selection.files.clone()).unwrap_or_default()
    }

    fn preflight(&self, moves: &[Move]) -> Result<(), String> {
        let sources: HashSet<String> = moves.iter().map(|move_| nfc(&move_.source)).collect();
        for move_ in moves {
            let current = file_identity(Path::new(&move_.source))?.ok_or_else(|| coded("changed"))?;
            if current != move_.identity {
                return fail("changed");
            }
            let parent = Path::new(&move_.target).parent().unwrap_or(Path::new(""));
            let target_name = Path::new(&move_.target).file_name().and_then(|name| name.to_str()).unwrap_or_default();
            let occupied = directory_names(parent)?;
            if occupied.into_iter().filter(|name| rename_name_key(name) == rename_name_key(target_name)).any(|name| !sources.contains(&nfc(&parent.join(name).to_string_lossy()))) {
                return fail("occupied");
            }
        }
        Ok(())
    }

    fn run(&self, moves: &[Move]) -> Result<(), std::io::Error> {
        for move_ in moves {
            relocate(&move_.source, &move_.temporary, &move_.identity)?;
        }
        for move_ in moves {
            relocate(&move_.temporary, &move_.target, &move_.identity)?;
        }
        Ok(())
    }

    fn rollback(&self, journal: &Journal) -> Result<(), String> {
        let mut restore = Vec::new();
        for move_ in &journal.moves {
            let source = exact_identity(Path::new(&move_.source))?;
            let temp = exact_identity(Path::new(&move_.temporary))?;
            let target = exact_identity(Path::new(&move_.target))?;
            let owned = [source.clone(), temp.clone(), target.clone()].into_iter().flatten().filter(|file| file.1 == move_.identity).collect::<Vec<_>>();
            if source.as_ref().is_some_and(|file| file.1 == move_.identity) {
                for file in &owned {
                    if file.0 != source.as_ref().unwrap().0 {
                        fs::remove_file(&file.0).map_err(|error| error.to_string())?;
                    }
                }
                continue;
            }
            if temp.as_ref().is_some_and(|file| file.1 != move_.identity) {
                return fail("recovery");
            }
            let Some(file) = owned.first() else { return fail("recovery"); };
            if temp.is_none() {
                relocate(&file.0.to_string_lossy(), &move_.temporary, &move_.identity).map_err(|_| coded("recovery"))?;
            }
            for other in &owned {
                if other.0 != PathBuf::from(&move_.temporary) && (temp.is_some() || other.0 != file.0) {
                    fs::remove_file(&other.0).map_err(|_| coded("recovery"))?;
                }
            }
            restore.push(move_.clone());
        }
        for move_ in &restore {
            relocate(&move_.temporary, &move_.source, &move_.identity).map_err(|_| coded("recovery"))?;
        }
        if let Some(previous) = &journal.previous {
            self.save_journal(previous).map_err(|_| coded("recovery"))?;
        } else {
            self.remove_journal().map_err(|_| coded("recovery"))?;
        }
        Ok(())
    }

    fn update_selections(&mut self, moves: &[Move]) {
        let mapping = moves.iter().map(|move_| (move_.source.clone(), move_.target.clone())).collect::<HashMap<_, _>>();
        for selection in self.selections.values_mut() {
            for file in &mut selection.files {
                if let Some(path) = mapping.get(&file.path) {
                    file.name = Path::new(path).file_name().and_then(|name| name.to_str()).unwrap_or(&file.name).to_string();
                    file.path = path.clone();
                }
            }
        }
        self.plans.clear();
    }

    fn save_journal(&self, journal: &Journal) -> Result<(), String> {
        if let Some(parent) = self.journal_path.parent() {
            fs::create_dir_all(parent).map_err(|error| error.to_string())?;
        }
        let temporary = format!("{}.{}.tmp", self.journal_path.to_string_lossy(), uuid());
        let mut file = fs::OpenOptions::new().write(true).create_new(true).open(&temporary).map_err(|error| error.to_string())?;
        std::io::Write::write_all(&mut file, serde_json::to_string(journal).map_err(|error| error.to_string())?.as_bytes()).map_err(|error| error.to_string())?;
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            fs::set_permissions(&temporary, fs::Permissions::from_mode(0o600)).map_err(|error| error.to_string())?;
        }
        fs::rename(&temporary, &self.journal_path).map_err(|error| error.to_string())
    }

    fn remove_journal(&self) -> Result<(), String> {
        match fs::remove_file(&self.journal_path) {
            Ok(()) => Ok(()),
            Err(error) if error.kind() == ErrorKind::NotFound => Ok(()),
            Err(error) => Err(error.to_string()),
        }
    }

    fn read_journal(&self) -> Result<Option<Journal>, String> {
        let metadata = match fs::symlink_metadata(&self.journal_path) {
            Ok(metadata) => metadata,
            Err(error) if error.kind() == ErrorKind::NotFound => return Ok(None),
            Err(error) => return Err(error.to_string()),
        };
        if !metadata.is_file() || metadata.file_type().is_symlink() || metadata.len() > 2_000_000 {
            return fail("journal");
        }
        let text = fs::read_to_string(&self.journal_path).map_err(|error| error.to_string())?;
        let value = serde_json::from_str::<Value>(&text).map_err(|_| coded("journal"))?;
        Ok(Some(validate_journal(&value, false)?))
    }
}

fn validate_journal(value: &Value, previous: bool) -> Result<Journal, String> {
    let object = value.as_object().ok_or_else(|| coded("journal"))?;
    let version = object.get("version").and_then(Value::as_u64).ok_or_else(|| coded("journal"))?;
    let phase = object.get("phase").and_then(Value::as_str).unwrap_or("");
    let moves = object.get("moves").and_then(Value::as_array).ok_or_else(|| coded("journal"))?;
    if version != 1 || (phase != "pending" && phase != "done") || moves.is_empty() || moves.len() > 500 {
        return fail("journal");
    }
    let mut parsed = Vec::new();
    for move_ in moves {
        let move_object = move_.as_object().ok_or_else(|| coded("journal"))?;
        let source = required_string(move_object.get("source"))?;
        let target = required_string(move_object.get("target"))?;
        let temporary = required_string(move_object.get("temporary"))?;
        if !Path::new(&source).is_absolute() || !Path::new(&target).is_absolute() || !Path::new(&temporary).is_absolute()
            || Path::new(&source).parent() != Path::new(&target).parent()
            || Path::new(&source).parent() != Path::new(&temporary).parent()
            || !valid_temporary_name(Path::new(&temporary).file_name().and_then(|name| name.to_str()).unwrap_or(""))
        {
            return fail("journal");
        }
        let identity = move_object.get("identity").and_then(Value::as_object).ok_or_else(|| coded("journal"))?;
        let parsed_identity = Identity {
            dev: digit_string(identity.get("dev"))?,
            ino: digit_string(identity.get("ino"))?,
            size: digit_string(identity.get("size"))?,
            mtime_ns: digit_string(identity.get("mtimeNs"))?,
        };
        parsed.push(Move { source, target, temporary, identity: parsed_identity });
    }
    let nested = if let Some(previous_value) = object.get("previous") {
        if previous || previous_value.get("phase").and_then(Value::as_str) != Some("done") {
            return fail("journal");
        }
        Some(Box::new(validate_journal(previous_value, true)?))
    } else {
        None
    };
    Ok(Journal { version: 1, phase: phase.into(), moves: parsed, previous: nested })
}

#[tauri::command]
pub fn select_rename_files(app: AppHandle, paths: Vec<String>) -> Result<Vec<RenameFile>, String> {
    with_service(&app, |service| service.select(1, &paths))
}

#[tauri::command]
pub fn preview_batch_rename(app: AppHandle, ids: Value, options: Value) -> Result<RenamePreview, String> {
    with_service(&app, |service| service.preview(1, &ids, &options))
}

#[tauri::command]
pub fn execute_batch_rename(app: AppHandle, token: Value) -> Result<RenameResult, String> {
    with_service(&app, |service| service.execute(1, &token))
}

#[tauri::command]
pub fn undo_batch_rename(app: AppHandle) -> Result<RenameResult, String> {
    with_service(&app, |service| service.undo(1))
}

#[tauri::command]
pub fn get_batch_rename_status(app: AppHandle) -> Result<RenameStatus, String> {
    with_service(&app, |service| service.status())
}

fn with_service<T>(app: &AppHandle, body: impl FnOnce(&mut BatchRenameService) -> Result<T, String>) -> Result<T, String> {
    static SERVICE: Mutex<Option<BatchRenameService>> = Mutex::new(None);
    let mut guard = match SERVICE.try_lock() {
        Ok(guard) => guard,
        Err(TryLockError::WouldBlock) => return fail("busy"),
        Err(TryLockError::Poisoned(poison)) => poison.into_inner(),
    };
    if guard.is_none() {
        let directory = app.path().app_data_dir().map_err(|error| error.to_string())?.join("file-operations");
        *guard = Some(BatchRenameService::new(directory));
    }
    body(guard.as_mut().expect("batch rename service"))
}

fn relocate(source: &str, target: &str, expected: &Identity) -> Result<(), std::io::Error> {
    let current = file_identity(Path::new(source)).map_err(|error| std::io::Error::other(error))?.ok_or_else(|| std::io::Error::new(ErrorKind::NotFound, "batchRename.error.changed"))?;
    if &current != expected {
        return Err(std::io::Error::other("batchRename.error.changed"));
    }
    fs::hard_link(source, target)?;
    let after = file_identity(Path::new(source)).map_err(|error| std::io::Error::other(error))?.ok_or_else(|| std::io::Error::other("batchRename.error.changed"))?;
    if &after != expected {
        let _ = fs::remove_file(target);
        return Err(std::io::Error::other("batchRename.error.changed"));
    }
    fs::remove_file(source)
}

fn file_identity(path: &Path) -> Result<Option<Identity>, String> {
    let metadata = match fs::symlink_metadata(path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == ErrorKind::NotFound => return Ok(None),
        Err(error) => return Err(error.to_string()),
    };
    if !metadata.is_file() || metadata.file_type().is_symlink() {
        return Ok(None);
    }
    #[cfg(unix)]
    {
        use std::os::unix::fs::MetadataExt;
        let mtime_ns = metadata.mtime().saturating_mul(1_000_000_000).saturating_add(metadata.mtime_nsec());
        return Ok(Some(Identity {
            dev: metadata.dev().to_string(),
            ino: metadata.ino().to_string(),
            size: metadata.size().to_string(),
            mtime_ns: mtime_ns.to_string(),
        }));
    }
    #[cfg(not(unix))]
    {
        let modified = metadata.modified().ok().and_then(|time| time.duration_since(UNIX_EPOCH).ok()).map(|time| time.as_nanos()).unwrap_or(0);
        Ok(Some(Identity { dev: "0".into(), ino: "0".into(), size: metadata.len().to_string(), mtime_ns: modified.to_string() }))
    }
}

fn exact_identity(path: &Path) -> Result<Option<(PathBuf, Identity)>, String> {
    let Some(parent) = path.parent() else { return Ok(None); };
    let Some(requested) = path.file_name().and_then(|name| name.to_str()) else { return Ok(None); };
    let names = match directory_names(parent) {
        Ok(names) => names,
        Err(_) => return Ok(None),
    };
    let equivalent = names.iter().filter(|name| nfc(name) == nfc(requested)).cloned().collect::<Vec<_>>();
    let Some(name) = names.iter().find(|name| name.as_str() == requested).or_else(|| if equivalent.len() == 1 { equivalent.first() } else { None }) else {
        return Ok(None);
    };
    let actual = parent.join(name);
    Ok(file_identity(&actual)?.map(|identity| (actual, identity)))
}

fn directory_names(path: &Path) -> Result<Vec<String>, String> {
    let mut names = Vec::new();
    for entry in fs::read_dir(path).map_err(|error| error.to_string())? {
        names.push(entry.map_err(|error| error.to_string())?.file_name().to_string_lossy().into_owned());
    }
    Ok(names)
}

fn normalize_options(value: &Value) -> Result<Options, String> {
    let source = value.as_object().ok_or_else(|| coded("options"))?;
    for key in ["find", "replacement", "prefix", "suffix", "separator"] {
        let text = source.get(key).and_then(Value::as_str).ok_or_else(|| coded("options"))?;
        if text.chars().count() > 120 {
            return fail("options");
        }
    }
    for key in ["caseSensitive", "preserveExtension", "numbering"] {
        if !source.get(key).is_some_and(Value::is_boolean) {
            return fail("options");
        }
    }
    let start = source.get("start").and_then(Value::as_i64).ok_or_else(|| coded("options"))?;
    let width = source.get("width").and_then(Value::as_i64).ok_or_else(|| coded("options"))?;
    if !(0..=1_000_000).contains(&start) || !(1..=8).contains(&width) {
        return fail("options");
    }
    Ok(Options {
        find: source["find"].as_str().unwrap().into(),
        replacement: source["replacement"].as_str().unwrap().into(),
        prefix: source["prefix"].as_str().unwrap().into(),
        suffix: source["suffix"].as_str().unwrap().into(),
        case_sensitive: source["caseSensitive"].as_bool().unwrap(),
        preserve_extension: source["preserveExtension"].as_bool().unwrap(),
        numbering: source["numbering"].as_bool().unwrap(),
        start,
        width: width as usize,
        separator: source["separator"].as_str().unwrap().into(),
    })
}

fn rename_candidate(name: &str, options: &Options, index: usize) -> String {
    let dot = name.rfind('.').unwrap_or(name.len());
    let split = if options.preserve_extension && dot > 0 { dot } else { name.len() };
    let (stem, extension) = name.split_at(split);
    let stem = if options.find.is_empty() { stem.to_string() } else { replace_literal(stem, &options.find, &options.replacement, options.case_sensitive) };
    let number = if options.numbering { format!("{}{:0width$}", options.separator, options.start + index as i64, width = options.width) } else { String::new() };
    format!("{}{}{}{}{}", options.prefix, stem, options.suffix, number, extension)
}

fn replace_literal(stem: &str, find: &str, replacement: &str, case_sensitive: bool) -> String {
    let source: Vec<char> = stem.chars().collect();
    let needle: Vec<char> = find.chars().collect();
    let mut result = String::new();
    let mut index = 0;
    while index < source.len() {
        let matches = index + needle.len() <= source.len() && source[index..index + needle.len()].iter().zip(&needle).all(|(left, right)| same_char(*left, *right, case_sensitive));
        if matches {
            result.push_str(replacement);
            index += needle.len();
        } else {
            result.push(source[index]);
            index += 1;
        }
    }
    result
}

fn same_char(left: char, right: char, case_sensitive: bool) -> bool {
    if case_sensitive { left == right } else { left.to_lowercase().eq(right.to_lowercase()) }
}

fn valid_rename_name(name: &str) -> bool {
    !name.is_empty() && name != "." && name != ".."
        && !name.chars().any(|character| matches!(character, '<' | '>' | ':' | '"' | '/' | '\\' | '|' | '?' | '*') || (character as u32) < 0x20)
        && !name.ends_with(['.', ' '])
        && !reserved_windows_name(name)
        && name.len() <= 255
}

fn reserved_windows_name(name: &str) -> bool {
    let stem = name.split('.').next().unwrap_or(name);
    let upper = stem.to_ascii_uppercase();
    matches!(upper.as_str(), "CON" | "PRN" | "AUX" | "NUL")
        || ((upper.starts_with("COM") || upper.starts_with("LPT")) && upper.len() == 4 && upper.as_bytes()[3].is_ascii_digit() && upper.as_bytes()[3] != b'0')
}

fn rename_name_key(name: &str) -> String {
    nfc(name).to_lowercase()
}

fn nfc(value: &str) -> String {
    value.chars().collect()
}

fn valid_temporary_name(name: &str) -> bool {
    let Some(rest) = name.strip_prefix(".mootool-rename-") else { return false; };
    rest.len() == 36 && rest.chars().all(|character| character.is_ascii_hexdigit() || character == '-')
}

fn uuid() -> String {
    let mut bytes = [0u8; 16];
    if let Ok(mut source) = fs::File::open("/dev/urandom") {
        use std::io::Read;
        let _ = source.read_exact(&mut bytes);
    } else {
        let nanos = SystemTime::now().duration_since(UNIX_EPOCH).map(|time| time.as_nanos()).unwrap_or(0);
        bytes.copy_from_slice(&nanos.to_le_bytes());
    }
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    format!(
        "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]
    )
}

fn required_string(value: Option<&Value>) -> Result<String, String> {
    value.and_then(Value::as_str).filter(|value| !value.is_empty()).map(str::to_string).ok_or_else(|| coded("journal"))
}

fn digit_string(value: Option<&Value>) -> Result<String, String> {
    let text = value.and_then(Value::as_str).ok_or_else(|| coded("journal"))?;
    if text.is_empty() || !text.chars().all(|character| character.is_ascii_digit()) {
        return fail("journal");
    }
    Ok(text.into())
}

fn unsupported_io(error: &std::io::Error) -> bool {
    error.kind() == ErrorKind::Unsupported || matches!(error.raw_os_error(), Some(1 | 18 | 45 | 95 | 102))
}

fn coded(code: &str) -> String {
    format!("batchRename.error.{code}")
}

fn fail<T>(code: &str) -> Result<T, String> {
    Err(coded(code))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    fn fixture() -> (tempfile::TempDir, BatchRenameService) {
        let directory = tempfile::tempdir().expect("temp directory");
        let service = BatchRenameService::new(directory.path().join("state"));
        (directory, service)
    }

    fn write_file(directory: &Path, name: &str, content: &str) -> String {
        let path = directory.join(name);
        let mut file = fs::File::create(&path).expect("file");
        file.write_all(content.as_bytes()).expect("content");
        fs::canonicalize(&path).expect("canonical").to_string_lossy().into_owned()
    }

    #[test]
    fn previews_without_writing_and_undoes_after_restart() {
        let (directory, mut service) = fixture();
        let first = write_file(directory.path(), "a.txt", "alpha");
        let second = write_file(directory.path(), "b.txt", "beta");
        let before = fs::metadata(&first).expect("metadata");
        let files = service.select(1, &[first.clone(), second.clone()]).expect("select");
        let preview = service.preview(1, &serde_json::json!(files.iter().map(|file| &file.id).collect::<Vec<_>>()), &serde_json::json!({
            "find": "", "replacement": "", "prefix": "new_", "suffix": "", "caseSensitive": true,
            "preserveExtension": true, "numbering": true, "start": 1, "width": 3, "separator": "_"
        })).expect("preview");
        assert_eq!(preview.rows.iter().map(|row| row.new_name.as_str()).collect::<Vec<_>>(), ["new_a_001.txt", "new_b_002.txt"]);
        assert!(directory.path().join("a.txt").is_file());
        let result = service.execute(1, &Value::String(preview.token.expect("token"))).expect("execute");
        assert_eq!(result.changed_count, 2);
        assert_eq!(fs::read_to_string(directory.path().join("new_a_001.txt")).expect("renamed"), "alpha");
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            assert_eq!(fs::metadata(directory.path().join("new_a_001.txt")).expect("renamed metadata").ino(), before.ino());
        }
        let mut restarted = BatchRenameService::new(directory.path().join("state"));
        assert!(restarted.status().expect("status").undo_available);
        restarted.undo(9).expect("undo");
        assert_eq!(fs::read_to_string(&first).expect("restored"), "alpha");
        assert_eq!(fs::read_to_string(&second).expect("restored"), "beta");
        assert!(!restarted.status().expect("status").undo_available);
    }

    #[test]
    fn blocks_duplicate_occupied_invalid_and_same_file_names() {
        let (directory, mut service) = fixture();
        let a = write_file(directory.path(), "a.txt", "a");
        let b = write_file(directory.path(), "b.txt", "b");
        let files = service.select(1, &[a.clone(), b]).expect("select");
        let duplicate = service.preview(1, &serde_json::json!([files[0].id, files[1].id]), &options("a", "b", "")).expect("preview");
        assert!(duplicate.token.is_none());
        assert_eq!(duplicate.rows[0].issue.as_deref(), Some("occupied"));
        let invalid = service.preview(1, &serde_json::json!([files[0].id]), &options("", "", "../")).expect("preview");
        assert_eq!(invalid.rows[0].issue.as_deref(), Some("invalidName"));
        assert_eq!(service.preview(1, &serde_json::json!([files[0].id]), &options("", "", "")).expect("unchanged").rows[0].status, "unchanged");
        fs::hard_link(&a, directory.path().join("alias.txt")).expect("link");
        let linked = service.select(1, &[a, directory.path().join("alias.txt").to_string_lossy().into_owned()]).expect("linked");
        let same = service.preview(1, &serde_json::json!(linked.iter().map(|file| &file.id).collect::<Vec<_>>()), &options("", "", "new_")).expect("same");
        assert!(same.rows.iter().all(|row| row.issue.as_deref() == Some("sameFile")));
    }

    #[test]
    fn refuses_undo_when_the_original_name_is_occupied() {
        let (directory, mut service) = fixture();
        let original = write_file(directory.path(), "a.txt", "original");
        let files = service.select(1, &[original.clone()]).expect("select");
        let preview = service.preview(1, &serde_json::json!([files[0].id]), &options("", "", "new_")).expect("preview");
        service.execute(1, &Value::String(preview.token.expect("token"))).expect("execute");
        write_file(directory.path(), "a.txt", "foreign");
        assert!(service.undo(1).unwrap_err().contains("occupied"));
        assert_eq!(fs::read_to_string(&original).expect("foreign"), "foreign");
        assert_eq!(fs::read_to_string(directory.path().join("new_a.txt")).expect("renamed"), "original");
        assert!(service.status().expect("status").undo_available);
    }

    fn options(find: &str, replacement: &str, prefix: &str) -> Value {
        serde_json::json!({
            "find": find, "replacement": replacement, "prefix": prefix, "suffix": "", "caseSensitive": true,
            "preserveExtension": true, "numbering": false, "start": 1, "width": 3, "separator": "_"
        })
    }
}
