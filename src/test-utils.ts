// @ts-nocheck
import React from 'react';
import { render } from '@testing-library/react';

// Add providers here if your app needs them (Router, Redux, Theme, etc.)
export function renderWithProviders(ui, options) {
  return render(ui, { ...options });
}

export * from '@testing-library/react';
