import { resolve } from 'node:path'
import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin({ exclude: ['electron-store'] })],
    build: {
      minify: true,
      rollupOptions: {
        input: {
          index: resolve(__dirname, 'electron/main/index.ts'),
          pdfWorker: resolve(__dirname, 'electron/main/pdfWorker.ts'),
          imageVectorizationWorker: resolve(__dirname, 'electron/main/imageVectorizationWorker.ts'),
          ppOcrWorker: resolve(__dirname, 'electron/main/ppOcrWorker.ts'),
          mcp: resolve(__dirname, 'electron/mcp/index.ts')
        }
      }
    }
  },
  preload: {
    plugins: [externalizeDepsPlugin()],
    build: {
      minify: true,
      rollupOptions: {
        input: resolve(__dirname, 'electron/preload/index.ts')
      }
    }
  },
  renderer: {
    root: resolve(__dirname),
    worker: { format: 'es' },
    plugins: [react()],
    build: {
      minify: true,
      rollupOptions: {
        input: resolve(__dirname, 'index.html')
      }
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    }
  }
})
