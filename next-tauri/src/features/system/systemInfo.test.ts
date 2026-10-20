import { describe, expect, it } from 'vitest'
import type { SystemSnapshot } from '../../platform/contracts/diagnostics'
import { hardwareGroups, hardwarePlainText, type HardwareText } from './systemInfo'

const text: HardwareText = {
  operatingSystem: '操作系统', processor: '处理器', physicalMemory: '物理内存',
  platform: '平台', distribution: '发行版', kernel: '内核', architecture: '架构', hostName: '主机名', uptime: '运行时间',
  brand: '品牌', physicalCores: '物理核心', logicalCores: '逻辑核心', currentLoad: '当前负载', currentFrequency: '当前频率',
  total: '总计', used: '已用', available: '可用', usage: '使用率', swapTotal: '交换空间总计', swapUsed: '交换空间已用',
  device: '设备', capacity: '容量', filesystem: '文件系统', ipv4: 'IPv4', ipv6: 'IPv6', mac: 'MAC', received: '接收', sent: '发送',
  uptimeValue: '1 天 0 小时',
  bytes: (value) => `${value} B`
}

const snapshot: SystemSnapshot = {
  osName: 'Darwin', osVersion: '25', kernelVersion: '25.0.0', hostName: 'moo', architecture: 'arm64',
  cpuBrand: 'Apple M', physicalCores: 8, logicalCores: 8, totalMemoryBytes: 1000, availableMemoryBytes: 400,
  processMemoryBytes: 10, uptimeSeconds: 86_400, cpuUsagePercent: 12.5, cpuFrequencyMhz: 3200,
  totalSwapBytes: 0, usedSwapBytes: 0,
  disks: [{ name: 'disk0', mountPoint: '/', fileSystem: 'apfs', totalBytes: 500, availableBytes: 200, removable: false }],
  networkInterfaces: [{ name: 'en0', addresses: ['127.0.0.1', 'fe80::1'], macAddress: 'aa:bb', receivedBytes: 3, transmittedBytes: 4 }]
}

describe('hardware sections', () => {
  it('formats the system tab like Electron plain text', () => {
    expect(hardwarePlainText(hardwareGroups(snapshot, 'system', text))).toBe([
      '========== 操作系统 ==========',
      '平台: Darwin',
      '发行版: 25',
      '内核: 25.0.0',
      '架构: arm64',
      '主机名: moo',
      '运行时间: 1 天 0 小时',
      ''
    ].join('\n'))
  })

  it('splits interface addresses and names the processor frequency in GHz', () => {
    expect(hardwareGroups(snapshot, 'cpu', text)[0]?.items.find((entry) => entry.label === '当前频率')?.value).toBe('3.20 GHz')
    const network = hardwareGroups(snapshot, 'network', text)[0]
    expect(network?.items.find((entry) => entry.label === 'IPv4')?.value).toBe('127.0.0.1')
    expect(network?.items.find((entry) => entry.label === 'IPv6')?.value).toBe('fe80::1')
  })
})
