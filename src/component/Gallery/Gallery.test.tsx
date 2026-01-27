import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Gallery, type GalleryItem } from './Gallery';

expect.extend(toHaveNoViolations);

describe('Gallery', () => {
  const mockItems: GalleryItem[] = [
    {
      id: '1',
      src: 'https://example.com/doll1.jpg',
      alt: 'Boneca de pano tradicional com vestido rosa',
      title: 'Boneca Tradicional',
    },
    {
      id: '2',
      src: 'https://example.com/doll2.jpg',
      alt: 'Boneca amigurumi de crochê em tons de lavanda',
      title: 'Amigurumi Lavanda',
    },
    {
      id: '3',
      src: 'https://example.com/doll3.jpg',
      alt: 'Boneca personalizada com características únicas',
    },
  ];

  it('renders gallery heading', () => {
    render(<Gallery items={mockItems} />);
    
    expect(screen.getByRole('heading', { name: /galeria de trabalhos/i })).toBeInTheDocument();
  });

  it('renders custom heading when provided', () => {
    render(<Gallery items={mockItems} heading="Minhas Criações" />);
    
    expect(screen.getByRole('heading', { name: /minhas criações/i })).toBeInTheDocument();
  });

  it('renders all gallery items as list items', () => {
    render(<Gallery items={mockItems} />);
    
    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(3);
  });

  it('renders images with proper alt text for accessibility', () => {
    render(<Gallery items={mockItems} />);
    
    expect(screen.getByAltText('Boneca de pano tradicional com vestido rosa')).toBeInTheDocument();
    expect(screen.getByAltText('Boneca amigurumi de crochê em tons de lavanda')).toBeInTheDocument();
    expect(screen.getByAltText('Boneca personalizada com características únicas')).toBeInTheDocument();
  });

  it('renders item titles when provided', () => {
    render(<Gallery items={mockItems} />);
    
    expect(screen.getByRole('heading', { name: 'Boneca Tradicional', level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Amigurumi Lavanda', level: 3 })).toBeInTheDocument();
  });

  it('uses lazy loading for images', () => {
    render(<Gallery items={mockItems} />);
    
    const images = screen.getAllByRole('img');
    images.forEach(img => {
      expect(img).toHaveAttribute('loading', 'lazy');
    });
  });

  it('renders Instagram link with proper security attributes', () => {
    render(<Gallery items={mockItems} />);
    
    const instagramLink = screen.getByRole('link', { name: /instagram/i });
    expect(instagramLink).toHaveAttribute('target', '_blank');
    expect(instagramLink).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('uses semantic section with aria-labelledby', () => {
    const { container } = render(<Gallery items={mockItems} />);
    
    const section = container.querySelector('section');
    expect(section).toHaveAttribute('aria-labelledby', 'gallery-heading');
  });

  it('renders list with aria-label for screen readers', () => {
    render(<Gallery items={mockItems} />);
    
    const list = screen.getByRole('list', { name: /galeria de bonecas artesanais/i });
    expect(list).toBeInTheDocument();
  });

  it('returns null when items array is empty', () => {
    const { container } = render(<Gallery items={[]} />);
    
    expect(container.firstChild).toBeNull();
  });

  it('renders default items when no items provided', () => {
    render(<Gallery />);
    
    // Default items have 6 dolls
    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(6);
  });

  it('has no accessibility violations', async () => {
    const { container } = render(<Gallery items={mockItems} />);
    const results = await axe(container);
    
    expect(results).toHaveNoViolations();
  });

  it('has no accessibility violations with default items', async () => {
    const { container } = render(<Gallery />);
    const results = await axe(container);
    
    expect(results).toHaveNoViolations();
  });
});
