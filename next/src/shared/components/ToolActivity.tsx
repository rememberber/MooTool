import { createContext, useContext, type ReactNode } from 'react'

const ToolActivityContext = createContext(true)

export function ToolActivityProvider({ active, children }: { active: boolean; children: ReactNode }) {
  return <ToolActivityContext.Provider value={active}>{children}</ToolActivityContext.Provider>
}

export function useToolActivity(): boolean {
  return useContext(ToolActivityContext)
}
