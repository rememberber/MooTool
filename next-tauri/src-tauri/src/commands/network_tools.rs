use std::{
    collections::HashMap,
    net::{Ipv4Addr, SocketAddr},
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

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NetworkConnectionInfo {
    protocol: String,
    local_address: String,
    remote_address: String,
    state: String,
    process: String,
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
                .map(ToString::to_string)
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
pub async fn resolve_network_host(host: String) -> AppResult<Vec<String>> {
    validate_host(&host)?;
    let mut addresses = tokio::net::lookup_host((host.as_str(), 0))
        .await
        .map_err(|error| format!("DNS lookup failed: {error}"))?
        .map(|address| address.ip().to_string())
        .collect::<Vec<_>>();
    addresses.sort();
    addresses.dedup();
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
    let resolved = match tokio::net::lookup_host((host.as_str(), ports[0])).await {
        Ok(mut addresses) => match addresses.next() {
            Some(address) => address.ip(),
            None => {
                manager.finish(&request_id);
                return Err("host lookup returned no address".into());
            }
        },
        Err(error) => {
            manager.finish(&request_id);
            return Err(format!("host lookup failed: {error}").into());
        }
    };
    let started = Instant::now();
    let open_ports = match collect_open_ports(
        resolved,
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
        host,
        resolved_address: resolved.to_string(),
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
pub async fn query_network_whois(query: String) -> AppResult<WhoisResult> {
    validate_host(&query)?;
    let started = Instant::now();
    let mut server = "whois.iana.org".to_string();
    let initial = query_whois_server(&server, &query).await?;
    if let Some(referral) = initial.lines().find_map(|line| {
        line.strip_prefix("refer:")
            .or_else(|| line.strip_prefix("whois:"))
            .map(str::trim)
            .filter(|value| !value.is_empty())
    }) {
        validate_host(referral)?;
        server = referral.to_string();
    }
    let output = if server == "whois.iana.org" {
        initial
    } else {
        query_whois_server(&server, &query).await?
    };
    Ok(WhoisResult {
        query,
        server,
        output,
        duration_ms: started.elapsed().as_millis(),
    })
}

#[tauri::command]
pub async fn list_network_connections() -> AppResult<Vec<NetworkConnectionInfo>> {
    let (program, arguments): (&str, &[&str]) = if cfg!(target_os = "windows") {
        ("netstat.exe", &["-ano"])
    } else if cfg!(target_os = "macos") {
        ("netstat", &["-anv", "-p", "tcp"])
    } else {
        ("ss", &["-tunap"])
    };
    let output = timeout(
        Duration::from_secs(8),
        Command::new(program)
            .args(arguments)
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .output(),
    )
    .await
    .map_err(|_| "connection listing timed out".to_string())?
    .map_err(|error| format!("failed to list network connections: {error}"))?;
    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr)
            .trim()
            .to_string()
            .into());
    }
    Ok(parse_connections(&String::from_utf8_lossy(&output.stdout)))
}

#[tauri::command]
pub async fn flush_network_dns_cache() -> AppResult<String> {
    let (program, arguments): (&str, &[&str]) = if cfg!(target_os = "windows") {
        ("ipconfig.exe", &["/flushdns"])
    } else if cfg!(target_os = "macos") {
        ("dscacheutil", &["-flushcache"])
    } else {
        ("resolvectl", &["flush-caches"])
    };
    let output = timeout(
        Duration::from_secs(10),
        Command::new(program)
            .args(arguments)
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .output(),
    )
    .await
    .map_err(|_| "DNS cache flush timed out".to_string())?
    .map_err(|error| format!("failed to start DNS cache flush: {error}"))?;
    let mut text = String::from_utf8_lossy(&output.stdout).trim().to_string();
    if !output.stderr.is_empty() {
        if !text.is_empty() {
            text.push('\n');
        }
        text.push_str(String::from_utf8_lossy(&output.stderr).trim());
    }
    if !output.status.success() {
        return Err(if text.is_empty() {
            "DNS cache flush requires elevated system permission".into()
        } else {
            text.into()
        });
    }
    Ok(if text.is_empty() {
        "DNS cache flushed".into()
    } else {
        text
    })
}

#[tauri::command]
pub async fn ping_network_host(host: String) -> AppResult<PingResult> {
    validate_host(&host)?;
    let started = Instant::now();
    let mut command = Command::new(if cfg!(windows) { "ping.exe" } else { "ping" });
    if cfg!(windows) {
        command.args(["-n", "4", "-w", "2000", host.as_str()]);
    } else {
        command.args(["-c", "4", host.as_str()]);
    }
    command
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);
    let output = timeout(Duration::from_secs(12), command.output())
        .await
        .map_err(|_| "ping timed out".to_string())?
        .map_err(|error| format!("failed to start ping: {error}"))?;
    let mut text = String::from_utf8_lossy(&output.stdout).into_owned();
    if !output.stderr.is_empty() {
        if !text.is_empty() {
            text.push('\n');
        }
        text.push_str(&String::from_utf8_lossy(&output.stderr));
    }
    if text.len() > 64 * 1024 {
        text.truncate(64 * 1024);
    }
    Ok(PingResult {
        host,
        success: output.status.success(),
        output: text,
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
        return Err("host name or IP address is invalid".into());
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

async fn query_whois_server(server: &str, query: &str) -> Result<String, String> {
    let mut stream = timeout(Duration::from_secs(8), TcpStream::connect((server, 43)))
        .await
        .map_err(|_| format!("WHOIS connection to {server} timed out"))?
        .map_err(|error| format!("failed to connect to WHOIS server {server}: {error}"))?;
    timeout(
        Duration::from_secs(3),
        stream.write_all(format!("{query}\r\n").as_bytes()),
    )
    .await
    .map_err(|_| "WHOIS request timed out".to_string())?
    .map_err(|error| format!("failed to send WHOIS request: {error}"))?;
    let mut output = Vec::new();
    timeout(
        Duration::from_secs(12),
        stream.take(512 * 1024).read_to_end(&mut output),
    )
    .await
    .map_err(|_| "WHOIS response timed out".to_string())?
    .map_err(|error| format!("failed to read WHOIS response: {error}"))?;
    Ok(String::from_utf8_lossy(&output).into_owned())
}

fn parse_connections(output: &str) -> Vec<NetworkConnectionInfo> {
    output
        .lines()
        .filter_map(|line| {
            let columns = line.split_whitespace().collect::<Vec<_>>();
            if columns.len() < 4 {
                return None;
            }
            if cfg!(not(any(target_os = "windows", target_os = "macos")))
                && matches!(columns[0], "tcp" | "udp")
                && columns.len() >= 6
            {
                return Some(NetworkConnectionInfo {
                    protocol: columns[0].to_ascii_uppercase(),
                    state: normalize_connection_state(columns[1]),
                    local_address: columns[4].to_string(),
                    remote_address: columns[5].to_string(),
                    process: columns.get(6).copied().unwrap_or("").to_string(),
                });
            }
            if matches!(
                columns[0].to_ascii_lowercase().as_str(),
                "tcp" | "tcp4" | "tcp6" | "udp" | "udp4" | "udp6"
            ) {
                let protocol = columns[0].to_ascii_uppercase();
                let (local_index, remote_index) = if cfg!(target_os = "windows") {
                    (1, 2)
                } else {
                    (3, 4)
                };
                if columns.len() <= remote_index {
                    return None;
                }
                return Some(NetworkConnectionInfo {
                    protocol,
                    local_address: columns[local_index].to_string(),
                    remote_address: columns[remote_index].to_string(),
                    state: columns
                        .get(remote_index + 1)
                        .copied()
                        .map(normalize_connection_state)
                        .unwrap_or_default(),
                    process: if cfg!(target_os = "macos") {
                        columns.get(10).copied().unwrap_or("")
                    } else {
                        columns.last().copied().unwrap_or("")
                    }
                    .to_string(),
                });
            }
            None
        })
        .take(10_000)
        .collect()
}

fn normalize_connection_state(state: &str) -> String {
    match state.to_ascii_uppercase().as_str() {
        "ESTAB" => "ESTABLISHED".to_string(),
        normalized => normalized.to_string(),
    }
}

fn normalize_ports(mut ports: Vec<u16>) -> Result<Vec<u16>, String> {
    ports.sort_unstable();
    ports.dedup();
    if ports.is_empty() || ports.len() > 4_096 || ports.iter().any(|port| *port == 0) {
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
    let Some(first) = parts.next() else { return Err("INVALID_TARGET".into()); };
    let Some(second) = parts.next() else { return Err("INVALID_TARGET".into()); };
    let Some(third) = parts.next() else { return Err("INVALID_TARGET".into()); };
    if parts.next().is_some() {
        return Err("INVALID_TARGET".into());
    }
    let octets = [first, second, third]
        .map(parse_octet)
        .into_iter()
        .collect::<Result<Vec<_>, _>>()?;
    Ok((1..=254).map(|host| Ipv4Addr::new(octets[0], octets[1], octets[2], host)).collect())
}

fn parse_octet(value: &str) -> Result<u8, String> {
    if value.is_empty() || value.len() > 3 || !value.bytes().all(|byte| byte.is_ascii_digit()) {
        return Err("INVALID_TARGET".into());
    }
    value.parse::<u8>().map_err(|_| "INVALID_TARGET".to_string())
}

async fn collect_open_ports(
    resolved: std::net::IpAddr,
    ports: &[u16],
    probe_timeout: Duration,
    deadline: Instant,
    cancelled: &AtomicBool,
) -> Result<Vec<u16>, String> {
    let next = Arc::new(AtomicUsize::new(0));
    let open_ports = Arc::new(Mutex::new(Vec::new()));
    let workers = std::cmp::min(96, ports.len());
    let results = futures_util::future::join_all((0..workers).map(|_| {
        let next = next.clone();
        let open_ports = open_ports.clone();
        async move {
            loop {
                if cancelled.load(Ordering::Relaxed) {
                    return Err("ABORTED".to_string());
                }
                if Instant::now() >= deadline {
                    return Err("TIMEOUT".to_string());
                }
                let index = next.fetch_add(1, Ordering::Relaxed);
                let Some(port) = ports.get(index).copied() else { return Ok(()); };
                if tcp_open(SocketAddr::new(resolved, port), probe_timeout, cancelled).await {
                    open_ports.lock().map_err(|_| "network task manager state poisoned".to_string())?.push(port);
                }
            }
        }
    })).await;
    if let Some(error) = results.into_iter().find_map(Result::err) {
        return Err(error);
    }
    let mut open_ports = open_ports.lock().map_err(|_| "network task manager state poisoned".to_string())?.clone();
    open_ports.sort_unstable();
    Ok(open_ports)
}

async fn tcp_open(address: SocketAddr, probe_timeout: Duration, cancelled: &AtomicBool) -> bool {
    let connect = timeout(probe_timeout, TcpStream::connect(address));
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
                let Some(address) = addresses.get(index).copied() else { return Ok(()); };
                if ping_once(&address.to_string(), probe_timeout, cancelled).await? {
                    reachable.lock().map_err(|_| "network task manager state poisoned".to_string())?.push(address);
                }
            }
        }
    })).await;
    if let Some(error) = results.into_iter().find_map(Result::err) {
        return Err(error);
    }
    let mut reachable = reachable.lock().map_err(|_| "network task manager state poisoned".to_string())?.clone();
    reachable.sort_by_key(|address| u32::from(*address));
    Ok(reachable.into_iter().map(|address| address.to_string()).collect())
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
        ("ping.exe", vec!["-n".into(), "1".into(), "-w".into(), timeout_ms.to_string(), host.into()])
    } else if cfg!(target_os = "macos") {
        ("/sbin/ping", vec!["-c".into(), "1".into(), "-W".into(), timeout_ms.to_string(), host.into()])
    } else {
        let seconds = timeout_ms.div_ceil(1_000).max(1);
        ("ping", vec!["-c".into(), "1".into(), "-W".into(), seconds.to_string(), host.into()])
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
        assert_eq!(*hosts.last().expect("last host"), Ipv4Addr::new(192, 168, 10, 254));
        assert_eq!(ipv4_prefix_hosts("192.168.10.").expect("trailing dot").len(), 254);
        assert!(ipv4_prefix_hosts("192.168.10.1").is_err());
        assert!(ipv4_prefix_hosts("192.168.10..").is_err());
        assert_eq!(normalize_ports(vec![3306, 80, 80, 22]).expect("ports"), vec![22, 80, 3306]);
        assert!(normalize_ports(vec![0]).is_err());
        assert!(normalize_ports((1..=4_097).map(|port| u16::try_from(port).expect("port")).collect()).is_err());

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
        assert!(ping_once("127.0.0.1", 800, &cancelled).await.expect("ping loopback"));
    }

    #[test]
    fn parses_platform_connection_output() {
        #[cfg(target_os = "macos")]
        let output = "tcp4 0 0 127.0.0.1.58090 127.0.0.1.9674 ESTABLISHED 3164 982 407040 146988 codex:21749 00102";
        #[cfg(target_os = "windows")]
        let output = "TCP 127.0.0.1:58090 127.0.0.1:9674 ESTABLISHED 21749";
        #[cfg(not(any(target_os = "macos", target_os = "windows")))]
        let output =
            "tcp ESTAB 0 0 127.0.0.1:58090 127.0.0.1:9674 users:((\"codex\",pid=21749,fd=9))";

        let connections = parse_connections(output);
        assert_eq!(connections.len(), 1);
        assert_eq!(connections[0].state, "ESTABLISHED");
        assert!(connections[0].process.contains("21749"));
        assert_eq!(normalize_connection_state("estab"), "ESTABLISHED");
    }
}
