/// <reference types="vite/client" />

interface Window {
  mootool: {
    platform: string
    getAppVersion: () => Promise<string>
  }
}
