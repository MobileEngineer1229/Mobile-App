import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api/login': {
        target:  'http://localhost:8000',
        rewrite: (path) => path.replace(/^\/api\/login/, ''),
      },
      '/api/match': {
        target:  'http://localhost:8001',
        rewrite: (path) => path.replace(/^\/api\/match/, ''),
      },
      '/api/rank': {
        target:  'http://localhost:8002',
        rewrite: (path) => path.replace(/^\/api\/rank/, ''),
      },
    },
  },
  build: {
    target: 'esnext',
    outDir: 'dist',
  },
});
