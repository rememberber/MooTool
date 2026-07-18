export type AdapterIdentity = {
  id: string
}

export class AdapterRegistry<T extends AdapterIdentity> {
  private readonly adapters = new Map<string, T>()

  constructor(initialAdapters: readonly T[] = []) {
    for (const adapter of initialAdapters) this.register(adapter)
  }

  register(adapter: T): void {
    if (!adapter.id.trim()) throw new Error('Adapter id is required')
    if (this.adapters.has(adapter.id)) throw new Error(`Duplicate adapter id: ${adapter.id}`)
    this.adapters.set(adapter.id, adapter)
  }

  get(id: string): T | undefined {
    return this.adapters.get(id)
  }

  list(): T[] {
    return [...this.adapters.values()]
  }
}
