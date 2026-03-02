import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToStaticMarkup } from 'react-dom/server';

// Mock React Router framework components that require HydratedRouter context.
vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  const noop = () => null;
  return { ...actual, Meta: noop, Links: noop, Scripts: noop, ScrollRestoration: noop, Outlet: noop };
});

import { Layout, ErrorBoundary } from './root';

describe('Layout', () => {
  it('sets lang="pt-BR" on the html element', () => {
    const html = renderToStaticMarkup(<Layout>content</Layout>);
    expect(html).toContain('lang="pt-BR"');
  });
});

describe('ErrorBoundary', () => {
  it('renders 404 message for a route error response', () => {
    const routeError = { status: 404, statusText: 'Not Found', data: null, internal: true };
    render(<ErrorBoundary error={routeError as never} params={{}} />);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('404');
    expect(screen.getByText(/página solicitada não foi encontrada/i)).toBeInTheDocument();
  });

  it('renders generic message for unknown errors', () => {
    render(<ErrorBoundary error={new Error('kaboom')} params={{}} />);
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Ops!');
  });
});
