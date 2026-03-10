/**
 * E2E test helpers — shared utilities for Playwright specs.
 */

/**
 * Returns a date string 6 months from now as DD/MM/YYYY.
 *
 * Uses the same offset across all E2E specs that need future dates.
 * The unit-test factory (orderFactory.ts) uses 90 days — both are
 * safely beyond any minimum-date validation in the form.
 */
export function futureDate(): string {
  const d = new Date();
  d.setMonth(d.getMonth() + 6);
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}
