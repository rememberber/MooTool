import { localDataApi } from '../platform/api/localDataApi'

export type ToolDraftKind = 'regex' | 'calculator'

export function toolDraftKey(kind: ToolDraftKind): string {
  return `mootool.${kind}.draft`
}

export async function readStoredToolDraft(kind: ToolDraftKind): Promise<string | null> {
  const local = readLocalToolDraft(kind)
  if (local !== null) return local
  try {
    const stored = await localDataApi.getToolDraft(kind)
    if (!stored) return null
    try {
      window.localStorage.setItem(toolDraftKey(kind), stored)
    } catch {
      // The database copy remains available on the next desktop launch.
    }
    return stored
  } catch {
    return null
  }
}

export function writeStoredToolDraft(kind: ToolDraftKind, value: string): void {
  try {
    window.localStorage.setItem(toolDraftKey(kind), value)
  } catch {
    // Database persistence remains available.
  }
  void localDataApi.saveToolDraft(kind, value).catch(() => undefined)
}

function readLocalToolDraft(kind: ToolDraftKind): string | null {
  try {
    return window.localStorage.getItem(toolDraftKey(kind))
  } catch {
    return null
  }
}
