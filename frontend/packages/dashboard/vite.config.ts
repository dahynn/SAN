// packages/dashboard/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: [
      { find: '@san/shared/utils', replacement: resolve(__dirname, '../shared/src/utils') },
      { find: '@san/shared', replacement: resolve(__dirname, '../shared/src/index.ts') },
      { find: '@san/ui', replacement: resolve(__dirname, '../ui/src/index.ts') },
      { find: '@ui', replacement: resolve(__dirname, '../ui/src') },
      { find: '@dashboard', replacement: resolve(__dirname, './src') },
    ],
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;

          if (id.includes('monaco-editor') || id.includes('@monaco-editor')) {
            return 'editor';
          }

          if (id.includes('react-markdown') || id.includes('remark-gfm') || id.includes('micromark') || id.includes('unified')) {
            return 'markdown';
          }

          if (id.includes('@tanstack')) {
            return 'query';
          }

          if (id.includes('react') || id.includes('react-dom') || id.includes('react-router-dom')) {
            return 'react-vendor';
          }

          return undefined;
        },
      },
    },
  },
});
