export type CodeRuntimeId = 'java' | 'groovy' | 'python' | 'node'
export interface CodeRuntimeStatus { id: CodeRuntimeId; available: boolean; command: string; version: string }
export interface CodeRunSpec { requestId: string; runtime: CodeRuntimeId; code: string; timeoutMs: number; arguments: string[]; workingDirectory: string }
export interface CodeOutputEvent { stream: 'stdout' | 'stderr'; text: string }
export interface CodeRunResult { exitCode: number | null; stdout: string; stderr: string; durationMs: number; command: string; timedOut: boolean; cancelled: boolean }
export interface CodeRuntimeApi { detect(): Promise<CodeRuntimeStatus[]>; run(spec: CodeRunSpec, onOutput: (event: CodeOutputEvent) => void): Promise<CodeRunResult>; cancel(requestId: string): Promise<boolean> }
