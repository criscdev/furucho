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
  test('axe scan — desktop 1280px (WCAG 2.2 AA)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag22aa'])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  test('axe scan — tablet 768px (WCAG 2.2 AA)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag22aa'])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  test('axe scan — mobile 375px (WCAG 2.2 AA)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag22aa'])
      .analyze();
    expect(results.violations).toEqual([]);
  });

  // 4B.3
  test('skip link Tab → Enter → focus on #main', async ({ page }) => {
    await page.keyboard.press('Tab');
    const skipLink = page.getByRole('link', { name: /pular para o conteúdo/i });
    await expect(skipLink).toBeFocused();

    await page.keyboard.press('Enter');

    // After clicking skip link, the main element should receive focus
    const main = page.locator('#main');
    await expect(main).toBeVisible();

    // The URL should now have #main hash
    expect(page.url()).toContain('#main');

    // If main has tabIndex=-1, the browser should focus it
    const focusedId = await page.evaluate(() => document.activeElement?.id);
    expect(focusedId).toBe('main');
  });

  // 4B.4 — keyboard-only form completion
  test('form can be completed and submitted using only keyboard', async ({ page }) => {
    // Intercept window.open to prevent navigation
    await page.evaluate(() => {
      (window as unknown as Record<string, unknown>).__whatsappUrl = '';
      window.open = (url?: string | URL) => {
        (window as unknown as Record<string, unknown>).__whatsappUrl = String(url ?? '');
        return {} as Window;
      };
    });

    // Tab to the order form and fill it
    const nameInput = page.getByLabel(/nome completo/i);
    await nameInput.focus();
    await page.keyboard.type('Maria Silva');

    await page.keyboard.press('Tab');
    await page.keyboard.type('maria@example.com');

    await page.keyboard.press('Tab');
    await page.keyboard.type('11999887766');

    await page.keyboard.press('Tab');
    await page.keyboard.type('Rua das Flores, 123');

    await page.keyboard.press('Tab');
    await page.keyboard.type('01234567');

    await page.keyboard.press('Tab');
    await page.keyboard.type('Boneca personalizada');

    await page.keyboard.press('Tab');
    await page.keyboard.type('Cabelo loiro, vestido azul');

    await page.keyboard.press('Tab');

    const futureDate = (() => {
      const d = new Date();
      d.setMonth(d.getMonth() + 6);
      const dd = String(d.getDate()).padStart(2, '0');
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      return `${dd}/${mm}/${d.getFullYear()}`;
    })();
    await page.keyboard.type(futureDate);

    // Tab to submit button and press Enter
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    // Success message should appear
    await expect(page.getByText(/redirecionando para o whatsapp/i)).toBeVisible();

    // Verify WhatsApp URL was constructed
    const whatsappUrl = await page.evaluate(
      () => (window as unknown as Record<string, string>).__whatsappUrl
    );
    expect(whatsappUrl).toContain('wa.me');
    expect(whatsappUrl).toContain('Maria');
  });

  // 4B.5 — form fields have autocomplete for assistive tech
  test('personal data fields have autocomplete attributes', async ({ page }) => {
    expect(await page.getByLabel(/nome completo/i).getAttribute('autocomplete')).toBe('name');
    expect(await page.getByLabel(/^email/i).getAttribute('autocomplete')).toBe('email');
    expect(await page.getByLabel(/telefone/i).getAttribute('autocomplete')).toBe('tel');
    expect(await page.getByLabel(/endereço completo/i).getAttribute('autocomplete')).toBe('street-address');
    expect(await page.getByLabel(/cep/i).getAttribute('autocomplete')).toBe('postal-code');
  });

  // 4B.6 — WCAG 2.2 SC 2.5.8 Target Size (Minimum) ≥ 24×24 CSS px
  test('interactive elements meet 24×24 minimum target size (WCAG 2.5.8)', async ({ page }) => {
    const MIN = 24;

    // Header nav links
    for (const name of ['Início', 'Encomendas']) {
      const link = page.getByRole('link', { name });
      const box = await link.boundingBox();
      expect(box, `nav link "${name}" has bounding box`).toBeTruthy();
      expect(box!.height).toBeGreaterThanOrEqual(MIN);
      expect(box!.width).toBeGreaterThanOrEqual(MIN);
    }

    // Instagram link in header
    const igLink = page.getByRole('link', { name: /instagram.*roberta/i });
    const igBox = await igLink.boundingBox();
    expect(igBox, 'Instagram header link has bounding box').toBeTruthy();
    expect(igBox!.height).toBeGreaterThanOrEqual(MIN);
    expect(igBox!.width).toBeGreaterThanOrEqual(MIN);

    // CTA buttons
    const ctaBtn = page.getByRole('button', { name: /fazer encomenda/i });
    const ctaBox = await ctaBtn.boundingBox();
    expect(ctaBox, 'CTA button has bounding box').toBeTruthy();
    expect(ctaBox!.height).toBeGreaterThanOrEqual(MIN);

    // Submit button
    const submitBtn = page.getByRole('button', { name: /enviar pelo whatsapp/i });
    const submitBox = await submitBtn.boundingBox();
    expect(submitBox, 'Submit button has bounding box').toBeTruthy();
    expect(submitBox!.height).toBeGreaterThanOrEqual(MIN);
  });

  // 4B.7 — WCAG 2.2 SC 2.5.8 Target Size on mobile (375px)
  test('interactive elements meet target size on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    const MIN = 24;

    // Header nav links must still meet minimum on mobile
    for (const name of ['Início', 'Encomendas']) {
      const link = page.getByRole('link', { name });
      const box = await link.boundingBox();
      expect(box, `mobile nav "${name}" has bounding box`).toBeTruthy();
      expect(box!.height).toBeGreaterThanOrEqual(MIN);
      expect(box!.width).toBeGreaterThanOrEqual(MIN);
    }

    // Instagram icon link in header (text is sr-only on mobile)
    const igLink = page.getByRole('link', { name: /instagram.*roberta/i });
    const igBox = await igLink.boundingBox();
    expect(igBox, 'Mobile Instagram link has bounding box').toBeTruthy();
    expect(igBox!.height).toBeGreaterThanOrEqual(MIN);
    expect(igBox!.width).toBeGreaterThanOrEqual(MIN);
  });
});
