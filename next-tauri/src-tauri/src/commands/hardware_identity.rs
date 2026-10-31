use std::{fs, path::Path, process::Command};

#[derive(Clone, Debug, PartialEq)]
pub struct MachineIdentity {
    pub manufacturer: String,
    pub model: String,
    pub serial: String,
    pub time_zone: String,
    pub performance_cores: Option<u32>,
    pub efficiency_cores: Option<u32>,
    pub active_memory_bytes: Option<u64>,
}

pub fn machine_identity() -> MachineIdentity {
    let platform = read_platform();
    MachineIdentity {
        manufacturer: platform.manufacturer,
        model: platform.model,
        serial: mask_serial(&platform.serial),
        time_zone: time_zone_name(),
        performance_cores: platform.performance_cores,
        efficiency_cores: platform.efficiency_cores,
        active_memory_bytes: active_memory_bytes(),
    }
}

struct PlatformHardware {
    manufacturer: String,
    model: String,
    serial: String,
    performance_cores: Option<u32>,
    efficiency_cores: Option<u32>,
}

fn read_platform() -> PlatformHardware {
    #[cfg(target_os = "macos")]
    {
        let model = command_text("sysctl", &["-n", "hw.model"]).unwrap_or_default();
        let ioreg = command_text("ioreg", &["-rd1", "-c", "IOPlatformExpertDevice"]).unwrap_or_default();
        return PlatformHardware {
            manufacturer: if model.is_empty() { String::new() } else { "Apple Inc.".into() },
            model,
            serial: ioreg_string(&ioreg, "IOPlatformSerialNumber"),
            performance_cores: command_u32("sysctl", &["-n", "hw.perflevel0.logicalcpu"]),
            efficiency_cores: command_u32("sysctl", &["-n", "hw.perflevel1.logicalcpu"]),
        };
    }
    #[cfg(target_os = "linux")]
    {
        return PlatformHardware {
            manufacturer: dmi("sys_vendor"),
            model: dmi("product_name"),
            serial: dmi("product_serial"),
            performance_cores: None,
            efficiency_cores: None,
        };
    }
    #[cfg(target_os = "windows")]
    {
        return PlatformHardware {
            manufacturer: registry_value("SystemManufacturer"),
            model: registry_value("SystemProductName"),
            serial: registry_value("SystemSerialNumber"),
            performance_cores: None,
            efficiency_cores: None,
        };
    }
    #[cfg(not(any(target_os = "macos", target_os = "linux", target_os = "windows")))]
    {
        PlatformHardware {
            manufacturer: String::new(),
            model: String::new(),
            serial: String::new(),
            performance_cores: None,
            efficiency_cores: None,
        }
    }
}

pub fn mask_serial(value: &str) -> String {
    let value = value.trim();
    if value.is_empty() || value.eq_ignore_ascii_case("unknown") || value.eq_ignore_ascii_case("none") || value.eq_ignore_ascii_case("default string") {
        return String::new();
    }
    let chars = value.chars().collect::<Vec<_>>();
    if chars.len() <= 4 {
        return value.to_string();
    }
    format!("{}****{}", chars[..2].iter().collect::<String>(), chars[chars.len() - 2..].iter().collect::<String>())
}

pub fn ioreg_string(text: &str, key: &str) -> String {
    let marker = format!("\"{key}\"");
    for line in text.lines() {
        let Some(index) = line.find(&marker) else { continue };
        let Some(value) = line[index + marker.len()..].split('=').nth(1) else { continue };
        return value.trim().trim_matches('"').trim().to_string();
    }
    String::new()
}

fn time_zone_name() -> String {
    let link = fs::read_link("/etc/localtime").ok();
    if let Some(name) = link.as_ref().and_then(|path| zoneinfo_name(path)) {
        return name;
    }
    time::OffsetDateTime::now_local()
        .ok()
        .map(|moment| {
            let offset = moment.offset();
            let hours = offset.whole_hours();
            let minutes = offset.minutes_past_hour().unsigned_abs();
            format!("UTC{hours:+03}:{minutes:02}")
        })
        .unwrap_or_default()
}

fn zoneinfo_name(path: &Path) -> Option<String> {
    let text = path.to_string_lossy();
    let marker = text.find("zoneinfo/")?;
    let name = text[marker + "zoneinfo/".len()..].trim().to_string();
    if name.is_empty() { None } else { Some(name) }
}

fn active_memory_bytes() -> Option<u64> {
    #[cfg(target_os = "macos")]
    { macos_active_memory() }
    #[cfg(target_os = "linux")]
    { linux_active_memory() }
    #[cfg(not(any(target_os = "macos", target_os = "linux")))]
    { None }
}

#[cfg(target_os = "macos")]
fn macos_active_memory() -> Option<u64> {
    let pages = vm_stat_pages("Pages active")?;
    let page_size = command_u32("sysctl", &["-n", "hw.pagesize"])?;
    Some(pages.saturating_mul(u64::from(page_size)))
}

#[cfg(target_os = "linux")]
fn linux_active_memory() -> Option<u64> {
    let text = fs::read_to_string("/proc/meminfo").ok()?;
    for line in text.lines() {
        let Some(value) = line.strip_prefix("Active:") else { continue };
        let kb = value.split_whitespace().next()?.parse::<u64>().ok()?;
        return Some(kb.saturating_mul(1024));
    }
    None
}

#[cfg(target_os = "macos")]
fn vm_stat_pages(label: &str) -> Option<u64> {
    let text = command_text("vm_stat", &[])?;
    for line in text.lines() {
        let Some(value) = line.trim().strip_prefix(label) else { continue };
        let digits = value.trim().trim_start_matches(':').trim().trim_end_matches('.').trim();
        return digits.parse().ok();
    }
    None
}

#[cfg(target_os = "linux")]
fn dmi(name: &str) -> String {
    fs::read_to_string(Path::new("/sys/class/dmi/id").join(name))
        .map(|value| value.trim().to_string())
        .unwrap_or_default()
}

#[cfg(target_os = "windows")]
fn registry_value(name: &str) -> String {
    let Some(text) = command_text("reg", &["query", r"HKLM\HARDWARE\DESCRIPTION\System\BIOS", "/v", name]) else {
        return String::new();
    };
    text.lines().find_map(|line| {
        let mut parts = line.split_whitespace();
        if parts.next()? != name { return None; }
        parts.nth(1).map(ToString::to_string)
    }).unwrap_or_default()
}

fn command_text(program: &str, args: &[&str]) -> Option<String> {
    let output = Command::new(program)
        .args(args)
        .stdin(std::process::Stdio::null())
        .output()
        .ok()?;
    if !output.status.success() {
        return None;
    }
    let text = String::from_utf8_lossy(&output.stdout).trim().to_string();
    if text.is_empty() { None } else { Some(text) }
}

fn command_u32(program: &str, args: &[&str]) -> Option<u32> {
    command_text(program, args)?.parse().ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn masks_serials_like_electron() {
        assert_eq!(mask_serial(""), "");
        assert_eq!(mask_serial("unknown"), "");
        assert_eq!(mask_serial("None"), "");
        assert_eq!(mask_serial("AB"), "AB");
        assert_eq!(mask_serial("FVFH1234Q6L"), "FV****6L");
    }

    #[test]
    fn reads_ioreg_serial_assignment() {
        let text = "\"IOPlatformSerialNumber\" = \"FVFH1234Q6L\"\n";
        assert_eq!(ioreg_string(text, "IOPlatformSerialNumber"), "FVFH1234Q6L");
    }

    #[test]
    fn reads_the_local_machine_without_a_full_serial() {
        let identity = machine_identity();
        assert!(identity.time_zone.contains('/') || identity.time_zone.starts_with("UTC"), "{}", identity.time_zone);
        if !identity.serial.is_empty() {
            assert!(identity.serial.len() <= 4 || identity.serial.contains("****"), "{}", identity.serial);
        }
        #[cfg(target_os = "macos")]
        assert!(!identity.model.is_empty(), "macOS model should come from hw.model");
    }

    #[test]
    fn reads_a_zoneinfo_name() {
        assert_eq!(zoneinfo_name(Path::new("/var/db/timezone/zoneinfo/Asia/Shanghai")).as_deref(), Some("Asia/Shanghai"));
    }
}
