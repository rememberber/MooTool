import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      string_decoder: 'string_decoder/'
    }
  },
  clearScreen: false,
  server: {
    host: '127.0.0.1',
    port: 1420,
    strictPort: true,
    watch: {
      ignored: ['**/src-tauri/target/**']
    }
  },
  build: {
    target: ['es2022', 'chrome105', 'safari13']
  }
})
