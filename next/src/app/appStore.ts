import { create } from 'zustand'
import { defaultWorkspaceState, type WorkspaceState } from '@/shared/contracts/app'
import { isToolId, type ToolId } from './toolRegistry'
import type { CommandActionId } from '@/shared/contracts/commandPalette'

const recentLimit = 5

type AppStore = {
  activeToolId: ToolId
  recentToolIds: ToolId[]
  searchOpen: boolean
  commandActionRequest: { actionId: CommandActionId; sequence: number } | null
  requestCommandAction: (actionId: CommandActionId) => void
  hydrated: boolean
  hydrate: () => Promise<void>
  openTool: (toolId: ToolId) => void
  setSearchOpen: (open: boolean) => void
}

export const useAppStore = create<AppStore>((set, get) => ({
  activeToolId: defaultWorkspaceState.activeToolId as ToolId,
  recentToolIds: [],
  searchOpen: false,
  commandActionRequest: null,
  requestCommandAction: (actionId) => set((state) => ({ searchOpen: true, commandActionRequest: { actionId, sequence: (state.commandActionRequest?.sequence ?? 0) + 1 } })),
  hydrated: false,
  hydrate: async () => {
    if (get().hydrated) {
      return
    }

    try {
      const saved = await window.mootool.getWorkspaceState()
      const activeToolId = isToolId(saved.activeToolId) ? saved.activeToolId : 'mootool'
      const recentToolIds = saved.recentToolIds.filter(isToolId).filter((id) => id !== 'mootool').slice(0, recentLimit)
      set({ activeToolId, recentToolIds, hydrated: true })
    } catch {
      set({ hydrated: true })
    }
  },
  openTool: (toolId) => {
    set((state) => ({
      activeToolId: toolId,
      searchOpen: false,
      commandActionRequest: null,
      recentToolIds: toolId === 'mootool'
        ? state.recentToolIds
        : [toolId, ...state.recentToolIds.filter((id) => id !== toolId)].slice(0, recentLimit)
    }))
    persistWorkspace(get())
  },
  setSearchOpen: (searchOpen) => set({ searchOpen, commandActionRequest: null })
}))

function persistWorkspace(state: Pick<AppStore, 'activeToolId' | 'recentToolIds'>): void {
  const workspaceState: WorkspaceState = {
    activeToolId: state.activeToolId,
    recentToolIds: state.recentToolIds
  }
  void window.mootool.setWorkspaceState(workspaceState)
}
