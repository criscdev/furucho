import { describe, it, expect, vi, beforeEach, afterEach, type MockInstance } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { OrderForm } from '../../component/OrderForm/OrderForm';
import { Header } from '../../component/Header/Header';
import { Gallery } from '../../component/Gallery/Gallery';
import { orderFactory } from '../factories/orderFactory';

expect.extend(toHaveNoViolations);

/**
 * Integration tests for the full order flow.
 * 
 * Simulates a user journey from landing on the page,
 * browsing the gallery, and completing an order.
 */
describe('Order Flow Integration', () => {
  let mockOpen: MockInstance;

  beforeEach(() => {
    mockOpen = vi.spyOn(window, 'open').mockImplementation(() => null);
  });

  afterEach(() => {
    mockOpen.mockRestore();
  });

  /**
   * Full page simulation with Header, Gallery, and OrderForm
   */
  function FullPage() {
    return (
      <>
        <Header />
        <main id="main">
          <Gallery />
          <OrderForm />
        </main>
      </>
    );
  }

  it('renders all main page sections', () => {
    render(<FullPage />);

    // Header with navigation
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: /navegação principal/i })).toBeInTheDocument();

    // Gallery section
    expect(screen.getByRole('heading', { name: /galeria de trabalhos/i })).toBeInTheDocument();

    // Order form section
    expect(screen.getByRole('heading', { name: /faça sua encomenda/i })).toBeInTheDocument();
  });

  it('allows keyboard navigation through the header', async () => {
    const user = userEvent.setup();
    render(<Header />);

    // Skip link should be first focusable element
    await user.tab();
    expect(screen.getByRole('link', { name: /pular para o conteúdo principal/i })).toHaveFocus();

    // Brand link
    await user.tab();
    const brandLinks = screen.getAllByRole('link');
    expect(brandLinks[1]).toHaveFocus(); // Brand is second link after skip link

    // Tab through nav links - Início
    await user.tab();
    expect(screen.getByRole('link', { name: /início/i })).toHaveFocus();

    // Encomendas
    await user.tab();
    expect(screen.getByRole('link', { name: /encomendas/i })).toHaveFocus();

    // Instagram (in header nav)
    await user.tab();
    const instagramLink = screen.getByRole('link', { name: /instagram de roberta furucho/i });
    expect(instagramLink).toHaveFocus();
  });

  it('completes full order submission flow', async () => {
    const user = userEvent.setup();
    const onSuccess = vi.fn();
    
    render(
      <>
        <Header />
        <main id="main">
          <OrderForm onSubmitSuccess={onSuccess} />
        </main>
      </>
    );

    const orderData = orderFactory();

    // Fill out the form
    await user.type(screen.getByLabelText(/nome completo/i), orderData.name);
    await user.type(screen.getByLabelText(/^email/i), orderData.email);
    await user.type(screen.getByLabelText(/telefone/i), orderData.phone);
    await user.type(screen.getByLabelText(/endereço completo/i), orderData.address);
    await user.type(screen.getByLabelText(/cep/i), orderData.postalCode);
    await user.type(screen.getByLabelText(/tipo de boneca desejada/i), orderData.orderScope);
    await user.type(screen.getByLabelText(/detalhes da boneca/i), orderData.orderScopeDetail);
    await user.type(screen.getByLabelText(/data desejada para receber/i), orderData.receiveDate);

    // Submit the form
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    // Verify WhatsApp was opened
    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalledWith(
        expect.stringContaining('wa.me'),
        '_blank',
        'noopener,noreferrer'
      );
    });

    // Verify success callback was called
    expect(onSuccess).toHaveBeenCalledWith(orderData);

    // Verify success message is shown
    expect(await screen.findByText(/redirecionando para o whatsapp/i)).toBeInTheDocument();
  });

  it('shows validation errors and allows correction', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    // Submit empty form
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    // Should show errors
    expect(await screen.findByText(/nome é obrigatório/i)).toBeInTheDocument();
    expect(screen.getByText(/email é obrigatório/i)).toBeInTheDocument();

    // Fill required field
    await user.type(screen.getByLabelText(/nome completo/i), 'Maria Silva');

    // Error should clear for that field
    expect(screen.queryByText(/nome é obrigatório/i)).not.toBeInTheDocument();
  });

  it('validates email format', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    // Enter invalid email
    await user.type(screen.getByLabelText(/^email/i), 'not-an-email');
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    expect(await screen.findByText(/email inválido/i)).toBeInTheDocument();

    // Clear and enter valid email
    await user.clear(screen.getByLabelText(/^email/i));
    await user.type(screen.getByLabelText(/^email/i), 'maria@exemplo.com');

    expect(screen.queryByText(/email inválido/i)).not.toBeInTheDocument();
  });

  it('validates CEP format', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    // Enter invalid CEP
    await user.type(screen.getByLabelText(/cep/i), '123');
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    expect(await screen.findByText(/cep inválido/i)).toBeInTheDocument();

    // Clear and enter valid CEP
    await user.clear(screen.getByLabelText(/cep/i));
    await user.type(screen.getByLabelText(/cep/i), '01234-567');

    expect(screen.queryByText(/cep inválido/i)).not.toBeInTheDocument();
  });

  it('has no accessibility violations in full page layout', async () => {
    const { container } = render(<FullPage />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('has no accessibility violations when form has errors', async () => {
    const user = userEvent.setup();
    const { container } = render(<OrderForm />);

    // Trigger validation errors
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    await waitFor(async () => {
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  it('formats WhatsApp message correctly', async () => {
    const user = userEvent.setup();
    render(<OrderForm whatsappNumber="5511888887777" />);

    const orderData = orderFactory({ name: 'Test User' });

    await user.type(screen.getByLabelText(/nome completo/i), orderData.name);
    await user.type(screen.getByLabelText(/^email/i), orderData.email);
    await user.type(screen.getByLabelText(/telefone/i), orderData.phone);
    await user.type(screen.getByLabelText(/endereço completo/i), orderData.address);
    await user.type(screen.getByLabelText(/cep/i), orderData.postalCode);
    await user.type(screen.getByLabelText(/tipo de boneca desejada/i), orderData.orderScope);
    await user.type(screen.getByLabelText(/detalhes da boneca/i), orderData.orderScopeDetail);
    await user.type(screen.getByLabelText(/data desejada para receber/i), orderData.receiveDate);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    await waitFor(() => {
      const whatsappUrl = mockOpen.mock.calls[0][0] as string;
      expect(whatsappUrl).toContain('wa.me/5511888887777');
      expect(whatsappUrl).toContain(encodeURIComponent('Test User'));
      expect(whatsappUrl).toContain(encodeURIComponent('Nova Encomenda'));
    });
  });
});
