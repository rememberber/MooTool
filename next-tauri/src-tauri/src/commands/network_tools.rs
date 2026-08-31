use std::{net::SocketAddr, process::Stdio, time::Instant};

use futures_util::{StreamExt, stream};
use serde::Serialize;
use sysinfo::Networks;
use tokio::{
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
    let resolved = tokio::net::lookup_host((host.as_str(), start_port))
        .await
        .map_err(|error| format!("host lookup failed: {error}"))?
        .next()
        .ok_or_else(|| "host lookup returned no address".to_string())?
        .ip();
    let started = Instant::now();
    let mut open_ports = stream::iter(start_port..=end_port)
        .map(|port| async move {
            let address = SocketAddr::new(resolved, port);
            timeout(
                Duration::from_millis(timeout_ms),
                TcpStream::connect(address),
            )
            .await
            .ok()
            .and_then(Result::ok)
            .map(|_| port)
        })
        .buffer_unordered(64)
        .filter_map(|port| async move { port })
        .collect::<Vec<_>>()
        .await;
    open_ports.sort_unstable();
    Ok(PortScanResult {
        host,
        resolved_address: resolved.to_string(),
        start_port,
        end_port,
        open_ports,
        duration_ms: started.elapsed().as_millis(),
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
}
