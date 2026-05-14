import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    // Use jsdom environment for DOM testing
    environment: 'jsdom',
    
    // Setup files for global test configuration
    setupFiles: ['./src/__tests__/setup.ts'],
    
    // Include patterns
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    
    // Coverage configuration
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'src/__tests__/',
        '**/*.test.ts',
        '**/*.spec.ts',
        '**/types/',
        'dist/',
      ],
      lines: 70,
      functions: 70,
      branches: 65,
      statements: 70,
    },
    
    // Global test timeout
    testTimeout: 10000,
    
    // Hook timeout
    hookTimeout: 10000,
    
    // Enable globals (describe, it, expect, etc.)
    globals: true,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
