import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Header } from './Header';

expect.extend(toHaveNoViolations);

describe('Header', () => {
  it('renders the brand link with correct href', () => {
    render(<Header />);
    
    // Get the first link with the brand name (the one with href="/")
    const brandLink = screen.getAllByRole('link', { name: /roberta furucho/i })[0];
    expect(brandLink).toBeInTheDocument();
    expect(brandLink).toHaveAttribute('href', '/');
  });

  it('renders skip link as first focusable element', () => {
    render(<Header />);
    
    const skipLink = screen.getByRole('link', { name: /pular para o conteúdo principal/i });
    expect(skipLink).toBeInTheDocument();
    expect(skipLink).toHaveAttribute('href', '#main');
  });

  it('renders navigation with proper aria-label', () => {
    render(<Header />);
    
    const nav = screen.getByRole('navigation', { name: /navegação principal/i });
    expect(nav).toBeInTheDocument();
  });

  it('renders all navigation links', () => {
    render(<Header />);
    
    expect(screen.getByRole('link', { name: /início/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /encomendas/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /instagram/i })).toBeInTheDocument();
  });

  it('opens Instagram link in new tab with security attributes', () => {
    render(<Header />);
    
    const instagramLink = screen.getByRole('link', { name: /instagram/i });
    expect(instagramLink).toHaveAttribute('target', '_blank');
    expect(instagramLink).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('uses custom Instagram URL when provided', () => {
    const customUrl = 'https://instagram.com/custom';
    render(<Header instagramUrl={customUrl} />);
    
    const instagramLink = screen.getByRole('link', { name: /instagram/i });
    expect(instagramLink).toHaveAttribute('href', customUrl);
  });

  it('has no accessibility violations', async () => {
    const { container } = render(<Header />);
    const results = await axe(container);
    
    expect(results).toHaveNoViolations();
  });
});
