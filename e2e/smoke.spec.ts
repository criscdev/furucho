import { test, expect } from '@playwright/test';

/**
 * Smoke tests — verify the app loads and core elements are visible.
 *
 * These are the most basic E2E sanity checks. They confirm the dev server
 * starts, the page renders, and the primary heading is present.
 */

test.describe('Smoke', () => {
  test('page loads and shows main heading', async ({ page }) => {
    await page.goto('/');

    // The h1 "Roberta Furucho" should be visible
    const heading = page.getByRole('heading', { name: /roberta furucho/i, level: 1 });
    await expect(heading).toBeVisible();
  });

  test('page has correct title', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle(/furucho/i);
  });
});
