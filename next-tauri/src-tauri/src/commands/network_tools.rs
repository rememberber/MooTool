use std::{
    collections::HashMap,
    net::{IpAddr, Ipv4Addr, SocketAddr},
    process::Stdio,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, AtomicUsize, Ordering},
    },
    time::Instant,
};

use futures_util::{StreamExt, stream};
use serde::Serialize;
use sysinfo::Networks;
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    net::TcpStream,
    process::Command,
    time::{Duration, timeout},
};

use crate::contracts::error::AppResult;
use crate::repositories::settings::SettingsRepository;

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkInterfaceInfo {
    name: String,
    addresses: Vec<String>,
    mac_address: String,
    mtu: u64,
    received_bytes: u64,
    transmitted_bytes: u64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PortScanResult {
    host: String,
    resolved_address: String,
    start_port: u16,
    end_port: u16,
    open_ports: Vec<u16>,
    duration_ms: u128,
    cancelled: bool,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkHostProbe {
    address: String,
    open_ports: Vec<u16>,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkRangeScanResult {
    cidr: String,
    scanned_hosts: usize,
    reachable_hosts: Vec<NetworkHostProbe>,
    duration_ms: u128,
    cancelled: bool,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WhoisResult {
    query: String,
    server: String,
    output: String,
    duration_ms: u128,
}

#[derive(Default)]
pub struct NetworkTaskManager {
    tasks: Mutex<HashMap<String, Arc<AtomicBool>>>,
}

impl NetworkTaskManager {
    fn begin(&self, request_id: &str) -> Result<Arc<AtomicBool>, String> {
        validate_request_id(request_id)?;
        let mut tasks = self
            .tasks
            .lock()
            .map_err(|_| "network task manager state poisoned".to_string())?;
        if tasks.contains_key(request_id) {
            return Err("network task request ID is already active".into());
        }
        let cancelled = Arc::new(AtomicBool::new(false));
        tasks.insert(request_id.to_string(), cancelled.clone());
        Ok(cancelled)
    }

    fn finish(&self, request_id: &str) {
        if let Ok(mut tasks) = self.tasks.lock() {
            tasks.remove(request_id);
        }
    }

    fn cancel(&self, request_id: &str) -> Result<bool, String> {
        validate_request_id(request_id)?;
        let tasks = self
            .tasks
            .lock()
            .map_err(|_| "network task manager state poisoned".to_string())?;
        Ok(tasks.get(request_id).is_some_and(|flag| {
            flag.store(true, Ordering::Relaxed);
            true
        }))
    }
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRangeResult {
    prefix: String,
    scanned_hosts: usize,
    reachable_hosts: Vec<String>,
    duration_ms: u128,
    cancelled: bool,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    host: String,
    success: bool,
    output: String,
    duration_ms: u128,
}

#[tauri::command]
pub fn list_network_interfaces() -> Vec<NetworkInterfaceInfo> {
    let networks = Networks::new_with_refreshed_list();
    let mut output = networks
        .iter()
        .map(|(name, network)| NetworkInterfaceInfo {
            name: name.clone(),
            addresses: network
                .ip_networks()
                .iter()
                .map(|network| interface_address(&network.to_string()))
                .collect(),
            mac_address: network.mac_address().to_string(),
            mtu: network.mtu(),
            received_bytes: network.total_received(),
            transmitted_bytes: network.total_transmitted(),
        })
        .collect::<Vec<_>>();
    output.sort_by(|left, right| left.name.cmp(&right.name));
    output
}

#[tauri::command]
pub async fn read_network_interface_report(
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<String> {
    let timeout_ms = clamp_network_timeout(settings.snapshot().network.request_timeout_ms);
    let (program, arguments) = interface_command();
    command_output(program, arguments, timeout_ms, "ifconfig")
        .await
        .map_err(Into::into)
}

#[tauri::command]
pub async fn resolve_network_host(host: String) -> AppResult<Vec<String>> {
    validate_host(&host)?;
    let addresses = tokio::net::lookup_host((host.as_str(), 0))
        .await
        .map_err(|error| error.to_string())?
        .map(|address| format_resolved_address(address.ip()))
        .collect::<Vec<_>>();
    Ok(addresses)
}

#[tauri::command]
pub async fn scan_tcp_ports(
    manager: tauri::State<'_, NetworkTaskManager>,
    request_id: String,
    host: String,
    ports: Vec<u16>,
    timeout_ms: u64,
    budget_ms: u64,
) -> AppResult<PortScanResult> {
    validate_host(&host)?;
    let ports = normalize_ports(ports)?;
    if !(100..=5_000).contains(&timeout_ms) {
        return Err("port timeout must be between 100 and 5000 milliseconds".into());
    }
    if !(1_000..=120_000).contains(&budget_ms) {
        return Err("network command timeout must be between 1 and 120 seconds".into());
    }
    let cancelled = manager.begin(&request_id)?;
    let started = Instant::now();
    let open_ports = match collect_open_ports(
        &host,
        &ports,
        Duration::from_millis(timeout_ms),
        started + Duration::from_millis(budget_ms),
        &cancelled,
    )
    .await
    {
        Ok(open_ports) => open_ports,
        Err(error) => {
            manager.finish(&request_id);
            return Err(error.into());
        }
    };
    manager.finish(&request_id);
    Ok(PortScanResult {
        resolved_address: host.clone(),
        host,
        start_port: ports[0],
        end_port: *ports.last().unwrap_or(&ports[0]),
        open_ports,
        duration_ms: started.elapsed().as_millis(),
        cancelled: false,
    })
}

#[tauri::command]
pub async fn scan_ipv4_range(
    manager: tauri::State<'_, NetworkTaskManager>,
    request_id: String,
    cidr: String,
    ports: Vec<u16>,
    timeout_ms: u64,
) -> AppResult<NetworkRangeScanResult> {
    let (network, prefix) = parse_ipv4_cidr(&cidr)?;
    if prefix < 24 {
        return Err("network range scan is limited to 256 IPv4 addresses (/24 or smaller)".into());
    }
    if ports.is_empty() || ports.len() > 8 || ports.contains(&0) {
        return Err("network range scan requires 1 to 8 valid probe ports".into());
    }
    if !(100..=5_000).contains(&timeout_ms) {
        return Err("network range timeout must be between 100 and 5000 milliseconds".into());
    }
    let cancelled = manager.begin(&request_id)?;
    let started = Instant::now();
    let mask = if prefix == 0 {
        0
    } else {
        u32::MAX << (32 - prefix)
    };
    let first = u32::from(network) & mask;
    let last = first | !mask;
    let addresses = if prefix <= 30 && last > first + 1 {
        (first + 1..last).collect::<Vec<_>>()
    } else {
        (first..=last).collect::<Vec<_>>()
    };
    let scanned_hosts = addresses.len();
    let task_flag = cancelled.clone();
    let probe_ports = Arc::new(ports);
    let mut reachable_hosts = stream::iter(addresses)
        .map(move |raw| {
            let task_flag = task_flag.clone();
            let probe_ports = probe_ports.clone();
            async move {
                if task_flag.load(Ordering::Relaxed) {
                    return None;
                }
                let address = Ipv4Addr::from(raw);
                let mut open_ports = Vec::new();
                for port in probe_ports.iter().copied() {
                    if task_flag.load(Ordering::Relaxed) {
                        break;
                    }
                    if timeout(
                        Duration::from_millis(timeout_ms),
                        TcpStream::connect(SocketAddr::new(address.into(), port)),
                    )
                    .await
                    .is_ok_and(|result| result.is_ok())
                    {
                        open_ports.push(port);
                    }
                }
                (!open_ports.is_empty()).then(|| NetworkHostProbe {
                    address: address.to_string(),
                    open_ports,
                })
            }
        })
        .buffer_unordered(32)
        .filter_map(|host| async move { host })
        .collect::<Vec<_>>()
        .await;
    reachable_hosts.sort_by(|left, right| {
        left.address
            .parse::<Ipv4Addr>()
            .map(u32::from)
            .unwrap_or_default()
            .cmp(
                &right
                    .address
                    .parse::<Ipv4Addr>()
                    .map(u32::from)
                    .unwrap_or_default(),
            )
    });
    let was_cancelled = cancelled.load(Ordering::Relaxed);
    manager.finish(&request_id);
    Ok(NetworkRangeScanResult {
        cidr,
        scanned_hosts,
        reachable_hosts,
        duration_ms: started.elapsed().as_millis(),
        cancelled: was_cancelled,
    })
}

#[tauri::command]
pub fn cancel_network_task(
    manager: tauri::State<'_, NetworkTaskManager>,
    request_id: String,
) -> AppResult<bool> {
    Ok(manager.cancel(&request_id)?)
}

#[tauri::command]
pub async fn query_network_whois(
    settings: tauri::State<'_, SettingsRepository>,
    query: String,
) -> AppResult<WhoisResult> {
    let query = normalize_whois_query(&query)?;
    let timeout_ms = clamp_network_timeout(settings.snapshot().network.request_timeout_ms);
    let started = Instant::now();
    let initial = query_whois_server("whois.iana.org", &query, timeout_ms).await?;
    let server = whois_referral(&initial).unwrap_or_else(|| "whois.iana.org".to_string());
    let output = if server == "whois.iana.org" {
        initial
    } else {
        validate_host(&server)?;
        query_whois_server(&server, &query, timeout_ms).await?
    };
    Ok(WhoisResult {
        query,
        server,
        output,
        duration_ms: started.elapsed().as_millis(),
    })
}

#[tauri::command]
pub async fn list_network_connections(
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<String> {
    let timeout_ms = clamp_network_timeout(settings.snapshot().network.request_timeout_ms);
    let (program, arguments) = connection_command();
    let output = timeout(
        Duration::from_millis(timeout_ms),
        Command::new(program)
            .args(arguments)
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .output(),
    )
    .await
    .map_err(|_| "TIMEOUT".to_string())?
    .map_err(|error| format!("failed to list network connections: {error}"))?;
    let stdout = String::from_utf8_lossy(&output.stdout);
    let stderr = String::from_utf8_lossy(&output.stderr);
    if stdout.len() > 2 * 1024 * 1024 || stderr.len() > 2 * 1024 * 1024 {
        return Err("Command output exceeds 2 MB".into());
    }
    if !output.status.success() {
        let message = stderr.trim();
        return Err(if message.is_empty() {
            format!(
                "netstat exited with code {}",
                output.status.code().unwrap_or(-1)
            )
        } else {
            message.to_string()
        }
        .into());
    }
    let text = if stdout.trim().is_empty() {
        stderr.trim()
    } else {
        stdout.trim()
    };
    Ok(text.to_string())
}

#[tauri::command]
pub async fn flush_network_dns_cache(
    settings: tauri::State<'_, SettingsRepository>,
) -> AppResult<String> {
    let timeout_ms = clamp_network_timeout(settings.snapshot().network.request_timeout_ms);
    let (program, arguments): (&str, &[&str]) = if cfg!(target_os = "windows") {
        ("ipconfig.exe", &["/flushdns"])
    } else if cfg!(target_os = "macos") {
        ("/usr/bin/dscacheutil", &["-flushcache"])
    } else {
        ("resolvectl", &["flush-caches"])
    };
    command_output(program, arguments, timeout_ms, "flush-dns")
        .await
        .map_err(Into::into)
}

#[tauri::command]
pub async fn ping_network_host(
    settings: tauri::State<'_, SettingsRepository>,
    host: String,
) -> AppResult<PingResult> {
    validate_host(&host)?;
    let timeout_ms = clamp_network_timeout(settings.snapshot().network.request_timeout_ms);
    let started = Instant::now();
    let (program, arguments) = ping_command(&host);
    let mut command = Command::new(program);
    command
        .args(arguments)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);
    let output = timeout(Duration::from_millis(timeout_ms), command.output())
        .await
        .map_err(|_| "TIMEOUT".to_string())?
        .map_err(|error| format!("failed to start ping: {error}"))?;
    let stdout = String::from_utf8_lossy(&output.stdout);
    let stderr = String::from_utf8_lossy(&output.stderr);
    if stdout.len() > 2 * 1024 * 1024 || stderr.len() > 2 * 1024 * 1024 {
        return Err("Command output exceeds 2 MB".into());
    }
    if !output.status.success() {
        let message = stderr.trim();
        return Err(if message.is_empty() {
            format!(
                "ping exited with code {}",
                output.status.code().unwrap_or(-1)
            )
        } else {
            message.to_string()
        }
        .into());
    }
    let text = if stdout.trim().is_empty() {
        stderr.trim()
    } else {
        stdout.trim()
    };
    Ok(PingResult {
        host,
        success: true,
        output: text.to_string(),
        duration_ms: started.elapsed().as_millis(),
    })
}

#[tauri::command]
pub async fn ping_ipv4_prefix(
    manager: tauri::State<'_, NetworkTaskManager>,
    request_id: String,
    prefix: String,
    timeout_ms: u64,
) -> AppResult<PingRangeResult> {
    let addresses = ipv4_prefix_hosts(&prefix)?;
    if !(1_000..=120_000).contains(&timeout_ms) {
        return Err("network command timeout must be between 1 and 120 seconds".into());
    }
    let cancelled = manager.begin(&request_id)?;
    let started = Instant::now();
    let probe_timeout = timeout_ms.min(800);
    let reachable = match ping_addresses(
        &addresses,
        probe_timeout,
        started + Duration::from_millis(timeout_ms),
        &cancelled,
    )
    .await
    {
        Ok(reachable) => reachable,
        Err(error) => {
            manager.finish(&request_id);
            return Err(error.into());
        }
    };
    let scanned_hosts = addresses.len();
    manager.finish(&request_id);
    Ok(PingRangeResult {
        prefix: prefix.trim().to_string(),
        scanned_hosts,
        reachable_hosts: reachable,
        duration_ms: started.elapsed().as_millis(),
        cancelled: false,
    })
}

fn validate_host(value: &str) -> Result<(), String> {
    let host = value.trim();
    if host.is_empty()
        || host.len() > 253
        || host.starts_with('-')
        || host.chars().any(|character| {
            !(character.is_ascii_alphanumeric() || matches!(character, '.' | '-' | ':' | '_'))
        })
    {
        return Err("INVALID_TARGET".into());
    }
    Ok(())
}

fn validate_request_id(value: &str) -> Result<(), String> {
    if value.is_empty()
        || value.len() > 128
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'_'))
    {
        return Err("invalid network task request ID".into());
    }
    Ok(())
}

fn parse_ipv4_cidr(value: &str) -> Result<(Ipv4Addr, u32), String> {
    let (address, prefix) = value
        .trim()
        .split_once('/')
        .ok_or_else(|| "IPv4 range must use CIDR notation".to_string())?;
    let address = address
        .parse::<Ipv4Addr>()
        .map_err(|_| "invalid IPv4 range address".to_string())?;
    let prefix = prefix
        .parse::<u32>()
        .map_err(|_| "invalid IPv4 range prefix".to_string())?;
    if prefix > 32 {
        return Err("invalid IPv4 range prefix".into());
    }
    Ok((address, prefix))
}

fn interface_address(value: &str) -> String {
    value.split('/').next().unwrap_or(value).to_string()
}

fn interface_command() -> (&'static str, &'static [&'static str]) {
    if cfg!(windows) {
        ("ipconfig.exe", &["/all"])
    } else if cfg!(target_os = "macos") {
        ("/sbin/ifconfig", &[])
    } else {
        ("ip", &["-details", "address"])
    }
}

async fn command_output(
    program: &str,
    arguments: &[&str],
    timeout_ms: u64,
    name: &str,
) -> Result<String, String> {
    let output = timeout(
        Duration::from_millis(timeout_ms),
        Command::new(program)
            .args(arguments)
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .output(),
    )
    .await
    .map_err(|_| "TIMEOUT".to_string())?
    .map_err(|error| format!("failed to start {name}: {error}"))?;
    let stdout = String::from_utf8_lossy(&output.stdout);
    let stderr = String::from_utf8_lossy(&output.stderr);
    if stdout.len() > 2 * 1024 * 1024 || stderr.len() > 2 * 1024 * 1024 {
        return Err("Command output exceeds 2 MB".into());
    }
    if !output.status.success() {
        let message = stderr.trim();
        return Err(if message.is_empty() {
            format!(
                "{name} exited with code {}",
                output.status.code().unwrap_or(-1)
            )
        } else {
            message.to_string()
        });
    }
    Ok((if stdout.trim().is_empty() {
        stderr.trim()
    } else {
        stdout.trim()
    })
    .to_string())
}

fn format_resolved_address(ip: IpAddr) -> String {
    let family = if ip.is_ipv4() { "IPv4" } else { "IPv6" };
    format!("{ip}\t{family}")
}

fn connection_command() -> (&'static str, &'static [&'static str]) {
    if cfg!(windows) {
        ("netstat.exe", &["-ano"])
    } else {
        ("netstat", &["-nat"])
    }
}

fn normalize_whois_query(value: &str) -> Result<String, String> {
    let target = value.trim().to_ascii_lowercase();
    if target.is_empty()
        || target.len() > 253
        || !target.bytes().all(|byte| {
            byte.is_ascii_lowercase() || byte.is_ascii_digit() || matches!(byte, b'.' | b'-')
        })
    {
        return Err("INVALID_TARGET".into());
    }
    Ok(target)
}

fn whois_referral(response: &str) -> Option<String> {
    for line in response.lines() {
        let lower = line.to_ascii_lowercase();
        let prefix_len = if lower.starts_with("refer:") {
            "refer:".len()
        } else if lower.starts_with("whois:") {
            "whois:".len()
        } else {
            continue;
        };
        let server = line[prefix_len..].split_whitespace().next().unwrap_or("");
        if server.is_empty() {
            continue;
        }
        if server == "whois.iana.org" {
            return None;
        }
        return Some(server.to_string());
    }
    None
}

fn clamp_network_timeout(value: u32) -> u64 {
    u64::from(value.clamp(1_000, 120_000))
}

fn ping_command(host: &str) -> (&'static str, Vec<String>) {
    if cfg!(windows) {
        ("ping.exe", vec!["-n".into(), "4".into(), host.into()])
    } else {
        ("/sbin/ping", vec!["-c".into(), "4".into(), host.into()])
    }
}

async fn query_whois_server(server: &str, query: &str, timeout_ms: u64) -> Result<String, String> {
    timeout(Duration::from_millis(timeout_ms), async move {
        let mut stream = TcpStream::connect((server, 43))
            .await
            .map_err(|error| format!("failed to connect to WHOIS server {server}: {error}"))?;
        stream
            .write_all(format!("{query}\r\n").as_bytes())
            .await
            .map_err(|error| format!("failed to send WHOIS request: {error}"))?;
        let mut output = Vec::new();
        let mut buffer = [0_u8; 8192];
        loop {
            let read = stream
                .read(&mut buffer)
                .await
                .map_err(|error| format!("failed to read WHOIS response: {error}"))?;
            if read == 0 {
                break;
            }
            if output.len() + read > 2 * 1024 * 1024 {
                return Err("WHOIS response exceeds 2 MB".to_string());
            }
            output.extend_from_slice(&buffer[..read]);
        }
        Ok(String::from_utf8_lossy(&output).trim().to_string())
    })
    .await
    .map_err(|_| "TIMEOUT".to_string())?
}

fn normalize_ports(mut ports: Vec<u16>) -> Result<Vec<u16>, String> {
    ports.sort_unstable();
    ports.dedup();
    if ports.is_empty() || ports.len() > 4_096 || ports.contains(&0) {
        return Err("INVALID_TARGET".into());
    }
    Ok(ports)
}

fn ipv4_prefix_hosts(value: &str) -> Result<Vec<Ipv4Addr>, String> {
    let value = value.trim();
    let core = value.strip_suffix('.').unwrap_or(value);
    if core.is_empty() || core.ends_with('.') || core.contains("..") {
        return Err("INVALID_TARGET".into());
    }
    let mut parts = core.split('.');
    let Some(first) = parts.next() else {
        return Err("INVALID_TARGET".into());
    };
    let Some(second) = parts.next() else {
        return Err("INVALID_TARGET".into());
    };
    let Some(third) = parts.next() else {
        return Err("INVALID_TARGET".into());
    };
    if parts.next().is_some() {
        return Err("INVALID_TARGET".into());
    }
    let octets = [first, second, third]
        .map(parse_octet)
        .into_iter()
        .collect::<Result<Vec<_>, _>>()?;
    Ok((1..=254)
        .map(|host| Ipv4Addr::new(octets[0], octets[1], octets[2], host))
        .collect())
}

fn parse_octet(value: &str) -> Result<u8, String> {
    if value.is_empty() || value.len() > 3 || !value.bytes().all(|byte| byte.is_ascii_digit()) {
        return Err("INVALID_TARGET".into());
    }
    value
        .parse::<u8>()
        .map_err(|_| "INVALID_TARGET".to_string())
}

async fn collect_open_ports(
    host: &str,
    ports: &[u16],
    probe_timeout: Duration,
    deadline: Instant,
    cancelled: &AtomicBool,
) -> Result<Vec<u16>, String> {
    let next = Arc::new(AtomicUsize::new(0));
    let open_ports = Arc::new(Mutex::new(Vec::new()));
    let host = Arc::<str>::from(host);
    let workers = std::cmp::min(96, ports.len());
    let results = futures_util::future::join_all((0..workers).map(|_| {
        let next = next.clone();
        let open_ports = open_ports.clone();
        let host = host.clone();
        async move {
            loop {
                if cancelled.load(Ordering::Relaxed) {
                    return Err("ABORTED".to_string());
                }
                if Instant::now() >= deadline {
                    return Err("TIMEOUT".to_string());
                }
                let index = next.fetch_add(1, Ordering::Relaxed);
                let Some(port) = ports.get(index).copied() else {
                    return Ok(());
                };
                if tcp_open(&host, port, probe_timeout, cancelled).await {
                    open_ports
                        .lock()
                        .map_err(|_| "network task manager state poisoned".to_string())?
                        .push(port);
                }
            }
        }
    }))
    .await;
    if let Some(error) = results.into_iter().find_map(Result::err) {
        return Err(error);
    }
    let mut open_ports = open_ports
        .lock()
        .map_err(|_| "network task manager state poisoned".to_string())?
        .clone();
    open_ports.sort_unstable();
    Ok(open_ports)
}

async fn tcp_open(host: &str, port: u16, probe_timeout: Duration, cancelled: &AtomicBool) -> bool {
    let connect = timeout(probe_timeout, TcpStream::connect((host, port)));
    tokio::pin!(connect);
    loop {
        tokio::select! {
            result = &mut connect => return result.ok().and_then(Result::ok).is_some(),
            _ = tokio::time::sleep(Duration::from_millis(50)) => {
                if cancelled.load(Ordering::Relaxed) {
                    return false;
                }
            }
        }
    }
}

async fn ping_addresses(
    addresses: &[Ipv4Addr],
    probe_timeout: u64,
    deadline: Instant,
    cancelled: &AtomicBool,
) -> Result<Vec<String>, String> {
    let next = Arc::new(AtomicUsize::new(0));
    let reachable = Arc::new(Mutex::new(Vec::new()));
    let workers = std::cmp::min(32, addresses.len());
    let results = futures_util::future::join_all((0..workers).map(|_| {
        let next = next.clone();
        let reachable = reachable.clone();
        async move {
            loop {
                if cancelled.load(Ordering::Relaxed) {
                    return Err("ABORTED".to_string());
                }
                if Instant::now() >= deadline {
                    return Err("TIMEOUT".to_string());
                }
                let index = next.fetch_add(1, Ordering::Relaxed);
                let Some(address) = addresses.get(index).copied() else {
                    return Ok(());
                };
                if ping_once(&address.to_string(), probe_timeout, cancelled).await? {
                    reachable
                        .lock()
                        .map_err(|_| "network task manager state poisoned".to_string())?
                        .push(address);
                }
            }
        }
    }))
    .await;
    if let Some(error) = results.into_iter().find_map(Result::err) {
        return Err(error);
    }
    let mut reachable = reachable
        .lock()
        .map_err(|_| "network task manager state poisoned".to_string())?
        .clone();
    reachable.sort_by_key(|address| u32::from(*address));
    Ok(reachable
        .into_iter()
        .map(|address| address.to_string())
        .collect())
}

async fn ping_once(address: &str, timeout_ms: u64, cancelled: &AtomicBool) -> Result<bool, String> {
    if cancelled.load(Ordering::Relaxed) {
        return Err("ABORTED".into());
    }
    let (program, args) = ping_once_command(address, timeout_ms);
    let mut child = Command::new(program)
        .args(args)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .kill_on_drop(true)
        .spawn()
        .map_err(|error| format!("failed to start ping: {error}"))?;
    let started = Instant::now();
    loop {
        if cancelled.load(Ordering::Relaxed) {
            let _ = child.kill().await;
            return Err("ABORTED".into());
        }
        if started.elapsed() > Duration::from_millis(timeout_ms + 1_000) {
            let _ = child.kill().await;
            return Ok(false);
        }
        match timeout(Duration::from_millis(50), child.wait()).await {
            Ok(Ok(status)) => return Ok(status.success()),
            Ok(Err(error)) => return Err(format!("ping failed: {error}")),
            Err(_) => continue,
        }
    }
}

fn ping_once_command(host: &str, timeout_ms: u64) -> (&'static str, Vec<String>) {
    if cfg!(windows) {
        (
            "ping.exe",
            vec![
                "-n".into(),
                "1".into(),
                "-w".into(),
                timeout_ms.to_string(),
                host.into(),
            ],
        )
    } else if cfg!(target_os = "macos") {
        (
            "/sbin/ping",
            vec![
                "-c".into(),
                "1".into(),
                "-W".into(),
                timeout_ms.to_string(),
                host.into(),
            ],
        )
    } else {
        let seconds = timeout_ms.div_ceil(1_000).max(1);
        (
            "ping",
            vec![
                "-c".into(),
                "1".into(),
                "-W".into(),
                seconds.to_string(),
                host.into(),
            ],
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn follows_whois_referrals_and_ping_arguments_like_electron() {
        assert_eq!(
            normalize_whois_query(" Example.COM ").expect("query"),
            "example.com"
        );
        assert!(normalize_whois_query("bad_host").is_err());
        assert_eq!(
            whois_referral("Refer: whois.verisign-grs.com\n"),
            Some("whois.verisign-grs.com".to_string())
        );
        assert_eq!(
            whois_referral("whois: whois.iana.org\nrefer: whois.example\n"),
            None
        );
        let (program, arguments) = ping_command("127.0.0.1");
        assert_eq!(arguments[0], if cfg!(windows) { "-n" } else { "-c" });
        assert_eq!(arguments[1], "4");
        assert_eq!(arguments.last().map(String::as_str), Some("127.0.0.1"));
        assert_eq!(
            program,
            if cfg!(windows) {
                "ping.exe"
            } else {
                "/sbin/ping"
            }
        );
        assert_eq!(
            format_resolved_address(IpAddr::from([127, 0, 0, 1])),
            "127.0.0.1\tIPv4"
        );
        assert_eq!(
            format_resolved_address(IpAddr::from([0, 0, 0, 0, 0, 0, 0, 1])),
            "::1\tIPv6"
        );
        assert_eq!(
            connection_command().1,
            if cfg!(windows) {
                &["-ano"][..]
            } else {
                &["-nat"][..]
            }
        );
        assert_eq!(interface_address("192.168.1.20/24"), "192.168.1.20");
        assert_eq!(interface_address("fe80::1/64"), "fe80::1");
        assert_eq!(
            interface_command(),
            if cfg!(windows) {
                ("ipconfig.exe", &["/all"][..])
            } else if cfg!(target_os = "macos") {
                ("/sbin/ifconfig", &[][..])
            } else {
                ("ip", &["-details", "address"][..])
            }
        );
    }

    #[test]
    fn rejects_shell_like_hosts() {
        assert!(validate_host("example.com").is_ok());
        assert!(validate_host("127.0.0.1").is_ok());
        assert!(validate_host("example.com; rm -rf /tmp/x").is_err());
        assert!(validate_host("--help").is_err());
    }

    #[test]
    fn validates_cidr_and_cancellable_task_lifecycle() {
        assert_eq!(
            parse_ipv4_cidr("192.168.1.42/24"),
            Ok((Ipv4Addr::new(192, 168, 1, 42), 24))
        );
        assert!(parse_ipv4_cidr("192.168.1.1/33").is_err());
        assert!(parse_ipv4_cidr("not-an-ip/24").is_err());
        let hosts = ipv4_prefix_hosts("192.168.10").expect("prefix");
        assert_eq!(hosts.len(), 254);
        assert_eq!(hosts[0], Ipv4Addr::new(192, 168, 10, 1));
        assert_eq!(
            *hosts.last().expect("last host"),
            Ipv4Addr::new(192, 168, 10, 254)
        );
        assert_eq!(
            ipv4_prefix_hosts("192.168.10.")
                .expect("trailing dot")
                .len(),
            254
        );
        assert!(ipv4_prefix_hosts("192.168.10.1").is_err());
        assert!(ipv4_prefix_hosts("192.168.10..").is_err());
        assert_eq!(
            normalize_ports(vec![3306, 80, 80, 22]).expect("ports"),
            vec![22, 80, 3306]
        );
        assert!(normalize_ports(vec![0]).is_err());
        assert!(
            normalize_ports(
                (1..=4_097)
                    .map(|port| u16::try_from(port).expect("port"))
                    .collect()
            )
            .is_err()
        );

        let manager = NetworkTaskManager::default();
        let flag = manager.begin("scan-1").expect("begin task");
        assert!(manager.begin("scan-1").is_err());
        assert!(manager.cancel("scan-1").expect("cancel task"));
        assert!(flag.load(Ordering::Relaxed));
        manager.finish("scan-1");
        assert!(!manager.cancel("scan-1").expect("finished task"));
    }

    #[tokio::test]
    async fn pings_the_loopback_address_once() {
        let cancelled = AtomicBool::new(false);
        assert!(
            ping_once("127.0.0.1", 800, &cancelled)
                .await
                .expect("ping loopback")
        );
    }
}
