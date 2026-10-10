export function randomUuid(): string {
  return globalThis.crypto.randomUUID()
}
