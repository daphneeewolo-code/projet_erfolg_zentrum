import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const frontendDirectory = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: process.env.VERCEL ? 'dist' : '../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(frontendDirectory, 'index.html'),
        admin: resolve(frontendDirectory, 'admin.html'),
      },
    },
  },
});
