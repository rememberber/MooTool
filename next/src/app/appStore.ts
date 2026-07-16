import { create } from 'zustand'
import { defaultWorkspaceState, type WorkspaceState } from '@/shared/contracts/app'
import { isToolId, type ToolId } from './toolRegistry'

const recentLimit = 5

type AppStore = {
  activeToolId: ToolId
  recentToolIds: ToolId[]
  searchOpen: boolean
  hydrated: boolean
  hydrate: () => Promise<void>
  openTool: (toolId: ToolId) => void
  setSearchOpen: (open: boolean) => void
}

export const useAppStore = create<AppStore>((set, get) => ({
  activeToolId: defaultWorkspaceState.activeToolId as ToolId,
  recentToolIds: [],
  searchOpen: false,
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
      recentToolIds: toolId === 'mootool'
        ? state.recentToolIds
        : [toolId, ...state.recentToolIds.filter((id) => id !== toolId)].slice(0, recentLimit)
    }))
    persistWorkspace(get())
  },
  setSearchOpen: (searchOpen) => set({ searchOpen })
}))

function persistWorkspace(state: Pick<AppStore, 'activeToolId' | 'recentToolIds'>): void {
  const workspaceState: WorkspaceState = {
    activeToolId: state.activeToolId,
    recentToolIds: state.recentToolIds
  }
  void window.mootool.setWorkspaceState(workspaceState)
}
