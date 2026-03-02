/**
 * Gallery — Responsive image grid showcasing handmade dolls.
 *
 * Displays a grid of doll photos with lazy loading, alt text,
 * and pastel-themed card styling.
 *
 * A11y:
 * - All images have descriptive alt text
 * - Grid uses semantic list structure
 * - Focus visible on interactive elements
 * - Lazy loading for performance
 *
 * @example
 * <Gallery items={dollPhotos} />
 */

import { useId } from 'react';

export interface GalleryItem {
  /** Unique identifier */
  id: string;
  /** Image source URL */
  src: string;
  /** Descriptive alt text for accessibility */
  alt: string;
  /** Optional title/caption */
  title?: string;
}

export interface GalleryProps {
  /** Array of gallery items to display (uses demo items if not provided) */
  items?: GalleryItem[];
  /** Optional heading for the gallery section */
  heading?: string;
}

// Placeholder images for demo - replace with actual doll photos
const defaultItems: GalleryItem[] = [
  {
    id: "1",
    src: "https://placehold.co/400x400/F4B8C5/4A4A4A?text=Boneca+1",
    alt: "Boneca de pano tradicional com vestido rosa e cabelos castanhos",
    title: "Boneca Tradicional",
  },
  {
    id: "2",
    src: "https://placehold.co/400x400/D8D0E8/4A4A4A?text=Boneca+2",
    alt: "Boneca amigurumi de crochê em tons de lavanda",
    title: "Amigurumi Lavanda",
  },
  {
    id: "3",
    src: "https://placehold.co/400x400/B8E0C8/4A4A4A?text=Boneca+3",
    alt: "Boneca de pano com roupa verde menta e detalhes florais",
    title: "Boneca Floral",
  },
  {
    id: "4",
    src: "https://placehold.co/400x400/FFE4D6/4A4A4A?text=Boneca+4",
    alt: "Boneca artesanal com vestido de festa em tons pastéis",
    title: "Boneca Festa",
  },
  {
    id: "5",
    src: "https://placehold.co/400x400/F4B8C5/4A4A4A?text=Boneca+5",
    alt: "Conjunto de mini bonecas decorativas em diferentes cores",
    title: "Mini Bonecas",
  },
  {
    id: "6",
    src: "https://placehold.co/400x400/D8D0E8/4A4A4A?text=Boneca+6",
    alt: "Boneca personalizada com características únicas",
    title: "Boneca Personalizada",
  },
];

export function Gallery({ 
  items = defaultItems, 
  heading = "Galeria de Trabalhos" 
}: GalleryProps) {
  const headingId = useId();

  if (items.length === 0) {
    return null;
  }

  return (
    <section 
      className="max-w-6xl mx-auto px-4 py-12"
      aria-labelledby={headingId}
    >
      <h2 
        id={headingId}
        className="text-2xl font-bold mb-8 text-center"
        style={{ color: 'var(--color-text-heading)' }}
      >
        {heading}
      </h2>

      <ul 
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6"
        role="list"
        aria-label="Galeria de bonecas artesanais"
      >
        {items.map((item) => (
          <li key={item.id} className="list-none">
            <article 
              className="card overflow-hidden p-0 transition-transform hover:scale-[1.02]"
            >
              <div className="aspect-square overflow-hidden">
                <img
                  src={item.src}
                  alt={item.alt}
                  loading="lazy"
                  decoding="async"
                  className="w-full h-full object-cover transition-transform hover:scale-105"
                  style={{ backgroundColor: 'var(--color-lavender)' }}
                />
              </div>
              {item.title && (
                <div className="p-4">
                  <h3 
                    className="font-semibold text-center"
                    style={{ color: 'var(--color-text-heading)' }}
                  >
                    {item.title}
                  </h3>
                </div>
              )}
            </article>
          </li>
        ))}
      </ul>

      <p 
        className="text-center mt-8"
        style={{ color: 'var(--color-text-light)' }}
      >
        Siga no{" "}
        <a 
          href="https://instagram.com/robertafurucho1"
          target="_blank"
          rel="noopener noreferrer"
          className="font-medium underline hover:opacity-80"
          style={{ color: 'var(--color-focus)' }}
        >
          Instagram
        </a>
        {" "}para ver mais trabalhos!
      </p>
    </section>
  );
}
