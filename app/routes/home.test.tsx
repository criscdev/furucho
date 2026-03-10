import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import Home, { meta } from './home';

expect.extend(toHaveNoViolations);

describe('Home route', () => {
  // ── meta() ────────────────────────────────────────────────────

  describe('meta', () => {
    const metaTags = meta({} as Parameters<typeof meta>[0]);

    it('returns page title with brand name', () => {
      const titleTag = metaTags.find(
        (tag) => 'title' in tag
      ) as { title: string } | undefined;

      expect(titleTag).toBeDefined();
      expect(titleTag!.title).toContain('Roberta Furucho');
    });

    it('returns meta description', () => {
      const descTag = metaTags.find(
        (tag) => 'name' in tag && tag.name === 'description'
      ) as { name: string; content: string } | undefined;

      expect(descTag).toBeDefined();
      expect(descTag!.content).toMatch(/bonecas artesanais de biscuit/i);
    });

    it('returns Open Graph tags with correct values', () => {
      const ogTitle = metaTags.find(
        (tag) => 'property' in tag && tag.property === 'og:title'
      ) as { property: string; content: string } | undefined;
      const ogDesc = metaTags.find(
        (tag) => 'property' in tag && tag.property === 'og:description'
      ) as { property: string; content: string } | undefined;
      const ogType = metaTags.find(
        (tag) => 'property' in tag && tag.property === 'og:type'
      ) as { property: string; content: string } | undefined;

      expect(ogTitle).toBeDefined();
      expect(ogTitle!.content).toContain('Roberta Furucho');
      expect(ogDesc).toBeDefined();
      expect(ogDesc!.content).toMatch(/bonecas artesanais/i);
      expect(ogType).toBeDefined();
      expect(ogType!.content).toBe('website');
    });

    it('returns theme-color meta tag', () => {
      const themeColor = metaTags.find(
        (tag) => 'name' in tag && tag.name === 'theme-color'
      ) as { name: string; content: string } | undefined;

      expect(themeColor).toBeDefined();
      expect(themeColor!.content).toBe('#F4B8C5');
    });
  });

  // ── Composition ───────────────────────────────────────────────

  describe('Home component', () => {
    it('renders Header', () => {
      render(<Home />);

      expect(screen.getByRole('navigation')).toBeInTheDocument();
    });

    it('renders Welcome section with heading', () => {
      render(<Home />);

      expect(screen.getByRole('heading', { name: /roberta furucho/i, level: 1 })).toBeInTheDocument();
    });

    it('renders Gallery section', () => {
      render(<Home />);

      expect(screen.getByRole('heading', { name: /galeria de trabalhos/i })).toBeInTheDocument();
    });

    it('renders OrderForm section', () => {
      render(<Home />);

      expect(screen.getByRole('heading', { name: /faça sua encomenda/i })).toBeInTheDocument();
    });

    it('wraps content in main landmark with skip-link target', () => {
      render(<Home />);

      const main = screen.getByRole('main');
      expect(main).toHaveAttribute('id', 'main');
      expect(main).toHaveAttribute('tabIndex', '-1');
    });

    it('has no accessibility violations', async () => {
      const { container } = render(<Home />);
      const results = await axe(container);

      expect(results).toHaveNoViolations();
    });
  });
});
