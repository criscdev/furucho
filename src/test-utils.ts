import type { ReactElement } from 'react';
import { render, type RenderOptions } from '@testing-library/react';

/**
 * Render helper that wraps components with any needed providers.
 *
 * Add providers here if your app needs them (Router, Redux, Theme, etc.)
 *
 * @param ui - The React element to render
 * @param options - Optional RTL render options
 * @returns RTL render result
 */
export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>,
) {
  return render(ui, { ...options });
}

export * from '@testing-library/react';
