import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E configuration for Furucho.
 *
 * @decision D4: Chromium + Firefox + Mobile Chrome. No WebKit on Linux —
 *   requires macOS for real Safari; WebKit on Linux is a simulation.
 *   Chromium + Firefox cover ~85% of Brazilian market.
 *   Mobile Chrome covers the primary use case (mobile ordering).
 *
 * @see docs/TDD_REFACTORING_PLAN.md §4 D4
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
