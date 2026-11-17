import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development /api and /files are proxied to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8095', changeOrigin: true },
      '/files': { target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8095', changeOrigin: true },
    },
  },
  test: { environment: 'jsdom' },
} as any);
