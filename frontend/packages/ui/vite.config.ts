// packages/ui/vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // tsconfig의 paths와 동일하게 맞춰야 함
      // TypeScript: paths → 타입 체크용
      // Vite: alias   → 실제 번들링 경로 해석용
      '@san/shared': resolve(__dirname, '../shared/src/index.ts'),
    },
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      formats: ['es'],
      fileName: 'index',
    },
    rollupOptions: {
      // React는 번들에 포함하지 않음 (peerDependency)
      external: ['react', 'react-dom'],
    },
  },
});