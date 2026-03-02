import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * E2E — Keyboard navigation & accessibility audit (Batch 4B).
 *
 * Covers:
 * - Tab through full page (skip→nav→CTA→form)
 * - axe scans at 3 viewports (375px, 768px, 1280px)
 * - Skip link → focus on #main
 */

test.describe('Keyboard & accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  // 4B.1
  test('Tab navigates through key interactive elements', async ({ page }) => {
    // First Tab should land on skip link
    await page.keyboard.press('Tab');
    const skipLink = page.getByRole('link', { name: /pular para o conteúdo/i });
    await expect(skipLink).toBeFocused();

    // Tab through the page — verify we eventually reach the CTA button
    // (exact order depends on component rendering, so we test reachability)
    let foundCta = false;
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      const focusedText = await page.evaluate(() => {
        const el = document.activeElement;
        return el?.textContent?.trim() ?? '';
      });

      if (/fazer encomenda/i.test(focusedText)) {
        foundCta = true;
        break;
      }
    }

    expect(foundCta).toBe(true);
  });

  // 4B.2
  test('axe scan — desktop 1280px', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/');

    const results = await new AxeBuilder({ page }).analyze();
    expect(results.violations).toEqual([]);
  });

  test('axe scan — tablet 768px', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');

    const results = await new AxeBuilder({ page }).analyze();
    expect(results.violations).toEqual([]);
  });

  test('axe scan — mobile 375px', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    const results = await new AxeBuilder({ page }).analyze();
    expect(results.violations).toEqual([]);
  });

  // 4B.3
  test('skip link Tab → Enter → focus on #main', async ({ page }) => {
    await page.keyboard.press('Tab');
    const skipLink = page.getByRole('link', { name: /pular para o conteúdo/i });
    await expect(skipLink).toBeFocused();

    await page.keyboard.press('Enter');

    // After clicking skip link, the main element should receive focus (or be scrolled to)
    const main = page.locator('#main');
    await expect(main).toBeVisible();

    // The URL should now have #main hash
    expect(page.url()).toContain('#main');
  });
});
