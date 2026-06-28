import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import fs from 'fs';
import {defineConfig} from 'vite';

// Statically declare the index.html entry point
const htmlFiles = [
  'index.html'
];

export default defineConfig(() => {
  const entries: Record<string, string> = {
    main: path.resolve(__dirname, 'index.html')
  };
  
  console.log('Resolved HTML entry point for Rollup:', entries);

  return {
    base: '/marudhara-result-system/',
    plugins: [react(), tailwindcss()],
    build: {
      target: 'esnext',
      rollupOptions: {
        input: entries,
      }
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});

