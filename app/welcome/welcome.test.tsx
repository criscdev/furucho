import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Welcome } from './welcome';

expect.extend(toHaveNoViolations);

describe('Welcome', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });
  // ── Hero Section ──────────────────────────────────────────────

  it('renders the page heading with brand name', () => {
    render(<Welcome />);

    const heading = screen.getByRole('heading', { name: /roberta furucho/i, level: 1 });
    expect(heading).toBeInTheDocument();
  });

  it('renders the subtitle "Bonecas Artesanais"', () => {
    render(<Welcome />);

    expect(screen.getByText(/bonecas artesanais/i)).toBeInTheDocument();
  });

  it('renders a descriptive paragraph about the dolls', () => {
    render(<Welcome />);

    expect(screen.getByText(/cada boneca é única/i)).toBeInTheDocument();
  });

  // ── CTA Button ────────────────────────────────────────────────

  it('renders "Fazer Encomenda" CTA button', () => {
    render(<Welcome />);

    const cta = screen.getByRole('button', { name: /fazer encomenda/i });
    expect(cta).toBeInTheDocument();
    expect(cta).toHaveAttribute('type', 'button');
  });

  it('scrolls to #order-form and focuses it on CTA click', async () => {
    const scrollIntoViewMock = vi.fn();
    const focusMock = vi.fn();
    const fakeElement = {
      scrollIntoView: scrollIntoViewMock,
      focus: focusMock,
    } as unknown as HTMLElement;

    vi.spyOn(document, 'getElementById').mockReturnValue(fakeElement);

    const user = userEvent.setup();
    render(<Welcome />);
    await user.click(screen.getByRole('button', { name: /fazer encomenda/i }));

    expect(document.getElementById).toHaveBeenCalledWith('order-form');
    expect(scrollIntoViewMock).toHaveBeenCalledWith({ behavior: 'smooth' });
    expect(focusMock).toHaveBeenCalled();
  });

  it('does not throw when #order-form element is absent', async () => {
    vi.spyOn(document, 'getElementById').mockReturnValue(null);

    const user = userEvent.setup();
    render(<Welcome />);

    await expect(
      user.click(screen.getByRole('button', { name: /fazer encomenda/i }))
    ).resolves.not.toThrow();
  });

  // ── Instagram Link ────────────────────────────────────────────

  it('renders Instagram link with default URL', () => {
    render(<Welcome />);

    const link = screen.getByRole('link', { name: /instagram/i });
    expect(link).toHaveAttribute('href', 'https://instagram.com/robertafurucho1');
  });

  it('opens Instagram link in new tab with security attributes', () => {
    render(<Welcome />);

    const link = screen.getByRole('link', { name: /instagram/i });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('uses custom Instagram URL when provided', () => {
    const customUrl = 'https://instagram.com/custom';
    render(<Welcome instagramUrl={customUrl} />);

    const link = screen.getByRole('link', { name: /instagram/i });
    expect(link).toHaveAttribute('href', customUrl);
  });

  it('Instagram link has descriptive aria-label', () => {
    render(<Welcome />);

    const link = screen.getByRole('link', { name: /instagram/i });
    expect(link).toHaveAttribute(
      'aria-label',
      expect.stringMatching(/instagram.*nova aba/i)
    );
  });

  // ── About Section ─────────────────────────────────────────────

  it('renders "Sobre as Bonecas" heading', () => {
    render(<Welcome />);

    expect(
      screen.getByRole('heading', { name: /sobre as bonecas/i, level: 2 })
    ).toBeInTheDocument();
  });

  it('renders three about cards with correct headings', () => {
    render(<Welcome />);

    expect(screen.getByRole('heading', { name: /feito à mão/i, level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /personalizado/i, level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /materiais de qualidade/i, level: 3 })).toBeInTheDocument();
  });

  it('each about card has a description paragraph', () => {
    render(<Welcome />);

    expect(screen.getByText(/técnicas artesanais tradicionais/i)).toBeInTheDocument();
    expect(screen.getByText(/feitas sob medida/i)).toBeInTheDocument();
    expect(screen.getByText(/enchimentos antialérgicos/i)).toBeInTheDocument();
  });

  // ── Semantic Structure ────────────────────────────────────────

  it('renders as a non-landmark div (main is in Home route)', () => {
    const { container } = render(<Welcome />);

    // Welcome no longer owns <main>; Home wraps all sections
    expect(container.querySelector('main')).not.toBeInTheDocument();
  });

  it('hero section uses aria-labelledby pointing to heading', () => {
    const { container } = render(<Welcome />);

    const heroSection = container.querySelector('section[aria-labelledby="hero-heading"]');
    expect(heroSection).toBeInTheDocument();
    expect(container.querySelector('#hero-heading')).toBeInTheDocument();
  });

  it('about section uses aria-labelledby pointing to heading', () => {
    const { container } = render(<Welcome />);

    const aboutSection = container.querySelector('section[aria-labelledby="about-heading"]');
    expect(aboutSection).toBeInTheDocument();
    expect(container.querySelector('#about-heading')).toBeInTheDocument();
  });

  // ── Accessibility ─────────────────────────────────────────────

  it('has no accessibility violations', async () => {
    const { container } = render(<Welcome />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });
});
