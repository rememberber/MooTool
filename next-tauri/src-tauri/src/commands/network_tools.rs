use std::{
    collections::HashMap,
    net::{Ipv4Addr, SocketAddr},
    process::Stdio,
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, Ordering},
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
    start_port: u16,
    end_port: u16,
    timeout_ms: u64,
) -> AppResult<PortScanResult> {
    validate_host(&host)?;
    if start_port == 0
        || end_port < start_port
        || u32::from(end_port) - u32::from(start_port) > 1_023
    {
        return Err("port range must contain 1 to 1024 ports".into());
    }
    if !(100..=5_000).contains(&timeout_ms) {
        return Err("port timeout must be between 100 and 5000 milliseconds".into());
    }
    let cancelled = manager.begin(&request_id)?;
    let resolved = tokio::net::lookup_host((host.as_str(), start_port))
        .await
        .map_err(|error| {
            manager.finish(&request_id);
            format!("host lookup failed: {error}")
        })?
        .next()
        .ok_or_else(|| {
            manager.finish(&request_id);
            "host lookup returned no address".to_string()
        })?
        .ip();
    let started = Instant::now();
    let task_flag = cancelled.clone();
    let mut open_ports = stream::iter(start_port..=end_port)
        .map(move |port| {
            let task_flag = task_flag.clone();
            async move {
                if task_flag.load(Ordering::Relaxed) {
                    return None;
                }
                let address = SocketAddr::new(resolved, port);
                timeout(
                    Duration::from_millis(timeout_ms),
                    TcpStream::connect(address),
                )
                .await
                .ok()
                .and_then(Result::ok)
                .map(|_| port)
            }
        })
        .buffer_unordered(64)
        .filter_map(|port| async move { port })
        .collect::<Vec<_>>()
        .await;
    open_ports.sort_unstable();
    let was_cancelled = cancelled.load(Ordering::Relaxed);
    manager.finish(&request_id);
    Ok(PortScanResult {
        host,
        resolved_address: resolved.to_string(),
        start_port,
        end_port,
        open_ports,
        duration_ms: started.elapsed().as_millis(),
        cancelled: was_cancelled,
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

        let manager = NetworkTaskManager::default();
        let flag = manager.begin("scan-1").expect("begin task");
        assert!(manager.begin("scan-1").is_err());
        assert!(manager.cancel("scan-1").expect("cancel task"));
        assert!(flag.load(Ordering::Relaxed));
        manager.finish("scan-1");
        assert!(!manager.cancel("scan-1").expect("finished task"));
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
