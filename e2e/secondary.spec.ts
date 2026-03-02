import { test, expect } from '@playwright/test';

/**
 * E2E — Secondary tests P1-P2 (Batch 4C).
 *
 * Covers:
 * - CTA scroll to #order-form
 * - Mobile responsive gallery layout
 * - SEO meta tags
 * - Instagram link security attributes
 */

test.describe('Secondary flows', () => {
  // 4C.1
  test('CTA "Fazer Encomenda" scrolls to #order-form', async ({ page }) => {
    await page.goto('/');

    const ctaButton = page.getByRole('button', { name: /fazer encomenda/i });
    await ctaButton.click();

    // Wait for smooth scroll to complete
    await page.waitForTimeout(500);

    const orderForm = page.locator('#order-form');
    await expect(orderForm).toBeInViewport();
  });

  // 4C.2
  test('gallery is single-column on 375px mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    // Get all gallery list items
    const items = page.locator('ul[aria-label*="bonecas"] > li');
    const count = await items.count();
    expect(count).toBeGreaterThan(0);

    // In single-column layout, all items should have roughly the same x-offset
    const firstBox = await items.first().boundingBox();
    const secondBox = await items.nth(1).boundingBox();

    expect(firstBox).not.toBeNull();
    expect(secondBox).not.toBeNull();

    // Both items should start at ~same x position (single column)
    expect(Math.abs(firstBox!.x - secondBox!.x)).toBeLessThan(5);
  });

  // 4C.3
  test('meta tags for SEO are present', async ({ page }) => {
    await page.goto('/');

    // Title
    await expect(page).toHaveTitle(/roberta furucho/i);

    // Description
    const description = page.locator('meta[name="description"]');
    await expect(description).toHaveAttribute('content', /bonecas artesanais/i);

    // OG title
    const ogTitle = page.locator('meta[property="og:title"]');
    await expect(ogTitle).toHaveAttribute('content', /roberta furucho/i);

    // OG type
    const ogType = page.locator('meta[property="og:type"]');
    await expect(ogType).toHaveAttribute('content', 'website');

    // Theme color
    const themeColor = page.locator('meta[name="theme-color"]');
    await expect(themeColor).toHaveAttribute('content', '#F4B8C5');
  });

  // 4C.4
  test('Instagram links have security attributes', async ({ page }) => {
    await page.goto('/');

    // Find all Instagram links on the page
    const instagramLinks = page.locator('a[href*="instagram.com"]');
    const count = await instagramLinks.count();
    expect(count).toBeGreaterThan(0);

    for (let i = 0; i < count; i++) {
      const link = instagramLinks.nth(i);
      await expect(link).toHaveAttribute('target', '_blank');
      await expect(link).toHaveAttribute('rel', /noopener/);
      await expect(link).toHaveAttribute('rel', /noreferrer/);
    }
  });
});
