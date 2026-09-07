// packages/extension/vite.config.ts
import { crx } from '@crxjs/vite-plugin';
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { defineConfig, loadEnv } from 'vite';
import manifest from './src/manifest.json';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REQUIRED_PRODUCTION_ENV = ['VITE_API_BASE_URL', 'VITE_DASHBOARD_BASE_URL'] as const;

export default defineConfig(({ mode }) => {
  if (mode === 'production') {
    const env = loadEnv(mode, __dirname, '');
    const missingEnv = REQUIRED_PRODUCTION_ENV.filter((key) => !env[key]);

    if (missingEnv.length > 0) {
      throw new Error(`Missing required extension production env: ${missingEnv.join(', ')}`);
    }
  }

  return {
    server: {
      port: 5183,
      strictPort: true,
      cors: true,
      origin: 'http://localhost:5183',
      hmr: {
        port: 5183,
      },
    },
    plugins: [
      react(),
      tailwindcss(),
      crx({ manifest }),
    ],
    resolve: {
      alias: {
        '@san/shared': resolve(__dirname, '../shared/src'),
        '@san/ui': resolve(__dirname, '../ui/src'),
        '@extension': resolve(__dirname, './src'),
        '@sidepanel': resolve(__dirname, './src/sidepanel'),
      },
    },
    build: {
      outDir: 'dist',
      emptyOutDir: true,
    },
  };
});
