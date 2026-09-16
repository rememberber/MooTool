import Foundation
import Darwin

public enum NetworkDiagnostics {
    private static let maxCustomPorts = 4096
    private static let commonPorts: [Int: String] = [
        20: "ftp-data", 21: "ftp", 22: "ssh", 23: "telnet", 25: "smtp", 53: "dns",
        67: "dhcp", 68: "dhcp", 69: "tftp", 80: "http", 110: "pop3", 123: "ntp",
        135: "msrpc", 139: "netbios", 143: "imap", 161: "snmp", 389: "ldap", 443: "https",
        445: "smb", 465: "smtps", 587: "smtp-submission", 631: "ipp", 636: "ldaps",
        873: "rsync", 993: "imaps", 995: "pop3s", 1080: "socks", 1433: "mssql",
        1521: "oracle", 2049: "nfs", 2181: "zookeeper", 2375: "docker", 3000: "http-alt",
        3306: "mysql", 3389: "rdp", 5432: "postgresql", 5601: "kibana", 5672: "amqp",
        5900: "vnc", 6379: "redis", 8080: "http-alt", 8081: "http-alt", 8443: "https-alt",
        8888: "http-alt", 9092: "kafka", 9200: "elasticsearch", 9300: "elasticsearch",
        11211: "memcached", 27017: "mongodb"
    ]

    public static func ipv4ToLong(_ value: String) throws -> UInt32 {
        let parts = value.trimmingCharacters(in: .whitespacesAndNewlines).split(separator: ".")
        guard parts.count == 4, parts.allSatisfy({ $0.range(of: #"^\d{1,3}$"#, options: .regularExpression) != nil }) else {
            throw ToolError("IPv4 地址无效。")
        }
        var result: UInt32 = 0
        for part in parts {
            let octet = Int(part)!
            guard octet <= 255 else { throw ToolError("IPv4 地址无效。") }
            result = (result << 8) + UInt32(octet)
        }
        return result
    }

    public static func longToIPv4(_ value: String) throws -> String {
        guard let number = UInt32(value.trimmingCharacters(in: .whitespacesAndNewlines)), number <= 0xffff_ffff else {
            throw ToolError("IPv4 数值无效。")
        }
        return [24, 16, 8, 0].map { shift in String((number >> UInt32(shift)) & 0xff) }.joined(separator: ".")
    }

    public static func parseIPv4Range(_ value: String) throws -> [String] {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        let regex = try NSRegularExpression(pattern: #"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.?$"#)
        let range = NSRange(trimmed.startIndex..., in: trimmed)
        guard let match = regex.firstMatch(in: trimmed, range: range), match.numberOfRanges == 4 else {
            throw ToolError("IP 段格式应为 192.168.1 或 192.168.1.")
        }
        let octets = (1...3).map { index -> Int in
            let r = match.range(at: index)
            return Int(trimmed[Range(r, in: trimmed)!])!
        }
        guard octets.allSatisfy({ $0 <= 255 }) else { throw ToolError("IP 段无效。") }
        let prefix = octets.map(String.init).joined(separator: ".")
        return (1...254).map { "\(prefix).\($0)" }
    }

    public static func parsePortSpec(_ value: String) throws -> [Int] {
        let input = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if input.isEmpty { return commonPorts.keys.sorted() }
        var ports = Set<Int>()
        for token in input.split(separator: ",") {
            let piece = token.trimmingCharacters(in: .whitespaces)
            if piece.contains("-") {
                let bounds = piece.split(separator: "-", maxSplits: 1).map { $0.trimmingCharacters(in: .whitespaces) }
                guard bounds.count == 2 else { throw ToolError("端口范围无效。") }
                let start = try validPort(bounds[0]), end = try validPort(bounds[1])
                guard start <= end, end - start + 1 <= maxCustomPorts else { throw ToolError("端口范围过大或无效。") }
                for port in start...end { ports.insert(port) }
            } else {
                ports.insert(try validPort(piece))
            }
            guard ports.count <= maxCustomPorts else { throw ToolError("端口数量超过 \(maxCustomPorts)。") }
        }
        return ports.sorted()
    }

    public static func resolveHost(_ host: String) throws -> String {
        let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.range(of: #"^[A-Za-z0-9.:_-]+$"#, options: .regularExpression) != nil else { throw ToolError("主机名无效。") }
        var hints = addrinfo(ai_flags: AI_ADDRCONFIG, ai_family: AF_UNSPEC, ai_socktype: SOCK_STREAM, ai_protocol: IPPROTO_TCP, ai_addrlen: 0, ai_canonname: nil, ai_addr: nil, ai_next: nil)
        var result: UnsafeMutablePointer<addrinfo>?
        let code = getaddrinfo(trimmed, nil, &hints, &result)
        guard code == 0, let result else { throw ToolError("解析失败：\(String(cString: gai_strerror(code)))") }
        defer { freeaddrinfo(result) }
        var lines: [String] = []
        var pointer: UnsafeMutablePointer<addrinfo>? = result
        while let node = pointer {
            var hostBuffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            if getnameinfo(node.pointee.ai_addr, node.pointee.ai_addrlen, &hostBuffer, socklen_t(hostBuffer.count), nil, 0, NI_NUMERICHOST) == 0 {
                let address = String(cString: hostBuffer)
                let family = node.pointee.ai_family == AF_INET6 ? "IPv6" : "IPv4"
                lines.append("\(address)\t\(family)")
            }
            pointer = node.pointee.ai_next
        }
        guard !lines.isEmpty else { throw ToolError("未解析到地址。") }
        return lines.joined(separator: "\n")
    }

    public static func localAddresses() -> String {
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else { return "无法读取网络接口。" }
        defer { freeifaddrs(ifaddr) }
        var ipv4: [String] = [], ipv6: [String] = []
        var pointer: UnsafeMutablePointer<ifaddrs>? = first
        while let interface = pointer {
            let flags = UInt32(interface.pointee.ifa_flags)
            guard (flags & UInt32(IFF_UP)) != 0, (flags & UInt32(IFF_LOOPBACK)) == 0 else { pointer = interface.pointee.ifa_next; continue }
            guard let addr = interface.pointee.ifa_addr else { pointer = interface.pointee.ifa_next; continue }
            var host = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            if getnameinfo(addr, socklen_t(addr.pointee.sa_len), &host, socklen_t(host.count), nil, 0, NI_NUMERICHOST) == 0 {
                let name = String(cString: interface.pointee.ifa_name)
                let value = String(cString: host)
                if addr.pointee.sa_family == UInt8(AF_INET) { ipv4.append("\(name)\t\(value)") }
                if addr.pointee.sa_family == UInt8(AF_INET6) { ipv6.append("\(name)\t\(value)") }
            }
            pointer = interface.pointee.ifa_next
        }
        return ["IPv4", ipv4.isEmpty ? "（无）" : ipv4.joined(separator: "\n"), "", "IPv6", ipv6.isEmpty ? "（无）" : ipv6.joined(separator: "\n")].joined(separator: "\n")
    }

    public static func scanPorts(host: String, portSpec: String, timeoutMs: Int = 500) async throws -> String {
        let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.range(of: #"^[A-Za-z0-9.:_-]+$"#, options: .regularExpression) != nil else { throw ToolError("主机无效。") }
        let ports = try parsePortSpec(portSpec)
        let timeout = max(100, min(timeoutMs, 2_000))
        var open: [(Int, String?)] = []
        try await withThrowingTaskGroup(of: (Int, Bool).self) { group in
            for port in ports {
                group.addTask { (port, await probeTCP(host: trimmed, port: port, timeoutMs: timeout)) }
            }
            for try await result in group where result.1 {
                open.append((result.0, commonPorts[result.0]))
            }
        }
        open.sort { $0.0 < $1.0 }
        let lines = open.map { entry in
            let service = entry.1.map { " \($0)" } ?? ""
            return "\(entry.0)/tcp open\(service)"
        }
        return (["\(trimmed) 开放 TCP 端口：\(open.count) / \(ports.count)", ""]
            + (lines.isEmpty ? ["未发现开放端口"] : lines))
            .joined(separator: "\n")
    }

    public static func scanIPv4Range(_ range: String) async throws -> String {
        let addresses = try parseIPv4Range(range)
        var reachable: [String] = []
        try await withThrowingTaskGroup(of: (String, Bool).self) { group in
            for address in addresses {
                group.addTask { (address, await pingOnce(address)) }
            }
            for try await result in group where result.1 { reachable.append(result.0) }
        }
        reachable.sort()
        return (["可达主机：\(reachable.count) / \(addresses.count)", ""]
            + (reachable.isEmpty ? ["未发现可达主机"] : reachable))
            .joined(separator: "\n")
    }

    private static func validPort(_ text: String) throws -> Int {
        guard text.range(of: #"^\d{1,5}$"#, options: .regularExpression) != nil, let port = Int(text), (1...65535).contains(port) else {
            throw ToolError("端口无效：\(text)")
        }
        return port
    }

    private static func probeTCP(host: String, port: Int, timeoutMs: Int) async -> Bool {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .utility).async {
                var hints = addrinfo(ai_flags: AI_ADDRCONFIG, ai_family: AF_INET, ai_socktype: SOCK_STREAM, ai_protocol: IPPROTO_TCP, ai_addrlen: 0, ai_canonname: nil, ai_addr: nil, ai_next: nil)
                var resolved: UnsafeMutablePointer<addrinfo>?
                guard getaddrinfo(host, nil, &hints, &resolved) == 0, let resolved else { continuation.resume(returning: false); return }
                defer { freeaddrinfo(resolved) }
                let socket = Darwin.socket(AF_INET, SOCK_STREAM, 0)
                guard socket >= 0 else { continuation.resume(returning: false); return }
                defer { close(socket) }
                var addr = resolved.pointee.ai_addr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee }
                addr.sin_port = in_port_t(UInt16(port)).bigEndian
                var tv = timeval(tv_sec: timeoutMs / 1000, tv_usec: Int32((timeoutMs % 1000) * 1000))
                setsockopt(socket, SOL_SOCKET, SO_SNDTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
                setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
                let result = withUnsafePointer(to: &addr) {
                    $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { connect(socket, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) }
                }
                continuation.resume(returning: result == 0)
            }
        }
    }

    private static func pingOnce(_ host: String) async -> Bool {
        do {
            let path = host.contains(":") ? "/sbin/ping6" : "/sbin/ping"
            let output = try await ProcessRunner.run(executable: path, arguments: ["-c", "1", "-W", "800", host], timeout: 2)
            return !output.isEmpty
        } catch { return false }
    }
}
