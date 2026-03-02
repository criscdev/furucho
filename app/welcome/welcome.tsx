/**
 * Welcome — Landing page hero and introduction section.
 *
 * Displays brand identity, brief description of handmade dolls,
 * Instagram link, and call-to-action to the order form.
 *
 * A11y:
 * - Uses semantic <main> landmark
 * - Single <h1> for page title
 * - Descriptive link text for Instagram
 * - CTA button with clear purpose
 *
 * @example
 * <Welcome />
 */

export interface WelcomeProps {
  /** Optional Instagram URL override */
  instagramUrl?: string;
}

export function Welcome({ 
  instagramUrl = "https://instagram.com/robertafurucho1" 
}: WelcomeProps) {
  const scrollToOrderForm = () => {
    const orderForm = document.getElementById('order-form');
    if (orderForm) {
      orderForm.scrollIntoView({ behavior: 'smooth' });
      orderForm.focus();
    }
  };

  return (
    <div className="px-4 py-8 md:py-16">
      {/* Hero Section */}
      <section 
        className="max-w-4xl mx-auto text-center"
        aria-labelledby="hero-heading"
      >
        <h1 
          id="hero-heading"
          className="text-4xl md:text-5xl font-bold mb-4"
          style={{ color: 'var(--color-text-heading)' }}
        >
          Roberta Furucho
        </h1>
        <p 
          className="text-xl md:text-2xl mb-2"
          style={{ color: 'var(--color-text-light)' }}
        >
          Bonecas Artesanais
        </p>
        <p 
          className="text-lg mb-8 max-w-2xl mx-auto"
          style={{ color: 'var(--color-text)' }}
        >
          Cada boneca é única, feita à mão com amor e dedicação. 
          Transformando tecidos e linhas em companheiras especiais 
          para todas as idades.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-12">
          <button
            type="button"
            onClick={scrollToOrderForm}
            className="btn btn-primary text-lg px-8 py-3"
          >
            Fazer Encomenda
          </button>
          <a
            href={instagramUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary text-lg px-8 py-3 inline-flex items-center gap-2"
            aria-label="Seguir Roberta Furucho no Instagram (abre em nova aba)"
          >
            <svg 
              aria-hidden="true" 
              className="w-5 h-5" 
              fill="currentColor" 
              viewBox="0 0 24 24"
            >
              <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
            </svg>
            Instagram
          </a>
        </div>
      </section>

      {/* About Section */}
      <section 
        className="max-w-4xl mx-auto"
        aria-labelledby="about-heading"
      >
        <div className="card">
          <h2 
            id="about-heading"
            className="text-2xl font-bold mb-4"
            style={{ color: 'var(--color-text-heading)' }}
          >
            Sobre as Bonecas
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            <div className="text-center p-4">
              <div 
                className="w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-rose)' }}
                aria-hidden="true"
              >
                <span className="text-2xl">🧵</span>
              </div>
              <h3 className="font-semibold mb-2">Feito à Mão</h3>
              <p style={{ color: 'var(--color-text-light)' }}>
                Cada detalhe é cuidadosamente costurado com técnicas artesanais tradicionais.
              </p>
            </div>
            <div className="text-center p-4">
              <div 
                className="w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-lavender)' }}
                aria-hidden="true"
              >
                <span className="text-2xl">💝</span>
              </div>
              <h3 className="font-semibold mb-2">Personalizado</h3>
              <p style={{ color: 'var(--color-text-light)' }}>
                Bonecas únicas, feitas sob medida para você ou para presentear alguém especial.
              </p>
            </div>
            <div className="text-center p-4">
              <div 
                className="w-16 h-16 mx-auto mb-4 rounded-full flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-mint)' }}
                aria-hidden="true"
              >
                <span className="text-2xl">🌸</span>
              </div>
              <h3 className="font-semibold mb-2">Materiais de Qualidade</h3>
              <p style={{ color: 'var(--color-text-light)' }}>
                Tecidos selecionados e enchimentos antialérgicos para segurança e durabilidade.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
