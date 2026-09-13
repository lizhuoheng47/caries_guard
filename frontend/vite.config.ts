import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  if (command === 'build' && env.VITE_USE_MOCK === 'true') {
    throw new Error('VITE_USE_MOCK=true is forbidden in production builds')
  }

  return {
    plugins: [
      vue(),
      tailwindcss(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ai/v1': {
          target: env.VITE_SEGMENTATION_PROXY_TARGET || 'http://127.0.0.1:8001',
          changeOrigin: true,
        }
      }
    },
    preview: {
      host: '0.0.0.0',
    }
  }
})
