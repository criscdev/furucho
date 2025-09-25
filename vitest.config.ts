// Export a plain config object to avoid requiring 'vitest/config' at compile time
// (this keeps the same runtime config for Vitest when it's installed locally).
export default {
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts'
  }
} as any;
