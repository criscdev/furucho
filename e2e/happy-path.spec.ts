import { test, expect } from '@playwright/test';

/** Returns a date 6 months from now as DD/MM/YYYY. */
function futureDate(): string {
  const d = new Date();
  d.setMonth(d.getMonth() + 6);
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}

/**
 * E2E — Happy path & validation (Batch 4A).
 *
 * Covers the critical user flows:
 * - Page loads with form visible
 * - Fill form → submit → WhatsApp redirect
 * - Submit empty → errors → fix → errors clear
 */

test.describe('Happy path & validation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  // 4A.1
  test('page loads and order form is visible', async ({ page }) => {
    const formHeading = page.getByRole('heading', { name: /faça sua encomenda/i });
    await expect(formHeading).toBeVisible();

    const submitBtn = page.getByRole('button', { name: /enviar pelo whatsapp/i });
    await expect(submitBtn).toBeVisible();
  });

  // 4A.2
  test('fill form → submit → WhatsApp redirect', async ({ page, context }) => {
    // Intercept the window.open call to prevent actual navigation
    await page.evaluate(() => {
      (window as unknown as Record<string, unknown>).__whatsappUrl = '';
      window.open = (url?: string | URL) => {
        (window as unknown as Record<string, unknown>).__whatsappUrl = String(url ?? '');
        return {} as Window; // Return truthy to signal popup succeeded
      };
    });

    // Fill form with valid data
    await page.getByLabel(/nome completo/i).fill('Maria Silva');
    await page.getByLabel(/^email/i).fill('maria@example.com');
    await page.getByLabel(/telefone/i).fill('11999887766');
    await page.getByLabel(/endereço completo/i).fill('Rua das Flores, 123');
    await page.getByLabel(/cep/i).fill('01234567');
    await page.getByLabel(/tipo de boneca desejada/i).fill('Boneca personalizada');
    await page.getByLabel(/detalhes da boneca/i).fill('Cabelo loiro, vestido azul');
    await page.getByLabel(/data desejada para receber/i).fill(futureDate());

    // Submit
    await page.getByRole('button', { name: /enviar pelo whatsapp/i }).click();

    // Success message should appear
    await expect(page.getByText(/redirecionando para o whatsapp/i)).toBeVisible();

    // Verify the WhatsApp URL was constructed
    const whatsappUrl = await page.evaluate(
      () => (window as unknown as Record<string, string>).__whatsappUrl
    );
    expect(whatsappUrl).toContain('wa.me');
    expect(whatsappUrl).toContain('Maria');
  });

  // 4A.3
  test('submit empty → errors visible → fix → errors clear', async ({ page }) => {
    // Submit the empty form
    await page.getByRole('button', { name: /enviar pelo whatsapp/i }).click();

    // Errors should appear
    await expect(page.getByText(/nome é obrigatório/i)).toBeVisible();
    await expect(page.getByText(/email é obrigatório/i)).toBeVisible();
    await expect(page.getByText(/telefone é obrigatório/i)).toBeVisible();

    // Fix the name field → its error should clear
    await page.getByLabel(/nome completo/i).fill('Maria');
    await expect(page.getByText(/nome é obrigatório/i)).not.toBeVisible();

    // Other errors should remain
    await expect(page.getByText(/email é obrigatório/i)).toBeVisible();
  });
});
