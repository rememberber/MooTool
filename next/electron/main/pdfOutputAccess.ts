/** Output actions accept only the last successful PDF results from this renderer. */
export class PdfOutputAccess {
  private readonly outputs = new Map<number, Set<string>>()
  remember(owner: number, paths: string[]): void { this.outputs.set(owner, new Set(paths)) }
  forget(owner: number): void { this.outputs.delete(owner) }
  authorize(owner: number, path: unknown, action: unknown): { path: string; action: 'open' | 'reveal' } {
    if (typeof path !== 'string' || !this.outputs.get(owner)?.has(path) || (action !== 'open' && action !== 'reveal')) {
      throw new Error('Invalid PDF output action')
    }
    return { path, action }
  }
}
