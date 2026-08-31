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
    target: ['es2022', 'chrome105', 'safari13'],
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('/node_modules/')) return
          if (/[\\/]node_modules[\\/](react|react-dom|scheduler)[\\/]/.test(id)) return 'vendor-react'
          if (id.includes('/node_modules/lucide-react/')) return 'vendor-icons'
          if (id.includes('/node_modules/@tauri-apps/')) return 'vendor-tauri'
          if (/node_modules\/(?:@codemirror|@lezer|crelt|style-mod|w3c-keyname)\//.test(id)) return 'vendor-editor'
        }
      }
    }
  }
})
